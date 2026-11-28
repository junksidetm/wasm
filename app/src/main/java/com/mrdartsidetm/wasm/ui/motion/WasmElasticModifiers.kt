package com.mrdartsidetm.wasm.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Subtle elastic press feedback (adapted from Expressive Clock's `elasticClickable`) for icon buttons,
 * nav icons, and circular pills.
 */
fun Modifier.wasmElasticClickable(
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    onClick: () -> Unit
): Modifier = wasmLuminousElasticClickable(
    enabled = enabled,
    shape = shape,
    maxStretchPercent = 0.055f,
    maxTranslateDp = 2.0.dp,
    pressedScale = 0.94f,
    luminousRadiusScale = 0.95f,
    onClick = onClick
)

/**
 * Touch-reactive expressive interaction modifier inspired by Expressive Clock's `luminousElasticClickable`:
 * - Touch-originated radial ripple
 * - Finger-following luminous bloom tuned to light/dark Material 3 surfaces
 * - Subtle one-sided directional elastic deformation & translation
 * - Spring release overshoot (honors system reduced-motion setting)
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.wasmLuminousElasticClickable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(20.dp),
    maxStretchPercent: Float = 0.042f,
    maxTranslateDp: Dp = 1.8.dp,
    pressedScale: Float = 0.968f,
    luminousRadiusScale: Float = 0.92f,
    luminousIntensity: Float = 0.85f,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    val animatorsEnabled = rememberAnimatorsEnabled()
    val interactionSource = remember { MutableInteractionSource() }
    val isInteractionPressed by interactionSource.collectIsPressedAsState()
    val scope = rememberCoroutineScope()

    var rawTouchPosition by remember { mutableStateOf<Offset?>(null) }
    var lastTouchPosition by remember { mutableStateOf<Offset?>(null) }
    var lastRippleOrigin by remember { mutableStateOf<Offset?>(null) }
    val rippleProgress = remember { Animatable(1f) }

    var targetDx by remember { mutableFloatStateOf(0f) }
    var targetDy by remember { mutableFloatStateOf(0f) }

    val isPressed = rawTouchPosition != null || isInteractionPressed

    val animatedDx by animateFloatAsState(
        targetValue = if (animatorsEnabled) targetDx else 0f,
        animationSpec = if (isPressed) {
            spring(stiffness = 550f, dampingRatio = 0.82f)
        } else {
            spring(stiffness = 350f, dampingRatio = 0.56f)
        },
        label = "wasmElasticDx"
    )

    val animatedDy by animateFloatAsState(
        targetValue = if (animatorsEnabled) targetDy else 0f,
        animationSpec = if (isPressed) {
            spring(stiffness = 550f, dampingRatio = 0.82f)
        } else {
            spring(stiffness = 350f, dampingRatio = 0.56f)
        },
        label = "wasmElasticDy"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) {
            if (animatorsEnabled) pressedScale else 0.985f
        } else {
            1.0f
        },
        animationSpec = if (isPressed) {
            tween(durationMillis = 85, easing = FastOutSlowInEasing)
        } else if (animatorsEnabled) {
            spring(stiffness = 420f, dampingRatio = 0.62f)
        } else {
            tween(durationMillis = 90)
        },
        label = "wasmElasticScale"
    )

    val luminousAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.0f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 110, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 240, easing = FastOutSlowInEasing)
        },
        label = "wasmLuminousAlpha"
    )

    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val centerColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.28f * luminousAlpha * luminousIntensity)
    } else {
        primaryColor.copy(alpha = 0.18f * luminousAlpha * luminousIntensity)
    }

    val outerColor = if (isDarkTheme) {
        primaryColor.copy(alpha = 0.16f * luminousAlpha * luminousIntensity)
    } else {
        tertiaryColor.copy(alpha = 0.08f * luminousAlpha * luminousIntensity)
    }

    var mod = this.pointerInput(enabled, animatorsEnabled) {
        if (!enabled) return@pointerInput
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull()
                if (change != null) {
                    if (change.pressed) {
                        val pos = change.position
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        if (rawTouchPosition == null && animatorsEnabled) {
                            lastRippleOrigin = pos
                            scope.launch {
                                rippleProgress.snapTo(0f)
                                rippleProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
                                )
                            }
                        }
                        rawTouchPosition = pos
                        lastTouchPosition = pos
                        if (cx > 0f && cy > 0f) {
                            targetDx = ((pos.x - cx) / cx).coerceIn(-1f, 1f)
                            targetDy = ((pos.y - cy) / cy).coerceIn(-1f, 1f)
                        }
                    } else {
                        rawTouchPosition = null
                        targetDx = 0f
                        targetDy = 0f
                    }
                }
            }
        }
    }

    mod = mod
        .graphicsLayer {
            val maxTranslatePx = if (animatorsEnabled) maxTranslateDp.toPx() else 0f
            val stretchFactor = if (animatorsEnabled) maxStretchPercent else 0f
            val stretchX = 1f + stretchFactor * abs(animatedDx) - (stretchFactor * 0.30f) * abs(animatedDy)
            val stretchY = 1f + (stretchFactor * 1.05f) * abs(animatedDy) - (stretchFactor * 0.30f) * abs(animatedDx)

            val pivotX = (0.5f - 0.28f * animatedDx).coerceIn(0.18f, 0.82f)
            val pivotY = (0.5f - 0.28f * animatedDy).coerceIn(0.18f, 0.82f)
            transformOrigin = TransformOrigin(pivotX, pivotY)

            scaleX = animatedScale * stretchX
            scaleY = animatedScale * stretchY
            translationX = animatedDx * maxTranslatePx
            translationY = animatedDy * maxTranslatePx
            clip = true
            this.shape = shape
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .clip(shape)
        .drawWithContent {
            drawContent()

            val w = size.width
            val h = size.height
            val currentTouchCenter = rawTouchPosition ?: lastTouchPosition ?: Offset(w / 2f, h / 2f)

            if (luminousAlpha > 0.01f && w > 0f && h > 0f) {
                val baseBloomRadius = if (isDarkTheme) {
                    size.maxDimension * 0.65f
                } else {
                    size.minDimension * 0.95f + size.maxDimension * 0.20f
                }
                val bloomRadius = (baseBloomRadius * luminousRadiusScale).coerceAtLeast(1f)

                val luminousBrush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to centerColor,
                        0.42f to primaryColor.copy(alpha = 0.09f * luminousAlpha * luminousIntensity),
                        0.76f to outerColor,
                        1.00f to Color.Transparent
                    ),
                    center = currentTouchCenter,
                    radius = bloomRadius
                )
                drawOutlineBrush(shape.createOutline(size, layoutDirection, this), luminousBrush)
            }

            val p = rippleProgress.value
            val rippleAlpha = (1f - p) * (if (isPressed) 1f else luminousAlpha)
            if (animatorsEnabled && p < 1f && rippleAlpha > 0.01f && lastRippleOrigin != null && w > 0f && h > 0f) {
                val maxRippleRadius = (size.maxDimension * 0.85f).coerceAtLeast(16.dp.toPx())
                val minRippleRadius = 10.dp.toPx()
                val currentRippleRadius = (minRippleRadius + (maxRippleRadius - minRippleRadius) * p).coerceAtLeast(1f)
                val rippleOrigin = Offset(
                    lastRippleOrigin!!.x.coerceIn(0f, w),
                    lastRippleOrigin!!.y.coerceIn(0f, h)
                )
                val rippleBrush = Brush.radialGradient(
                    colors = listOf(
                        centerColor.copy(alpha = centerColor.alpha * 0.80f * rippleAlpha),
                        outerColor.copy(alpha = outerColor.alpha * 0.45f * rippleAlpha),
                        Color.Transparent
                    ),
                    center = rippleOrigin,
                    radius = currentRippleRadius
                )
                drawOutlineBrush(shape.createOutline(size, layoutDirection, this), rippleBrush)
            }
        }

    if (onClick != null) {
        mod.combinedClickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        mod
    }
}

private fun androidx.compose.ui.graphics.drawscope.ContentDrawScope.drawOutlineBrush(
    outline: Outline,
    brush: Brush
) {
    when (outline) {
        is Outline.Rounded -> drawRoundRect(
            brush = brush,
            cornerRadius = outline.roundRect.topLeftCornerRadius
        )
        is Outline.Generic -> drawPath(
            path = outline.path,
            brush = brush
        )
        is Outline.Rectangle -> drawRect(brush = brush)
    }
}

/**
 * Expressive June-style connected corner radius animation for stacked rows when pressed.
 */
@Composable
fun rememberJuneTileShape(
    index: Int,
    totalCount: Int,
    isPressed: Boolean,
    outerCorner: Dp = 20.dp,
    innerCorner: Dp = 6.dp
): RoundedCornerShape {
    val topBase = if (index == 0 || totalCount <= 1) outerCorner else innerCorner
    val bottomBase = if (index == totalCount - 1 || totalCount <= 1) outerCorner else innerCorner
    val topTarget = if (isPressed) outerCorner else topBase
    val bottomTarget = if (isPressed) outerCorner else bottomBase

    val topAnimated by animateDpAsState(
        targetValue = topTarget,
        animationSpec = WasmMotionTokens.SpringPillCorner,
        label = "juneTopCorner"
    )
    val bottomAnimated by animateDpAsState(
        targetValue = bottomTarget,
        animationSpec = WasmMotionTokens.SpringPillCorner,
        label = "juneBottomCorner"
    )
    return RoundedCornerShape(
        topStart = topAnimated,
        topEnd = topAnimated,
        bottomStart = bottomAnimated,
        bottomEnd = bottomAnimated
    )
}

/**
 * Softens scroll clipping on top and bottom edges using pure `CompositingStrategy.Offscreen` + `DstIn`.
 * 100% compatible with API 26+ (no RenderEffect crash risk on older devices).
 */
fun Modifier.wasmFeatheredVerticalViewport(
    active: Boolean = true,
    topFadeFraction: Float = 0.035f,
    bottomFadeFraction: Float = 0.085f
): Modifier = if (!active) this else this
    .graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }
    .drawWithContent {
        drawContent()
        if (size.height <= 0f) return@drawWithContent
        val topStop = topFadeFraction.coerceIn(0f, 0.35f)
        val bottomStop = (1f - bottomFadeFraction.coerceIn(0f, 0.35f)).coerceAtLeast(topStop + 0.01f)
        val stops = buildList {
            if (topStop > 0f) {
                add(0.0f to Color.Transparent)
                add(topStop to Color.Black)
            } else {
                add(0.0f to Color.Black)
            }
            if (bottomStop < 1f) {
                add(bottomStop to Color.Black)
                add(1.0f to Color.Transparent)
            } else {
                add(1.0f to Color.Black)
            }
        }.toTypedArray()

        drawRect(
            brush = Brush.verticalGradient(*stops),
            blendMode = BlendMode.DstIn
        )
    }

/**
 * Smooth scroll-linked normalized falloff (0f at rest -> 1f after scrolling `fadeDistance`).
 * Modeled on Expressive Clock's `headerFalloff`.
 */
@Composable
fun rememberScrollHeaderFalloff(
    scrollState: ScrollState,
    fadeDistance: Dp = 28.dp
): State<Float> {
    val fadePx = with(LocalDensity.current) { fadeDistance.toPx().coerceAtLeast(1f) }
    val rawFraction by remember(scrollState, fadePx) {
        derivedStateOf { (scrollState.value / fadePx).coerceIn(0f, 1f) }
    }
    return animateFloatAsState(
        targetValue = rawFraction,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "wasmScrollHeaderFalloff"
    )
}

/**
 * Smooth scroll-linked normalized falloff for [LazyListState].
 */
@Composable
fun rememberLazyListHeaderFalloff(
    listState: LazyListState,
    fadeDistance: Dp = 32.dp
): State<Float> {
    val fadePx = with(LocalDensity.current) { fadeDistance.toPx().coerceAtLeast(1f) }
    val rawFraction by remember(listState, fadePx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / fadePx).coerceIn(0f, 1f)
        }
    }
    return animateFloatAsState(
        targetValue = rawFraction,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "wasmLazyHeaderFalloff"
    )
}
