package com.mrdartsidetm.wasm.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.FontVariationAxis
import android.os.Build
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mrdartsidetm.wasm.R

/**
 * Creates a Google Sans Flex variable FontFamily with explicit axis controls.
 */
@OptIn(ExperimentalTextApi::class)
fun createGoogleSansFlexFamily(
    weight: Int = 400,
    width: Float = 100f,
    grade: Float = 0f,
    roundness: Float = 100f,
    opticalSize: Float = 24f,
    slant: Float = 0f,
    style: FontStyle = FontStyle.Normal
): FontFamily {
    val w = weight.coerceIn(100, 1000)
    val wd = width.coerceIn(50f, 150f)
    val gr = grade.coerceIn(-200f, 150f)
    val rn = roundness.coerceIn(0f, 100f)
    val op = opticalSize.coerceIn(8f, 144f)
    val sl = slant.coerceIn(-10f, 0f)

    return FontFamily(
        Font(
            resId = R.font.google_sans_flex,
            weight = FontWeight(w),
            style = style,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(w),
                FontVariation.width(wd),
                FontVariation.slant(sl),
                FontVariation.Setting("GRAD", gr),
                FontVariation.Setting("ROND", rn),
                FontVariation.Setting("opsz", op)
            )
        )
    )
}

/**
 * Exact Figma Google Sans Flex axis configurations for Wasm Onboarding & Language Changer:
 * - GoogleSansFlexOnboarding535: wght=535, GRAD=36, ROND=100, wdth=119, opsz=24
 *   ("Get started", "How do you want to start?", "Choose Language", Screen 4 "Whatsapp"/"Instagram",
 *    "Everything is set", "en", "Natively Supports", all "Hello" greetings, "Next", "Let's Dive In")
 * - GoogleSansFlexOnboarding875: wght=875, GRAD=36, ROND=100, wdth=105, opsz=24
 *   ("Whatsapp & Instagram*", Screen 3 "Whatsapp"/"Instagram")
 * - GoogleSansFlexMetaMedium: wght=500, ROND=100, opsz=24
 *   ("Whatsapp and Instagram are copyright under Meta Services.")
 */
val GoogleSansFlexOnboarding535: FontFamily = createGoogleSansFlexFamily(
    weight = 535,
    width = 119f,
    grade = 36f,
    roundness = 100f,
    opticalSize = 24f
)

val GoogleSansFlexOnboarding875: FontFamily = createGoogleSansFlexFamily(
    weight = 875,
    width = 105f,
    grade = 36f,
    roundness = 100f,
    opticalSize = 24f
)

val GoogleSansFlexMetaMedium: FontFamily = createGoogleSansFlexFamily(
    weight = 500,
    width = 100f,
    grade = 0f,
    roundness = 100f,
    opticalSize = 24f
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexDisplayFontFamily = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Thin,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(100),
            FontVariation.Setting("ROND", 100f),
            FontVariation.Setting("opsz", 24f)
        )
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(300),
            FontVariation.Setting("ROND", 100f),
            FontVariation.Setting("opsz", 24f)
        )
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.Setting("ROND", 100f),
            FontVariation.Setting("opsz", 24f)
        )
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.slant(-10f),
            FontVariation.Setting("ROND", 100f),
            FontVariation.Setting("opsz", 24f)
        )
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500),
            FontVariation.Setting("ROND", 100f),
            FontVariation.Setting("opsz", 24f)
        )
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
            FontVariation.Setting("ROND", 100f),
            FontVariation.Setting("opsz", 24f)
        )
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.Setting("ROND", 100f),
            FontVariation.Setting("opsz", 24f)
        )
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Bold,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.slant(-10f),
            FontVariation.Setting("ROND", 100f),
            FontVariation.Setting("opsz", 24f)
        )
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(800),
            FontVariation.Setting("ROND", 100f),
            FontVariation.Setting("opsz", 24f)
        )
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(900),
            FontVariation.Setting("ROND", 100f),
            FontVariation.Setting("opsz", 24f)
        )
    )
)

// Individual FontFamily aliases mapped to Google Sans Flex so the entire UI uses Google Sans Flex uniformly
val SfProBold = createGoogleSansFlexFamily(weight = 700)
val SfProItalic = createGoogleSansFlexFamily(weight = 400, slant = -10f, style = FontStyle.Italic)
val SfProBoldItalic = createGoogleSansFlexFamily(weight = 700, slant = -10f, style = FontStyle.Italic)
val SfProRegular = createGoogleSansFlexFamily(weight = 400)
val SfProMedium = createGoogleSansFlexFamily(weight = 500)
val SfProMediumItalic = createGoogleSansFlexFamily(weight = 500, slant = -10f, style = FontStyle.Italic)
val SfProSemibold = createGoogleSansFlexFamily(weight = 600)
val SfProSemiboldItalic = createGoogleSansFlexFamily(weight = 600, slant = -10f, style = FontStyle.Italic)
val SfProLight = createGoogleSansFlexFamily(weight = 300)
val SfProLightItalic = createGoogleSansFlexFamily(weight = 300, slant = -10f, style = FontStyle.Italic)
val SfProThin = createGoogleSansFlexFamily(weight = 100)
val SfProThinItalic = createGoogleSansFlexFamily(weight = 100, slant = -10f, style = FontStyle.Italic)
val SfProUltralight = createGoogleSansFlexFamily(weight = 200)
val SfProUltralightItalic = createGoogleSansFlexFamily(weight = 200, slant = -10f, style = FontStyle.Italic)
val SfProHeavy = createGoogleSansFlexFamily(weight = 800)
val SfProHeavyItalic = createGoogleSansFlexFamily(weight = 800, slant = -10f, style = FontStyle.Italic)
val SfProBlack = createGoogleSansFlexFamily(weight = 900)
val SfProBlackItalic = createGoogleSansFlexFamily(weight = 900, slant = -10f, style = FontStyle.Italic)

/**
 * Supported app typography options selectable from General Settings.
 */
enum class AppFontOption(val id: String, val displayName: String) {
    GOOGLE_SANS_FLEX("google_sans_flex", "Google Sans Flex"),
    SF_PRO("sf_pro", "SF Pro Display"),
    SYSTEM("system", "System Default"),
    MONOSPACE("monospace", "Monospace");

    companion object {
        fun fromId(id: String): AppFontOption =
            values().firstOrNull { it.id == id } ?: GOOGLE_SANS_FLEX
    }
}

val SfProDisplayFontFamily = FontFamily(
    Font(resId = R.font.sf_pro_display_thin, weight = FontWeight.Thin),
    Font(resId = R.font.sf_pro_display_ultralight, weight = FontWeight.ExtraLight),
    Font(resId = R.font.sf_pro_display_light, weight = FontWeight.Light),
    Font(resId = R.font.sf_pro_display_regular, weight = FontWeight.Normal),
    Font(resId = R.font.sf_pro_display_regular_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(resId = R.font.sf_pro_display_medium, weight = FontWeight.Medium),
    Font(resId = R.font.sf_pro_display_semibold, weight = FontWeight.SemiBold),
    Font(resId = R.font.sf_pro_display_bold, weight = FontWeight.Bold),
    Font(resId = R.font.sf_pro_display_bold_italic, weight = FontWeight.Bold, style = FontStyle.Italic),
    Font(resId = R.font.sf_pro_display_heavy, weight = FontWeight.ExtraBold),
    Font(resId = R.font.sf_pro_display_black, weight = FontWeight.Black)
)

/**
 * Builds SF Pro Display FontFamily with bundled iOS emoji fallback on Android 10+ (API 29+).
 */
fun getSfProFontFamily(context: Context): FontFamily {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val weightResPairs = listOf(
                R.font.sf_pro_display_thin,
                R.font.sf_pro_display_ultralight,
                R.font.sf_pro_display_light,
                R.font.sf_pro_display_regular,
                R.font.sf_pro_display_regular_italic,
                R.font.sf_pro_display_medium,
                R.font.sf_pro_display_semibold,
                R.font.sf_pro_display_bold,
                R.font.sf_pro_display_bold_italic,
                R.font.sf_pro_display_heavy,
                R.font.sf_pro_display_black
            )
            var familyBuilder: android.graphics.fonts.FontFamily.Builder? = null
            for (resId in weightResPairs) {
                val f = android.graphics.fonts.Font.Builder(context.resources, resId).build()
                if (familyBuilder == null) {
                    familyBuilder = android.graphics.fonts.FontFamily.Builder(f)
                } else {
                    familyBuilder.addFont(f)
                }
            }
            val primaryFamily = familyBuilder?.build()
            if (primaryFamily != null) {
                val emojiFont = android.graphics.fonts.Font.Builder(context.assets, "fonts/ios_emoji.ttf").build()
                val emojiFamily = android.graphics.fonts.FontFamily.Builder(emojiFont).build()
                val compositeTypeface = Typeface.CustomFallbackBuilder(primaryFamily)
                    .addCustomFallback(emojiFamily)
                    .build()
                return FontFamily(compositeTypeface)
            }
        } catch (_: Throwable) {
            // Fall back to static SfProDisplayFontFamily
        }
    }
    return SfProDisplayFontFamily
}

/**
 * Resolves the user's selected FontFamily with bundled iOS emoji fallback.
 */
fun getSelectedAppFontFamily(context: Context, fontSelection: String): FontFamily {
    return when (AppFontOption.fromId(fontSelection)) {
        AppFontOption.GOOGLE_SANS_FLEX -> getAppFontFamily(context)
        AppFontOption.SF_PRO -> getSfProFontFamily(context)
        AppFontOption.SYSTEM -> FontFamily.Default
        AppFontOption.MONOSPACE -> FontFamily.Monospace
    }
}

/**
 * Builds the primary Google Sans Flex FontFamily with bundled iOS emoji fallback on Android 10+ (API 29+).
 */
fun getAppFontFamily(context: Context): FontFamily {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val weights = listOf(100, 300, 400, 500, 600, 700, 800, 900)
            var familyBuilder: android.graphics.fonts.FontFamily.Builder? = null
            for (w in weights) {
                val f = android.graphics.fonts.Font.Builder(context.resources, R.font.google_sans_flex)
                    .setFontVariationSettings(
                        arrayOf(
                            FontVariationAxis("wght", w.toFloat()),
                            FontVariationAxis("ROND", 100f),
                            FontVariationAxis("opsz", 24f)
                        )
                    )
                    .build()
                if (familyBuilder == null) {
                    familyBuilder = android.graphics.fonts.FontFamily.Builder(f)
                } else {
                    familyBuilder.addFont(f)
                }
            }
            val primaryFamily = familyBuilder?.build()
            if (primaryFamily != null) {
                val emojiFont = android.graphics.fonts.Font.Builder(context.assets, "fonts/ios_emoji.ttf").build()
                val emojiFamily = android.graphics.fonts.FontFamily.Builder(emojiFont).build()

                val compositeTypeface = Typeface.CustomFallbackBuilder(primaryFamily)
                    .addCustomFallback(emojiFamily)
                    .build()

                return FontFamily(compositeTypeface)
            }
        } catch (_: Throwable) {
            // Fall back to Compose variable FontFamily
        }
    }
    return GoogleSansFlexDisplayFontFamily
}

/**
 * Builds Material 3 Expressive Typography utilizing Google Sans Flex across the entire UI.
 */
fun buildWasmTypography(fontFamily: FontFamily): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 45.sp,
            lineHeight = 52.sp
        ),
        displaySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 36.sp,
            lineHeight = 44.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 40.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 36.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}
