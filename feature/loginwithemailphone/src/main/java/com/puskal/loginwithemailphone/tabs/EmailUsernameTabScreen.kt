package com.puskal.loginwithemailphone.tabs

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.navigation.NavController
import com.google.gson.Gson
import com.puskal.composable.CustomButton
import com.puskal.core.AppContract
import com.puskal.core.DestinationRoute
import com.puskal.core.extension.Space
import com.puskal.data.model.UserModel
import com.puskal.loginwithemailphone.LoginEmailPhoneEvent
import com.puskal.loginwithemailphone.LoginWithEmailPhoneViewModel
import com.puskal.loginwithemailphone.suggestedDomainList
import com.puskal.theme.*
import com.puskal.theme.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * Created by Puskal Khadka on 3/27/2023.
 */

// Retrofit Interface
interface AuthService {
    @POST("auth/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<LoginResponse>

    // Register ALSO returns a LoginResponse
    @POST("auth/register")
    suspend fun registerUser(@Body registerRequest: RegisterRequest): Response<LoginResponse>

    @GET("creators/{username}")
    suspend fun checkUserExists(@Path("username") username: String): Response<Boolean>
}
data class RegisterRequest(val username: String, val password: String, val email: String)
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val token: String, val user: UserModel)

object SharedPreferencesManager {
    private const val PREFS_NAME = "MyAppPrefs"
    private const val TOKEN_KEY = "auth_token"
    private const val USER_KEY = "auth_user"

    fun saveToken(context: Context, token: String) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putString(TOKEN_KEY, token).apply()
    }

    fun TikTokVerticalVideoPager(context: Context): String? {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getString(TOKEN_KEY, null)
    }

    fun saveUser(context: Context, user: UserModel) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userJson = Gson().toJson(user)
        sharedPrefs.edit().putString(USER_KEY, userJson).apply()
    }

    fun getUser(context: Context): UserModel? {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userJson = sharedPrefs.getString(USER_KEY, null)
        return userJson?.let { Gson().fromJson(it, UserModel::class.java) }
    }
}


val retrofit = Retrofit.Builder()
    .baseUrl("https://api.reemix.co/api/v2/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
val authService = retrofit.create(AuthService::class.java)


@Composable
fun EmailUsernameTabScreen(viewModel: LoginWithEmailPhoneViewModel, navController: NavController) {
    val email by viewModel.email.collectAsState()
    val username by viewModel.username.collectAsState() // Add state for username
    val password by viewModel.password.collectAsState()

    // Get the current context from Composables
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = 42.dp, start = 28.dp, end = 28.dp)
                .weight(1f),
        ) {

            UsernameField(username.first, viewModel) // Add this line for the username field
            8.dp.Space()
            PasswordField(password.first, viewModel)
            8.dp.Space()


            if (!viewModel.isUserExists.value) {
                EmailField(email, viewModel)
                8.dp.Space()
            }

            PrivacyPolicyText {}
            16.dp.Space()
            val buttonText = if (viewModel.isUserExists.value) "Log in" else "Sign up"
            CustomButton(
                buttonText = buttonText,
                modifier = Modifier.fillMaxWidth(),
                isEnabled = password.first.isNotEmpty() && username.first.isNotEmpty() && (email.first.isNotEmpty() || viewModel.isUserExists.value) // Check if username is not empty
            )
            {


                // Authentication logic

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if(!viewModel.isUserExists.value) {
                            val loginRequest = RegisterRequest(username = username.first,
                                password = password.first,
                                email = email.first)
                            // REGISTER A USER
                            val response = authService.registerUser(loginRequest)
                            if (response.isSuccessful) {
                                // Handle successful authentication
                                val loginResponse = response.body()!!
                                // Save the token and navigate to the next screen or show success message

                                Log.d("AUTH", "Registration success.! :)" + loginResponse.token + loginResponse.user.toString())

                                // Use withContext to switch to the Main thread before accessing context
                                withContext(Dispatchers.Main) {
                                    SharedPreferencesManager.saveToken(context, loginResponse.token)
                                    SharedPreferencesManager.saveUser(context, loginResponse.user)
                                    val uniqueUserName = loginResponse.user.uniqueUserName
                                    navController.navigate("${DestinationRoute.CREATOR_PROFILE_ROUTE}/$uniqueUserName")
                                }


                            } else {
                                // Handle error - log the error reason
                                val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                                val errorCode = response.code()
                                withContext(Dispatchers.Main) {
                                    Log.d("AUTH", "Registration failed: Error Code: $errorCode, Message: $errorMessage")
                                }
                            }
                        } else {
                            // LOG IN INSTEAD
                            val loginRequest = LoginRequest(username = username.first, password = password.first)
                            val response = authService.loginUser(loginRequest)
                            if (response.isSuccessful) {
                                // Handle successful authentication
                                val loginResponse = response.body()!!
                                // Save the token and navigate to the next screen or show success message

                                Log.d("AUTH", "Authentication success :)" + loginResponse.token  + loginResponse.user.toString())

                                // Use withContext to switch to the Main thread before accessing context
                                withContext(Dispatchers.Main) {
                                    SharedPreferencesManager.saveToken(context, loginResponse.token)
                                    SharedPreferencesManager.saveUser(context, loginResponse.user)
                                    val uniqueUserName = loginResponse.user.uniqueUserName
                                    navController.navigate("${DestinationRoute.CREATOR_PROFILE_ROUTE}/$uniqueUserName")
                                }

                            } else {
                                // Handle error - log the error reason
                                val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                                val errorCode = response.code()
                                withContext(Dispatchers.Main) {
                                    Log.d("AUTH", "Authentication failed: Error Code: $errorCode, Message: $errorMessage")
                                }
                            }
                        }

                    } catch (e: Exception) {
                        // Handle exception - show error message to the user
                        withContext(Dispatchers.Main) {
                            Log.d("AUTH", "Error: ${e.message}")
                        }
                    }
                }



            }
        }

        DomainSuggestion {
            viewModel.onTriggerEvent(
                LoginEmailPhoneEvent.OnChangeEmailEntry(
                    "${email.first.substringBefore("@")}$it"
                )
            )
        }
        16.dp.Space()

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordField(password: String, viewModel: LoginWithEmailPhoneViewModel) {
    val focusRequester = remember { FocusRequester() }
    var isPasswordVisible by remember { mutableStateOf(false) }

    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        value = password,
        textStyle = MaterialTheme.typography.labelLarge,
        onValueChange = { newValue ->
            // Update this to handle password change
            viewModel.onTriggerEvent(LoginEmailPhoneEvent.OnChangePasswordEntry(newValue))
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), // Use password keyboard
        singleLine = true,
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        colors = TextFieldDefaults.textFieldColors(
            containerColor = Color.Transparent,
            focusedIndicatorColor = SubTextColor,
            unfocusedIndicatorColor = SubTextColor,
        ),
        placeholder = {
            Text(
                text = "Enter your password", // Update this placeholder text
                style = MaterialTheme.typography.labelLarge,
                color = SubTextColor
            )
        },
        trailingIcon = {
            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                Icon(
                    painter = if (isPasswordVisible) painterResource(id = R.drawable.ic_hamburger) else painterResource(id = R.drawable.ic_add),
                    contentDescription = null
                )
            }
        }
    )
    // Add any specific LaunchedEffect if needed
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsernameField(username: String, viewModel: LoginWithEmailPhoneViewModel) {

    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        value = username,
        onValueChange = { newValue: String ->
            viewModel.onTriggerEvent(LoginEmailPhoneEvent.OnChangeUsernameEntry(newValue))
            debounceJob?.cancel() // Cancel previous job if still running
            debounceJob = coroutineScope.launch {
                delay(1000) // Wait for 1 second after the user stops typing
                viewModel.checkUserExists(newValue, authService) // Replace with your user existence check logic
            }
        },
        textStyle = MaterialTheme.typography.labelLarge,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), // Use standard keyboard
        singleLine = true,
        colors = TextFieldDefaults.textFieldColors(
            containerColor = Color.Transparent,
            focusedIndicatorColor = SubTextColor,
            unfocusedIndicatorColor = SubTextColor,
        ),
        placeholder = {
            Text(
                text = stringResource(id = R.string.enter_username), // Update this placeholder text
                style = MaterialTheme.typography.labelLarge,
                color = SubTextColor
            )
        },
        trailingIcon = {
            IconButton(onClick = {
                // Update this to clear username
                viewModel.onTriggerEvent(LoginEmailPhoneEvent.OnChangeUsernameEntry(""))
            }) {
                if (username.isNotEmpty()) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cancel),
                        contentDescription = null
                    )
                }

            }
        }
    )
    // Add any specific LaunchedEffect if needed

}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailField(email: Pair<String, String?>, viewModel: LoginWithEmailPhoneViewModel) {
    val currentPage by viewModel.settledPage.collectAsState()
    val focusRequester = remember { FocusRequester() }
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        value = email.first,
        textStyle = MaterialTheme.typography.labelLarge,
        onValueChange = { viewModel.onTriggerEvent(LoginEmailPhoneEvent.OnChangeEmailEntry(it)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        colors = TextFieldDefaults.textFieldColors(
            containerColor = Color.Transparent,
            focusedIndicatorColor = SubTextColor,
            unfocusedIndicatorColor = SubTextColor,
        ),
        placeholder = {
            Text(
                text = stringResource(id = R.string.enter_email_address_or_username),
                style = MaterialTheme.typography.labelLarge,
                color = SubTextColor
            )
        },
        trailingIcon = {
            IconButton(onClick = {
                viewModel.onTriggerEvent(
                    LoginEmailPhoneEvent.OnChangeEmailEntry(
                        ""
                    )
                )
            }) {
                if (email.first.isNotEmpty()) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cancel),
                        contentDescription = null
                    )
                }

            }
        }
    )
    LaunchedEffect(key1 = currentPage) {
        if (currentPage == 1) {
            focusRequester.requestFocus()
        }
    }
}


@Composable
fun PrivacyPolicyText(onClickAnnotation: (String) -> Unit) {
    val annotatedString = buildAnnotatedString {
        append(stringResource(id = R.string.by_continuing_you_agree))

        pushStringAnnotation(
            tag = AppContract.Annotate.ANNOTATED_TAG,
            annotation = AppContract.Annotate.ANNOTATED_TERMS_OF_SERVICE
        )
        withStyle(
            style = SpanStyle(
                color = Color.Black,
                fontWeight = FontWeight.SemiBold
            )
        ) { append(" ${stringResource(id = R.string.terms_of_service)}") }
        pop()

        append(stringResource(id = R.string.and_confirm_that_you_have_read))

        pushStringAnnotation(
            tag = AppContract.Annotate.ANNOTATED_TAG,
            annotation = AppContract.Annotate.ANNOTATED_PRIVACY_POLICY
        )
        withStyle(
            style = SpanStyle(
                color = Color.Black,
                fontWeight = FontWeight.SemiBold
            )
        ) { append(" ${stringResource(id = R.string.privacy_policy)}") }
        pop()
    }

    ClickableText(
        text = annotatedString, onClick = { offset ->
            annotatedString.getStringAnnotations(
                tag = AppContract.Annotate.ANNOTATED_TAG, start = offset, end = offset
            ).firstOrNull()?.let { annotation ->
                onClickAnnotation(annotation.item)
            }
        }, style = TextStyle(
            fontFamily = fontFamily
        )
    )
}

@Composable
fun DomainSuggestion(onClickDomain: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(suggestedDomainList) { domain ->
            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        shape = RoundedCornerShape(2.dp),
                        color = SeparatorColor
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                ClickableText(
                    text = AnnotatedString(domain),
                    style = MaterialTheme.typography.titleSmall,
                    onClick = {
                        onClickDomain(domain)
                    })
            }
        }
    }
}
