package com.mrdartsidetm.wasm.ui.motion

import android.animation.ValueAnimator
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Material 3 Expressive physics & motion tokens aligned with Expressive Clock (`v0.1.19`).
 */
object WasmMotionTokens {
    val SpringNav: FiniteAnimationSpec<Float> = spring(
        stiffness = 380f,
        dampingRatio = 0.86f
    )

    val SpringNavIntOffset: FiniteAnimationSpec<IntOffset> = spring(
        stiffness = 380f,
        dampingRatio = 0.86f
    )

    val SpringBouncy: FiniteAnimationSpec<Float> = spring(
        stiffness = 420f,
        dampingRatio = 0.62f
    )

    val SpringSelectionTick: FiniteAnimationSpec<Float> = spring(
        stiffness = 700f,
        dampingRatio = 0.55f
    )

    val SpringPillCorner: FiniteAnimationSpec<Dp> = spring(
        stiffness = 500f,
        dampingRatio = 0.72f
    )

    val SpringListPlacement: FiniteAnimationSpec<IntOffset> = spring(
        stiffness = 380f,
        dampingRatio = 0.72f
    )

    val SpringExpandCollapse: FiniteAnimationSpec<androidx.compose.ui.unit.IntSize> = spring(
        stiffness = 450f,
        dampingRatio = 0.80f
    )

    val SpringDetent: FiniteAnimationSpec<Float> = spring(
        stiffness = 430f,
        dampingRatio = 0.58f
    )

    val ExpressiveSettlement = CubicBezierEasing(0.20f, 0.85f, 0.25f, 1.04f)
    val StandardEasing = FastOutSlowInEasing

    const val DurationQuickMs = 120
    const val DurationStandardMs = 220
    const val DurationEmphasisMs = 280
    const val StaggerCardDelayMs = 55L
    const val StaggerMaxItems = 8

    val DockHeight: Dp = 64.dp
    val DockClearance: Dp = 14.dp
    val DockReserve: Dp = 94.dp
}

/**
 * Observes [ValueAnimator.areAnimatorsEnabled] (supported on API 26+, matching Wasm's minSdk)
 * and refreshes whenever the host resumes so reduced-motion preference changes take effect live.
 */
@Composable
fun rememberAnimatorsEnabled(): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = ValueAnimator.areAnimatorsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return enabled
}

/**
 * Performs subtle tactile haptic feedback across API 26–34 without requiring newer API guards at call sites.
 */
fun View.performWasmTickHaptic(isPositive: Boolean = true) {
    val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (isPositive) HapticFeedbackConstants.GESTURE_START else HapticFeedbackConstants.GESTURE_END
    } else {
        HapticFeedbackConstants.KEYBOARD_TAP
    }
    performHapticFeedback(constant)
}
