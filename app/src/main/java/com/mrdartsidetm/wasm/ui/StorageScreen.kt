package com.mrdartsidetm.wasm.ui

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdartsidetm.wasm.data.UserPreferencesRepository
import com.mrdartsidetm.wasm.ui.instagram.InstagramViewModel
import com.mrdartsidetm.wasm.ui.localization.LocalWasmStrings
import com.mrdartsidetm.wasm.ui.motion.WasmMechanicalText
import com.mrdartsidetm.wasm.ui.motion.WasmMotionTokens
import com.mrdartsidetm.wasm.ui.motion.performWasmTickHaptic
import com.mrdartsidetm.wasm.ui.motion.rememberAnimatorsEnabled
import com.mrdartsidetm.wasm.ui.motion.rememberLazyListHeaderFalloff
import com.mrdartsidetm.wasm.ui.motion.wasmFeatheredVerticalViewport
import com.mrdartsidetm.wasm.ui.motion.wasmLuminousElasticClickable
import com.mrdartsidetm.wasm.ui.theme.GoogleSansFlexOnboarding535
import com.mrdartsidetm.wasm.ui.theme.GoogleSansFlexOnboarding875
import com.mrdartsidetm.wasm.util.StorageMediaCategory
import com.mrdartsidetm.wasm.util.StorageMediaHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min

// Decorative dashboard shapes with "- Move" axis rotation (`Storage Screen.svg`)
private const val STORAGE_DECOR_BURST_PATH =
    "M 73.95 39.93 C 79.86 50.96 75.39 60.94 63.98 63.98 C 60.94 75.39 50.96 79.86 39.93 73.95 C 28.9 79.86 18.92 75.39 15.88 63.98 C 4.47 60.94 0 50.96 5.91 39.93 C 0 28.9 4.47 18.92 15.88 15.88 C 18.92 4.47 28.9 0 39.93 5.91 C 50.96 0 60.94 4.47 63.98 15.88 C 75.39 18.92 79.86 28.9 73.95 39.93 Z"

private const val STORAGE_DECOR_CAPSULE_PATH =
    "M 21.27 11.59 C 32.86 0 51.64 0 63.23 11.59 C 74.82 23.18 74.82 41.96 63.23 53.55 L 53.55 63.23 C 41.96 74.82 23.18 74.82 11.59 63.23 C 0 51.64 0 32.86 11.59 21.27 L 21.27 11.59 Z"

// Exact normalized SVG icon paths from `assests/wireframes/main/Storage Screen.svg`
private val STORAGE_IG_PATHS = listOf(
    "M13.25 6.03 C17.24 6.03 20.47 9.26 20.47 13.25 C20.47 17.24 17.24 20.47 13.25 20.47 C9.26 20.47 6.02 17.24 6.02 13.25 C6.02 9.26 9.26 6.03 13.25 6.03 ZM13.25 8.43 C10.59 8.43 8.43 10.59 8.43 13.25 C8.43 15.91 10.59 18.07 13.25 18.07 C15.91 18.07 18.06 15.91 18.06 13.25 C18.06 10.59 15.91 8.43 13.25 8.43 Z" to true,
    "M20.47 4.82 C21.14 4.82 21.68 5.36 21.68 6.03 C21.68 6.69 21.14 7.23 20.47 7.23 C19.81 7.23 19.27 6.69 19.27 6.03 C19.27 5.36 19.81 4.82 20.47 4.82 Z" to false,
    "M14.93 0 C18.98 0 21 0 22.55 0.79 C23.91 1.48 25.01 2.59 25.7 3.95 C26.49 5.49 26.49 7.52 26.49 11.56 V14.94 C26.49 18.98 26.49 21.01 25.7 22.55 C25.01 23.91 23.91 25.02 22.55 25.71 C21 26.5 18.98 26.5 14.93 26.5 H11.56 C7.51 26.5 5.49 26.5 3.94 25.71 C2.59 25.02 1.48 23.91 0.79 22.55 C0 21.01 0 18.98 0 14.94 V11.56 C0 7.52 0 5.49 0.79 3.95 C1.48 2.59 2.59 1.48 3.94 0.79 C5.49 0 7.51 0 11.56 0 H14.93 ZM11.56 2.41 C9.5 2.41 8.09 2.41 7.01 2.5 C5.95 2.59 5.41 2.75 5.04 2.94 C4.13 3.4 3.39 4.14 2.93 5.04 C2.74 5.42 2.58 5.96 2.5 7.01 C2.41 8.1 2.41 9.5 2.41 11.56 V14.94 C2.41 17 2.41 18.4 2.5 19.49 C2.58 20.54 2.74 21.08 2.93 21.46 C3.39 22.36 4.13 23.1 5.04 23.56 C5.41 23.75 5.95 23.91 7.01 24 C8.09 24.09 9.5 24.09 11.56 24.09 H14.93 C16.99 24.09 18.4 24.09 19.48 24 C20.54 23.91 21.08 23.75 21.45 23.56 C22.36 23.1 23.1 22.36 23.56 21.46 C23.75 21.08 23.91 20.54 23.99 19.49 C24.08 18.4 24.08 17 24.08 14.94 V11.56 C24.08 9.5 24.08 8.1 23.99 7.01 C23.91 5.96 23.75 5.42 23.56 5.04 C23.1 4.14 22.36 3.4 21.45 2.94 C21.08 2.75 20.54 2.59 19.48 2.5 C18.4 2.41 16.99 2.41 14.93 2.41 H11.56 Z" to true
)

private val STORAGE_WA_PATHS = listOf(
    "M22.93 3.92 C20.51 1.5 17.17 0 13.48 0 C6.1 0 0.12 5.99 0.12 13.37 C0.12 15.82 0.78 18.12 1.93 20.1 L1.9 20.04 L0 26.96 L7.09 25.1 C8.93 26.13 11.13,26.73 13.47 26.73 H13.48 C20.86 26.73 26.84 20.74 26.84 13.36 C26.84 9.68 25.34 6.34 22.93 3.92 L22.93 3.92 ZM13.48 24.47 H13.47 C11.39 24.47 9.44 23.9 7.77 22.9 L7.82 22.93 L7.42 22.69 L3.21 23.79 L4.33 19.69 L4.07 19.27 C3 17.6 2.37 15.56 2.37 13.37 C2.37 7.24 7.34 2.26 13.48 2.26 C19.61 2.26 24.58 7.24 24.58 13.37 C24.58 19.5 19.61 24.47 13.48 24.47 ZM19.57 16.16 C19.24 15.99 17.59 15.18 17.29 15.07 C16.98 14.96 16.76 14.9 16.54 15.24 C16.31 15.57 15.67 16.32 15.48 16.55 C15.28 16.77 15.09 16.8 14.76 16.63 C13.72 16.21 12.83 15.65 12.06 14.97 L12.07 14.97 C11.35 14.31 10.74 13.55 10.24 12.71 L10.21 12.66 C10.02 12.33 10.19 12.15 10.36 11.98 C10.51 11.83 10.69 11.59 10.86 11.4 C10.99 11.24 11.1 11.05 11.19 10.85 L11.19 10.84 C11.23 10.76 11.26 10.67 11.26 10.57 C11.26 10.45 11.22 10.35 11.16 10.25 L11.17 10.25 C11.08 10.09 10.42 8.44 10.14 7.77 C9.87 7.12 9.59 7.21 9.39 7.2 C9.19 7.19 8.97 7.19 8.75 7.19 C8.39 7.2 8.07 7.36 7.86 7.61 L7.86 7.61 C7.14 8.29 6.69 9.26 6.69 10.33 C6.69 10.35 6.69 10.37 6.69 10.4 V10.39 C6.81 11.71 7.3 12.9 8.06 13.86 L8.05 13.85 C9.49 15.99 11.41 17.7 13.67 18.85 L13.75 18.89 C14.25 19.11 14.88 19.35 15.52 19.55 L15.66 19.59 C16.06 19.72 16.51 19.79 16.99 19.79 C17.26 19.79 17.53 19.77 17.79 19.72 L17.76 19.72 C18.72 19.52 19.52 18.94 20 18.15 L20.01 18.14 C20.16 17.81 20.25 17.42 20.25 17.02 C20.25 16.85 20.23 16.69 20.2 16.53 L20.21 16.55 C20.12 16.41 19.9 16.33 19.57 16.16 L19.57 16.16 Z" to false
)

private val STORAGE_VOICE_PATHS = listOf(
    "M19.29 12.27 C20.54 12.27 20.54 13.5 20.54 13.5 C20.54 13.52 21.11 21.4 11.8 22.05 V25.77 C11.8 26.45 11.24 27 10.55 27 C9.86 27 9.3 26.45 9.3 25.77 V22.05 C0 21.4 0.56 13.52 0.56 13.5 C0.56 13.5 0.56 12.27 1.81 12.27 C3.06 12.27 3.06 13.5 3.06 13.5 C3.06 13.5 2.43 19.64 10.55 19.64 C18.65 19.64 18.05 13.52 18.05 13.5 C18.05 13.5 18.04 12.27 19.29 12.27 Z" to false,
    "M10.55 0 C13.65 0 16.17 2.47 16.17 5.52 V11.66 C16.17 14.71 13.65 17.18 10.55 17.18 C7.45 17.18 4.93 14.71 4.93 11.66 V5.52 C4.93 2.47 7.45 0 10.55 0 ZM10.55 2.45 C8.83 2.45 7.43 3.83 7.43 5.52 V11.66 C7.43 13.35 8.83 14.73 10.55 14.73 C12.28 14.73 13.67 13.35 13.67 11.66 V5.52 C13.67 3.83 12.28 2.45 10.55 2.45 Z" to true
)

private val STORAGE_IMAGES_PATHS = listOf(
    "M2.67 24 H21.33 C22.81 24 24 22.81 24 21.34 V2.67 C24 1.2 22.81 0 21.33 0 H2.67 C1.19 0 0 1.2 0 2.67 V21.34 C0 22.81 1.19 24 2.67 24 ZM2.67 24 L17.33 9.34 L24 16 M9.33 7.34 C9.33 8.44 8.44 9.34 7.33 9.34 C6.23 9.34 5.33 8.44 5.33 7.34 C5.33 6.23 6.23 5.34 7.33 5.34 C8.44 5.34 9.33 6.23 9.33 7.34 Z" to false
)

private val STORAGE_VIDEO_PATHS = listOf(
    "M17.54 0 C19.52 0 21.12 1.6 21.12 3.58 V7.89 L28.34 2.73 C28.66 2.5 29.08 2.47 29.43 2.65 C29.78 2.83 30 3.19 30 3.58 V16.27 C30 16.66 29.78 17.02 29.43 17.2 C29.08 17.38 28.66 17.35 28.34 17.12 L21.12 11.96 V16.27 C21.12 18.25 19.52 19.85 17.54 19.85 H3.59 C1.61 19.85 0 18.25 0 16.27 V3.58 C0 1.6 1.61 0 3.59 0 H17.54 ZM3.59 2.1 C2.77 2.1 2.1 2.76 2.1 3.58 V16.27 C2.1 17.09 2.77 17.75 3.59 17.75 H17.54 C18.36 17.75 19.02 17.09 19.02 16.27 V3.58 C19.02 2.76 18.36 2.1 17.54 2.1 H3.59 ZM21.88 9.93 L27.9 14.23 V5.62 L21.88 9.93 Z" to true
)

private val STORAGE_GIF_PATHS = listOf(
    "M11.06 6.71 C11.65 6.71 12.23 6.8 12.8 6.97 C13.46 7.18 13.83 7.89 13.62 8.55 C13.41 9.21 12.71 9.58 12.04 9.38 C11.73 9.28 11.41 9.23 11.08 9.23 C9.07 9.29 7.5 10.96 7.55 12.97 C7.55 12.99 7.55 13.01 7.55 13.03 C7.5 15.04 9.07 16.71 11.08 16.77 C11.4 16.77 11.72 16.72 12.02 16.63 C13.16 16.24 14.01 15.35 14.38 14.26 H11.06 C10.36 14.26 9.8 13.69 9.8 13 C9.8 12.3 10.36 11.74 11.06 11.74 H15.83 L15.95 11.75 C16.58 11.81 17.07 12.34 17.08 12.98 C17.13 15.7 15.4 18.14 12.82 19.02 L12.4 17.78 L12.42 17.82 L12.82 19.02 C12.81 19.02 12.81 19.02 12.8 19.02 C12.23 19.2 11.65 19.29 11.06 19.29 C11.04 19.29 11.03 19.29 11.02 19.29 C7.73 19.2 5.11 16.54 5.03 13.28 V12.72 C5.11 9.46 7.73 6.8 11.02 6.71 C11.03 6.71 11.04 6.71 11.06 6.71 Z" to false,
    "M17.75 18.03 V7.97 C17.75 7.27 18.31 6.71 19 6.71 C19.7 6.71 20.26 7.27 20.26 7.97 V18.03 C20.26 18.73 19.7 19.29 19 19.29 C18.31 19.29 17.75 18.73 17.75 18.03 Z" to false,
    "M23.04 18.03 V13.7 H22.18 C21.49 13.7 20.92 13.14 20.92 12.44 C20.92 11.75 21.49 11.18 22.18 11.18 H23.04 V10.2 C23.01 8.32 24.5 6.76 26.38 6.71 C27.08 6.69 27.66 7.24 27.68 7.93 C27.7 8.63 27.15 9.21 26.45 9.23 C25.95 9.24 25.54 9.66 25.56 10.17 C25.56 10.18 25.56 10.19 25.56 10.2 V11.18 H26.42 C27.11 11.18 27.68 11.75 27.68 12.44 C27.68 13.14 27.11 13.7 26.42 13.7 H25.56 V18.03 C25.56 18.73 25 19.29 24.3 19.29 C23.61 19.29 23.04 18.73 23.04 18.03 Z" to false,
    "M24.74 0 C29.14 0 32.71 3.57 32.71 7.97 V18.03 C32.71 22.43 29.14 26 24.74 26 H7.97 C3.57 26 0 22.43 0 18.03 V7.97 C0 3.57 3.57 0 7.97 0 H24.74 ZM7.97 2.52 C4.96 2.52 2.52 4.96 2.52 7.97 V18.03 C2.52 21.04 4.96 23.48 7.97 23.48 H24.74 C27.75 23.48 30.19 21.04 30.19 18.03 V7.97 C30.19 4.96 27.75 2.52 24.74 2.52 H7.97 Z" to true
)

private val STORAGE_STICKER_PATHS = listOf(
    "M19.5 0 C23.09 0 26 2.91 26 6.5 V13.72 C26 20.5 20.5 26 13.72 26 H6.5 C2.91 26 0 23.09 0 19.5 V6.5 C0 2.91 2.91 0 6.5 0 H19.5 ZM6.5 1.44 C3.71 1.44 1.44 3.71 1.44 6.5 V19.5 C1.44 22.29 3.71 24.56 6.5 24.56 H13.72 C14.92 24.56 15.89 23.59 15.89 22.39 V19.5 C15.89 16.71 18.15 14.44 20.94 14.44 H22.39 C23.59 14.44 24.56 13.47 24.56 12.28 V6.5 C24.56 3.71 22.29 1.44 19.5 1.44 H6.5 ZM24.45 15.24 C23.87 15.65 23.16 15.89 22.39 15.89 H20.94 C18.95 15.89 17.33 17.51 17.33 19.5 V22.39 C17.33 23 17.18 23.57 16.92 24.08 C20.87 22.86 23.86 19.44 24.45 15.24 Z" to true
)

// Category accent colors shared between row icons and Storage Pill segments
private val ColorInstagram = Color(0xFFE1306C)
private val ColorWhatsApp = Color(0xFF4CAF50)
private val ColorVoice = Color(0xFFF59E0B)
private val ColorImages = Color(0xFF2196F3)
private val ColorVideo = Color(0xFF8B5CF6)
private val ColorGif = Color(0xFF14B8A6)
private val ColorSticker = Color(0xFFEC4899)

private data class StorageDiskMetrics(
    val dbBytes: Long = 0L,
    val waMediaBytes: Long = 0L,
    val igMediaBytes: Long = 0L,
    val voiceCount: Int = 0,
    val voiceBytes: Long = 0L,
    val imageCount: Int = 0,
    val imageBytes: Long = 0L,
    val videoCount: Int = 0,
    val videoBytes: Long = 0L,
    val gifCount: Int = 0,
    val gifBytes: Long = 0L,
    val stickerCount: Int = 0,
    val stickerBytes: Long = 0L,
    val deviceFreeBytes: Long = 0L,
    val deviceTotalBytes: Long = 1L
)

private data class StorageSvgIconSpec(
    val srcWidth: Float,
    val srcHeight: Float,
    val paths: List<Pair<String, Boolean>>,
    val isStroked: Boolean = false,
    val strokeWidth: Float = 2.48889f
)

private data class StorageRowUi(
    val id: String,
    val countText: String,
    val categoryLabel: String,
    val sizeText: String,
    val color: Color,
    val iconSpec: StorageSvgIconSpec
)

private data class StoragePillSegment(
    val id: String,
    val color: Color,
    val bytes: Long
)

/**
 * Material 3 Expressive Storage Screen matching `assests/wireframes/main/Storage Screen.svg`:
 * - Exact SVG icon paths for Instagram, WhatsApp, Voice, Images, Video, GIF, and Sticker
 * - Consistent color-coding across each row icon and the segmented Storage Pill bar
 * - Continuous center-axis rotation for `- Move` dashboard shapes (`8 leaf Clover - Move`, `Pill - Move`)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StorageScreen(
    whatsappViewModel: ChatViewModel,
    instagramViewModel: InstagramViewModel,
    prefs: UserPreferencesRepository,
    onOpenImport: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onOpenStorageData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val animatorsEnabled = rememberAnimatorsEnabled()
    val strings = LocalWasmStrings.current

    val waConversations by whatsappViewModel.conversations.collectAsStateWithLifecycle()
    val waTotalMessages by whatsappViewModel.totalMessageCount.collectAsStateWithLifecycle(initialValue = 0)
    val igConversations by instagramViewModel.conversations.collectAsStateWithLifecycle()
    val igTotalMessages = remember(igConversations) { igConversations.sumOf { it.messageCount } }
    val totalMessages = waTotalMessages + igTotalMessages

    var showOverflowMenu by remember { mutableStateOf(false) }
    var isPersonalizeOpen by rememberSaveable { mutableStateOf(false) }
    var highlightedCategoryId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val headerFalloff by rememberLazyListHeaderFalloff(listState)

    var diskMetrics by remember { mutableStateOf(StorageDiskMetrics()) }
    LaunchedEffect(waTotalMessages, igTotalMessages, waConversations.size, igConversations.size) {
        diskMetrics = withContext(Dispatchers.IO) {
            scanStorageMetrics(
                context = context,
                waDir = whatsappViewModel.mediaDir,
                igDir = instagramViewModel.instagramBaseDir
            )
        }
    }

    val numFmt = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val formattedTotal = remember(totalMessages, numFmt) {
        numFmt.format(totalMessages)
    }

    val appUsedBytes = diskMetrics.dbBytes + diskMetrics.waMediaBytes + diskMetrics.igMediaBytes
    val totalMsgCountSafe = totalMessages.coerceAtLeast(1).toFloat()
    val waDbShare = (diskMetrics.dbBytes * (waTotalMessages / totalMsgCountSafe)).toLong()
    val igDbShare = (diskMetrics.dbBytes * (igTotalMessages / totalMsgCountSafe)).toLong()

    val waTotalBytes = waDbShare + diskMetrics.waMediaBytes
    val igTotalBytes = igDbShare + diskMetrics.igMediaBytes

    // Color-coded segments across all 7 categories (DB message shares + per-media totals)
    val rawSegments = remember(
        igDbShare,
        waDbShare,
        diskMetrics.voiceBytes,
        diskMetrics.imageBytes,
        diskMetrics.videoBytes,
        diskMetrics.gifBytes,
        diskMetrics.stickerBytes
    ) {
        listOf(
            StoragePillSegment("ig_msgs", ColorInstagram, igDbShare),
            StoragePillSegment("wa_msgs", ColorWhatsApp, waDbShare),
            StoragePillSegment("voice_notes", ColorVoice, diskMetrics.voiceBytes),
            StoragePillSegment("images", ColorImages, diskMetrics.imageBytes),
            StoragePillSegment("video", ColorVideo, diskMetrics.videoBytes),
            StoragePillSegment("gifs", ColorGif, diskMetrics.gifBytes),
            StoragePillSegment("stickers", ColorSticker, diskMetrics.stickerBytes)
        )
    }

    val categorizedTotalBytes = remember(rawSegments) { rawSegments.sumOf { it.bytes } }
    val totalDeviceSafe = diskMetrics.deviceTotalBytes.coerceAtLeast(1L).toFloat()
    val deviceUsedFraction = ((diskMetrics.deviceTotalBytes - diskMetrics.deviceFreeBytes).coerceAtLeast(0L) / totalDeviceSafe)
        .coerceIn(0f, 1f)
    val visualFillFraction = if (categorizedTotalBytes > 0L) {
        deviceUsedFraction.coerceIn(0.42f, 0.90f)
    } else {
        0f
    }
    val animatedFillFraction by animateFloatAsState(
        targetValue = visualFillFraction,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "StoragePillTotalFill"
    )

    // Continuous center-axis rotations for `8 leaf Clover - Move` & `Pill - Move`
    val infiniteTransition = rememberInfiniteTransition(label = "StorageMoveShapes")
    val cloverRotation by if (animatorsEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 16000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "CloverMoveAxis"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val pillRotation by if (animatorsEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "PillMoveAxis"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val burstPath = remember { PathParser().parsePathString(STORAGE_DECOR_BURST_PATH).toPath() }
    val capsulePath = remember { PathParser().parsePathString(STORAGE_DECOR_CAPSULE_PATH).toPath() }
    val rows = remember(waTotalMessages, igTotalMessages, igTotalBytes, waTotalBytes, diskMetrics, strings) {
        listOf(
            StorageRowUi(
                id = "ig_msgs",
                countText = numFmt.format(igTotalMessages),
                categoryLabel = strings.matchingMessagesLabel.lowercase(),
                sizeText = Formatter.formatShortFileSize(context, igTotalBytes),
                color = ColorInstagram,
                iconSpec = StorageSvgIconSpec(26.49f, 26.5f, STORAGE_IG_PATHS)
            ),
            StorageRowUi(
                id = "wa_msgs",
                countText = numFmt.format(waTotalMessages),
                categoryLabel = strings.matchingMessagesLabel.lowercase(),
                sizeText = Formatter.formatShortFileSize(context, waTotalBytes),
                color = ColorWhatsApp,
                iconSpec = StorageSvgIconSpec(26.84f, 26.96f, STORAGE_WA_PATHS)
            ),
            StorageRowUi(
                id = "voice_notes",
                countText = numFmt.format(diskMetrics.voiceCount),
                categoryLabel = strings.voiceNotesLabel,
                sizeText = Formatter.formatShortFileSize(context, diskMetrics.voiceBytes),
                color = ColorVoice,
                iconSpec = StorageSvgIconSpec(21.11f, 27f, STORAGE_VOICE_PATHS)
            ),
            StorageRowUi(
                id = "images",
                countText = numFmt.format(diskMetrics.imageCount),
                categoryLabel = strings.imagesLabel,
                sizeText = Formatter.formatShortFileSize(context, diskMetrics.imageBytes),
                color = ColorImages,
                iconSpec = StorageSvgIconSpec(24f, 24f, STORAGE_IMAGES_PATHS, isStroked = true, strokeWidth = 2.48889f)
            ),
            StorageRowUi(
                id = "video",
                countText = numFmt.format(diskMetrics.videoCount),
                categoryLabel = strings.videoLabel,
                sizeText = Formatter.formatShortFileSize(context, diskMetrics.videoBytes),
                color = ColorVideo,
                iconSpec = StorageSvgIconSpec(30f, 19.85f, STORAGE_VIDEO_PATHS)
            ),
            StorageRowUi(
                id = "gifs",
                countText = numFmt.format(diskMetrics.gifCount),
                categoryLabel = strings.gifsLabel,
                sizeText = Formatter.formatShortFileSize(context, diskMetrics.gifBytes),
                color = ColorGif,
                iconSpec = StorageSvgIconSpec(32.71f, 26f, STORAGE_GIF_PATHS)
            ),
            StorageRowUi(
                id = "stickers",
                countText = numFmt.format(diskMetrics.stickerCount),
                categoryLabel = strings.stickerLabel,
                sizeText = Formatter.formatShortFileSize(context, diskMetrics.stickerBytes),
                color = ColorSticker,
                iconSpec = StorageSvgIconSpec(26f, 26f, STORAGE_STICKER_PATHS)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .wasmFeatheredVerticalViewport(
                    active = listState.canScrollForward || listState.canScrollBackward,
                    topFadeFraction = 0.025f * (0.4f + 0.6f * headerFalloff),
                    bottomFadeFraction = 0.055f
                ),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Top Section: WASM Wordmark + ⋮ Menu
            item(key = "storage_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .graphicsLayer {
                            alpha = 1f - 0.14f * headerFalloff
                            translationY = -4.dp.toPx() * headerFalloff
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WASM",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = GoogleSansFlexOnboarding875,
                            fontWeight = FontWeight(875),
                            fontSize = 30.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = strings.settingsTitle,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.importFile, maxLines = 1) },
                                leadingIcon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenImport()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.personalizeProfile, maxLines = 1) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    isPersonalizeOpen = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.settingsTitle, maxLines = 1) },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onNavigateToSettings()
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Dashboard Total Messages Card + Color-Coded Storage Pill Bar
            item(key = "storage_dashboard") {
                val shapeTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                val shapeSecondaryTint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f)

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(186.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Top-right `Pill - Move` rotating on its exact center axis
                            val capSize = 72.dp.toPx()
                            val capCenter = Offset(size.width - capSize * 0.28f, capSize * 0.38f)
                            val capScale = capSize / 74.82f
                            withTransform({
                                translate(capCenter.x, capCenter.y)
                                rotate(pillRotation, pivot = Offset.Zero)
                                translate(-capSize / 2f, -capSize / 2f)
                                scale(capScale, capScale, pivot = Offset.Zero)
                            }) {
                                drawPath(path = capsulePath, color = shapeSecondaryTint)
                            }

                            // Bottom-left `8 leaf Clover - Move` rotating on its exact center axis
                            val burstSize = 80.dp.toPx()
                            val burstCenter = Offset(burstSize * 0.32f, size.height - burstSize * 0.22f)
                            val burstScale = burstSize / 79.86f
                            withTransform({
                                translate(burstCenter.x, burstCenter.y)
                                rotate(cloverRotation, pivot = Offset.Zero)
                                translate(-burstSize / 2f, -burstSize / 2f)
                                scale(burstScale, burstScale, pivot = Offset.Zero)
                            }) {
                                drawPath(path = burstPath, color = shapeTint)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = strings.totalMessagesTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = GoogleSansFlexOnboarding535,
                                    fontWeight = FontWeight(535),
                                    fontSize = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            WasmMechanicalText(
                                text = formattedTotal,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = GoogleSansFlexOnboarding875,
                                    fontWeight = FontWeight(875),
                                    fontSize = 54.sp,
                                    letterSpacing = (-1.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Storage Teller Caption Row
                val usedText = Formatter.formatShortFileSize(context, appUsedBytes)
                val freeText = Formatter.formatShortFileSize(context, diskMetrics.deviceFreeBytes)
                val capText = Formatter.formatShortFileSize(context, diskMetrics.deviceTotalBytes)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${strings.totalStorageUsed} $usedText",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${strings.freeStorageLabel} $freeText/$capText",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Color-coded Multi-Segment Storage Pill matching row icon colors
                val activeSegments = remember(rawSegments) {
                    rawSegments.filter { it.bytes > 0L }
                }
                val normalizedWeights = remember(activeSegments, categorizedTotalBytes) {
                    if (activeSegments.isEmpty() || categorizedTotalBytes <= 0L) {
                        emptyList()
                    } else {
                        val rawFracs = activeSegments.map {
                            (it.bytes.toFloat() / categorizedTotalBytes.toFloat()).coerceAtLeast(0.05f)
                        }
                        val sumFracs = rawFracs.sum().coerceAtLeast(0.001f)
                        activeSegments.mapIndexed { idx, seg -> seg to (rawFracs[idx] / sumFracs) }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    if (normalizedWeights.isNotEmpty() && animatedFillFraction > 0.005f) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedFillFraction)
                                .clip(CircleShape),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            normalizedWeights.forEach { (seg, weight) ->
                                val isSegmentHighlighted = highlightedCategoryId == null || highlightedCategoryId == seg.id
                                val segmentAlpha by animateFloatAsState(
                                    targetValue = if (isSegmentHighlighted) 1f else 0.30f,
                                    animationSpec = tween(220),
                                    label = "segAlpha_${seg.id}"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(weight)
                                        .background(seg.color.copy(alpha = segmentAlpha))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Breakdown List Items with exact Storage Screen.svg icons & matching color coding
            itemsIndexed(
                items = rows,
                key = { _, item -> item.id }
            ) { index, row ->
                val shape = storageConnectedItemShape(index = index, totalCount = rows.size)
                val isSelectedRow = highlightedCategoryId == row.id
                val rowBg by animateColorAsState(
                    targetValue = if (isSelectedRow) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    animationSpec = tween(200),
                    label = "storageRowBg_${row.id}"
                )
                Surface(
                    shape = shape,
                    color = rowBg,
                    border = if (isSelectedRow) BorderStroke(1.25.dp, row.color.copy(alpha = 0.65f)) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItemPlacement(WasmMotionTokens.SpringListPlacement)
                        .wasmLuminousElasticClickable(
                            shape = shape,
                            maxStretchPercent = 0.025f,
                            pressedScale = 0.978f,
                            onClick = {
                                view.performWasmTickHaptic(true)
                                highlightedCategoryId = if (isSelectedRow) null else row.id
                            }
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StorageExactSvgIcon(
                            spec = row.iconSpec,
                            tint = row.color,
                            modifier = Modifier.size(26.dp)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        WasmMechanicalText(
                            text = row.countText,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = GoogleSansFlexOnboarding535,
                                fontWeight = FontWeight(535),
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            staggerDelayMs = 20,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = row.categoryLabel,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = CircleShape,
                            color = row.color.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = row.sizeText,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = row.color,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            item(key = "storage_delete_button") {
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = onOpenStorageData,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    contentPadding = PaddingValues(vertical = 15.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Delete Storage",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Top-to-Bottom Personalize Overlay
        androidx.compose.animation.AnimatedVisibility(
            visible = isPersonalizeOpen,
            enter = androidx.compose.animation.slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeIn(tween(400)),
            exit = androidx.compose.animation.slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeOut(tween(350))
        ) {
            PersonalizeScreen(
                prefs = prefs,
                onDismiss = { isPersonalizeOpen = false },
                onNavigateToSettings = {
                    isPersonalizeOpen = false
                    onNavigateToSettings()
                }
            )
        }
    }
}

@Composable
private fun StorageExactSvgIcon(
    spec: StorageSvgIconSpec,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val parsedPaths = remember(spec) {
        spec.paths.map { (data, evenOdd) ->
            PathParser().parsePathString(data).toPath().apply {
                fillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero
            }
        }
    }
    Canvas(modifier = modifier) {
        val s = min(size.width / spec.srcWidth, size.height / spec.srcHeight)
        val dx = (size.width - spec.srcWidth * s) / 2f
        val dy = (size.height - spec.srcHeight * s) / 2f
        withTransform({
            translate(dx, dy)
            scale(scaleX = s, scaleY = s, pivot = Offset.Zero)
        }) {
            if (spec.isStroked) {
                val strokeStyle = Stroke(
                    width = spec.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                parsedPaths.forEach { path ->
                    drawPath(path = path, color = tint, style = strokeStyle)
                }
            } else {
                parsedPaths.forEach { path ->
                    drawPath(path = path, color = tint)
                }
            }
        }
    }
}

private fun storageConnectedItemShape(index: Int, totalCount: Int): RoundedCornerShape {
    val outer = 20.dp
    val inner = 6.dp
    return when {
        totalCount <= 1 -> RoundedCornerShape(outer)
        index == 0 -> RoundedCornerShape(
            topStart = outer,
            topEnd = outer,
            bottomStart = inner,
            bottomEnd = inner
        )
        index == totalCount - 1 -> RoundedCornerShape(
            topStart = inner,
            topEnd = inner,
            bottomStart = outer,
            bottomEnd = outer
        )
        else -> RoundedCornerShape(inner)
    }
}

private fun scanStorageMetrics(
    context: Context,
    waDir: File,
    igDir: File
): StorageDiskMetrics {
    val dbFile = context.getDatabasePath("chat_database")
    val dbWal = File(dbFile.path + "-wal")
    val dbShm = File(dbFile.path + "-shm")
    val dbBytes = (if (dbFile.exists()) dbFile.length() else 0L) +
        (if (dbWal.exists()) dbWal.length() else 0L) +
        (if (dbShm.exists()) dbShm.length() else 0L)

    var waMediaBytes = 0L
    var igMediaBytes = 0L
    var voiceCount = 0
    var voiceBytes = 0L
    var imageCount = 0
    var imageBytes = 0L
    var videoCount = 0
    var videoBytes = 0L
    var gifCount = 0
    var gifBytes = 0L
    var stickerCount = 0
    var stickerBytes = 0L

    fun classifyDir(root: File, isInstagram: Boolean) {
        if (!root.exists()) return
        root.walkTopDown().filter { it.isFile }.forEach { f ->
            if (f.name.contains("Instagram-Logo", ignoreCase = true)) return@forEach
            val relPath = f.relativeToOrNull(root)?.invariantSeparatorsPath ?: ""
            if (relPath.startsWith("_avatars/")) return@forEach

            val len = f.length()
            if (isInstagram) igMediaBytes += len else waMediaBytes += len

            when (StorageMediaHelper.classifyFile(f, root)) {
                StorageMediaCategory.STICKERS -> {
                    stickerCount++
                    stickerBytes += len
                }
                StorageMediaCategory.GIFS -> {
                    gifCount++
                    gifBytes += len
                }
                StorageMediaCategory.VOICE_NOTES -> {
                    voiceCount++
                    voiceBytes += len
                }
                StorageMediaCategory.VIDEOS -> {
                    videoCount++
                    videoBytes += len
                }
                StorageMediaCategory.IMAGES -> {
                    imageCount++
                    imageBytes += len
                }
                StorageMediaCategory.TEXT,
                StorageMediaCategory.OTHERS -> Unit
            }
        }
    }

    classifyDir(waDir, isInstagram = false)
    classifyDir(igDir, isInstagram = true)

    val statFs = StatFs(Environment.getDataDirectory().path)
    val freeBytes = statFs.availableBytes
    val totalBytes = statFs.totalBytes.coerceAtLeast(1L)

    return StorageDiskMetrics(
        dbBytes = dbBytes,
        waMediaBytes = waMediaBytes,
        igMediaBytes = igMediaBytes,
        voiceCount = voiceCount,
        voiceBytes = voiceBytes,
        imageCount = imageCount,
        imageBytes = imageBytes,
        videoCount = videoCount,
        videoBytes = videoBytes,
        gifCount = gifCount,
        gifBytes = gifBytes,
        stickerCount = stickerCount,
        stickerBytes = stickerBytes,
        deviceFreeBytes = freeBytes,
        deviceTotalBytes = totalBytes
    )
}
