package com.mrdartsidetm.wasm.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Theme mode persisted in DataStore and selectable in General Settings.
 */
enum class ThemeModeOption(val id: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromId(id: String): ThemeModeOption =
            values().firstOrNull { it.id == id } ?: SYSTEM
    }
}

/**
 * Curated Material 3 Expressive color palette variants selectable in General Settings.
 */
enum class ColorVariantOption(
    val id: String,
    val displayName: String,
    val previewPrimary: Color,
    val previewSecondary: Color,
    val previewTertiary: Color
) {
    DEFAULT("default", "Default (Monet)", Color(0xFF006687), Color(0xFF4E616D), Color(0xFF6B5778)),
    OCEAN("ocean", "Ocean Blue", Color(0xFF0061A4), Color(0xFF535F70), Color(0xFF6B5778)),
    EMERALD("emerald", "Emerald Green", Color(0xFF006D3B), Color(0xFF4F6354), Color(0xFF3A6470)),
    VIOLET("violet", "Expressive Violet", Color(0xFF6750A4), Color(0xFF625B71), Color(0xFF7D5260)),
    CORAL("coral", "Sunset Coral", Color(0xFF9C413C), Color(0xFF775653), Color(0xFF725B2E)),
    ROSE("rose", "Rose Quartz", Color(0xFF984061), Color(0xFF74565F), Color(0xFF7C5635));

    companion object {
        fun fromId(id: String): ColorVariantOption =
            values().firstOrNull { it.id == id } ?: DEFAULT
    }
}

/**
 * Chat background wallpaper presets and custom image configuration.
 */
enum class ChatWallpaperPreset(
    val id: String,
    val displayName: String,
    val lightColor: Color,
    val darkColor: Color
) {
    DEFAULT("default", "Classic WhatsApp", Color(0xFFEFEAE2), Color(0xFF0B141A)),
    SURFACE("surface", "Material Surface", Color(0xFFFBFCFF), Color(0xFF191C1E)),
    SAGE("sage", "Soft Sage", Color(0xFFE3EFE5), Color(0xFF0F1D15)),
    OCEAN("ocean", "Ocean Mist", Color(0xFFE2EEF6), Color(0xFF0C1821)),
    SAND("sand", "Warm Sand", Color(0xFFF5EDE0), Color(0xFF1C1710)),
    LAVENDER("lavender", "Lavender Dusk", Color(0xFFEDE7F6), Color(0xFF161321)),
    AMOLED("amoled", "OLED Black", Color(0xFFEBECEF), Color(0xFF000000)),
    CUSTOM("custom", "Custom Photo", Color(0xFFEFEAE2), Color(0xFF0B141A));

    companion object {
        fun fromId(id: String): ChatWallpaperPreset =
            values().firstOrNull { it.id == id } ?: DEFAULT
    }
}

@Immutable
data class ChatWallpaperConfig(
    val presetId: String = "default",
    val customImagePath: String? = null
)

val LocalIsDarkTheme = staticCompositionLocalOf { false }
val LocalChatWallpaper = staticCompositionLocalOf { ChatWallpaperConfig() }

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF80D4FF),
    onPrimary = Color(0xFF003548),
    primaryContainer = Color(0xFF004D67),
    onPrimaryContainer = Color(0xFFBFE9FF),
    secondary = Color(0xFFB5C9D7),
    onSecondary = Color(0xFF20333E),
    secondaryContainer = Color(0xFF364955),
    onSecondaryContainer = Color(0xFFD1E5F3),
    tertiary = Color(0xFFD6BEE4),
    onTertiary = Color(0xFF3B2947),
    tertiaryContainer = Color(0xFF523F5F),
    onTertiaryContainer = Color(0xFFF2DAFF),
    background = Color(0xFF191C1E),
    onBackground = Color(0xFFE1E2E5),
    surface = Color(0xFF191C1E),
    onSurface = Color(0xFFE1E2E5),
    surfaceVariant = Color(0xFF40484C),
    onSurfaceVariant = Color(0xFFC0C8CD)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006687),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBFE9FF),
    onPrimaryContainer = Color(0xFF001F2C),
    secondary = Color(0xFF4E616D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E5F3),
    onSecondaryContainer = Color(0xFF0A1D28),
    tertiary = Color(0xFF6B5778),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2DAFF),
    onTertiaryContainer = Color(0xFF251432),
    background = Color(0xFFFBFCFF),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFFBFCFF),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFDCE4E9),
    onSurfaceVariant = Color(0xFF40484C)
)

private fun variantLightColorScheme(variant: ColorVariantOption): ColorScheme = when (variant) {
    ColorVariantOption.DEFAULT -> LightColorScheme
    ColorVariantOption.OCEAN -> lightColorScheme(
        primary = Color(0xFF0061A4),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD1E4FF),
        onPrimaryContainer = Color(0xFF001D36),
        secondary = Color(0xFF535F70),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD7E3F7),
        onSecondaryContainer = Color(0xFF101C2B),
        tertiary = Color(0xFF6B5778),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFF2DAFF),
        onTertiaryContainer = Color(0xFF251431),
        background = Color(0xFFFDFCFF),
        onBackground = Color(0xFF1A1C1E),
        surface = Color(0xFFFDFCFF),
        onSurface = Color(0xFF1A1C1E),
        surfaceVariant = Color(0xFFDFE2EB),
        onSurfaceVariant = Color(0xFF43474E)
    )
    ColorVariantOption.EMERALD -> lightColorScheme(
        primary = Color(0xFF006D3B),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF97F7B6),
        onPrimaryContainer = Color(0xFF00210E),
        secondary = Color(0xFF4F6354),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD2E8D5),
        onSecondaryContainer = Color(0xFF0D1F13),
        tertiary = Color(0xFF3A6470),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFBEEAF7),
        onTertiaryContainer = Color(0xFF001F27),
        background = Color(0xFFFBFDF8),
        onBackground = Color(0xFF191C19),
        surface = Color(0xFFFBFDF8),
        onSurface = Color(0xFF191C19),
        surfaceVariant = Color(0xFFDCE5DC),
        onSurfaceVariant = Color(0xFF414942)
    )
    ColorVariantOption.VIOLET -> lightColorScheme(
        primary = Color(0xFF6750A4),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF21005D),
        secondary = Color(0xFF625B71),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE8DEF8),
        onSecondaryContainer = Color(0xFF1D192B),
        tertiary = Color(0xFF7D5260),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFD8E4),
        onTertiaryContainer = Color(0xFF31111D),
        background = Color(0xFFFEF7FF),
        onBackground = Color(0xFF1D1B20),
        surface = Color(0xFFFEF7FF),
        onSurface = Color(0xFF1D1B20),
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF49454F)
    )
    ColorVariantOption.CORAL -> lightColorScheme(
        primary = Color(0xFF9C413C),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFDAD6),
        onPrimaryContainer = Color(0xFF410003),
        secondary = Color(0xFF775653),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFDAD6),
        onSecondaryContainer = Color(0xFF2C1513),
        tertiary = Color(0xFF725B2E),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFEDFA6),
        onTertiaryContainer = Color(0xFF271900),
        background = Color(0xFFFFF8F7),
        onBackground = Color(0xFF231918),
        surface = Color(0xFFFFF8F7),
        onSurface = Color(0xFF231918),
        surfaceVariant = Color(0xFFF5DDDA),
        onSurfaceVariant = Color(0xFF534341)
    )
    ColorVariantOption.ROSE -> lightColorScheme(
        primary = Color(0xFF984061),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFD9E2),
        onPrimaryContainer = Color(0xFF3E001D),
        secondary = Color(0xFF74565F),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFD9E2),
        onSecondaryContainer = Color(0xFF2B151C),
        tertiary = Color(0xFF7C5635),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFDCC1),
        onTertiaryContainer = Color(0xFF2E1500),
        background = Color(0xFFFFF8F8),
        onBackground = Color(0xFF22191C),
        surface = Color(0xFFFFF8F8),
        onSurface = Color(0xFF22191C),
        surfaceVariant = Color(0xFFF2DDE2),
        onSurfaceVariant = Color(0xFF514347)
    )
}

private fun variantDarkColorScheme(variant: ColorVariantOption): ColorScheme = when (variant) {
    ColorVariantOption.DEFAULT -> DarkColorScheme
    ColorVariantOption.OCEAN -> darkColorScheme(
        primary = Color(0xFF9ECAFF),
        onPrimary = Color(0xFF003258),
        primaryContainer = Color(0xFF00497D),
        onPrimaryContainer = Color(0xFFD1E4FF),
        secondary = Color(0xFFBBC7DB),
        onSecondary = Color(0xFF253140),
        secondaryContainer = Color(0xFF3B4858),
        onSecondaryContainer = Color(0xFFD7E3F7),
        tertiary = Color(0xFFD6BEE4),
        onTertiary = Color(0xFF3B2948),
        tertiaryContainer = Color(0xFF523F5F),
        onTertiaryContainer = Color(0xFFF2DAFF),
        background = Color(0xFF111318),
        onBackground = Color(0xFFE2E2E9),
        surface = Color(0xFF111318),
        onSurface = Color(0xFFE2E2E9),
        surfaceVariant = Color(0xFF43474E),
        onSurfaceVariant = Color(0xFFC3C6CF)
    )
    ColorVariantOption.EMERALD -> darkColorScheme(
        primary = Color(0xFF7CDA9C),
        onPrimary = Color(0xFF00391C),
        primaryContainer = Color(0xFF00522B),
        onPrimaryContainer = Color(0xFF97F7B6),
        secondary = Color(0xFFB6CCB9),
        onSecondary = Color(0xFF223527),
        secondaryContainer = Color(0xFF384B3D),
        onSecondaryContainer = Color(0xFFD2E8D5),
        tertiary = Color(0xFFA2CDDA),
        onTertiary = Color(0xFF023640),
        tertiaryContainer = Color(0xFF214C57),
        onTertiaryContainer = Color(0xFFBEEAF7),
        background = Color(0xFF111411),
        onBackground = Color(0xFFE1E3DE),
        surface = Color(0xFF111411),
        onSurface = Color(0xFFE1E3DE),
        surfaceVariant = Color(0xFF414942),
        onSurfaceVariant = Color(0xFFC1C9BF)
    )
    ColorVariantOption.VIOLET -> darkColorScheme(
        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFFCCC2DC),
        onSecondary = Color(0xFF332D41),
        secondaryContainer = Color(0xFF4A4458),
        onSecondaryContainer = Color(0xFFE8DEF8),
        tertiary = Color(0xFFEFB8C8),
        onTertiary = Color(0xFF492532),
        tertiaryContainer = Color(0xFF633B48),
        onTertiaryContainer = Color(0xFFFFD8E4),
        background = Color(0xFF141218),
        onBackground = Color(0xFFE6E0E9),
        surface = Color(0xFF141218),
        onSurface = Color(0xFFE6E0E9),
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCAC4D0)
    )
    ColorVariantOption.CORAL -> darkColorScheme(
        primary = Color(0xFFFFB4AC),
        onPrimary = Color(0xFF5F1413),
        primaryContainer = Color(0xFF7D2A27),
        onPrimaryContainer = Color(0xFFFFDAD6),
        secondary = Color(0xFFE7BDB8),
        onSecondary = Color(0xFF442927),
        secondaryContainer = Color(0xFF5D3F3C),
        onSecondaryContainer = Color(0xFFFFDAD6),
        tertiary = Color(0xFFE1C38C),
        onTertiary = Color(0xFF402D04),
        tertiaryContainer = Color(0xFF584319),
        onTertiaryContainer = Color(0xFFFEDFA6),
        background = Color(0xFF1A1110),
        onBackground = Color(0xFFF1DEDC),
        surface = Color(0xFF1A1110),
        onSurface = Color(0xFFF1DEDC),
        surfaceVariant = Color(0xFF534341),
        onSurfaceVariant = Color(0xFFD8C2BF)
    )
    ColorVariantOption.ROSE -> darkColorScheme(
        primary = Color(0xFFFFB0C8),
        onPrimary = Color(0xFF5E1133),
        primaryContainer = Color(0xFF7B2949),
        onPrimaryContainer = Color(0xFFFFD9E2),
        secondary = Color(0xFFE3BDC6),
        onSecondary = Color(0xFF422931),
        secondaryContainer = Color(0xFF5B3F47),
        onSecondaryContainer = Color(0xFFFFD9E2),
        tertiary = Color(0xFFEFBD94),
        onTertiary = Color(0xFF48290C),
        tertiaryContainer = Color(0xFF613F20),
        onTertiaryContainer = Color(0xFFFFDCC1),
        background = Color(0xFF191114),
        onBackground = Color(0xFFEFDFE2),
        surface = Color(0xFF191114),
        onSurface = Color(0xFFEFDFE2),
        surfaceVariant = Color(0xFF514347),
        onSurfaceVariant = Color(0xFFD5C2C6)
    )
}

@Composable
fun WasmTheme(
    themeMode: String = ThemeModeOption.SYSTEM.id,
    dynamicColor: Boolean = true,
    colorVariant: String = ColorVariantOption.DEFAULT.id,
    fontSelection: String = AppFontOption.GOOGLE_SANS_FLEX.id,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (ThemeModeOption.fromId(themeMode)) {
        ThemeModeOption.SYSTEM -> systemDark
        ThemeModeOption.LIGHT -> false
        ThemeModeOption.DARK -> true
    }

    val variantOption = ColorVariantOption.fromId(colorVariant)
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && variantOption == ColorVariantOption.DEFAULT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> variantDarkColorScheme(variantOption)
        else -> variantLightColorScheme(variantOption)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.surface.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val appFontFamily = remember(context, fontSelection) {
        getSelectedAppFontFamily(context, fontSelection)
    }
    val typography = remember(appFontFamily) { buildWasmTypography(appFontFamily) }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
