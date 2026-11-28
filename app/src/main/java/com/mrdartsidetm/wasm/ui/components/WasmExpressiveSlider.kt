package com.mrdartsidetm.wasm.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mrdartsidetm.wasm.ui.motion.rememberAnimatorsEnabled
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Material 3 Expressive Thick Slider matching https://m3.material.io/ components specs:
 * - Thick pill track (`18.dp` active & inactive segments with rounded outer caps)
 * - Clean gap separating active/inactive tracks around the tall vertical handle
 * - Interactive thumb that widens smoothly when grabbed
 * - Optional anchor detent dot with crisp haptic tick
 */
@Composable
fun WasmExpressiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    anchorValue: Float? = null,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 18.dp,
    activeTrackColor: Color = MaterialTheme.colorScheme.primary,
    inactiveTrackColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    thumbColor: Color = MaterialTheme.colorScheme.primary
) {
    val haptic = LocalHapticFeedback.current
    val animatorsEnabled = rememberAnimatorsEnabled()
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val clampedFraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    var widthPx by remember { mutableIntStateOf(1) }
    var isDragging by remember { mutableStateOf(false) }
    var lastHapticBucket by remember { mutableIntStateOf(-1) }

    val thumbWidth by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 4.dp,
        animationSpec = spring(stiffness = 480f, dampingRatio = 0.72f),
        label = "m3ExpressiveSliderThumbWidth"
    )
    val thumbHeight by animateDpAsState(
        targetValue = if (isDragging) 36.dp else 32.dp,
        animationSpec = spring(stiffness = 480f, dampingRatio = 0.72f),
        label = "m3ExpressiveSliderThumbHeight"
    )
    val animatedFraction by animateFloatAsState(
        targetValue = clampedFraction,
        animationSpec = if (isDragging || !animatorsEnabled) tween(0) else spring(stiffness = 500f, dampingRatio = 0.85f),
        label = "m3ExpressiveSliderFraction"
    )

    fun updateFromX(xPx: Float) {
        val pad = 10f
        val usable = (widthPx - pad * 2).coerceAtLeast(1f)
        val frac = ((xPx - pad) / usable).coerceIn(0f, 1f)
        var raw = valueRange.start + frac * span
        if (anchorValue != null && abs(raw - anchorValue) <= span * 0.018f) {
            raw = anchorValue
        }
        val bucket = (frac * 40f).roundToInt()
        if (bucket != lastHapticBucket) {
            lastHapticBucket = bucket
            if (raw == anchorValue) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
        onValueChange(raw.coerceIn(valueRange.start, valueRange.endInclusive))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .pointerInput(valueRange, anchorValue) {
                detectTapGestures { offset ->
                    updateFromX(offset.x)
                }
            }
            .pointerInput(valueRange, anchorValue) {
                detectHorizontalDragGestures(
                    onDragStart = { startOffset ->
                        isDragging = true
                        updateFromX(startOffset.x)
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        updateFromX(change.position.x)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
            val trackH = trackHeight.toPx()
            val trackY = (size.height - trackH) / 2f
            val horizontalPad = 8.dp.toPx()
            val usableW = (size.width - horizontalPad * 2f).coerceAtLeast(1f)
            val thumbW = thumbWidth.toPx()
            val thumbH = thumbHeight.toPx()
            val gap = 5.dp.toPx()
            val thumbX = horizontalPad + animatedFraction * usableW
            val cornerR = CornerRadius(trackH / 2f, trackH / 2f)

            // Active left segment (clipped cleanly before thumb gap)
            val activeEnd = (thumbX - thumbW / 2f - gap).coerceAtLeast(horizontalPad)
            if (activeEnd > horizontalPad + 1f) {
                drawRoundRect(
                    color = activeTrackColor,
                    topLeft = Offset(horizontalPad, trackY),
                    size = Size(activeEnd - horizontalPad, trackH),
                    cornerRadius = cornerR
                )
            }

            // Inactive right segment (starts cleanly after thumb gap)
            val inactiveStart = (thumbX + thumbW / 2f + gap).coerceAtMost(size.width - horizontalPad)
            val inactiveEnd = size.width - horizontalPad
            if (inactiveEnd > inactiveStart + 1f) {
                drawRoundRect(
                    color = inactiveTrackColor,
                    topLeft = Offset(inactiveStart, trackY),
                    size = Size(inactiveEnd - inactiveStart, trackH),
                    cornerRadius = cornerR
                )
            }

            // Subtle end stop indicator dot inside inactive track
            drawCircle(
                color = activeTrackColor.copy(alpha = 0.45f),
                radius = 2.dp.toPx(),
                center = Offset(inactiveEnd - trackH / 2f, size.height / 2f)
            )

            // Anchor detent dot (if specified and uncovered)
            if (anchorValue != null) {
                val anchorFrac = ((anchorValue - valueRange.start) / span).coerceIn(0f, 1f)
                val anchorX = horizontalPad + anchorFrac * usableW
                if (abs(anchorX - thumbX) > thumbW + gap) {
                    drawCircle(
                        color = if (anchorX < thumbX) Color.White.copy(alpha = 0.65f) else activeTrackColor.copy(alpha = 0.6f),
                        radius = 2.2.dp.toPx(),
                        center = Offset(anchorX, size.height / 2f)
                    )
                }
            }

            // Expressive vertical pill handle
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(thumbX - thumbW / 2f, (size.height - thumbH) / 2f),
                size = Size(thumbW, thumbH),
                cornerRadius = CornerRadius(thumbW / 2f, thumbW / 2f)
            )
        }
    }
}

/**
 * Animated wavy / squiggly Canvas seekbar inspired by `marlboro-advance/mpvEx`'s `SquigglySeekbar`:
 * - Animates sinusoidal wave on active played progress while playing
 * - Flattens gracefully when paused or reduced-motion is enabled
 * - Large touch strip for effortless scrubbing in voice notes and full-screen video player
 */
@Composable
fun WasmSquigglySeekbar(
    progress: Float,
    isPlaying: Boolean,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    waveAmplitudeDp: Dp = 3.2.dp,
    wavelengthDp: Dp = 24.dp,
    strokeWidthDp: Dp = 3.5.dp
) {
    val animatorsEnabled = rememberAnimatorsEnabled()
    val clamped = progress.coerceIn(0f, 1f)
    var widthPx by remember { mutableIntStateOf(1) }
    var isDragging by remember { mutableStateOf(false) }

    val waveAmplitudeFraction by animateFloatAsState(
        targetValue = if (isPlaying && !isDragging && animatorsEnabled) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "squigglyAmp"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "squigglyPhase")
    val phase by if (isPlaying && animatorsEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "squigglyPhaseFloat"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val wavePath = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeekFraction((offset.x / widthPx.toFloat()).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { start ->
                        isDragging = true
                        onSeekFraction((start.x / widthPx.toFloat()).coerceIn(0f, 1f))
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onSeekFraction((change.position.x / widthPx.toFloat()).coerceIn(0f, 1f))
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(34.dp)) {
            val pad = 8.dp.toPx()
            val usableW = (size.width - pad * 2f).coerceAtLeast(1f)
            val cy = size.height / 2f
            val strokeW = strokeWidthDp.toPx()
            val activeEndX = pad + clamped * usableW

            // Unplayed straight track
            if (activeEndX < pad + usableW) {
                drawLine(
                    color = inactiveColor,
                    start = Offset(activeEndX, cy),
                    end = Offset(pad + usableW, cy),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }

            // Played wavy or flat segment
            if (activeEndX > pad + 1f) {
                val amp = waveAmplitudeDp.toPx() * waveAmplitudeFraction
                if (amp < 0.2f) {
                    drawLine(
                        color = activeColor,
                        start = Offset(pad, cy),
                        end = Offset(activeEndX, cy),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                } else {
                    wavePath.reset()
                    val wl = wavelengthDp.toPx().coerceAtLeast(8f)
                    val step = 3.dp.toPx().coerceAtLeast(2f)
                    var x = pad
                    wavePath.moveTo(x, cy + amp * sin(((x - pad) / wl) * (2f * PI.toFloat()) - phase))
                    while (x < activeEndX) {
                        x = (x + step).coerceAtMost(activeEndX)
                        val y = cy + amp * sin(((x - pad) / wl) * (2f * PI.toFloat()) - phase)
                        wavePath.lineTo(x, y)
                    }
                    drawPath(
                        path = wavePath,
                        color = activeColor,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
            }

            // Handle pill / dot
            val thumbW = if (isDragging) 6.dp.toPx() else 4.dp.toPx()
            val thumbH = if (isDragging) 22.dp.toPx() else 16.dp.toPx()
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(activeEndX - thumbW / 2f, cy - thumbH / 2f),
                size = Size(thumbW, thumbH),
                cornerRadius = CornerRadius(thumbW / 2f, thumbW / 2f)
            )
        }
    }
}
