package com.mrdartsidetm.wasm.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdartsidetm.wasm.data.UserPreferencesRepository
import com.mrdartsidetm.wasm.ui.motion.WasmMotionTokens
import com.mrdartsidetm.wasm.ui.motion.wasmFeatheredVerticalViewport
import com.mrdartsidetm.wasm.ui.motion.wasmLuminousElasticClickable
import com.mrdartsidetm.wasm.ui.theme.ChatWallpaperPreset
import com.mrdartsidetm.wasm.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Dedicated 9:16 Chat Wallpaper HUD Screen.
 * Features a full-width top button to pick a custom photo from the gallery and
 * a 9:16 rounded box HUD displaying all built-in and custom chat wallpapers.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WallpaperScreen(
    prefs: UserPreferencesRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)
    val selectedPresetId by prefs.chatWallpaper.collectAsStateWithLifecycle(initialValue = ChatWallpaperPreset.DEFAULT.id)
    val customImagePath by prefs.chatWallpaperImagePath.collectAsStateWithLifecycle(initialValue = null)
    val isDark = LocalIsDarkTheme.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val wallpapersDir = File(context.filesDir, "wallpapers").apply { mkdirs() }
                    val destFile = File(wallpapersDir, "chat_wallpaper_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output -> input.copyTo(output) }
                    }
                    if (destFile.exists() && destFile.length() > 0L) {
                        prefs.saveChatWallpaperImagePath(destFile.absolutePath)
                        prefs.saveChatWallpaper(ChatWallpaperPreset.CUSTOM.id)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    val presetItems = remember(customImagePath) {
        val builtIn = ChatWallpaperPreset.values().filter { it != ChatWallpaperPreset.CUSTOM }
        if (!customImagePath.isNullOrBlank() && File(customImagePath!!).exists()) {
            listOf(ChatWallpaperPreset.CUSTOM) + builtIn
        } else {
            builtIn
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chat Wallpaper",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!customImagePath.isNullOrBlank()) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    customImagePath?.let { path ->
                                        withContext(Dispatchers.IO) { File(path).delete() }
                                    }
                                    prefs.saveChatWallpaperImagePath(null)
                                    if (selectedPresetId == ChatWallpaperPreset.CUSTOM.id) {
                                        prefs.saveChatWallpaper(ChatWallpaperPreset.DEFAULT.id)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Custom Photo")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top full-width Add Photo button
            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 15.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Add Photo from Gallery",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 9:16 Wallpaper HUD Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .wasmFeatheredVerticalViewport(topFadeFraction = 0.02f, bottomFadeFraction = 0.06f)
            ) {
                items(presetItems, key = { it.id }) { preset ->
                    val isSelected = preset.id == selectedPresetId
                    WallpaperHudTile(
                        preset = preset,
                        customImagePath = if (preset == ChatWallpaperPreset.CUSTOM) customImagePath else null,
                        isDark = isDark,
                        isSelected = isSelected,
                        onClick = {
                            scope.launch { prefs.saveChatWallpaper(preset.id) }
                        },
                        modifier = Modifier.animateItemPlacement(WasmMotionTokens.SpringListPlacement)
                    )
                }
            }
        }
    }
}

@Composable
private fun WallpaperHudTile(
    preset: ChatWallpaperPreset,
    customImagePath: String?,
    isDark: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isDark) preset.darkColor else preset.lightColor
    val outgoingColor = preset.accentBubblePreview
    val incomingColor = if (isDark) Color(0xFF232B32) else Color.White

    val customBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = customImagePath) {
        value = if (!customImagePath.isNullOrBlank()) {
            val file = File(customImagePath)
            if (file.exists()) {
                BitmapMemoryCache.get(file.absolutePath) ?: withContext(Dispatchers.IO) {
                    decodeSampledBitmap(file, 450, 800)?.also {
                        BitmapMemoryCache.put(file.absolutePath, it)
                    }
                }
            } else null
        } else null
    }

    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.5.dp else 1.dp,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.72f),
        label = "wallpaperBorderWidth"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        animationSpec = tween(220),
        label = "wallpaperBorderColor"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .wasmLuminousElasticClickable(
                shape = RoundedCornerShape(20.dp),
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
        ) {
            customBitmap?.let { bmp ->
                Image(
                    bitmap = bmp,
                    contentDescription = preset.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Mini chat HUD preview inside 9:16 canvas
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 2.dp),
                    color = incomingColor.copy(alpha = 0.92f),
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(14.dp)
                ) {}
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Surface(
                        shape = RoundedCornerShape(8.dp, 8.dp, 2.dp, 8.dp),
                        color = outgoingColor.copy(alpha = 0.95f),
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .height(16.dp)
                    ) {}
                }
                Surface(
                    shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 2.dp),
                    color = incomingColor.copy(alpha = 0.92f),
                    modifier = Modifier
                        .fillMaxWidth(0.54f)
                        .height(12.dp)
                ) {}
            }

            // Active check indicator
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(initialScale = 0.55f, animationSpec = WasmMotionTokens.SpringSelectionTick) + fadeIn(tween(110)),
                exit = scaleOut(targetScale = 0.7f, animationSpec = tween(140)) + fadeOut(tween(110)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Wallpaper name pill at bottom
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Text(
                    text = preset.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}
