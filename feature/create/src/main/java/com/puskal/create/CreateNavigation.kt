package com.puskal.create

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.puskal.core.DestinationRoute
import com.puskal.create.SimpleCreateScreen

/**
 * Created by Puskal Khadka on 4/1/2023.
 */
fun NavGraphBuilder.createNavGraph(navController: NavController) {
    composable(route = DestinationRoute.CREATE_NEW_VIDEO_ROUTE) {
        SimpleCreateScreen(navController)
    }
}