package com.puskal.data.source

import com.puskal.data.model.UserModel
import com.puskal.data.model.VideoModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Created by Puskal Khadka on 3/18/2023.
 */
object VideoDataSource {

    // Dummy data for demonstration
    private val dummyVideos = listOf(
        VideoModel(authorDetails = UserModel(likes=123), description = "TEasd", videoId="a", thumbnailLink = "",playable=true,  videoLink = "", videoStats= VideoModel.VideoStats(comment=1, like=3, share = 3, favourite = 123)),
        VideoModel(authorDetails = UserModel(likes=123), description = "TEasd", videoId="a", thumbnailLink = "",playable=true,  videoLink = "", videoStats=VideoModel.VideoStats(comment=1, like=3, share = 3, favourite = 123)),
        // Add more dummy VideoModel instances as needed
    )

    private val dummyUserVideos = mapOf(
        1L to listOf(VideoModel(authorDetails = UserModel(likes=123), description = "TEasd", thumbnailLink = "", videoId="a",playable=true,  videoLink = "", videoStats=VideoModel.VideoStats(comment=1, like=3, share = 3, favourite = 123))),
        2L to listOf(VideoModel(authorDetails = UserModel(likes=123), description = "TEasd", thumbnailLink = "", videoId="a",playable=true,  videoLink = "", videoStats=VideoModel.VideoStats(comment=1, like=3, share = 3, favourite = 123))),
        // Map user IDs to their respective video lists
    )

    fun fetchVideos(): Flow<List<VideoModel>> {
        return flow {
            emit(dummyVideos)
        }
    }

    fun fetchVideosOfParticularUser(userId: Long): Flow<List<VideoModel>> {
        return flow {
            val userVideoList = dummyUserVideos[userId] ?: emptyList()
            emit(userVideoList)
        }
    }
}