package com.mrdartsidetm.wasm.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.FontVariationAxis
import android.os.Build
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mrdartsidetm.wasm.R

@Immutable
data class GoogleSansFlexAxisConfig(
    val grade: Float = 0f,
    val weight: Float = 400f,
    val width: Float = 100f,
    val roundness: Float = 100f,
    val opticalSize: Float = 24f,
    val slant: Float = 0f
)

/**
 * Supported app typography options selectable from Font Selection screen.
 */
enum class AppFontOption(val id: String, val displayName: String) {
    GOOGLE_SANS_FLEX("google_sans_flex", "Google Sans Flex"),
    SF_PRO("sf_pro", "SF Pro Display"),
    SYSTEM("system", "System Default"),
    MONOSPACE("monospace", "Google Sans Code (Monospace)");

    companion object {
        fun fromId(id: String): AppFontOption =
            values().firstOrNull { it.id == id } ?: GOOGLE_SANS_FLEX
    }
}

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
 * Google Sans Code (Google Sans Mono) variable FontFamily for monospace blocks (`code` and ```mono```)
 * as well as the Monospace app font option.
 */
@OptIn(ExperimentalTextApi::class)
val GoogleSansCodeFontFamily: FontFamily = FontFamily(
    Font(
        resId = R.font.google_sans_code,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.Setting("MONO", 1f)
        )
    ),
    Font(
        resId = R.font.google_sans_code,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500),
            FontVariation.Setting("MONO", 1f)
        )
    ),
    Font(
        resId = R.font.google_sans_code,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.Setting("MONO", 1f)
        )
    ),
    Font(
        resId = R.font.google_sans_code_italic,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.Setting("MONO", 1f)
        )
    )
)

/**
 * Exact Figma Google Sans Flex axis configurations for Wasm Onboarding & Wordmarks.
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

// Individual FontFamily aliases mapped to Google Sans Flex / SF Pro
val SfProBold = createGoogleSansFlexFamily(weight = 700)
val SfProItalic = createGoogleSansFlexFamily(weight = 400, slant = -10f, style = FontStyle.Italic)
val SfProBoldItalic = createGoogleSansFlexFamily(weight = 700, slant = -10f, style = FontStyle.Italic)
val SfProRegular = createGoogleSansFlexFamily(weight = 400)
val SfProMedium = createGoogleSansFlexFamily(weight = 500)
val SfProSemibold = createGoogleSansFlexFamily(weight = 600)

fun getSfProFontFamily(
    context: Context,
    emojiPackId: String = EmojiPackOption.DEFAULT.id,
    emojiRevision: Int = 0
): FontFamily {
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
                if (familyBuilder == null) familyBuilder = android.graphics.fonts.FontFamily.Builder(f)
                else familyBuilder.addFont(f)
            }
            val primaryFamily = familyBuilder?.build()
            if (primaryFamily != null) {
                return buildCompositeTypefaceFamily(primaryFamily, context, emojiPackId, emojiRevision)
            }
        } catch (_: Throwable) {
        }
    }
    return SfProDisplayFontFamily
}

fun getGoogleSansCodeFontFamily(
    context: Context,
    emojiPackId: String = EmojiPackOption.DEFAULT.id,
    emojiRevision: Int = 0
): FontFamily {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val weights = listOf(300, 400, 500, 600, 700, 800)
            var familyBuilder: android.graphics.fonts.FontFamily.Builder? = null
            for (w in weights) {
                val f = android.graphics.fonts.Font.Builder(context.resources, R.font.google_sans_code)
                    .setFontVariationSettings(
                        arrayOf(
                            FontVariationAxis("wght", w.toFloat()),
                            FontVariationAxis("MONO", 1f)
                        )
                    )
                    .build()
                if (familyBuilder == null) familyBuilder = android.graphics.fonts.FontFamily.Builder(f)
                else familyBuilder.addFont(f)
            }
            val primaryFamily = familyBuilder?.build()
            if (primaryFamily != null) {
                return buildCompositeTypefaceFamily(primaryFamily, context, emojiPackId, emojiRevision)
            }
        } catch (_: Throwable) {
        }
    }
    return GoogleSansCodeFontFamily
}

fun getAppFontFamily(
    context: Context,
    axisConfig: GoogleSansFlexAxisConfig = GoogleSansFlexAxisConfig(),
    emojiPackId: String = EmojiPackOption.DEFAULT.id,
    emojiRevision: Int = 0
): FontFamily {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val baseWeight = axisConfig.weight.coerceIn(100f, 900f)
            // Build multi-weight family biased around the user's chosen weight, width, grade, roundness, opsz, and slant
            val weightOffsets = listOf(-200f, -100f, 0f, 150f, 300f)
            var familyBuilder: android.graphics.fonts.FontFamily.Builder? = null
            val wd = axisConfig.width.coerceIn(50f, 150f)
            val gr = axisConfig.grade.coerceIn(-200f, 150f)
            val rn = axisConfig.roundness.coerceIn(0f, 100f)
            val op = axisConfig.opticalSize.coerceIn(8f, 144f)
            val sl = axisConfig.slant.coerceIn(-10f, 0f)

            for (offset in weightOffsets) {
                val w = (baseWeight + offset).coerceIn(100f, 1000f)
                val f = android.graphics.fonts.Font.Builder(context.resources, R.font.google_sans_flex)
                    .setFontVariationSettings(
                        arrayOf(
                            FontVariationAxis("wght", w),
                            FontVariationAxis("wdth", wd),
                            FontVariationAxis("GRAD", gr),
                            FontVariationAxis("ROND", rn),
                            FontVariationAxis("opsz", op),
                            FontVariationAxis("slnt", sl)
                        )
                    )
                    .build()
                if (familyBuilder == null) familyBuilder = android.graphics.fonts.FontFamily.Builder(f)
                else familyBuilder.addFont(f)
            }
            val primaryFamily = familyBuilder?.build()
            if (primaryFamily != null) {
                return buildCompositeTypefaceFamily(primaryFamily, context, emojiPackId, emojiRevision)
            }
        } catch (_: Throwable) {
        }
    }
    return createGoogleSansFlexFamily(
        weight = axisConfig.weight.toInt(),
        width = axisConfig.width,
        grade = axisConfig.grade,
        roundness = axisConfig.roundness,
        opticalSize = axisConfig.opticalSize,
        slant = axisConfig.slant
    )
}

fun getSystemFontFamilyWithEmoji(
    context: Context,
    emojiPackId: String = EmojiPackOption.DEFAULT.id,
    emojiRevision: Int = 0
): FontFamily {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val emojiFamily = EmojiFontManager.getCachedPlatformEmojiFamily(context, emojiPackId, emojiRevision)
            ?: return FontFamily.Default
        try {
            val robotoFonts = android.graphics.fonts.SystemFonts.getAvailableFonts()
                .filter { f ->
                    val name = f.file?.name ?: return@filter false
                    name.startsWith("Roboto-", ignoreCase = true) && name.endsWith(".ttf", ignoreCase = true)
                }
            var familyBuilder: android.graphics.fonts.FontFamily.Builder? = null
            for (rf in robotoFonts.take(10)) {
                val file = rf.file ?: continue
                val built = android.graphics.fonts.Font.Builder(file).build()
                if (familyBuilder == null) familyBuilder = android.graphics.fonts.FontFamily.Builder(built)
                else familyBuilder.addFont(built)
            }
            val primary = familyBuilder?.build()
            if (primary != null) {
                val compositeTypeface = Typeface.CustomFallbackBuilder(primary)
                    .addCustomFallback(emojiFamily)
                    .setSystemFallback("sans-serif")
                    .build()
                return FontFamily(compositeTypeface)
            }
        } catch (_: Throwable) {
        }
    }
    return FontFamily.Default
}

private fun buildCompositeTypefaceFamily(
    primaryFamily: android.graphics.fonts.FontFamily,
    context: Context,
    emojiPackId: String,
    emojiRevision: Int
): FontFamily {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return FontFamily.Default
    }
    val emojiFamily = EmojiFontManager.getCachedPlatformEmojiFamily(context, emojiPackId, emojiRevision)
    val builder = Typeface.CustomFallbackBuilder(primaryFamily)
    if (emojiFamily != null) {
        builder.addCustomFallback(emojiFamily)
    }
    builder.setSystemFallback("sans-serif")
    return FontFamily(builder.build())
}

fun getSelectedAppFontFamily(
    context: Context,
    fontSelection: String,
    axisConfig: GoogleSansFlexAxisConfig = GoogleSansFlexAxisConfig(),
    emojiPackId: String = EmojiPackOption.DEFAULT.id,
    emojiRevision: Int = 0
): FontFamily {
    return when (AppFontOption.fromId(fontSelection)) {
        AppFontOption.GOOGLE_SANS_FLEX -> getAppFontFamily(context, axisConfig, emojiPackId, emojiRevision)
        AppFontOption.SF_PRO -> getSfProFontFamily(context, emojiPackId, emojiRevision)
        AppFontOption.SYSTEM -> getSystemFontFamilyWithEmoji(context, emojiPackId, emojiRevision)
        AppFontOption.MONOSPACE -> getGoogleSansCodeFontFamily(context, emojiPackId, emojiRevision)
    }
}

fun buildWasmTypography(
    fontFamily: FontFamily,
    axisConfig: GoogleSansFlexAxisConfig = GoogleSansFlexAxisConfig(),
    isGoogleSansFlex: Boolean = true
): Typography {
    val bodyWeight = if (isGoogleSansFlex) FontWeight(axisConfig.weight.toInt().coerceIn(100, 900)) else FontWeight.Normal
    val bodyStyle = if (isGoogleSansFlex && axisConfig.slant <= -3f) FontStyle.Italic else FontStyle.Normal

    return Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = bodyWeight,
            fontStyle = bodyStyle,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = bodyWeight,
            fontStyle = bodyStyle,
            fontSize = 45.sp,
            lineHeight = 52.sp
        ),
        displaySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = bodyWeight,
            fontStyle = bodyStyle,
            fontSize = 36.sp,
            lineHeight = 44.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontStyle = bodyStyle,
            fontSize = 32.sp,
            lineHeight = 40.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontStyle = bodyStyle,
            fontSize = 28.sp,
            lineHeight = 36.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontStyle = bodyStyle,
            fontSize = 24.sp,
            lineHeight = 32.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontStyle = bodyStyle,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontStyle = bodyStyle,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontStyle = bodyStyle,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = bodyWeight,
            fontStyle = bodyStyle,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = bodyWeight,
            fontStyle = bodyStyle,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = bodyWeight,
            fontStyle = bodyStyle,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontStyle = bodyStyle,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontStyle = bodyStyle,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontStyle = bodyStyle,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}
