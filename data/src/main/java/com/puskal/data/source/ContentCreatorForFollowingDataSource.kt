package com.puskal.data.source

import com.puskal.data.model.ContentCreatorFollowingModel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Created by Puskal Khadka on 3/15/2023.
 */

/**
 * this is fake data source
 * you can use your api
 */
object ContentCreatorForFollowingDataSource {

    fun fetchContentCreatorForFollowing(): Flow<List<ContentCreatorFollowingModel>> {
        return flow {
            val creatorForFollowing: List<ContentCreatorFollowingModel> = listOf(

            )
            emit(creatorForFollowing.shuffled())
        }

    }

}