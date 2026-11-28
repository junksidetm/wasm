package com.mrdartsidetm.wasm.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdartsidetm.wasm.data.UserPreferencesRepository
import com.mrdartsidetm.wasm.ui.motion.WasmMotionTokens
import com.mrdartsidetm.wasm.ui.motion.wasmFeatheredVerticalViewport
import com.mrdartsidetm.wasm.ui.motion.wasmLuminousElasticClickable
import com.mrdartsidetm.wasm.ui.theme.ColorVariantOption
import com.mrdartsidetm.wasm.ui.theme.DynamicVariantOption
import com.mrdartsidetm.wasm.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.launch

/**
 * Dedicated Dynamic Color & Scheme Variants screen.
 * When Dynamic Color is ON, displays all 9 Material 3 DynamicSchemeVariant palettes.
 * When Dynamic Color is OFF, displays static Material 3 color palettes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorVariantsScreen(
    prefs: UserPreferencesRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)
    val dynamicEnabled by prefs.dynamicColorEnabled.collectAsStateWithLifecycle(initialValue = true)
    val selectedDynamicId by prefs.dynamicVariant.collectAsStateWithLifecycle(initialValue = DynamicVariantOption.TONAL_SPOT.id)
    val selectedStaticId by prefs.colorVariant.collectAsStateWithLifecycle(initialValue = ColorVariantOption.DEFAULT.id)
    val isDark = LocalIsDarkTheme.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dynamic Color & Variants",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Dynamic Color Master Toggle Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .wasmLuminousElasticClickable(
                        shape = RoundedCornerShape(22.dp),
                        maxStretchPercent = 0.025f,
                        pressedScale = 0.975f,
                        onClick = { scope.launch { prefs.saveDynamicColorEnabled(!dynamicEnabled) } }
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dynamic Color",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (dynamicEnabled) {
                                "Material You wallpaper scheme variants active"
                            } else {
                                "Classic fixed Material 3 color palettes active"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .width(1.dp)
                            .height(28.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Switch(
                        checked = dynamicEnabled,
                        onCheckedChange = { checked ->
                            scope.launch { prefs.saveDynamicColorEnabled(checked) }
                        }
                    )
                }
            }

            AnimatedContent(
                targetState = dynamicEnabled,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = spring(stiffness = 380f, dampingRatio = 0.80f))) togetherWith
                        (fadeOut(tween(160)) + scaleOut(targetScale = 0.97f, animationSpec = tween(160)))
                },
                label = "ColorVariantsGridTransition",
                modifier = Modifier.weight(1f)
            ) { isDynamicMode ->
                if (isDynamicMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Material 3 Dynamic Variants",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .wasmFeatheredVerticalViewport(topFadeFraction = 0.02f, bottomFadeFraction = 0.06f)
                        ) {
                            items(DynamicVariantOption.values(), key = { it.id }) { option ->
                                val selected = option.id == selectedDynamicId
                                val topColor = if (isDark) option.darkTop else option.lightTop
                                val blColor = if (isDark) option.darkBottomLeft else option.lightBottomLeft
                                val brColor = if (isDark) option.darkBottomRight else option.lightBottomRight
                                TriTonePaletteCard(
                                    label = option.displayName,
                                    topColor = topColor,
                                    bottomLeftColor = blColor,
                                    bottomRightColor = brColor,
                                    isSelected = selected,
                                    onClick = {
                                        scope.launch { prefs.saveDynamicVariant(option.id) }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Static Palettes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(ColorVariantOption.values(), key = { it.id }) { option ->
                                val selected = option.id == selectedStaticId
                                TriTonePaletteCard(
                                    label = option.displayName,
                                    topColor = option.previewPrimary,
                                    bottomLeftColor = option.previewSecondary,
                                    bottomRightColor = option.previewTertiary,
                                    isSelected = selected,
                                    onClick = {
                                        scope.launch { prefs.saveColorVariant(option.id) }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TriTonePaletteCard(
    label: String,
    topColor: Color,
    bottomLeftColor: Color,
    bottomRightColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        animationSpec = tween(220),
        label = "paletteBorderColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.72f),
        label = "paletteBorderWidth"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(220),
        label = "paletteContainerColor"
    )
    val outlineStroke = MaterialTheme.colorScheme.outlineVariant

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .wasmLuminousElasticClickable(
                shape = RoundedCornerShape(20.dp),
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val diameter = size.minDimension
                    val arcSize = Size(diameter, diameter)
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

                    // Top semi-circle (-180..0)
                    drawArc(
                        color = topColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = topLeft,
                        size = arcSize
                    )
                    // Bottom-left quarter (90..180)
                    drawArc(
                        color = bottomLeftColor,
                        startAngle = 90f,
                        sweepAngle = 90f,
                        useCenter = true,
                        topLeft = topLeft,
                        size = arcSize
                    )
                    // Bottom-right quarter (0..90)
                    drawArc(
                        color = bottomRightColor,
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = true,
                        topLeft = topLeft,
                        size = arcSize
                    )
                    // Crisp inner ring border
                    drawCircle(
                        color = outlineStroke.copy(alpha = 0.45f),
                        radius = diameter / 2f,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn(initialScale = 0.55f, animationSpec = WasmMotionTokens.SpringSelectionTick) + fadeIn(tween(110)),
                    exit = scaleOut(targetScale = 0.7f, animationSpec = tween(140)) + fadeOut(tween(110))
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
