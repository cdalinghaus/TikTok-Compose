package com.puskal.data.source

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.puskal.data.model.UserModel
import com.puskal.data.model.VideoModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.flow.flowOn
import okhttp3.Interceptor

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


class UsersDataSource(context: Context) {
    private val authInterceptor = AuthInterceptor(context)
    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()
    private val gson = Gson()


    fun fetchUserFromApi(userId: String): Flow<UserModel?> = flow {
        //Log.d("FETCHING", userId)
        val url = "https://api.reemix.co/api/v2/creators/$userId"
        val request = Request.Builder().url(url).build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val responseBody = response.body?.string() ?: ""
            Log.d("GSON", "Raw JSON: $responseBody")

            val jsonObject = JSONObject(responseBody)
            val userData = jsonObject.getJSONObject("data")
            val user = gson.fromJson(userData.toString(), UserModel::class.java)
            user.fix()
            Log.d("GSON", "Parsed User Object: $user")
            emit(user)
        } else {
            emit(null) // Handle the error appropriately
        }
    }.flowOn(Dispatchers.IO) // Switch the flow's context to IO dispatcher

    fun fetchVideosOfParticularUser(userId: String): Flow<List<VideoModel>> = flow {
        val url =
            "https://api.reemix.co/api/v2/creators/$userId/videos" // You can modify the URL to include userId if needed
        val request = Request.Builder().url(url).build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val responseBody = response.body?.string() ?: ""
            val jsonObject = JSONObject(responseBody)
            val videoDataArray = jsonObject.getJSONArray("data")
            val videos = (0 until videoDataArray.length()).map { index ->
                gson.fromJson(
                    videoDataArray.getJSONObject(index).toString(),
                    VideoModel::class.java
                )
            }
            Log.d("VIDEOS", videos.toString())
            emit(videos)
        } else {
            emit(emptyList<VideoModel>()) // Handle the error appropriately
        }
    }.flowOn(Dispatchers.IO) // Switch the flow's context to IO dispatcher


    fun fetchSpecificUser(userId: String): Flow<UserModel?> {
        return fetchUserFromApi(userId)
    }
}


