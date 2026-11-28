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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlin.math.max
import kotlin.math.min

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
 * Material 3 Dynamic Color Scheme Variants shown when Dynamic Color is ON
 * (matching Wallet-Flutter PersonalizationPage).
 */
enum class DynamicVariantOption(
    val id: String,
    val displayName: String,
    val lightTop: Color,
    val lightBottomLeft: Color,
    val lightBottomRight: Color,
    val darkTop: Color,
    val darkBottomLeft: Color,
    val darkBottomRight: Color
) {
    TONAL_SPOT(
        "tonal_spot", "Tonal Spot",
        Color(0xFFD3DFFF), Color(0xFFE8D5EC), Color(0xFF677799),
        Color(0xFF9BB1E8), Color(0xFFCBBCD6), Color(0xFF42567D)
    ),
    VIBRANT(
        "vibrant", "Vibrant",
        Color(0xFFFFD8E4), Color(0xFFFFDCC1), Color(0xFFE91E63),
        Color(0xFFFFB0C8), Color(0xFFFFB787), Color(0xFFB01D56)
    ),
    EXPRESSIVE(
        "expressive", "Expressive",
        Color(0xFFFFDBCA), Color(0xFFE5DEFF), Color(0xFFFF9800),
        Color(0xFFFFB596), Color(0xFFC7BFFF), Color(0xFFB36700)
    ),
    RAINBOW(
        "rainbow", "Rainbow",
        Color(0xFFFFD8E6), Color(0xFFC4EED0), Color(0xFF9C27B0),
        Color(0xFFFFB0D0), Color(0xFFA3DDB3), Color(0xFF7B1FA2)
    ),
    FRUIT_SALAD(
        "fruit_salad", "Fruit Salad",
        Color(0xFFC3EEDD), Color(0xFFFFD8DF), Color(0xFF00B074),
        Color(0xFFA1DECA), Color(0xFFFFB1C1), Color(0xFF007A50)
    ),
    FIDELITY(
        "fidelity", "Fidelity",
        Color(0xFFDFE2F9), Color(0xFFE1D3F8), Color(0xFF3F51B5),
        Color(0xFFB8C4F6), Color(0xFFC4B2EE), Color(0xFF303F9F)
    ),
    CONTENT(
        "content", "Content",
        Color(0xFFD7E2FF), Color(0xFFD9E2F1), Color(0xFF2196F3),
        Color(0xFFA8C8FF), Color(0xFFB9C9DF), Color(0xFF1976D2)
    ),
    NEUTRAL(
        "neutral", "Neutral",
        Color(0xFFE3E2E6), Color(0xFFE2E2E9), Color(0xFF75787F),
        Color(0xFFC7C6CA), Color(0xFFC6C6CD), Color(0xFF5A5C63)
    ),
    MONOCHROME(
        "monochrome", "Monochrome",
        Color(0xFFE0E0E0), Color(0xFFBDBDBD), Color(0xFF424242),
        Color(0xFFBDBDBD), Color(0xFF757575), Color(0xFF212121)
    );

    companion object {
        fun fromId(id: String): DynamicVariantOption =
            values().firstOrNull { it.id == id } ?: TONAL_SPOT
    }
}

/**
 * Static color palette presets shown when Dynamic Color is OFF.
 */
enum class ColorVariantOption(
    val id: String,
    val displayName: String,
    val previewPrimary: Color,
    val previewSecondary: Color,
    val previewTertiary: Color
) {
    DEFAULT("default", "Wasm Blue", Color(0xFF006687), Color(0xFF4E616D), Color(0xFF6B5778)),
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
    val darkColor: Color,
    val accentBubblePreview: Color
) {
    DEFAULT("default", "Classic WhatsApp", Color(0xFFEFEAE2), Color(0xFF0B141A), Color(0xFFD9FDD3)),
    SURFACE("surface", "Material Surface", Color(0xFFFBFCFF), Color(0xFF191C1E), Color(0xFFBFE9FF)),
    SAGE("sage", "Soft Sage", Color(0xFFE3EFE5), Color(0xFF0F1D15), Color(0xFFC4EED0)),
    OCEAN("ocean", "Ocean Mist", Color(0xFFE2EEF6), Color(0xFF0C1821), Color(0xFFD1E4FF)),
    SAND("sand", "Warm Sand", Color(0xFFF5EDE0), Color(0xFF1C1710), Color(0xFFFEDFA6)),
    LAVENDER("lavender", "Lavender Dusk", Color(0xFFEDE7F6), Color(0xFF161321), Color(0xFFE8DEF8)),
    ROSE("rose", "Blush Rose", Color(0xFFFCE8EE), Color(0xFF211116), Color(0xFFFFD9E2)),
    AMOLED("amoled", "OLED Black", Color(0xFFEBECEF), Color(0xFF000000), Color(0xFFDCE4E9)),
    CUSTOM("custom", "Custom Photo", Color(0xFFEFEAE2), Color(0xFF0B141A), Color(0xFFD9FDD3));

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
val LocalWasmMonoFontFamily = staticCompositionLocalOf { GoogleSansCodeFontFamily }

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
    background = Color(0xFF111416),
    onBackground = Color(0xFFE1E2E5),
    surface = Color(0xFF111416),
    onSurface = Color(0xFFE1E2E5),
    surfaceVariant = Color(0xFF30383D),
    onSurfaceVariant = Color(0xFFC0C8CD),
    outline = Color(0xFF8A9297),
    outlineVariant = Color(0xFF40484C)
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
    background = Color(0xFFF9FAFC),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFF9FAFC),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFE1E7EC),
    onSurfaceVariant = Color(0xFF40484C),
    outline = Color(0xFF70787D),
    outlineVariant = Color(0xFFC0C8CD)
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
        surfaceVariant = Color(0xFFE3E7F0),
        onSurfaceVariant = Color(0xFF43474E),
        outline = Color(0xFF73777F),
        outlineVariant = Color(0xFFC3C6CF)
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
        surfaceVariant = Color(0xFFE1EAE1),
        onSurfaceVariant = Color(0xFF414942),
        outline = Color(0xFF717971),
        outlineVariant = Color(0xFFC1C9BF)
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
        surfaceVariant = Color(0xFFE9E2EE),
        onSurfaceVariant = Color(0xFF49454F),
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0)
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
        surfaceVariant = Color(0xFFF4E1DF),
        onSurfaceVariant = Color(0xFF534341),
        outline = Color(0xFF857371),
        outlineVariant = Color(0xFFD8C2BF)
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
        surfaceVariant = Color(0xFFF2E0E5),
        onSurfaceVariant = Color(0xFF514347),
        outline = Color(0xFF837377),
        outlineVariant = Color(0xFFD5C2C6)
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
        surfaceVariant = Color(0xFF2D333D),
        onSurfaceVariant = Color(0xFFC3C6CF),
        outline = Color(0xFF8D9199),
        outlineVariant = Color(0xFF43474E)
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
        surfaceVariant = Color(0xFF2B342D),
        onSurfaceVariant = Color(0xFFC1C9BF),
        outline = Color(0xFF8B938A),
        outlineVariant = Color(0xFF414942)
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
        surfaceVariant = Color(0xFF322E3B),
        onSurfaceVariant = Color(0xFFCAC4D0),
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F)
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
        surfaceVariant = Color(0xFF3B2B2A),
        onSurfaceVariant = Color(0xFFD8C2BF),
        outline = Color(0xFFA08C8A),
        outlineVariant = Color(0xFF534341)
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
        surfaceVariant = Color(0xFF392B30),
        onSurfaceVariant = Color(0xFFD5C2C6),
        outline = Color(0xFF9E8C90),
        outlineVariant = Color(0xFF514347)
    )
}

/**
 * Generates an opaque Material 3 ColorScheme calibrated to one of the 9 DynamicSchemeVariants
 * derived from the Monet seed (or fallback seed) without transparent alpha bleeding.
 */
private fun applyDynamicSchemeVariant(
    baseScheme: ColorScheme,
    variant: DynamicVariantOption,
    darkTheme: Boolean
): ColorScheme {
    if (variant == DynamicVariantOption.TONAL_SPOT) return baseScheme

    val seedHue = rgbToHue(baseScheme.primary)
    return when (variant) {
        DynamicVariantOption.MONOCHROME -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFE2E2E2),
                    onPrimary = Color(0xFF1B1B1B),
                    primaryContainer = Color(0xFF474747),
                    onPrimaryContainer = Color(0xFFF2F2F2),
                    secondary = Color(0xFFC6C6C6),
                    onSecondary = Color(0xFF262626),
                    secondaryContainer = Color(0xFF3B3B3B),
                    onSecondaryContainer = Color(0xFFE2E2E2),
                    tertiary = Color(0xFFA8A8A8),
                    onTertiary = Color(0xFF1F1F1F),
                    tertiaryContainer = Color(0xFF444444),
                    onTertiaryContainer = Color(0xFFE8E8E8),
                    background = Color(0xFF121212),
                    onBackground = Color(0xFFE5E5E5),
                    surface = Color(0xFF121212),
                    onSurface = Color(0xFFE5E5E5),
                    surfaceVariant = Color(0xFF2C2C2C),
                    onSurfaceVariant = Color(0xFFC4C4C4),
                    outline = Color(0xFF8E8E8E),
                    outlineVariant = Color(0xFF444444)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF2F2F2F),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFE0E0E0),
                    onPrimaryContainer = Color(0xFF111111),
                    secondary = Color(0xFF5E5E5E),
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFFEAEAEA),
                    onSecondaryContainer = Color(0xFF1B1B1B),
                    tertiary = Color(0xFF4A4A4A),
                    onTertiary = Color.White,
                    tertiaryContainer = Color(0xFFDADADA),
                    onTertiaryContainer = Color(0xFF161616),
                    background = Color(0xFFFAFAFA),
                    onBackground = Color(0xFF161616),
                    surface = Color(0xFFFAFAFA),
                    onSurface = Color(0xFF161616),
                    surfaceVariant = Color(0xFFE5E5E5),
                    onSurfaceVariant = Color(0xFF474747),
                    outline = Color(0xFF767676),
                    outlineVariant = Color(0xFFC8C8C8)
                )
            }
        }
        DynamicVariantOption.NEUTRAL -> buildTonalVariantFromHues(
            primaryHue = seedHue,
            secondaryHue = seedHue,
            tertiaryHue = (seedHue + 30f) % 360f,
            saturation = 0.22f,
            darkTheme = darkTheme
        )
        DynamicVariantOption.VIBRANT -> buildTonalVariantFromHues(
            primaryHue = seedHue,
            secondaryHue = (seedHue + 28f) % 360f,
            tertiaryHue = (seedHue + 56f) % 360f,
            saturation = 0.84f,
            darkTheme = darkTheme
        )
        DynamicVariantOption.EXPRESSIVE -> buildTonalVariantFromHues(
            primaryHue = (seedHue + 18f) % 360f,
            secondaryHue = (seedHue + 140f) % 360f,
            tertiaryHue = (seedHue + 265f) % 360f,
            saturation = 0.72f,
            darkTheme = darkTheme
        )
        DynamicVariantOption.RAINBOW -> buildTonalVariantFromHues(
            primaryHue = seedHue,
            secondaryHue = (seedHue + 120f) % 360f,
            tertiaryHue = (seedHue + 240f) % 360f,
            saturation = 0.68f,
            darkTheme = darkTheme
        )
        DynamicVariantOption.FRUIT_SALAD -> buildTonalVariantFromHues(
            primaryHue = (seedHue + 35f) % 360f,
            secondaryHue = (seedHue + 195f) % 360f,
            tertiaryHue = (seedHue + 310f) % 360f,
            saturation = 0.76f,
            darkTheme = darkTheme
        )
        DynamicVariantOption.FIDELITY -> buildTonalVariantFromHues(
            primaryHue = seedHue,
            secondaryHue = seedHue,
            tertiaryHue = (seedHue + 45f) % 360f,
            saturation = 0.64f,
            darkTheme = darkTheme
        )
        DynamicVariantOption.CONTENT -> buildTonalVariantFromHues(
            primaryHue = seedHue,
            secondaryHue = (seedHue + 15f) % 360f,
            tertiaryHue = (seedHue + 60f) % 360f,
            saturation = 0.52f,
            darkTheme = darkTheme
        )
        DynamicVariantOption.TONAL_SPOT -> baseScheme
    }
}

private fun buildTonalVariantFromHues(
    primaryHue: Float,
    secondaryHue: Float,
    tertiaryHue: Float,
    saturation: Float,
    darkTheme: Boolean
): ColorScheme {
    return if (darkTheme) {
        val bg = hslColor(primaryHue, saturation * 0.14f, 0.08f)
        darkColorScheme(
            primary = hslColor(primaryHue, saturation, 0.76f),
            onPrimary = hslColor(primaryHue, saturation, 0.16f),
            primaryContainer = hslColor(primaryHue, saturation * 0.85f, 0.28f),
            onPrimaryContainer = hslColor(primaryHue, saturation, 0.90f),
            secondary = hslColor(secondaryHue, saturation * 0.55f, 0.78f),
            onSecondary = hslColor(secondaryHue, saturation * 0.55f, 0.18f),
            secondaryContainer = hslColor(secondaryHue, saturation * 0.45f, 0.28f),
            onSecondaryContainer = hslColor(secondaryHue, saturation * 0.6f, 0.88f),
            tertiary = hslColor(tertiaryHue, saturation * 0.7f, 0.78f),
            onTertiary = hslColor(tertiaryHue, saturation * 0.7f, 0.18f),
            tertiaryContainer = hslColor(tertiaryHue, saturation * 0.6f, 0.28f),
            onTertiaryContainer = hslColor(tertiaryHue, saturation * 0.7f, 0.90f),
            background = bg,
            onBackground = Color(0xFFE5E4E8),
            surface = bg,
            onSurface = Color(0xFFE5E4E8),
            surfaceVariant = hslColor(primaryHue, saturation * 0.18f, 0.20f),
            onSurfaceVariant = hslColor(primaryHue, saturation * 0.22f, 0.76f),
            outline = hslColor(primaryHue, saturation * 0.18f, 0.56f),
            outlineVariant = hslColor(primaryHue, saturation * 0.18f, 0.30f)
        )
    } else {
        val bg = hslColor(primaryHue, saturation * 0.16f, 0.985f)
        lightColorScheme(
            primary = hslColor(primaryHue, saturation, 0.36f),
            onPrimary = Color.White,
            primaryContainer = hslColor(primaryHue, saturation * 0.85f, 0.90f),
            onPrimaryContainer = hslColor(primaryHue, saturation, 0.12f),
            secondary = hslColor(secondaryHue, saturation * 0.48f, 0.40f),
            onSecondary = Color.White,
            secondaryContainer = hslColor(secondaryHue, saturation * 0.50f, 0.90f),
            onSecondaryContainer = hslColor(secondaryHue, saturation * 0.55f, 0.14f),
            tertiary = hslColor(tertiaryHue, saturation * 0.65f, 0.40f),
            onTertiary = Color.White,
            tertiaryContainer = hslColor(tertiaryHue, saturation * 0.75f, 0.90f),
            onTertiaryContainer = hslColor(tertiaryHue, saturation * 0.7f, 0.14f),
            background = bg,
            onBackground = Color(0xFF1A1B1E),
            surface = bg,
            onSurface = Color(0xFF1A1B1E),
            surfaceVariant = hslColor(primaryHue, saturation * 0.20f, 0.90f),
            onSurfaceVariant = hslColor(primaryHue, saturation * 0.20f, 0.32f),
            outline = hslColor(primaryHue, saturation * 0.18f, 0.46f),
            outlineVariant = hslColor(primaryHue, saturation * 0.20f, 0.78f)
        )
    }
}

private fun hslColor(hue: Float, sat: Float, light: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val s = sat.coerceIn(0f, 1f)
    val l = light.coerceIn(0f, 1f)
    return Color.hsl(h, s, l)
}

private fun rgbToHue(color: Color): Float {
    val r = color.red
    val g = color.green
    val b = color.blue
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC
    if (delta < 0.0001f) return 210f
    val h = when (maxC) {
        r -> ((g - b) / delta) % 6f
        g -> (b - r) / delta + 2f
        else -> (r - g) / delta + 4f
    } * 60f
    return ((h % 360f) + 360f) % 360f
}

/**
 * Calibrated opaque surface container hierarchy helpers so surfaces never use translucent
 * alpha blending or bleed through background layers.
 */
fun ColorScheme.opaqueContainer(level: Float): Color =
    lerp(surface, surfaceVariant, level.coerceIn(0f, 1f))

@Composable
fun WasmTheme(
    themeMode: String = ThemeModeOption.SYSTEM.id,
    dynamicColor: Boolean = true,
    colorVariant: String = ColorVariantOption.DEFAULT.id,
    dynamicVariant: String = DynamicVariantOption.TONAL_SPOT.id,
    fontSelection: String = AppFontOption.GOOGLE_SANS_FLEX.id,
    axisConfig: GoogleSansFlexAxisConfig = GoogleSansFlexAxisConfig(),
    emojiFont: String = EmojiPackOption.DEFAULT.id,
    emojiRevision: Int = 0,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (ThemeModeOption.fromId(themeMode)) {
        ThemeModeOption.SYSTEM -> systemDark
        ThemeModeOption.LIGHT -> false
        ThemeModeOption.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = if (dynamicColor) {
        val baseMonet = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
        applyDynamicSchemeVariant(
            baseScheme = baseMonet,
            variant = DynamicVariantOption.fromId(dynamicVariant),
            darkTheme = darkTheme
        )
    } else {
        val staticVariant = ColorVariantOption.fromId(colorVariant)
        if (darkTheme) variantDarkColorScheme(staticVariant) else variantLightColorScheme(staticVariant)
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

    val isGoogleSansFlex = AppFontOption.fromId(fontSelection) == AppFontOption.GOOGLE_SANS_FLEX
    val appFontFamily = remember(context, fontSelection, axisConfig, emojiFont, emojiRevision) {
        getSelectedAppFontFamily(context, fontSelection, axisConfig, emojiFont, emojiRevision)
    }
    val monoFontFamily = remember(context, emojiFont, emojiRevision) {
        getGoogleSansCodeFontFamily(context, emojiFont, emojiRevision)
    }
    val typography = remember(appFontFamily, axisConfig, isGoogleSansFlex) {
        buildWasmTypography(appFontFamily, axisConfig, isGoogleSansFlex)
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalWasmMonoFontFamily provides monoFontFamily
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
