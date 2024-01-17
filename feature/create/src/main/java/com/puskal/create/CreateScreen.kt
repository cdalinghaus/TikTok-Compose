package com.puskal.create

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.puskal.composable.CustomButton
import com.puskal.composable.TopBar
import com.puskal.core.DestinationRoute
import com.puskal.core.DestinationRoute.AUTHENTICATION_ROUTE
import com.puskal.theme.R
import com.puskal.theme.SubTextColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import com.google.gson.Gson
import com.puskal.data.model.VideoModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleCreateScreen(navController: NavController) {
    Scaffold(topBar = {
        TopBar(
            navIcon = null,
            title = stringResource(id = R.string.inbox)
        )
    }) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            UnAuthorizedInboxScreen(navController)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnAuthorizedInboxScreen(navController: NavController) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp, alignment = Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_msg),
            contentDescription = null,
            modifier = Modifier.size(68.dp)
        )
        Text(
            text = "Enter your prompt below",
            color = SubTextColor
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Prompt") }
        )
        CustomButton(
            buttonText = "Create a new video!",
            modifier = Modifier.fillMaxWidth(0.66f)
        ) {
            sendPromptToAPI(text, navController, context)
        }
    }
}

class AuthInterceptor(context: Context) : Interceptor {
    private val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

    private fun getToken(): String? {
        return sharedPreferences.getString("auth_token", null)
    }

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()
        val token = getToken()
        Log.d("AUTH TOKEN!!!", token.toString())

        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}

fun sendPromptToAPI(prompt: String, navController: NavController, context: Context) {
    val url = "https://api.reemix.co/api/v2/prompt"
    val data = "{\"prompt\":\"$prompt\"}"
    Log.d("SENDPROMPT", "Sending prompt to api")

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val requestBody = data.toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(context)) // Assuming AuthInterceptor is defined
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d("SENDPROMPT", "Response: $responseBody")
                // Process the response as needed
                val gson = Gson()
                val videoModel = gson.fromJson(responseBody, VideoModel::class.java)
                val videoId = videoModel.videoId
                val author_name = videoModel.authorDetails.uniqueUserName

                Log.d("SENDPROMPT", "Videomodel ${author_name}")

                // Switch to the Main Thread for UI updates
                withContext(Dispatchers.Main) {
                    Log.d("SENDPROMPT", "Videomodel ${author_name}")
                    navController.navigate("${DestinationRoute.CREATOR_PROFILE_ROUTE}/$author_name")
                }


            } else {
                Log.d("SENDPROMPT", "API Request Failed with response code: ${response.code}")
                val errorBody = response.body?.string()
                Log.d("SENDPROMPT", "Error Body: $errorBody")
            }
        } catch (e: Exception) {
            Log.e("SENDPROMPT", "Exception: ${e.message}")
        }
    }
}
