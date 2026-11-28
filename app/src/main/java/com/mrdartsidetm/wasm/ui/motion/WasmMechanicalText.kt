package com.mrdartsidetm.wasm.ui.motion

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import java.text.BreakIterator
import kotlin.math.roundToInt

/**
 * Staggered mechanical rolling odometer text inspired by Expressive Clock's `MechanicalText`:
 * each glyph scrolls vertically inside a clipped slot with subtle 3D X-axis tilt.
 * Automatically degrades to static text when system animations are disabled (`minSdk = 26` safe).
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WasmMechanicalText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    color: Color = LocalContentColor.current,
    blurRadius: Dp = 0.dp,
    staggerDelayMs: Int = 28,
    rollUpward: Boolean = true,
    maxLines: Int = 1
) {
    val animatorsEnabled = rememberAnimatorsEnabled()
    val effectiveBlur = if (animatorsEnabled && blurRadius > 0.dp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        blurRadius
    } else {
        0.dp
    }

    if (!animatorsEnabled) {
        Text(
            text = text,
            modifier = modifier,
            style = style,
            color = color,
            maxLines = maxLines,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        return
    }

    val cells = remember(text) { splitMechanicalGlyphs(text) }

    Row(
        modifier = modifier
            .then(if (effectiveBlur > 0.dp) Modifier.blur(effectiveBlur) else Modifier),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        cells.forEachIndexed { index, glyph ->
            // Right-to-left stagger so the least significant digits move first, just like an odometer
            val reverseIndex = (cells.lastIndex - index).coerceAtLeast(0)
            val delay = (reverseIndex * staggerDelayMs).coerceAtMost(140)
            val dir = if (rollUpward) 1 else -1

            Box(
                modifier = Modifier
                    .clipToBounds()
                    .padding(vertical = 1.dp)
            ) {
                AnimatedContent(
                    targetState = glyph,
                    transitionSpec = {
                        val slideInSpec = spring<IntOffset>(
                            dampingRatio = 0.68f,
                            stiffness = 460f
                        )
                        val slideOutSpec = spring<IntOffset>(
                            dampingRatio = 0.85f,
                            stiffness = 540f
                        )
                        (slideInVertically(animationSpec = slideInSpec) { height -> dir * height } +
                            fadeIn(tween(durationMillis = 120, delayMillis = delay, easing = FastOutSlowInEasing))) togetherWith
                            (slideOutVertically(animationSpec = slideOutSpec) { height -> -dir * height } +
                                fadeOut(tween(durationMillis = 90, easing = FastOutSlowInEasing))) using
                            SizeTransform(clip = false)
                    },
                    label = "mechanicalGlyph_$index"
                ) { targetGlyph ->
                    val isEntering = transition.targetState == EnterExitState.Visible &&
                        transition.currentState != transition.targetState

                    Text(
                        text = targetGlyph,
                        style = style,
                        color = color,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.graphicsLayer {
                            cameraDistance = 10f * density
                            if (isEntering) {
                                transformOrigin = TransformOrigin(0.5f, if (rollUpward) 0.8f else 0.2f)
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun splitMechanicalGlyphs(text: String): List<String> {
    if (text.isEmpty()) return emptyList()
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(text)
    val glyphs = ArrayList<String>(text.length)
    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        glyphs.add(text.substring(start, end))
        start = end
        end = iterator.next()
    }
    return glyphs
}
