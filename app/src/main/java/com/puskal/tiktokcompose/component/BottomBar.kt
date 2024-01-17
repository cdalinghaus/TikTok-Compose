package com.puskal.tiktokcompose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.puskal.tiktokcompose.navigation.BottomBarDestination

/**
 * Created by Puskal Khadka on 11/25/2022.
 */

@Composable
fun BottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    isDarkTheme: Boolean
) {
    NavigationBar(
        modifier = Modifier
            .height(52.dp)
            .shadow(elevation = 16.dp)
            .padding(top = 2.dp)
    ) {
        BottomBarDestination.values().asList().forEach {
            BottomItem(it, navController, currentDestination, isDarkTheme)
        }
    }
}

@Composable
fun RowScope.BottomItem(
    screen: BottomBarDestination,
    navController: NavHostController,
    currentDestination: NavDestination?,
    isDarkTheme: Boolean
) {
    val isCurrentBottomItemSelected = currentDestination?.hierarchy?.any {
        it.route == screen.route
    } ?: false

    val (iconSize, offsetY) = if (screen == BottomBarDestination.ADD) Pair(42.dp, (-8).dp)
    else Pair(22.dp, 0.dp)

    var icon: Int = screen.unFilledIcon
    var isButtonEnabled = true

    val isButtonDisabled = screen == BottomBarDestination.FRIENDS ||
            screen == BottomBarDestination.INBOX


    screen.apply {
        when(this) {
            BottomBarDestination.ADD -> {
                if (isDarkTheme) darkModeIcon?.let { icon = it }
            }
            BottomBarDestination.FRIENDS, BottomBarDestination.INBOX -> {
                isButtonEnabled = false // Disable the button for these routes
            }
            else -> {
                if (isCurrentBottomItemSelected) {
                    filledIcon?.let { icon = it }
                }
            }
        }
    }

    // Adjust icon and text color when disabled
    val iconTint = if (!isButtonEnabled) Color.DarkGray else Color.Unspecified
    val textColorAlpha = if (!isButtonEnabled) 0.4f else if (isCurrentBottomItemSelected) 1f else 0.7f


    NavigationBarItem(
        modifier = Modifier.offset(y = -BottomBarItemVerticalOffset),
        label = {
            screen.title?.let {
                Text(
                    modifier = Modifier.offset(y = BottomBarItemVerticalOffset.times(1.85f)),
                    text = stringResource(id = screen.title),
                    style = MaterialTheme.typography.labelSmall,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = textColorAlpha)
                )
            }
        },
        icon = {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                Modifier
                    .padding(bottom = 9.dp)
                    .size(iconSize)
                    .offset(y = offsetY),
                tint = iconTint,
            )
        },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.surface,
            selectedIconColor = MaterialTheme.colorScheme.secondary,
            selectedTextColor = MaterialTheme.colorScheme.secondary
        ),
        selected = isCurrentBottomItemSelected,
        onClick = {
            if (!isButtonDisabled) {
                screen.route.let {
                    navController.navigate(it) {
                        launchSingleTop = true
                    }
                }
            }
        }
    )
}

private val BottomBarItemVerticalOffset = 10.dp