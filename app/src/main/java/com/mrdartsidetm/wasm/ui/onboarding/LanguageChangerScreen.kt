package com.mrdartsidetm.wasm.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrdartsidetm.wasm.ui.localization.LocalWasmStrings
import com.mrdartsidetm.wasm.ui.localization.SupportedLanguage
import com.mrdartsidetm.wasm.ui.localization.SupportedLanguages

/**
 * Language Changer screen matching the Figma "Language Changer" SVG:
 * - Background gradient (#758AEF -> #062362)
 * - Top to Middle Section: Rotating 4 Leaf Clover (#E5931F) + white Globe icon, "Choose Language" heading
 * - Middle to Down Section: 2-column scrollable grid of rounded white language pills with flags
 */
@Composable
fun LanguageChangerScreen(
    selectedLanguageCode: String,
    onSelectLanguage: (SupportedLanguage) -> Unit,
    onBack: () -> Unit,
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
            .background(
                Brush.linearGradient(
                    colors = listOf(OnboardingBgTop, OnboardingBgBottom),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        val isCompactHeight = maxHeight < 680.dp
        val iconSize = if (isCompactHeight) 156.dp else 204.dp

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
                    color = Color.White.copy(alpha = 0.16f),
                    contentColor = Color.White,
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

            // Top to Middle Section: 4 Leaf Clover - Move + Globe Icon
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

            Spacer(modifier = Modifier.height(if (isCompactHeight) 12.dp else 24.dp))

            // Heading: Choose Language
            Text(
                text = strings.chooseLanguage,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 42.sp,
                    textAlign = TextAlign.Center
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(if (isCompactHeight) 18.dp else 28.dp))

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
                    // Subtle 3.dp vertical offset on right-column items matching the Figma stagger
                    val topOffset = if (index % 2 == 1) 3.dp else 0.dp
                    Box(modifier = Modifier.padding(top = topOffset)) {
                        LanguagePillCard(
                            language = lang,
                            isSelected = isSelected,
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
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = if (isSelected) Color(0xFFE8F1FF) else Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .then(
                if (isSelected) {
                    Modifier.border(2.5.dp, OnboardingOuterYellow, RoundedCornerShape(32.dp))
                } else {
                    Modifier
                }
            )
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
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = OnboardingButtonBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
