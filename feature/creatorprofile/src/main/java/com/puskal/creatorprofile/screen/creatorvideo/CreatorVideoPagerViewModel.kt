package com.puskal.creatorprofile.screen.creatorvideo

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.puskal.core.DestinationRoute.PassedKey.USER_ID
import com.puskal.core.DestinationRoute.PassedKey.VIDEO_INDEX
import com.puskal.core.base.BaseViewModel
import com.puskal.data.model.VideoModel
import com.puskal.domain.creatorprofile.GetCreatorProfileUseCase
import com.puskal.domain.creatorprofile.GetCreatorPublicVideoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by Puskal Khadka on 3/22/2023.
 */
@HiltViewModel
class CreatorVideoPagerViewModel
@Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getCreatorProfileUseCase: GetCreatorProfileUseCase,
    private val getCreatorPublicVideoUseCase: GetCreatorPublicVideoUseCase,
    @ApplicationContext private val context: Context
) : BaseViewModel<ViewState, CreatorVideoEvent>() {
    val userId: String? = savedStateHandle[USER_ID]
    val videoIndex: Int? = savedStateHandle[VIDEO_INDEX]

    private val _videosList = MutableStateFlow<List<VideoModel>>(arrayListOf())
    val publicVideosList = _videosList.asStateFlow()

    override fun onTriggerEvent(event: CreatorVideoEvent) {
    }

    init {
        userId?.let {
            fetchCreatorVideo(it, context)
        }
    }


    private fun fetchCreatorVideo(id: String, context: Context) {
        viewModelScope.launch {
            getCreatorPublicVideoUseCase(id, this@CreatorVideoPagerViewModel.context).collect {
                updateState(ViewState(creatorVideosList = it))
            }
        }
    }

}