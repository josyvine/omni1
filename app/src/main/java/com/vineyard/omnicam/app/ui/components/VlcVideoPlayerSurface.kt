package com.vineyard.omnicam.app.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.vineyard.omnicam.app.core.theme.CyanAccent
import com.vineyard.omnicam.app.core.theme.EmeraldLive
import com.vineyard.omnicam.app.core.theme.RoseAlert
import com.vineyard.omnicam.app.data.models.CameraEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun VlcVideoPlayerSurface(
    camera: CameraEntity,
    modifier: Modifier = Modifier,
    isFocusMode: Boolean = false,
    onPtzToggle: (() -> Unit)? = null,
    onFullscreenToggle: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val timeString = remember {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    val exoPlayer = remember(camera.id) {
        ExoPlayer.Builder(context)
            .setRenderersFactory(
                DefaultRenderersFactory(context).setEnableDecoderFallback(true)
            )
            .build().apply {
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_READY -> {
                                isPlaying = true
                                isLoading = false
                                hasError = false
                            }
                            Player.STATE_BUFFERING -> {
                                isLoading = true
                            }
                            Player.STATE_ENDED -> {
                                isPlaying = false
                            }
                            Player.STATE_IDLE -> {}
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        hasError = true
                        isLoading = false
                        isPlaying = false
                    }
                })

                val fullUrl = camera.getFullRtspUrl()
                val mediaItem = MediaItem.fromUri(fullUrl)
                val mediaSource = RtspMediaSource.Factory()
                    .setForceUseRtpTcp(true)
                    .createMediaSource(mediaItem)
                setMediaSource(mediaSource)
                prepare()
            }
    }

    DisposableEffect(camera.id) {
        onDispose {
            // Immediately stop RTSP TCP socket and release hardware decoders
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (isFocusMode) 0.dp else 16.dp))
            .background(Color.Black)
            .border(
                1.dp,
                if (camera.isOnline) CyanAccent.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(if (isFocusMode) 0.dp else 16.dp)
            )
            .testTag("player_surface_${camera.id}")
    ) {
        if (!hasError) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                onRelease = { playerView ->
                    playerView.player = null
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Snapshot preview fallback if buffering or stream is demo
        if (isLoading || hasError) {
            camera.previewImageUrl?.let { previewUrl ->
                AsyncImage(
                    model = previewUrl,
                    contentDescription = camera.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.85f)
                )
            }
        }

        // Top In-Tile Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (camera.isOnline) EmeraldLive.copy(alpha = pulseAlpha) else RoseAlert)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (camera.isOnline) "LIVE" else "OFFLINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (camera.isOnline) EmeraldLive else RoseAlert
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = camera.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "24ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent
                )
                if (onPtzToggle != null && camera.hasPtz) {
                    IconButton(
                        onClick = onPtzToggle,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("tile_ptz_${camera.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ControlCamera,
                            contentDescription = "PTZ Quick Toggle",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (onFullscreenToggle != null) {
                    IconButton(
                        onClick = onFullscreenToggle,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("tile_fullscreen_${camera.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Bottom In-Tile Overlay: Brand + Protocol + Resolution
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${camera.brand} • ${camera.mode}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray
            )
            Text(
                text = "$timeString | 1080p60",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        // Loading Spinner
        if (isLoading && !hasError) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center),
                color = CyanAccent,
                strokeWidth = 3.dp
            )
        }

        // Reconnect overlay if disconnected
        if (hasError) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.VideocamOff,
                    contentDescription = "Offline",
                    tint = RoseAlert,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Direct P2P Reconnecting...",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                IconButton(
                    onClick = {
                        hasError = false
                        isLoading = true
                        exoPlayer.prepare()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry Stream",
                        tint = CyanAccent
                    )
                }
            }
        }
    }
}