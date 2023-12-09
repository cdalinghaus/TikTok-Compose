package com.puskal.commentlisting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.puskal.core.base.BaseViewModel
import com.puskal.domain.comment.GetCommentOnVideoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by Puskal Khadka on 3/22/2023.
 */
@HiltViewModel
class CommentListViewModel @Inject constructor(
    private val getCommentOnVideoUseCase: GetCommentOnVideoUseCase,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<ViewState, CommentEvent>() {

    init {

        val videoId = savedStateHandle.get<String>("videoId") ?: throw IllegalStateException("Video ID is required")
        getContentCreator(videoId)
    }

    private fun getContentCreator(videoId: String) {
        viewModelScope.launch {
            getCommentOnVideoUseCase(videoId).collect {
                updateState(ViewState(comments = it))
            }
        }
    }

    override fun onTriggerEvent(event: CommentEvent) {
    }


}