package com.puskal.composable


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
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Response


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class Author(
    val id: Int,
    val username: String,
    val picture_url: String,
    val follower_count: Int,
    val play_count: Int,
    val video_count: Int
)

data class Video(
    val unique_id: String,
    val slug: String,
    val playable: Boolean,
    val watermark_available: Boolean,
    val plays: Int,
    val like_count: Int,
    val save_count: Int,
    val prompt: String,
    val description: String,
    val derived: Boolean,
    val url: String,
    val desktop_url: String,
    val download_url: String,
    val boomerang_url: String,
    val thumbnail_url: String,
    val async_watchtime99: Double,
    val async_watchtime95: Double,
    val async_watchtime90: Double,
    val author: Author,
    val has_liked: Boolean,
    val sfw_status: Int,
    val created_at: String
)

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
    onClickUser: (userId: Long) -> Unit,
    onClickFavourite: (isFav: Boolean) -> Unit = {},
    onClickShare: (() -> Unit)? = null
) {
    val pagerState = rememberPagerState(initialPage = initialPage ?: 0)
    val coroutineScope = rememberCoroutineScope()
    val localDensity = LocalDensity.current

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

            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.reemix.co/api/v2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val statisticsApi = retrofit.create(StatisticsApi::class.java)

            CoroutineScope(Dispatchers.IO).launch {

                val tmpr = statisticsApi.getFeed(stream_id, explored_until)
                //Log.d("STREAM DEBUG", "Loading stream " + stream_id)


                val response = statisticsApi.getFeed(stream_id, explored_until)
                //Log.d("STREAM DEBUG", "Loading stream " + stream_id)
                if (response.isSuccessful) {
                    val feedResponse = response.body()

                    //Log.d("MUTABLE VIDEOS", mutable_videos.toString())
                    //Log.d("VIDEO PICK", response.body()!!.videos[0].toString())

                    if(stream_id.length < 3) {
                        stream_id = response.body()!!.stream_id
                    }

                    val videos_from_api = response.body()!!.videos;
                    if(videos_from_api.size >= mutable_videos.size) {
                        //Log.d("VIDEO INFO", videos_from_api.toString())
                        mutable_videos = videos_from_api
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
                        showUploadDate=showUploadDate,
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


@Composable
fun SideItems(
    modifier: Modifier,
    item: VideoModel,
    doubleTabState: Triple<Offset, Boolean, Float>,
    onclickComment: (videoId: String) -> Unit,
    onClickUser: (userId: Long) -> Unit,
    onClickShare: (() -> Unit)? = null,
    onClickFavourite: (isFav: Boolean) -> Unit
) {

    val context = LocalContext.current
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
                    onClickUser.invoke(item.authorDetails.userId)
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

        var isLiked by remember {
            mutableStateOf(item.currentViewerInteraction.isLikedByYou)
        }

        LaunchedEffect(key1 = doubleTabState) {
            if (doubleTabState.first != Offset.Unspecified && doubleTabState.second) {
                isLiked = doubleTabState.second
            }
        }
        LikeIconButton(isLiked = isLiked,
            likeCount = "123",
            onLikedClicked = {
                isLiked = it
                item.currentViewerInteraction.isLikedByYou = it
            })


        Icon(painter = painterResource(id = R.drawable.ic_comment),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(33.dp)
                .clickable {
                    onclickComment(item.videoId)
                })
        Text(
            text = "this is the text",
            style = MaterialTheme.typography.labelMedium
        )
        16.dp.Space()



        Icon(
            painter = painterResource(id = R.drawable.ic_bookmark),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(33.dp)
        )
        Text(
            text = "0",
            style = MaterialTheme.typography.labelMedium
        )
        14.dp.Space()

        Icon(
            painter = painterResource(id = R.drawable.ic_share),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(32.dp)
                .clickable {
                    onClickShare?.let { onClickShare.invoke() } ?: run {
                        context.share(
                            text = "https://github.com/puskal-khadka"
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
    isLiked: Boolean, likeCount: String, onLikedClicked: (Boolean) -> Unit
) {

    val maxSize = 38.dp
    val iconSize by animateDpAsState(targetValue = if (isLiked) 33.dp else 32.dp,
        animationSpec = keyframes {
            durationMillis = 400
            24.dp.at(50)
            maxSize.at(190)
            26.dp.at(330)
            32.dp.at(400).with(FastOutLinearInEasing)
        })

    Box(
        modifier = Modifier
            .size(maxSize)
            .clickable(interactionSource = MutableInteractionSource(), indication = null) {
                onLikedClicked(!isLiked)
            }, contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_heart),
            contentDescription = null,
            tint = if (isLiked) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier.size(iconSize)
        )
    }

    Text(text = likeCount, style = MaterialTheme.typography.labelMedium)
    16.dp.Space()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FooterUi(
    modifier: Modifier,
    item: VideoModel,
    showUploadDate: Boolean,
    onClickAudio: (VideoModel) -> Unit,
    onClickUser: (userId: Long) -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.Bottom) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
            onClickUser(item.authorDetails.userId)
        }) {
            Text(
                text = item.authorDetails.fullName, style = MaterialTheme.typography.bodyMedium
            )
            if (showUploadDate) {
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
            modifier = Modifier.fillMaxWidth(0.85f)
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


