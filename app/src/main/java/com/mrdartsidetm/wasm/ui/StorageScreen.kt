package com.mrdartsidetm.wasm.ui

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdartsidetm.wasm.R
import com.mrdartsidetm.wasm.data.UserPreferencesRepository
import com.mrdartsidetm.wasm.ui.instagram.InstagramViewModel
import com.mrdartsidetm.wasm.ui.localization.LocalWasmStrings
import com.mrdartsidetm.wasm.ui.theme.GoogleSansFlexOnboarding535
import com.mrdartsidetm.wasm.ui.theme.GoogleSansFlexOnboarding875
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min

private const val STORAGE_DECOR_BURST_PATH =
    "M 73.95 39.93 C 79.86 50.96 75.39 60.94 63.98 63.98 C 60.94 75.39 50.96 79.86 39.93 73.95 C 28.9 79.86 18.92 75.39 15.88 63.98 C 4.47 60.94 0 50.96 5.91 39.93 C 0 28.9 4.47 18.92 15.88 15.88 C 18.92 4.47 28.9 0 39.93 5.91 C 50.96 0 60.94 4.47 63.98 15.88 C 75.39 18.92 79.86 28.9 73.95 39.93 Z"

private const val STORAGE_DECOR_CAPSULE_PATH =
    "M 21.27 11.59 C 32.86 0 51.64 0 63.23 11.59 C 74.82 23.18 74.82 41.96 63.23 53.55 L 53.55 63.23 C 41.96 74.82 23.18 74.82 11.59 63.23 C 0 51.64 0 32.86 11.59 21.27 L 21.27 11.59 Z"

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

private data class StorageRowUi(
    val id: String,
    val countText: String,
    val categoryLabel: String,
    val sizeText: String,
    val drawableRes: Int? = null,
    val iconVector: ImageVector? = null,
    val iconTint: Color? = null
)

/**
 * Material 3 Expressive Storage Screen matching `assests/wireframes/main/Storage Screen.svg`.
 * Displays real offline message totals, device free/used storage bars, and per-type media breakdowns.
 */
@Composable
fun StorageScreen(
    whatsappViewModel: ChatViewModel,
    instagramViewModel: InstagramViewModel,
    prefs: UserPreferencesRepository,
    onOpenImport: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalWasmStrings.current

    val waConversations by whatsappViewModel.conversations.collectAsStateWithLifecycle()
    val waTotalMessages by whatsappViewModel.totalMessageCount.collectAsStateWithLifecycle(initialValue = 0)
    val igConversations by instagramViewModel.conversations.collectAsStateWithLifecycle()
    val igTotalMessages = remember(igConversations) { igConversations.sumOf { it.messageCount } }
    val totalMessages = waTotalMessages + igTotalMessages

    var showOverflowMenu by remember { mutableStateOf(false) }
    var isPersonalizeOpen by rememberSaveable { mutableStateOf(false) }

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

    val animatedTotal by animateIntAsState(
        targetValue = totalMessages,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "StorageTotalMsgAnim"
    )

    val formattedTotal = remember(animatedTotal) {
        NumberFormat.getNumberInstance(Locale.getDefault()).format(animatedTotal)
    }

    val appUsedBytes = diskMetrics.dbBytes + diskMetrics.waMediaBytes + diskMetrics.igMediaBytes
    val totalMsgCountSafe = totalMessages.coerceAtLeast(1).toFloat()
    val waDbShare = (diskMetrics.dbBytes * (waTotalMessages / totalMsgCountSafe)).toLong()
    val igDbShare = (diskMetrics.dbBytes * (igTotalMessages / totalMsgCountSafe)).toLong()

    val waTotalBytes = waDbShare + diskMetrics.waMediaBytes
    val igTotalBytes = igDbShare + diskMetrics.igMediaBytes

    val totalDeviceSafe = diskMetrics.deviceTotalBytes.coerceAtLeast(1L).toFloat()
    val usedTargetFraction = ((diskMetrics.deviceTotalBytes - diskMetrics.deviceFreeBytes).coerceAtLeast(0L) / totalDeviceSafe)
        .coerceIn(0f, 1f)
    val waSegmentTarget = if (appUsedBytes > 0L) {
        (usedTargetFraction * (waTotalBytes.toFloat() / appUsedBytes.toFloat())).coerceIn(0.03f, 0.45f)
    } else 0f
    val igSegmentTarget = if (appUsedBytes > 0L) {
        (usedTargetFraction * (igTotalBytes.toFloat() / appUsedBytes.toFloat())).coerceIn(0.03f, 0.45f)
    } else 0f

    val animWaFrac by animateFloatAsState(
        targetValue = waSegmentTarget,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "WaStorageSeg"
    )
    val animIgFrac by animateFloatAsState(
        targetValue = igSegmentTarget,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "IgStorageSeg"
    )

    val burstPath = remember { PathParser().parsePathString(STORAGE_DECOR_BURST_PATH).toPath() }
    val capsulePath = remember { PathParser().parsePathString(STORAGE_DECOR_CAPSULE_PATH).toPath() }

    val numFmt = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val rows = remember(waTotalMessages, igTotalMessages, diskMetrics, strings) {
        listOf(
            StorageRowUi(
                id = "ig_msgs",
                countText = numFmt.format(igTotalMessages),
                categoryLabel = strings.matchingMessagesLabel.lowercase(),
                sizeText = Formatter.formatShortFileSize(context, igTotalBytes),
                drawableRes = R.drawable.ic_nav_instagram,
                iconTint = null
            ),
            StorageRowUi(
                id = "wa_msgs",
                countText = numFmt.format(waTotalMessages),
                categoryLabel = strings.matchingMessagesLabel.lowercase(),
                sizeText = Formatter.formatShortFileSize(context, waTotalBytes),
                drawableRes = R.drawable.ic_nav_whatsapp,
                iconTint = null
            ),
            StorageRowUi(
                id = "voice_notes",
                countText = numFmt.format(diskMetrics.voiceCount),
                categoryLabel = strings.voiceNotesLabel,
                sizeText = Formatter.formatShortFileSize(context, diskMetrics.voiceBytes),
                iconVector = Icons.Default.Mic
            ),
            StorageRowUi(
                id = "images",
                countText = numFmt.format(diskMetrics.imageCount),
                categoryLabel = strings.imagesLabel,
                sizeText = Formatter.formatShortFileSize(context, diskMetrics.imageBytes),
                iconVector = Icons.Default.Image
            ),
            StorageRowUi(
                id = "video",
                countText = numFmt.format(diskMetrics.videoCount),
                categoryLabel = strings.videoLabel,
                sizeText = Formatter.formatShortFileSize(context, diskMetrics.videoBytes),
                iconVector = Icons.Default.Videocam
            ),
            StorageRowUi(
                id = "gifs",
                countText = numFmt.format(diskMetrics.gifCount),
                categoryLabel = strings.gifsLabel,
                sizeText = Formatter.formatShortFileSize(context, diskMetrics.gifBytes),
                iconVector = Icons.Default.Gif
            ),
            StorageRowUi(
                id = "stickers",
                countText = numFmt.format(diskMetrics.stickerCount),
                categoryLabel = strings.stickerLabel,
                sizeText = Formatter.formatShortFileSize(context, diskMetrics.stickerBytes),
                iconVector = Icons.Default.EmojiEmotions
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Top Section: WASM Wordmark + ⋮ Menu
            item(key = "storage_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
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

            // Dashboard Total Messages Card + Storage Progress Bar
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
                            // Top-right tilted capsule matching shape_17 in Storage Screen.svg
                            val capSize = 72.dp.toPx()
                            withTransform({
                                translate(size.width - capSize * 0.78f, -capSize * 0.12f)
                                scale(capSize / 74.82f, capSize / 74.82f, pivot = Offset.Zero)
                            }) {
                                drawPath(path = capsulePath, color = shapeSecondaryTint)
                            }
                            // Bottom-left 8-petal burst matching shape_16 in Storage Screen.svg
                            val burstSize = 80.dp.toPx()
                            withTransform({
                                translate(-burstSize * 0.18f, size.height - burstSize * 0.72f)
                                scale(burstSize / 79.86f, burstSize / 79.86f, pivot = Offset.Zero)
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
                            Text(
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

                // Multi-segment Storage Bar matching Storage Screen.svg (Instagram pill layered over WhatsApp pill)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    val igFrac = animIgFrac.coerceIn(0f, 1f)
                    val waFrac = animWaFrac.coerceIn(0f, (1f - igFrac).coerceAtLeast(0f))
                    val combinedFrac = (igFrac + waFrac).coerceIn(0f, 1f)
                    if (waFrac > 0.005f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(combinedFrac)
                                .background(Color(0xFF709D5B), CircleShape)
                        )
                    }
                    if (igFrac > 0.005f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(igFrac)
                                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Breakdown List Items matching Storage Screen.svg
            itemsIndexed(
                items = rows,
                key = { _, item -> item.id }
            ) { index, row ->
                val shape = storageConnectedItemShape(index = index, totalCount = rows.size)
                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (row.drawableRes != null) {
                            Icon(
                                painter = painterResource(id = row.drawableRes),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        } else if (row.iconVector != null) {
                            Icon(
                                imageVector = row.iconVector,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = row.countText,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = GoogleSansFlexOnboarding535,
                                fontWeight = FontWeight(535),
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
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
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = row.sizeText,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
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
            val len = f.length()
            if (isInstagram) igMediaBytes += len else waMediaBytes += len

            val ext = f.extension.lowercase(Locale.ROOT)
            val pathLower = f.absolutePath.lowercase(Locale.ROOT)
            when {
                ext == "webp" || pathLower.contains("sticker") -> {
                    stickerCount++
                    stickerBytes += len
                }
                ext == "gif" -> {
                    gifCount++
                    gifBytes += len
                }
                ext in setOf("opus", "ogg", "mp3", "m4a", "aac", "wav", "amr") -> {
                    voiceCount++
                    voiceBytes += len
                }
                ext in setOf("mp4", "mkv", "mov", "3gp", "avi", "webm") -> {
                    videoCount++
                    videoBytes += len
                }
                ext in setOf("jpg", "jpeg", "png", "heic", "heif", "bmp") &&
                    !f.name.contains("Instagram-Logo", ignoreCase = true) -> {
                    imageCount++
                    imageBytes += len
                }
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
