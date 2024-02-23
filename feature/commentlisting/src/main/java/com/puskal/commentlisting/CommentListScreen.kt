package com.puskal.commentlisting

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.gson.Gson
import com.puskal.composable.SharedPreferencesManager
import com.puskal.composable.StatisticsApi



import com.puskal.composable.downloadAndSaveVideo
import com.puskal.core.extension.Space
import com.puskal.data.model.CommentList
import com.puskal.data.model.UserModel
import com.puskal.data.model.VideoModel
import com.puskal.theme.DarkBlue
import com.puskal.theme.GrayMainColor
import com.puskal.theme.R
import com.puskal.theme.SubTextColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import okhttp3.Response as okhttpResponse


/**
 * Created by Puskal Khadka on 3/22/2023.
 */

object SharedPreferencesManager {
    private const val PREFS_NAME = "MyAppPrefs"
    private const val TOKEN_KEY = "auth_token"
    private const val USER_KEY = "auth_user"

    fun saveToken(context: Context, token: String) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putString(TOKEN_KEY, token).apply()
    }

    fun getToken(context: Context): String? {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getString(TOKEN_KEY, null)
    }

    fun saveUser(context: Context, user: UserModel) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userJson = Gson().toJson(user)
        sharedPrefs.edit().putString(USER_KEY, userJson).apply()
    }

    fun getUser(context: Context): UserModel? {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userJson = sharedPrefs.getString(USER_KEY, null)
        return userJson?.let { Gson().fromJson(it, UserModel::class.java) }
    }
}


interface CommentInterface {
    @POST("videos/{video_id}/comments")
    suspend fun createComment(
        @Path("video_id") videoId: String,
        @Body commentRequest: CommentRequest
    ): Response<CommentResponse>
}

class AuthInterceptor(context: Context) : Interceptor {
    private val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

    private fun getToken(): String? {
        return sharedPreferences.getString("auth_token", null)
    }

    override fun intercept(chain: Interceptor.Chain): okhttpResponse {
        val originalRequest = chain.request()
        val token = getToken()

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



data class CommentRequest(val comment: String)
data class CommentResponse(val placeholder: String)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CommentListScreen(
    videoId: String = "pass_test",
    onClickCancel: () -> Unit
) {

    val viewModel: CommentListViewModel = hiltViewModel()
    val viewState by viewModel.viewState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxHeight(0.75f)
    ) {
        12.dp.Space()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "${viewState?.comments?.totalComment ?: ""} ${stringResource(id = R.string.comments)}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.Center)
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_cancel),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable {
                        onClickCancel()
                    }
            )
        }

        6.dp.Space()
        LazyColumn(contentPadding = PaddingValues(top = 4.dp), modifier = Modifier.weight(1f)) {
            viewState?.comments?.comments?.let {
                items(it) {
                    CommentItem(it)
                }
            }
        }




        viewState?.let { CommentUserField(it) }
        }
    }

@RequiresApi(Build.VERSION_CODES.O)
fun formatTimeAgo(timestamp: String): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val past = LocalDateTime.parse(timestamp, formatter)
    val now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)

    val seconds = ChronoUnit.SECONDS.between(past, now)
    val minutes = ChronoUnit.MINUTES.between(past, now)
    val hours = ChronoUnit.HOURS.between(past, now)
    val days = ChronoUnit.DAYS.between(past, now)

    return when {
        seconds < 60 -> "$seconds seconds ago"
        minutes < 60 -> "$minutes minutes ago"
        hours < 24 -> "$hours hours ago"
        else -> "$days days ago"
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CommentItem(item: CommentList.Comment) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        val (profileImg, name, comment, createdOn, reply, like, dislike) = createRefs()

        AsyncImage(model = ImageRequest.Builder(LocalContext.current)
            .data(item.commentBy.profilePic)
            .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(GrayMainColor)
                .constrainAs(profileImg) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                })


        Text(text = item.commentBy.fullName,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.constrainAs(name) {
                start.linkTo(profileImg.end, margin = 12.dp)
                top.linkTo(profileImg.top)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            })
        Text(text = item.comment ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = DarkBlue,
            modifier = Modifier.constrainAs(comment) {
                start.linkTo(name.start)
                top.linkTo(name.bottom, margin = 5.dp)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            })
        Text(text = formatTimeAgo(item.createdAt), modifier = Modifier.constrainAs(createdOn) {
            start.linkTo(name.start)
            top.linkTo(comment.bottom, margin = 5.dp)
        })

        Text(text = stringResource(id = R.string.reply),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.constrainAs(reply) {
                start.linkTo(createdOn.end, margin = 16.dp)
                top.linkTo(createdOn.top)
                end.linkTo(like.end, margin = 4.dp)
                width = Dimension.fillToConstraints
            })

        Row(
            modifier = Modifier.constrainAs(like) {
                bottom.linkTo(reply.bottom)
                end.linkTo(dislike.start, margin = 24.dp)
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_like_outline),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            item.totalLike.takeIf { it != 0L }?.let {
                Text(text = it.toString(), fontSize = 13.sp, color = SubTextColor)
            }

        }

        Row(
            modifier = Modifier.constrainAs(dislike) {
                bottom.linkTo(reply.bottom)
                end.linkTo(parent.end)
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_dislike_outline),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            // Text(text = "") //dislike not display
        }
    }
    24.dp.Space()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentUserField(viewState: ViewState) {
    val context = LocalContext.current
    val isUserLoggedIn = SharedPreferencesManager.getToken(context) != null

    Column(
        modifier = Modifier
            .shadow(elevation = (0.4).dp)
            .padding(horizontal = 16.dp)
    ) {
        HighlightedEmoji.values().toList().let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                it.forEach { emoji ->
                    Text(text = emoji.unicode, fontSize = 25.sp)
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = "", contentDescription = null, modifier = Modifier
                    .size(38.dp)
                    .background(
                        shape =
                        CircleShape, color = GrayMainColor
                    )
            )
            var textValue by remember { mutableStateOf("") }
            OutlinedTextField(value = textValue,
                onValueChange = { textValue = it },
                shape = RoundedCornerShape(36.dp),
                placeholder = {
                    Text(text = stringResource(R.string.add_comment))
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = if (!isUserLoggedIn) Color.Gray else GrayMainColor,
                    unfocusedBorderColor = Color.Transparent,
                ),
                modifier = Modifier.height(46.dp),
                enabled = isUserLoggedIn,
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(end = 10.dp, start = 2.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_mention),
                            contentDescription = null
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_emoji),
                            contentDescription = null
                        )
                    }

                }

            )
            val viewModel: CommentListViewModel = hiltViewModel()
            Button(
                onClick = {
                    val commentText = textValue // Capture the text from the input field

                    if (commentText.isNotEmpty()) {
                        // Initialize Retrofit
                        val okHttpClient = OkHttpClient.Builder()
                            .addInterceptor(AuthInterceptor(context))
                            .build()

                        val retrofit = Retrofit.Builder()
                            .baseUrl("https://api.reemix.co/api/v2/")
                            .client(okHttpClient)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()

                        val commentApi = retrofit.create(CommentInterface::class.java)


                        // Create a coroutine to make the network request
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val response = commentApi.createComment(viewState.videoId, CommentRequest(commentText))

                                if (response.isSuccessful) {
                                    // Handle successful response
                                    val commentResponse = response.body()
                                    // Do something with the response, e.g., update UI
                                    Log.d("ENDPOINT", "SUCCESS!")
                                    textValue = ""
                                    withContext(Dispatchers.Main) { // Switch to Main thread for UI operations
                                        textValue = ""
                                        viewModel.refreshComments() // Refresh comments using ViewModel
                                    }
                                } else {
                                    // Handle error
                                    // Log or show error message
                                    Log.d("ENDPOINT", "NO SUCCESS! Status Code: ${response.code()}")
                                    val errorBody = response.errorBody()?.string()
                                    Log.d("ENDPOINT", "Error Body: $errorBody")

                                }
                            } catch (e: Exception) {
                                // Handle exception
                                // Log or show error message
                            }
                        }
                    }
                },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        shape =
                        CircleShape, color = GrayMainColor
                    )
            ) {
                Icon(
                    painter = painterResource(id = com.puskal.commentlisting.R.drawable.ic_send), // Replace with your send icon resource
                    contentDescription = "Send",
                    tint = Color.White,
                )
            }




        }
    }
}