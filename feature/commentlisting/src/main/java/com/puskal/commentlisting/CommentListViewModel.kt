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

    private val videoId = savedStateHandle.get<String>("videoId") ?: throw IllegalStateException("Video ID is required")

    init {
        loadComments(videoId)
    }

    fun loadComments(videoId: String) {
        viewModelScope.launch {
            getCommentOnVideoUseCase(videoId).collect {
                updateState(ViewState(comments = it, videoId = videoId))
            }
        }
    }

    // Call this function after successfully posting a comment
    fun refreshComments() {
        loadComments(videoId)
    }

    override fun onTriggerEvent(event: CommentEvent) {
        // Handle other events as needed
    }
}