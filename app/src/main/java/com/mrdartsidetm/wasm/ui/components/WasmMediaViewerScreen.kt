package com.mrdartsidetm.wasm.ui.components

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mrdartsidetm.wasm.util.GalleryDownloader
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class WasmViewerMediaType {
    PHOTO,
    VIDEO
}

data class WasmViewerMediaItem(
    val file: File,
    val type: WasmViewerMediaType,
    val title: String = file.name
)

/**
 * Dedicated WhatsApp-style pure-black full-screen media viewer page for Photos & Videos:
 * - Photos: pinch-to-zoom, pan, double-tap zoom, top bar with 3-dots -> Rotate & Save to Gallery.
 * - Videos: `mpvEx`-inspired wavy seekbar, play/pause, mute/unmute, speed pills (0.5x–2x),
 *   double-tap ±10s seek, zoom toggle, top bar with 3-dots -> ONLY Save to Gallery.
 */
@Composable
fun WasmMediaViewerScreen(
    item: WasmViewerMediaItem,
    onDismiss: () -> Unit,
    onConfirmation: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showChrome by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }

    // Photo state
    var targetRotationDeg by remember(item.file.absolutePath) { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotationDeg,
        animationSpec = spring(stiffness = 380f, dampingRatio = 0.78f),
        label = "photoViewerRotation"
    )
    var zoomScale by remember(item.file.absolutePath) { mutableFloatStateOf(1f) }
    var panOffset by remember(item.file.absolutePath) { mutableStateOf(Offset.Zero) }

    // Video state
    var isVideoPlaying by remember(item.file.absolutePath) { mutableStateOf(true) }
    var isMuted by remember(item.file.absolutePath) { mutableStateOf(false) }
    var playbackSpeed by remember(item.file.absolutePath) { mutableFloatStateOf(1.0f) }
    var currentPosMs by remember(item.file.absolutePath) { mutableIntStateOf(0) }
    var durationMs by remember(item.file.absolutePath) { mutableIntStateOf(0) }
    var seekBadgeText by remember { mutableStateOf<String?>(null) }

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    // Keep volume synced
    LaunchedEffect(isMuted, mediaPlayerRef) {
        val vol = if (isMuted) 0f else 1f
        try {
            mediaPlayerRef?.setVolume(vol, vol)
        } catch (_: Exception) {
        }
    }

    // Keep speed synced
    LaunchedEffect(playbackSpeed, mediaPlayerRef, isVideoPlaying) {
        try {
            if (isVideoPlaying) {
                mediaPlayerRef?.playbackParams = PlaybackParams().setSpeed(playbackSpeed)
            }
        } catch (_: Exception) {
        }
    }

    // Poll video playback position cleanly at 10Hz
    LaunchedEffect(item.file.absolutePath, item.type) {
        if (item.type != WasmViewerMediaType.VIDEO) return@LaunchedEffect
        while (isActive) {
            videoViewRef?.let { vv ->
                try {
                    if (vv.duration > 0) durationMs = vv.duration
                    currentPosMs = vv.currentPosition
                    isVideoPlaying = vv.isPlaying
                } catch (_: Exception) {
                }
            }
            delay(100)
        }
    }

    // Auto-clear double-tap seek feedback badge
    LaunchedEffect(seekBadgeText) {
        if (seekBadgeText != null) {
            delay(750)
            seekBadgeText = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler(onBack = onDismiss)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (item.type == WasmViewerMediaType.PHOTO) {
                val bitmap = remember(item.file.absolutePath) {
                    BitmapFactory.decodeFile(item.file.absolutePath)?.asImageBitmap()
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(item.file.absolutePath) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (zoomScale * zoom).coerceIn(1f, 5f)
                                zoomScale = nextScale
                                panOffset = if (nextScale > 1.01f) {
                                    panOffset + pan
                                } else {
                                    Offset.Zero
                                }
                            }
                        }
                        .pointerInput(item.file.absolutePath) {
                            detectTapGestures(
                                onTap = { showChrome = !showChrome },
                                onDoubleTap = {
                                    if (zoomScale > 1.15f) {
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    } else {
                                        zoomScale = 2.5f
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    bitmap?.let { img ->
                        Image(
                            bitmap = img,
                            contentDescription = item.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoomScale
                                    scaleY = zoomScale
                                    translationX = panOffset.x
                                    translationY = panOffset.y
                                    rotationZ = animatedRotation
                                }
                        )
                    }
                }
            } else {
                // Video Surface on Pure Black Canvas
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(item.file.absolutePath) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (zoomScale * zoom).coerceIn(1f, 3f)
                                zoomScale = nextScale
                                panOffset = if (nextScale > 1.01f) panOffset + pan else Offset.Zero
                            }
                        }
                        .pointerInput(item.file.absolutePath) {
                            detectTapGestures(
                                onTap = { showChrome = !showChrome },
                                onDoubleTap = { tapOffset ->
                                    val vv = videoViewRef ?: return@detectTapGestures
                                    val isRightHalf = tapOffset.x >= size.width / 2f
                                    val delta = if (isRightHalf) 10_000 else -10_000
                                    val next = (vv.currentPosition + delta).coerceIn(0, vv.duration.coerceAtLeast(0))
                                    vv.seekTo(next)
                                    currentPosMs = next
                                    seekBadgeText = if (isRightHalf) "+10s" else "-10s"
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            FrameLayout(ctx).apply {
                                val vv = VideoView(ctx).apply {
                                    layoutParams = FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        Gravity.CENTER
                                    )
                                    setVideoURI(Uri.fromFile(item.file))
                                    setOnPreparedListener { mp ->
                                        mediaPlayerRef = mp
                                        durationMs = mp.duration
                                        val vol = if (isMuted) 0f else 1f
                                        mp.setVolume(vol, vol)
                                        mp.isLooping = false
                                        start()
                                        isVideoPlaying = true
                                    }
                                    setOnCompletionListener {
                                        isVideoPlaying = false
                                        showChrome = true
                                    }
                                }
                                videoViewRef = vv
                                addView(vv)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = panOffset.x
                                translationY = panOffset.y
                            }
                    )

                    // Double-tap seek feedback pill
                    AnimatedVisibility(
                        visible = seekBadgeText != null,
                        enter = scaleIn(initialScale = 0.8f) + fadeIn(tween(120)),
                        exit = scaleOut(targetScale = 0.8f) + fadeOut(tween(150))
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.72f),
                            contentColor = Color.White
                        ) {
                            Text(
                                text = seekBadgeText ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            // Top Chrome Bar (Back, Title, 3-dots)
            AnimatedVisibility(
                visible = showChrome,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(160)),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    if (item.type == WasmViewerMediaType.VIDEO) {
                        IconButton(onClick = { isMuted = !isMuted }) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = Color.White
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (item.type == WasmViewerMediaType.PHOTO) {
                                DropdownMenuItem(
                                    text = { Text("Rotate") },
                                    leadingIcon = { Icon(Icons.Default.RotateRight, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        targetRotationDeg += 90f
                                    }
                                )
                            }
                            // Both Photo and Video support Save to Gallery (and ONLY Save to Gallery for Video)
                            DropdownMenuItem(
                                text = { Text("Save to Gallery") },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    scope.launch {
                                        val ok = if (item.type == WasmViewerMediaType.PHOTO) {
                                            GalleryDownloader.saveImageToGallery(
                                                context,
                                                item.file,
                                                targetRotationDeg.toInt()
                                            )
                                        } else {
                                            GalleryDownloader.saveToGallery(context, item.file, mediaType = "video")
                                        }
                                        onConfirmation(if (ok) "Saved to Gallery" else "Could not save media")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Bottom Video Controls (`mpvEx`-style seekbar, Play/Pause, Mute, Speed)
            AnimatedVisibility(
                visible = showChrome && item.type == WasmViewerMediaType.VIDEO,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(160)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val progress = if (durationMs > 0) (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = formatMediaDuration(currentPosMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        WasmSquigglySeekbar(
                            progress = progress,
                            isPlaying = isVideoPlaying,
                            activeColor = MaterialTheme.colorScheme.primary,
                            inactiveColor = Color.White.copy(alpha = 0.28f),
                            thumbColor = Color.White,
                            onSeekFraction = { frac ->
                                val target = (frac * durationMs).toInt()
                                videoViewRef?.seekTo(target)
                                currentPosMs = target
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatMediaDuration(durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speed cycle pill (`0.5x`, `1x`, `1.5x`, `2x`)
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier.clickable {
                                playbackSpeed = when (playbackSpeed) {
                                    1.0f -> 1.5f
                                    1.5f -> 2.0f
                                    2.0f -> 0.5f
                                    else -> 1.0f
                                }
                            }
                        ) {
                            Text(
                                text = "${if (playbackSpeed % 1f == 0f) playbackSpeed.toInt() else playbackSpeed}x",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    videoViewRef?.let { vv ->
                                        val next = (vv.currentPosition - 10_000).coerceAtLeast(0)
                                        vv.seekTo(next)
                                        currentPosMs = next
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White)
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clickable {
                                        val vv = videoViewRef ?: return@clickable
                                        if (vv.isPlaying) {
                                            vv.pause()
                                            isVideoPlaying = false
                                        } else {
                                            vv.start()
                                            isVideoPlaying = true
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isVideoPlaying) "Pause" else "Play",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    videoViewRef?.let { vv ->
                                        val next = (vv.currentPosition + 10_000).coerceAtMost(vv.duration.coerceAtLeast(0))
                                        vv.seekTo(next)
                                        currentPosMs = next
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White)
                            }
                        }

                        // Zoom/Fit toggle
                        IconButton(
                            onClick = {
                                if (zoomScale > 1.1f) {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                } else {
                                    zoomScale = 1.6f
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (zoomScale > 1.1f) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Toggle Zoom",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatMediaDuration(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return "$mins:%02d".format(secs)
}
