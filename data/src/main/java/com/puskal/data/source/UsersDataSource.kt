package com.puskal.data.source

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


object UsersDataSource {
    private val client = OkHttpClient()
    private val gson = Gson()


    fun fetchUserFromApi(userId: String): Flow<UserModel?> = flow {
        //Log.d("FETCHING", userId)
        val url = "https://api.reemix.co/api/v2/creators/$userId"
        val request = Request.Builder().url(url).build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val responseBody = response.body?.string() ?: ""
            val jsonObject = JSONObject(responseBody)
            val userData = jsonObject.getJSONObject("data")
            val user = gson.fromJson(userData.toString(), UserModel::class.java)
            emit(user)
        } else {
            emit(null) // Handle the error appropriately
        }
    }.flowOn(Dispatchers.IO) // Switch the flow's context to IO dispatcher

    fun fetchVideosOfParticularUser(userId: String): Flow<List<VideoModel>> = flow {
        val url = "https://api.reemix.co/api/v2/creators/$userId/videos" // You can modify the URL to include userId if needed
        val request = Request.Builder().url(url).build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val responseBody = response.body?.string() ?: ""
            val jsonObject = JSONObject(responseBody)
            val videoDataArray = jsonObject.getJSONArray("data")
            val videos = (0 until videoDataArray.length()).map { index ->
                gson.fromJson(videoDataArray.getJSONObject(index).toString(), VideoModel::class.java)
            }
            Log.d("VIDEOS", videos.toString())
            emit(videos)
        } else {
            emit(emptyList<VideoModel>()) // Handle the error appropriately
        }
    }.flowOn(Dispatchers.IO) // Switch the flow's context to IO dispatcher


    fun fetchSpecificUser(userId: String): Flow<UserModel?>{
        return fetchUserFromApi(userId)
    }
}


