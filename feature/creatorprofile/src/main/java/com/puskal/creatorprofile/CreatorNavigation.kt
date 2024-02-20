package com.puskal.creatorprofile

import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.puskal.core.DestinationRoute.COMMENT_BOTTOM_SHEET_ROUTE
import com.puskal.core.DestinationRoute.CREATOR_PROFILE_ROUTE
import com.puskal.core.DestinationRoute.FORMATTED_COMPLETE_CREATOR_VIDEO_ROUTE
import com.puskal.core.DestinationRoute.PassedKey.USER_ID
import com.puskal.core.DestinationRoute.PassedKey.VIDEO_INDEX
import com.puskal.creatorprofile.screen.creatorvideo.CreatorVideoPagerScreen
import com.puskal.creatorprofile.screen.creatorprofile.CreatorProfileScreen as CreatorProfileScreen1

/**
 * Created by Puskal Khadka on 3/22/2023.
 */

fun NavGraphBuilder.creatorProfileNavGraph(navController: NavController) {

    composable(route = "$CREATOR_PROFILE_ROUTE/{$USER_ID}",
        arguments = listOf(
            navArgument(USER_ID) { type = NavType.StringType }
        )
    ) {
        CreatorProfileScreen1(
            onClickNavIcon = { navController.navigate("home_screen_route") },
            navController = navController,

        )
    }

    composable(route = FORMATTED_COMPLETE_CREATOR_VIDEO_ROUTE,
        arguments = listOf(
            navArgument(USER_ID) { type = NavType.StringType },
            navArgument(VIDEO_INDEX) { type = NavType.IntType }
        )
    ) {
        CreatorVideoPagerScreen(
            onClickNavIcon = { navController.navigateUp() },
            navController = navController,
            onclickComment = { navController.navigate(COMMENT_BOTTOM_SHEET_ROUTE) },
            onClickAudio = {},
            onClickUser = { navController.navigateUp() }
        )
    }
}

