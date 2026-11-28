package com.mrdartsidetm.wasm.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.mrdartsidetm.wasm.ui.theme.ChatWallpaperPreset
import com.mrdartsidetm.wasm.ui.theme.LocalChatWallpaper
import com.mrdartsidetm.wasm.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Renders the user-selected chat background wallpaper (preset tint or custom gallery photo)
 * behind chat message streams without duplicating decoding logic across platforms.
 */
@Composable
fun ChatWallpaperContainer(
    usePlatformDefaultWhenUnset: Boolean,
    defaultBackgroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val wallpaperConfig = LocalChatWallpaper.current
    val preset = ChatWallpaperPreset.fromId(wallpaperConfig.presetId)

    val customBitmapState = produceState<Bitmap?>(
        initialValue = null,
        key1 = wallpaperConfig.presetId,
        key2 = wallpaperConfig.customImagePath
    ) {
        value = if (preset == ChatWallpaperPreset.CUSTOM && !wallpaperConfig.customImagePath.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                decodeDownsampledWallpaper(File(wallpaperConfig.customImagePath))
            }
        } else {
            null
        }
    }

    val resolvedBg = when {
        preset == ChatWallpaperPreset.DEFAULT && usePlatformDefaultWhenUnset -> defaultBackgroundColor
        preset == ChatWallpaperPreset.SURFACE -> MaterialTheme.colorScheme.background
        else -> if (isDark) preset.darkColor else preset.lightColor
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(resolvedBg)
    ) {
        val customBitmap = customBitmapState.value
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle scrim to preserve chat bubble contrast over busy photos
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isDark) Color.Black.copy(alpha = 0.45f)
                        else Color.White.copy(alpha = 0.22f)
                    )
            )
        }
        content()
    }
}

private fun decodeDownsampledWallpaper(file: File, maxDim: Int = 1440): Bitmap? {
    if (!file.exists() || file.length() == 0L) return null
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val rawMax = max(bounds.outWidth, bounds.outHeight)
        var sampleSize = 1
        while (rawMax / sampleSize > maxDim) {
            sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        BitmapFactory.decodeFile(file.absolutePath, opts)
    } catch (_: Throwable) {
        null
    }
}
