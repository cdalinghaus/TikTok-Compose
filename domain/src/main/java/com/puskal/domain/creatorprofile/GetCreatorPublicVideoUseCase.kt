package com.puskal.domain.creatorprofile

import android.content.Context
import com.puskal.data.model.VideoModel
import com.puskal.data.repository.creatorprofile.CreatorProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Created by Puskal Khadka on 3/22/2023.
 */
class GetCreatorPublicVideoUseCase @Inject constructor(
    private val creatorProfileRepository: CreatorProfileRepository
) {
    operator fun invoke(id: String, context: Context): Flow<List<VideoModel>> {
        return creatorProfileRepository.getCreatorPublicVideo(id, context)
    }
}