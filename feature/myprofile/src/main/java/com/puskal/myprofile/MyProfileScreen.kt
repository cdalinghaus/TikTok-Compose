package com.puskal.myprofile

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.puskal.composable.CustomButton
import com.puskal.composable.TopBar
import com.puskal.core.DestinationRoute
import com.puskal.core.DestinationRoute.SETTING_ROUTE
import com.puskal.loginwithemailphone.tabs.SharedPreferencesManager
import com.puskal.theme.R
import com.puskal.theme.SubTextColor
import com.puskal.data.model.UserModel

/**
 * Created by Puskal Khadka on 4/1/2023.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(navController: NavController) {
    Scaffold(topBar = {
        TopBar(
            navIcon = null,
            title = stringResource(id = R.string.profile),
            actions = {
                IconButton(onClick = {
                    navController.navigate(SETTING_ROUTE)
                }) {
                    Icon(painterResource(id = R.drawable.ic_hamburger), contentDescription = null)
                }
            }
        )
    }) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            UnAuthorizedInboxScreen({
                navController.navigate(DestinationRoute.AUTHENTICATION_ROUTE)
            }, navController)
        }
    }
}


@Composable
fun UnAuthorizedInboxScreen(onClickSignup: () -> Unit, navController: NavController) {

    val context = LocalContext.current

    var authToken by remember { mutableStateOf<String?>(null) }
    var user by remember { mutableStateOf<UserModel?>(null) }

    LaunchedEffect(key1 = authToken, key2 = user) {
        authToken = SharedPreferencesManager.getToken(context)
        user = SharedPreferencesManager.getUser(context)

        // If userModel is not null, navigate to the creator profile page
        if (user != null) {
            val userId = user!!.uniqueUserName
            Log.d("NAV", "NAVIGATING TO USER PAGE!")
            //navController.navigate("${DestinationRoute.CREATOR_PROFILE_ROUTE}/anonymous")
            navController.navigate("${DestinationRoute.CREATOR_PROFILE_ROUTE}/$userId")
        }
    }


    Column(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp, alignment = Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_profile),
            contentDescription = null,
            modifier = Modifier.size(68.dp)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth(0.66F)
        ) {
            Text(
                text = stringResource(id = R.string.sign_up_for_an_account),
                color = SubTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(0.66.dp)
            )
        }

        CustomButton(
            buttonText = stringResource(id =  R.string.sign_up),
            modifier = Modifier.fillMaxWidth(0.66f)
        )
        {
            onClickSignup()
        }
        Text(text="Auth Token: $authToken, User: $user", color = Color.Black)
    }
}