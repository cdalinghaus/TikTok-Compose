package com.puskal.data.repository.creatorprofile

import com.puskal.data.model.UserModel
import com.puskal.data.model.VideoModel
import com.puskal.data.source.UsersDataSource.fetchSpecificUser
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import com.puskal.data.source.UsersDataSource.fetchVideosOfParticularUser

/**
 * Created by Puskal Khadka on 3/22/2023.
 */




class CreatorProfileRepository @Inject constructor() {
    fun getCreatorDetails(id: String): Flow<UserModel?> {
        return fetchSpecificUser(id)
    }

    fun getCreatorPublicVideo(id: String): Flow<List<VideoModel>> {
        return fetchVideosOfParticularUser(id)
    }
}