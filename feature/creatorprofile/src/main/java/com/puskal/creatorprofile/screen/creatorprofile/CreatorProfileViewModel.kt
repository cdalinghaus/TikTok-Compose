package com.puskal.creatorprofile.screen.creatorprofile

import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.puskal.composable.AuthInterceptor
import com.puskal.core.DestinationRoute.PassedKey.USER_ID
import com.puskal.core.base.BaseViewModel
import com.puskal.data.model.UserModel
import com.puskal.data.model.VideoModel
import com.puskal.domain.creatorprofile.GetCreatorProfileUseCase
import com.puskal.domain.creatorprofile.GetCreatorPublicVideoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
/**
 * Created by Puskal Khadka on 3/22/2023.
 */

class AuthInterceptor(context: Context) : Interceptor {
    private val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

    private fun getToken(): String? {
        return sharedPreferences.getString("auth_token", null)
    }

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()
        val token = getToken()
        Log.d("AUTH TOKEN!!!", token.toString())

        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}

@HiltViewModel
class CreatorProfileViewModel
@Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getCreatorProfileUseCase: GetCreatorProfileUseCase,
    private val getCreatorPublicVideoUseCase: GetCreatorPublicVideoUseCase,
    @ApplicationContext private val context: Context
) : BaseViewModel<ViewState, CreatorProfileEvent>()  {
    val userId: String? = savedStateHandle[USER_ID]

    private val _publicVideosList = MutableStateFlow<List<VideoModel>>(arrayListOf())
    val publicVideosList = _publicVideosList.asStateFlow()

    private val _likedVideosList = MutableStateFlow<List<VideoModel>>(arrayListOf())
    val likedVideosList = _likedVideosList.asStateFlow()

    private val _creatorProfile = MutableStateFlow<UserModel?>(null)
    val creatorProfile = _creatorProfile.asStateFlow()


    override fun onTriggerEvent(event: CreatorProfileEvent) {
    }

    init {
        userId?.let {
            fetchUser(it, context)
            fetchCreatorPublicVideo(it)
        }
    }



    fun toggleFollow(context: Context) {

        val currentProfile = _creatorProfile.value
        if (currentProfile != null) {

            // Toggle the follow state
            var new_follower_count = 0L
            var new_isfollowed = false
            if (currentProfile.isFollowed) {
                new_follower_count = currentProfile.followers - 1
                new_isfollowed = false
            } else {
                new_follower_count = currentProfile.followers + 1
                new_isfollowed = true
            }

            val updatedProfile = currentProfile.copy(
                isFollowed = new_isfollowed,
                followers = new_follower_count
            )

            // Update the MutableStateFlow with the new instance
            _creatorProfile.value = updatedProfile

            // Rest of your network request logic...

            val currentState = currentProfile.isFollowed
            // Update the MutableStateFlow
            Log.d("State change", currentProfile.isFollowed.toString())
            _creatorProfile.value?.isFollowed  = !currentState
            Log.d("State change", currentProfile.isFollowed.toString())

            // API call logic...

            // Determine the endpoint based on the follow status
            val endpoint = if (currentState) "unfollow" else "follow"
            val url = "https://api.reemix.co/api/v2/creators/${creatorProfile.value?.uniqueUserName}/$endpoint"
            Log.d("FOLLOW_ENDPOINT", url.toString())

            // Create a coroutine to make the network request
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Prepare the request
                    val request = Request.Builder()
                        .url(url)
                        .post(FormBody.Builder().build()) // Assuming a POST request
                        .build()

                    val okHttpClient = OkHttpClient.Builder()
                        .addInterceptor(AuthInterceptor(context))
                        .build()

                    // Execute the request
                    val response = okHttpClient.newCall(request).execute()

                    if (response.isSuccessful) {
                        // Handle successful response
                        Log.d("FOLLOW_ENDPOINT", "SUCCESS! Status Code: ${response.code}")

                    } else {
                        // Handle error
                        Log.d("FOLLOW_ENDPOINT", "NO SUCCESS! Status Code: ${response.code}")
                        val errorBody = response.body?.string()
                        Log.d("FOLLOW_ENDPOINT", "Error Body: $errorBody")
                    }
                } catch (e: Exception) {
                    // Handle exception
                    Log.e("FOLLOW_ENDPOINT", "Exception: ${e.message}")
                }
            }
        }
    }


    private fun fetchUser(id: String, context: Context) {
        viewModelScope.launch {
            getCreatorProfileUseCase(id, context).collect { userModel ->
                _creatorProfile.value = userModel
            }
        }
    }

    private fun fetchCreatorPublicVideo(id: String) {
        viewModelScope.launch {
            getCreatorPublicVideoUseCase(id, context).collect {
                Log.d("d", "my video is ${it}")
                _publicVideosList.value = it
            }
        }
    }

}