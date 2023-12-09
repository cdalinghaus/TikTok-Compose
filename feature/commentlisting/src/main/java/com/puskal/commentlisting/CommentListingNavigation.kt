package com.puskal.commentlisting

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import com.puskal.core.DestinationRoute.COMMENT_BOTTOM_SHEET_ROUTE

/**
 * Created by Puskal Khadka on 3/22/2023.
 */

@OptIn(ExperimentalMaterialNavigationApi::class)
fun NavGraphBuilder.commentListingNavGraph(navController: NavController) {
    bottomSheet(route = "$COMMENT_BOTTOM_SHEET_ROUTE/{videoId}") { backStackEntry ->
        val videoId = backStackEntry.arguments?.getString("videoId")
            ?: throw IllegalStateException("Video ID is required")
        CommentListScreen(videoId = videoId, onClickCancel = { navController.navigateUp() })
    }
}
