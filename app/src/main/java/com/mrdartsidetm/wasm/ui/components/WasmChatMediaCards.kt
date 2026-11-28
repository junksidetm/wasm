package com.mrdartsidetm.wasm.ui.components

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.media.MediaMetadataRetriever
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdartsidetm.wasm.ui.motion.performWasmTickHaptic
import com.mrdartsidetm.wasm.ui.motion.wasmLuminousElasticClickable
import com.mrdartsidetm.wasm.ui.surfaceContainerHigh
import com.mrdartsidetm.wasm.util.AttachmentOpenHelper
import com.mrdartsidetm.wasm.util.AudioPlayerManager
import com.mrdartsidetm.wasm.util.GalleryDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Renders animated or static WebP Stickers & GIFs inline.
 * Long-pressing opens the download menu to save straight into `Downloads/!WASM/`.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WasmInlineStickerOrGif(
    file: File,
    isSticker: Boolean,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onConfirmation: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    var showDownloadMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        val boxMod = if (isSticker) {
            Modifier
                .sizeIn(maxWidth = 148.dp, maxHeight = 148.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        view.performWasmTickHaptic(true)
                        showDownloadMenu = true
                    }
                )
        } else {
            Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 210.dp)
                .clip(RoundedCornerShape(10.dp))
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        view.performWasmTickHaptic(true)
                        showDownloadMenu = true
                    }
                )
        }

        Box(modifier = boxMod) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                AndroidView(
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            scaleType = if (isSticker) ImageView.ScaleType.FIT_CENTER else ImageView.ScaleType.CENTER_CROP
                            adjustViewBounds = true
                        }
                    },
                    update = { iv ->
                        try {
                            val source = ImageDecoder.createSource(file)
                            val drawable = ImageDecoder.decodeDrawable(source)
                            iv.setImageDrawable(drawable)
                            if (drawable is AnimatedImageDrawable) {
                                drawable.start()
                            }
                        } catch (_: Exception) {
                            val bmp = BitmapFactory.decodeFile(file.absolutePath)
                            iv.setImageBitmap(bmp)
                        }
                    },
                    modifier = if (isSticker) Modifier.size(144.dp) else Modifier.fillMaxWidth().height(175.dp)
                )
            } else {
                val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = file.absolutePath) {
                    value = withContext(Dispatchers.IO) {
                        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                    }
                }
                bitmap?.let { img ->
                    Image(
                        bitmap = img,
                        contentDescription = file.name,
                        contentScale = if (isSticker) ContentScale.Fit else ContentScale.Crop,
                        modifier = if (isSticker) Modifier.size(144.dp) else Modifier.fillMaxWidth().height(175.dp)
                    )
                }
            }

            if (!isSticker) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.62f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "GIF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showDownloadMenu,
            onDismissRequest = { showDownloadMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Save to Downloads (!WASM)") },
                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                onClick = {
                    showDownloadMenu = false
                    val ok = GalleryDownloader.saveToWasmDownloads(context, file)
                    onConfirmation(if (ok) "Saved to Downloads/!WASM" else "Download failed")
                }
            )
        }
    }
}

/**
 * Native inline Voice Note / Audio bubble powered by [AudioPlayerManager], featuring:
 * - Elastic Play/Pause toggle
 * - `mpvEx`-inspired animated `WasmSquigglySeekbar` while playing
 * - Elapsed / total duration + `1x / 1.5x / 2x` speed toggle
 * - Long-press menu to save directly to `Downloads/!WASM/`
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WasmInlineAudioBubble(
    file: File,
    audioPlayerManager: AudioPlayerManager,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    onConfirmation: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val currentPath by audioPlayerManager.currentPlayingPath.collectAsStateWithLifecycle()
    val isPlayingGlobal by audioPlayerManager.isPlaying.collectAsStateWithLifecycle()
    val currentPosGlobal by audioPlayerManager.currentPositionMs.collectAsStateWithLifecycle()
    val durationGlobal by audioPlayerManager.durationMs.collectAsStateWithLifecycle()
    val speed by audioPlayerManager.playbackSpeed.collectAsStateWithLifecycle()

    val isThisFileActive = currentPath == file.absolutePath
    val isThisPlaying = isThisFileActive && isPlayingGlobal

    val fileDurationMs by produceState(initialValue = 0, key1 = file.absolutePath) {
        value = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull() ?: 0
            } catch (_: Exception) {
                0
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    val effectiveDuration = if (isThisFileActive && durationGlobal > 0) durationGlobal else fileDurationMs
    val effectivePos = if (isThisFileActive) currentPosGlobal else 0
    val progress = if (effectiveDuration > 0) (effectivePos.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f) else 0f

    var showDownloadMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = containerColor,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .combinedClickable(
                    onClick = { audioPlayerManager.togglePlay(file) },
                    onLongClick = {
                        view.performWasmTickHaptic(true)
                        showDownloadMenu = true
                    }
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor,
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { audioPlayerManager.togglePlay(file) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isThisPlaying) "Pause" else "Play voice note",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    WasmSquigglySeekbar(
                        progress = progress,
                        isPlaying = isThisPlaying,
                        activeColor = accentColor,
                        inactiveColor = contentColor.copy(alpha = 0.22f),
                        thumbColor = accentColor,
                        waveAmplitudeDp = 2.6.dp,
                        wavelengthDp = 18.dp,
                        strokeWidthDp = 3.dp,
                        onSeekFraction = { frac ->
                            if (!isThisFileActive) {
                                audioPlayerManager.togglePlay(file)
                            }
                            if (effectiveDuration > 0) {
                                audioPlayerManager.seekTo((frac * effectiveDuration).toInt())
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isThisFileActive) {
                                "${formatMediaDuration(effectivePos)} / ${formatMediaDuration(effectiveDuration)}"
                            } else {
                                formatMediaDuration(effectiveDuration)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.72f)
                        )

                        if (isThisFileActive) {
                            Surface(
                                shape = CircleShape,
                                color = accentColor.copy(alpha = 0.16f),
                                modifier = Modifier.clickable { audioPlayerManager.cycleSpeed() }
                            ) {
                                Text(
                                    text = "${if (speed % 1f == 0f) speed.toInt() else speed}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showDownloadMenu,
            onDismissRequest = { showDownloadMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Save to Downloads (!WASM)") },
                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                onClick = {
                    showDownloadMenu = false
                    val ok = GalleryDownloader.saveToWasmDownloads(context, file)
                    onConfirmation(if (ok) "Saved to Downloads/!WASM" else "Download failed")
                }
            )
        }
    }
}

/**
 * Clean thumbnail card for inline videos inside chat bubbles; tapping opens [WasmMediaViewerScreen].
 */
@Composable
fun WasmInlineVideoCard(
    file: File,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbnail by produceState<ImageBitmap?>(initialValue = null, key1 = file.absolutePath) {
        value = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                retriever.getFrameAtTime(0)?.asImageBitmap()
            } catch (_: Exception) {
                null
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .wasmLuminousElasticClickable(
                shape = RoundedCornerShape(10.dp),
                pressedScale = 0.97f,
                onClick = onClick
            )
            .background(Color(0xFF141B20)),
        contentAlignment = Alignment.Center
    ) {
        thumbnail?.let { bmp ->
            Image(
                bitmap = bmp,
                contentDescription = file.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.62f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play video",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Clickable external file attachment card for non-media files (.pdf, .apk, .zip, .vcf, documents).
 * Opens strictly via external Android apps using [AttachmentOpenHelper.openFileExternally].
 */
@Composable
fun WasmExternalFileCard(
    file: File,
    fileName: String,
    sizeText: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onConfirmation: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ext = fileName.substringAfterLast('.', "").lowercase()
    val icon = when (ext) {
        "pdf" -> Icons.Default.PictureAsPdf
        "apk" -> Icons.Default.Android
        "zip", "rar", "7z" -> Icons.Default.FolderZip
        "vcf" -> Icons.Default.ContactPage
        else -> Icons.Default.InsertDriveFile
    }
    val badgeColor = when (ext) {
        "pdf" -> Color(0xFFE53935)
        "apk" -> Color(0xFF2E7D32)
        "zip", "rar", "7z" -> Color(0xFFFB8C00)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = modifier
            .fillMaxWidth()
            .wasmLuminousElasticClickable(
                shape = RoundedCornerShape(12.dp),
                pressedScale = 0.97f,
                onClick = {
                    val launched = AttachmentOpenHelper.openFileExternally(context, file)
                    if (!launched) {
                        onConfirmation("No app found to open .$ext file")
                    }
                }
            )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = badgeColor.copy(alpha = 0.16f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        if (sizeText.isNotBlank()) append("$sizeText • ")
                        if (ext.isNotBlank()) append("${ext.uppercase()} • ")
                        append("Tap to open externally")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.68f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open externally",
                tint = contentColor.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
