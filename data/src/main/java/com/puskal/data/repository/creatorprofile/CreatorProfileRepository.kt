package com.puskal.data.repository.creatorprofile

import android.content.Context
import com.puskal.data.model.UserModel
import com.puskal.data.model.VideoModel
//import com.puskal.data.source.UsersDataSource.fetchSpecificUser
import com.puskal.data.source.UsersDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
//import com.puskal.data.source.UsersDataSource.fetchVideosOfParticularUser

/**
 * Created by Puskal Khadka on 3/22/2023.
 */




class CreatorProfileRepository @Inject constructor() {
    fun getCreatorDetails(id: String, context: Context): Flow<UserModel?> {
        return UsersDataSource(context).fetchSpecificUser(id)
    }

    fun getCreatorPublicVideo(id: String, context: Context): Flow<List<VideoModel>> {
        return UsersDataSource(context).fetchVideosOfParticularUser(id)
    }
}