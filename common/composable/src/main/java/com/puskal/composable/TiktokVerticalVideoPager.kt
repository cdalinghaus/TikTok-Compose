package com.puskal.composable


import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.puskal.core.extension.Space
import com.puskal.core.utils.IntentUtils.share
import com.puskal.data.model.VideoModel
import com.puskal.theme.*
import com.puskal.theme.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Response


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

import android.content.Context
import android.os.Environment
import androidx.compose.ui.text.style.TextOverflow
import com.google.gson.Gson
import com.puskal.data.model.UserModel
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.http.POST
import retrofit2.http.Path
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit


data class FeedResponse(
    val stream_id: String,
    val videos: List<VideoModel>
)

interface StatisticsApi {

    @GET("feed")
    suspend fun getFeed(
        @Query("stream") param: String,
        @Query("explored_until") exploredUntil: Int
    ): Response<FeedResponse>
}

object SharedPreferencesManager {
    private const val PREFS_NAME = "MyAppPrefs"
    private const val TOKEN_KEY = "auth_token"
    private const val USER_KEY = "auth_user"

    fun saveToken(context: Context, token: String) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putString(TOKEN_KEY, token).apply()
    }

    fun getToken(context: Context): String? {
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


suspend fun downloadAndSaveVideo(context: Context, videoUrl: String, fileName: String) {
    withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(videoUrl).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val inputStream = response.body?.byteStream()
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), fileName)

                FileOutputStream(file).use { outputStream ->
                    inputStream?.copyTo(outputStream)
                    //Log.d("DOWNLOADED", file.toString())
                }

                // Update the video.videoLink with the local file path
                // video.videoLink = file.absolutePath
            } else {
                // Handle the error
            }
        } catch (e: IOException) {
            // Handle the exception
        }
    }
}


/**
 * Created by Puskal Khadka on 3/16/2023.
 */
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun TikTokVerticalVideoPager(
    modifier: Modifier = Modifier,
    videos: List<VideoModel>,
    initialPage: Int? = 0,
    showUploadDate: Boolean = false,
    onclickComment: (videoId: String) -> Unit,
    onClickLike: (videoId: String, likeStatus: Boolean) -> Unit,
    onclickFavourite: (videoId: String) -> Unit,
    onClickAudio: (VideoModel) -> Unit,
    onClickUser: (userId: String) -> Unit,
    onClickFavourite: (isFav: Boolean) -> Unit = {},
    onClickShare: (() -> Unit)? = null
) {
    val pagerState = rememberPagerState(initialPage = initialPage ?: 0)
    val coroutineScope = rememberCoroutineScope()
    val localDensity = LocalDensity.current
    val scaffoldState = rememberScaffoldState()

    // Make videos mutable
    var mutable_videos by remember { mutableStateOf(videos) }
    var stream_id by remember { mutableStateOf("") }
    var explored_until by remember { mutableStateOf(0) }
    val context = LocalContext.current


    val fling = PagerDefaults.flingBehavior(
        state = pagerState, lowVelocityAnimationSpec = tween(
            easing = LinearEasing, durationMillis = 300
        )
    )

        LaunchedEffect(pagerState) {
            // Collect from the a snapshotFlow reading the currentPage
            snapshotFlow { pagerState.currentPage }.collect { page ->
                // Do something with each page change, for example:
                // viewModel.sendPageSelectedEvent(page)
                //Log.d("Page change", "Page changed to $page")
                explored_until = page

                val length: Int = mutable_videos.size
                //Log.d("Page change", "List length is now $length")
                // Ensure the list is not empty to avoid an exception


                //Log.d("Page change", "List length is now (after the code $length")

                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS) // Set the connection timeout
                    .readTimeout(30, TimeUnit.SECONDS) // Set the read timeout
                    .writeTimeout(30, TimeUnit.SECONDS) // Set the write timeout
                    .addInterceptor(AuthInterceptor(context))
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api.reemix.co/api/v2/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient) // Set the OkHttpClient as the client for Retrofit
                    .build()


                val statisticsApi = retrofit.create(StatisticsApi::class.java)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // val tmpr = statisticsApi.getFeed(stream_id, explored_until)
                        //Log.d("STREAM DEBUG", "Loading stream " + stream_id)


                        val response = statisticsApi.getFeed(stream_id, explored_until)


                        Log.d("STREAMID", "Loading stream " + stream_id)
                        if (response.isSuccessful) {
                            val feedResponse = response.body()

                            Log.d("MUTABLE VIDEOS", mutable_videos.toString())
                            //Log.d("VIDEO PICK", response.body()!!.videos[0].toString())

                            if (stream_id.length < 3) {
                                stream_id = response.body()!!.stream_id
                            }

                            val videos_from_api = response.body()!!.videos;
                            if (videos_from_api.size >= mutable_videos.size) {
                                //Log.d("VIDEO INFO", videos_from_api.toString())
                                if (videos_from_api.size == 1) {
                                    mutable_videos = videos_from_api + videos_from_api
                                } else {
                                    mutable_videos = videos_from_api
                                }

                            }

                        for (video in videos_from_api) {
                            //Log.d("videolink", video.videoLink)

                            CoroutineScope(Dispatchers.IO).launch {
                                val videoUrl = video.videoLink
                                val fileName = video.videoLink.split("/").last()
                                downloadAndSaveVideo(context, videoUrl, fileName)
                            }
                            // Update UI or video player with the new local path


                        }



                            //Log.d("VIDEO THAT WAS ADDED", response.body()!!.videos[0].toString())

                            //Log.d("FEED RESPONSE", feedResponse.toString())
                        } else {
                            // Handle error
                            Log.d("Load ERROR", "unsuccessful request")
                        }
                    } catch (e: Exception) {

                        // Handle exceptions like timeouts, no internet connection, etc.
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = "Error: ${e.message}",
                            duration = SnackbarDuration.Short
                        )
                        }

                }


            }
        }


    VerticalPager(
        pageCount = mutable_videos.size,
        state = pagerState,
        flingBehavior = fling,
        beyondBoundsPageCount = 1,
        modifier = modifier
    ) {
        var pauseButtonVisibility by remember { mutableStateOf(false) }
        var doubleTapState by remember {
            mutableStateOf(
                Triple(
                    Offset.Unspecified, //offset
                    false, //double tap anim start
                    0f //rotation angle
                )
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            VideoPlayer(mutable_videos[it], pagerState, it, onSingleTap = {
                pauseButtonVisibility = it.isPlaying
                it.playWhenReady = !it.isPlaying
            },
                onDoubleTap = { exoPlayer, offset ->
                    coroutineScope.launch {
                        mutable_videos[it].currentViewerInteraction.isLikedByYou = true
                        val rotationAngle = (-10..10).random()
                        doubleTapState = Triple(offset, true, rotationAngle.toFloat())
                        delay(400)
                        doubleTapState = Triple(offset, false, rotationAngle.toFloat())
                    }
                },
                onVideoDispose = { pauseButtonVisibility = false },
                onVideoGoBackground = { pauseButtonVisibility = false }

            )


            Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    FooterUi(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        item = mutable_videos[it],
                        showUploadDate = showUploadDate,
                        onClickAudio = onClickAudio,
                        onClickUser = onClickUser,
                    )

                    SideItems(
                        modifier = Modifier,
                        mutable_videos[it],
                        doubleTabState = doubleTapState,
                        onclickComment = onclickComment,
                        onClickUser = onClickUser,
                        onClickFavourite = onClickFavourite,
                        onClickShare = onClickShare
                    )
                }
                12.dp.Space()
            }


            AnimatedVisibility(
                visible = pauseButtonVisibility,
                enter = scaleIn(spring(Spring.DampingRatioMediumBouncy), initialScale = 1.5f),
                exit = scaleOut(tween(150)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(36.dp)
                )
            }

            val iconSize = 110.dp
            AnimatedVisibility(visible = doubleTapState.second,
                enter = scaleIn(spring(Spring.DampingRatioMediumBouncy), initialScale = 1.3f),
                exit = scaleOut(
                    tween(600), targetScale = 1.58f
                ) + fadeOut(tween(600)) + slideOutVertically(
                    tween(600)
                ),
                modifier = Modifier.run {
                    if (doubleTapState.first != Offset.Unspecified) {
                        this.offset(x = localDensity.run {
                            doubleTapState.first.x.toInt().toDp().plus(-iconSize.div(2))
                        }, y = localDensity.run {
                            doubleTapState.first.y.toInt().toDp().plus(-iconSize.div(2))
                        })
                    } else this
                }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_like),
                    contentDescription = null,
                    tint = if (doubleTapState.second) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.8f
                    ),
                    modifier = Modifier
                        .size(iconSize)
                        .rotate(doubleTapState.third)
                )
            }


        }
    }

}
data class LikeResponse(val placeholder: String)
interface CommentInterface {
    @POST("videos/{video_id}/like")
    suspend fun like(
        @Path("video_id") videoId: String
    ): Response<LikeResponse>

    @POST("videos/{video_id}/unlike")
    suspend fun unlike(
        @Path("video_id") videoId: String
    ): Response<LikeResponse>
}

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

@Composable
fun SideItems(
    modifier: Modifier,
    item: VideoModel,
    doubleTabState: Triple<Offset, Boolean, Float>,
    onclickComment: (videoId: String) -> Unit,
    onClickUser: (userId: String) -> Unit,
    onClickShare: (() -> Unit)? = null,
    onClickFavourite: (isFav: Boolean) -> Unit
) {

    val context = LocalContext.current
    val isUserLoggedIn = SharedPreferencesManager.getToken(context) != null

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = item.authorDetails.profilePic,
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .border(
                    BorderStroke(width = 1.dp, color = White), shape = CircleShape
                )
                .clip(shape = CircleShape)
                .clickable {
                    onClickUser.invoke(item.authorDetails.uniqueUserName)
                },
            contentScale = ContentScale.Crop
        )
        Image(
            painter = painterResource(id = R.drawable.ic_plus),
            contentDescription = null,
            modifier = Modifier
                .offset(y = (-10).dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(color = MaterialTheme.colorScheme.primary)
                .padding(5.5.dp),
            colorFilter = ColorFilter.tint(Color.White)
        )

        12.dp.Space()

        // Usage in parent composable
        var isLiked by remember { mutableStateOf(item.currentViewerInteraction.isLikedByYou) }
        var likeCount by remember { mutableStateOf(item.videoStats.like) }

        LaunchedEffect(key1 = doubleTabState) {
            if (doubleTabState.first != Offset.Unspecified && doubleTabState.second) {
                isLiked = doubleTabState.second
            }
        }


        LikeIconButton(isLiked = isLiked,
            likeCount = likeCount.toInt(),
            isEnabled = isUserLoggedIn,
            onLikedClicked = { liked ->
                isLiked = !isLiked
                likeCount = if (liked) likeCount + 1 else likeCount - 1

                // Determine the endpoint based on the like status
                val endpoint = if (liked) "like" else "unlike"
                val url = "https://api.reemix.co/api/v2/videos/${item.videoId}/$endpoint"

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
                            Log.d("LIKED_ENDPOINT", "SUCCESS! Status Code: ${response.code}")

                        } else {
                            // Handle error
                            Log.d("LIKED_ENDPOINT", "NO SUCCESS! Status Code: ${response.code}")
                            val errorBody = response.body?.string()
                            Log.d("LIKED_ENDPOINT", "Error Body: $errorBody")
                        }
                    } catch (e: Exception) {
                        // Handle exception
                        Log.e("LIKED_ENDPOINT", "Exception: ${e.message}")
                    }
                }
            }
        )
        16.dp.Space()



        Icon(painter = painterResource(id = R.drawable.ic_comment),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(33.dp)
                .clickable {
                    onclickComment(item.videoId)
                })
        Text(
            text = item.videoStats.comment.toString(),
            style = MaterialTheme.typography.labelMedium
        )
        16.dp.Space()

        var isSaved by remember {
            mutableStateOf(item.currentViewerInteraction.isAddedToFavourite)
        }

        var saveCount by remember {
            mutableStateOf(item.videoStats.favourite)
        }

        SaveIconButton(isSaved = isSaved,
            saveCount = saveCount.toString(),
            isEnabled = isUserLoggedIn,
            onSavedClicked = { saved ->
                isSaved = !isSaved
                saveCount = if (saved) saveCount + 1 else saveCount - 1

                // Determine the endpoint based on the save status
                val endpoint = if (saved) "save" else "unsave"
                val url = "https://api.reemix.co/api/v2/videos/${item.videoId}/$endpoint"

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
                            Log.d("SAVED_ENDPOINT", "SUCCESS! Status Code: ${response.code}")
                        } else {
                            // Handle error
                            Log.d("SAVED_ENDPOINT", "NO SUCCESS! Status Code: ${response.code}")
                            val errorBody = response.body?.string()
                            Log.d("SAVED_ENDPOINT", "Error Body: $errorBody")
                        }
                    } catch (e: Exception) {
                        // Handle exception
                        Log.e("SAVED_ENDPOINT", "Exception: ${e.message}")
                    }
                }
            }
        )




        Icon(
            painter = painterResource(id = R.drawable.ic_share),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(32.dp)
                .clickable {
                    onClickShare?.let { onClickShare.invoke() } ?: run {
                        context.share(
                            text = "https://reemix.co/v/${item.videoId}"
                        )
                    }
                }
        )
        Text(
            text = "0", style = MaterialTheme.typography.labelMedium
        )
        20.dp.Space()

        RotatingAudioView(item.authorDetails.profilePic)

    }
}

@Composable
fun LikeIconButton(
    isLiked: Boolean,
    likeCount: Int,
    onLikedClicked: (Boolean) -> Unit,
    isEnabled: Boolean = true // Adding isEnabled parameter with default value true
) {
    val iconSize = 32.dp // Adjust size as needed

    Box(
        modifier = Modifier
            .clickable(enabled = isEnabled) { onLikedClicked(!isLiked) }, // Use isEnabled to control clickable
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_heart),
            contentDescription = null,
            tint = if (!isEnabled) Color.Gray else if  (isLiked) MaterialTheme.colorScheme.primary else Color.White, // Use MaterialTheme.colors for compatibility
            modifier = Modifier.size(iconSize)
        )
    }

    Text(text = likeCount.toString(), style = MaterialTheme.typography.labelMedium)
}

@Composable
fun SaveIconButton(
    isSaved: Boolean,
    saveCount: String,
    onSavedClicked: (Boolean) -> Unit,
    isEnabled: Boolean = true // Add isEnabled parameter with default value true
) {
    var saved by remember { mutableStateOf(isSaved) }

    val maxSize = 38.dp
    val iconSize by animateDpAsState(
        targetValue = if (saved) 33.dp else 32.dp,
        animationSpec = keyframes {
            durationMillis = 400
            24.dp.at(50)
            maxSize.at(190)
            26.dp.at(330)
            32.dp.at(400).with(FastOutLinearInEasing)
        }
    )

    Box(
        modifier = Modifier
            .size(maxSize)
            .clickable(
                enabled = isEnabled, // Use isEnabled to control clickable
                interactionSource = MutableInteractionSource(),
                indication = null
            ) {
                saved = !saved
                onSavedClicked(saved)
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = if (saved) R.drawable.ic_bookmark else R.drawable.ic_bookmark),
            contentDescription = "Save",
            tint = if (!isEnabled) Color.Gray else if (saved) MaterialTheme.colorScheme.primary else Color.White, // Adjust tint based on isEnabled and saved state
            modifier = Modifier.size(iconSize)
        )
    }

    Text(text = saveCount, style = MaterialTheme.typography.labelMedium)
    // Removed the incorrect `16.dp.Space()` line as it's not a valid code statement
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FooterUi(
    modifier: Modifier,
    item: VideoModel,
    showUploadDate: Boolean,
    onClickAudio: (VideoModel) -> Unit,
    onClickUser: (userId: String) -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.Bottom) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
            onClickUser(item.authorDetails.uniqueUserName)
        }) {
            Text(
                text = item.authorDetails.fullName, style = MaterialTheme.typography.bodyMedium
            )
            if (true) {
                Text(
                    text = " . ${item.createdAt} ago",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
        5.dp.Space()
        Text(
            text = item.description,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(0.85f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        10.dp.Space()
        val audioInfo: String = item.audioModel?.run {
            "Original sound - ${audioAuthor.uniqueUserName} - ${audioAuthor.fullName}"
        }
            ?: item.run { "Original sound - ${item.authorDetails.uniqueUserName} - ${item.authorDetails.fullName}" }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.clickable {
                onClickAudio(item)
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_music_note),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = audioInfo,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .basicMarquee()
            )
        }
    }
}


@Composable
fun RotatingAudioView(img: String) {
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = keyframes { durationMillis = 7000 })
    )

    Box(modifier = Modifier.rotate(angle)) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Gray20, Gray20, GrayLight, Gray20, Gray20,
                        )
                    ), shape = CircleShape
                )
                .size(46.dp), contentAlignment = Alignment.Center
        ) {

            AsyncImage(
                model = img,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

        }
    }

}


