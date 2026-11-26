package com.mrdartsidetm.wasm.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrdartsidetm.wasm.ui.localization.LocalWasmStrings
import com.mrdartsidetm.wasm.ui.localization.SupportedLanguage
import com.mrdartsidetm.wasm.ui.localization.SupportedLanguages
import com.mrdartsidetm.wasm.ui.theme.GoogleSansFlexOnboarding535
import kotlin.math.min

/**
 * Language Changer screen matching both Figma variants:
 * - Onboarding (`isOnboardingStyle = true`): Rotated blue SVG backdrop + animated 4-Leaf Clover + white Globe & heading.
 * - Main App (`isOnboardingStyle = false`): Clean Monet Light/Dark surface + outlined Globe + bordered pill buttons (`main/Language Changer.svg`).
 */
@Composable
fun LanguageChangerScreen(
    selectedLanguageCode: String,
    onSelectLanguage: (SupportedLanguage) -> Unit,
    onBack: () -> Unit,
    isOnboardingStyle: Boolean = true,
    modifier: Modifier = Modifier
) {
    val strings = LocalWasmStrings.current

    BackHandler(onBack = onBack)

    val infiniteTransition = rememberInfiniteTransition(label = "CloverRotation")
    val cloverRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CloverAxis"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (isOnboardingStyle) Modifier else Modifier.background(MaterialTheme.colorScheme.background)
            )
    ) {
        if (isOnboardingStyle) {
            OnboardingSvgBackground(
                startNormalized = OnboardingBackgroundPresets.Screen2And4AndLangStart,
                endNormalized = OnboardingBackgroundPresets.Screen2And4AndLangEnd
            )
        }
        val isCompactHeight = maxHeight < 680.dp
        val iconSize = if (isOnboardingStyle) {
            if (isCompactHeight) 156.dp else 204.dp
        } else {
            if (isCompactHeight) 96.dp else 118.dp
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar back affordance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = if (isOnboardingStyle) {
                        Color.White.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = if (isOnboardingStyle) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = strings.back,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (isOnboardingStyle) {
                // Onboarding: 4 Leaf Clover - Move + White Globe Icon
                ExpressiveGroupedBadge(
                    shapePathData = OnboardingSvgPaths.cloverPath,
                    shapeSrcWidth = OnboardingSvgPaths.cloverWidth,
                    shapeSrcHeight = OnboardingSvgPaths.cloverHeight,
                    shapeColor = OnboardingCloverOrange,
                    shapeRotation = cloverRotation,
                    iconPaths = listOf(OnboardingSvgPaths.globeIconPath),
                    iconColors = listOf(Color.White),
                    iconSrcWidth = OnboardingSvgPaths.globeIconWidth,
                    iconSrcHeight = OnboardingSvgPaths.globeIconHeight,
                    iconScaleFraction = 0.58f,
                    modifier = Modifier.size(iconSize)
                )
            } else {
                // Main App (`main/Language Changer.svg`): Clean Globe Icon
                val globePath = remember { PathParser().parsePathString(OnboardingSvgPaths.globeIconPath).toPath() }
                val globeTint = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.size(iconSize)) {
                    val scale = min(
                        size.width / OnboardingSvgPaths.globeIconWidth,
                        size.height / OnboardingSvgPaths.globeIconHeight
                    )
                    val dx = (size.width - OnboardingSvgPaths.globeIconWidth * scale) / 2f
                    val dy = (size.height - OnboardingSvgPaths.globeIconHeight * scale) / 2f
                    withTransform({
                        translate(dx, dy)
                        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
                    }) {
                        drawPath(path = globePath, color = globeTint)
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isCompactHeight) 12.dp else 22.dp))

            // Heading: Choose Language
            Text(
                text = strings.chooseLanguage,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = GoogleSansFlexOnboarding535,
                    fontWeight = FontWeight(535),
                    lineHeight = 42.sp,
                    textAlign = TextAlign.Center
                ),
                color = if (isOnboardingStyle) Color.White else MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(if (isCompactHeight) 16.dp else 24.dp))

            // Middle to Down Section: Scrollable 2-column grid of language pill buttons
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(SupportedLanguages, key = { _, item -> item.code }) { index, lang ->
                    val isSelected = lang.code == selectedLanguageCode
                    val topOffset = if (index % 2 == 1) 3.dp else 0.dp
                    Box(modifier = Modifier.padding(top = topOffset)) {
                        LanguagePillCard(
                            language = lang,
                            isSelected = isSelected,
                            isOnboardingStyle = isOnboardingStyle,
                            onClick = { onSelectLanguage(lang) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguagePillCard(
    language: SupportedLanguage,
    isSelected: Boolean,
    isOnboardingStyle: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isOnboardingStyle && isSelected -> Color(0xFFE8F1FF)
        isOnboardingStyle -> Color.White
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val textColor = when {
        isOnboardingStyle -> Color.Black
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderMod = when {
        isOnboardingStyle && isSelected -> Modifier.border(2.5.dp, OnboardingOuterYellow, RoundedCornerShape(32.dp))
        isOnboardingStyle -> Modifier
        isSelected -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(32.dp))
        else -> Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(32.dp))
    }

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .then(borderMod)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = language.flagEmoji,
                fontSize = 22.sp
            )
            Text(
                text = language.nativeName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (isOnboardingStyle) OnboardingButtonBlue else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
