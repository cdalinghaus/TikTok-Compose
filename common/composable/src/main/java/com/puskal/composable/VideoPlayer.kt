package com.puskal.composable


import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.puskal.core.utils.FileUtils
import com.puskal.data.model.VideoModel
import com.puskal.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by Puskal Khadka on 3/16/2023.
 */
@OptIn(ExperimentalFoundationApi::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayer(
    video: VideoModel,
    pagerState: PagerState,
    pageIndex: Int,
    onSingleTap: (exoPlayer: ExoPlayer) -> Unit,
    onDoubleTap: (exoPlayer: ExoPlayer, offset: Offset) -> Unit,
    onVideoDispose: () -> Unit = {},
    onVideoGoBackground: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var previewUrl by remember { mutableStateOf<String?>(null) }



    var thumbnail by remember {
        mutableStateOf<Pair<Bitmap?, Boolean>>(Pair(null, true))  //bitmap, isShow
    }
    var isFirstFrameLoad = remember { false }

    var previewLoaded = remember { false }

    val thisvideo = remember { mutableStateOf(video) }

    class VideoResponseWrapper {
        var data: VideoModel? = null
    }



    if (!video.playable) {
        LaunchedEffect(Unit) {
            val targetEndpoint = "https://api.reemix.co/api/v2/videos/${video.videoId}"
            Log.d("xd", "before loop")

            while (true) { // Ensures the coroutine stops when no longer active
                Log.d("xd", "in loop")
                try {
                    withContext(Dispatchers.IO) {
                        // Create a URL and connection object
                        val url = URL(targetEndpoint)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.connect()

                        val responseCode = connection.responseCode
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            val inputStream = connection.inputStream
                            val response = inputStream.bufferedReader().use { it.readText() }
                            withContext(Dispatchers.Main) {
                                val gson = Gson()
                                val responseWrapper: VideoResponseWrapper = gson.fromJson(response, VideoResponseWrapper::class.java)
                                val updatedVideo: VideoModel? = responseWrapper.data

                                Log.d("theoretically found", "new video" + updatedVideo.toString() )
                                if (updatedVideo != null) {
                                    thisvideo.value = updatedVideo
                                }


                            }
                        } else {
                            Log.e("VideoFetch", "responseCode == HttpURLConnection.HTTP_OK not ok")
                        }
                    } as HttpURLConnection
                } catch (e: Exception) {
                    Log.e("VideoPreviewFetch", "Error fetching video preview: ${e.message}")
                }
                delay(5000) // Wait for 5 seconds before the next fetch
            }
        }
    }



    LaunchedEffect(thisvideo.value) {
        withContext(Dispatchers.IO) {
            val bitmap: Bitmap? = if (thisvideo.value.videoLink.startsWith("https://") == true) {
                // For remote URLs
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(thisvideo.value.videoLink, HashMap())
                    val timeUs = 1_000L  // 1 millisecond into the video
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                } catch (e: Exception) {
                    Log.e("ThumbnailExtraction", "Failed to extract thumbnail", e)
                    null
                } finally {
                    retriever.release()
                }
            } else {
                // For local assets
                FileUtils.extractThumbnail(
                    context.assets.openFd("videos/${thisvideo.value.videoLink}"), 1
                )
            }
            bitmap?.let { bm ->
                withContext(Dispatchers.Main) {
                    thumbnail = thumbnail.copy(first = bm, second = thumbnail.second)
                }
            }
        }
    }


    if (pagerState.settledPage == pageIndex) {
            // Remember the ExoPlayer instance
            val exoPlayer = remember {
                ExoPlayer.Builder(context).build().apply {
                    videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    repeatMode = Player.REPEAT_MODE_ONE
                    playWhenReady = true
                    prepare()
                    addListener(object : Player.Listener {
                        override fun onRenderedFirstFrame() {
                            super.onRenderedFirstFrame()
                            isFirstFrameLoad = true
                            thumbnail = thumbnail.copy(second = false)
                        }
                    })
                }
            }

            // Update ExoPlayer's media item when videolink changes
            LaunchedEffect(thisvideo.value) {
                Log.d("LIFECYCLE", "updated!!! (theoretically)" + thisvideo.value.videoLink)
                exoPlayer.setMediaItem(
                    MediaItem.fromUri(Uri.parse(thisvideo.value.videoLink)), /* resetPosition */
                    true
                )
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                exoPlayer.play()
            }

            // Add Listener to exoPlayer to handle first frame rendered
            LaunchedEffect(key1 = exoPlayer) {
                exoPlayer.addListener(object : Player.Listener {
                    override fun onRenderedFirstFrame() {
                        super.onRenderedFirstFrame()
                        isFirstFrameLoad = true
                        thumbnail = thumbnail.copy(second = false)
                    }
                })
            }


        //val lifecycleOwner by rememberUpdatedState(LocalLifecycleOwner.current)
        DisposableEffect(key1 = lifecycleOwner) {
            val lifeCycleObserver = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        exoPlayer.pause()
                        onVideoGoBackground()
                    }
                    Lifecycle.Event.ON_START -> exoPlayer.play()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(lifeCycleObserver)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(lifeCycleObserver)
            }
        }

        val playerView = remember {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }

        DisposableEffect(key1 = AndroidView(factory = {
            playerView
        }, modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                onSingleTap(exoPlayer)
            }, onDoubleTap = { offset ->
                onDoubleTap(exoPlayer, offset)
            })
        }), effect = {
            onDispose {
                thumbnail = thumbnail.copy(second = true)
                exoPlayer.release()
                onVideoDispose()
            }
        })
    }

    if (thumbnail.second) {
        AsyncImage(
            model = thumbnail.first,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }

}


