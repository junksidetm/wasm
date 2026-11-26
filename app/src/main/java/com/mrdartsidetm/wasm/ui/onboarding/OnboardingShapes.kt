package com.mrdartsidetm.wasm.ui.onboarding

import android.graphics.Matrix
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Figma color palette constants from the onboarding SVGs
val OnboardingBgTop = Color(0xFF758AEF)
val OnboardingBgBottom = Color(0xFF062362)
val OnboardingSkyTintBottom = Color(0xFF1365CF)
val OnboardingButtonBlue = Color(0xFF1B377D)
val OnboardingLangPillBg = Color(0xFFD9D9D9)
val OnboardingDarkCard = Color(0xFF121327)
val OnboardingWhatsAppNeon = Color(0xFF8FFF52)
val OnboardingWhatsAppArrowNeon = Color(0xFF91FF56)
val OnboardingWhatsAppAccent = Color(0xFF0EFA3E)
val OnboardingInstagramCyan = Color(0xFF0EFAEE)
val OnboardingInstagramAmber = Color(0xFFFFC107)
val OnboardingGhostRed = Color(0xFFFF3636)
val OnboardingCloverOrange = Color(0xFFE5931F)
val OnboardingOuterYellow = Color(0xFFE5EB2B)

/**
 * Exact transformed linearGradient stop endpoints (normalized to 412x917 Figma Android Compact frame)
 * matching each onboarding SVG's <g id="Background"> transform & rotation.
 */
object OnboardingBackgroundPresets {
    val Screen1And5Start = Offset(-0.2058f, 0.1296f)
    val Screen1And5End = Offset(1.1144f, 0.7969f)

    // Screens 2, 4 (WhatsApp & Instagram), and Language Changer (rotated 151.032 deg in Figma)
    val Screen2And4AndLangStart = Offset(1.5217f, 0.7058f)
    val Screen2And4AndLangEnd = Offset(-0.8129f, 0.3364f)

    // Screen 3 (rotated 118.136 deg in Figma)
    val Screen3Start = Offset(1.5687f, 0.3657f)
    val Screen3End = Offset(-0.8381f, 0.6253f)
}

@Composable
fun OnboardingSvgBackground(
    startNormalized: Offset,
    endNormalized: Offset,
    skyTintAlpha: Float = 0f,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(OnboardingBgTop, OnboardingBgBottom),
                start = Offset(w * startNormalized.x, h * startNormalized.y),
                end = Offset(w * endNormalized.x, h * endNormalized.y),
                tileMode = TileMode.Clamp
            )
        )
        if (skyTintAlpha > 0.01f) {
            drawRect(
                brush = Brush.verticalGradient(
                    0.453f to Color.Transparent,
                    1.0f to OnboardingSkyTintBottom
                ),
                alpha = skyTintAlpha
            )
        }
    }
}

val InstagramBadgeBrush = Brush.radialGradient(
    colors = listOf(
        Color(0xFFE0E8B7),
        Color(0xFFFB8A2E),
        Color(0xFFE2425C),
        Color(0xFFB13589),
        Color(0xFF6A45BE)
    )
)

val InstagramFullFillBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF833AB4),
        Color(0xFFC13584),
        Color(0xFFE1306C),
        Color(0xFFFD1D1D),
        Color(0xFFFCAF45)
    )
)

/**
 * Parses and caches an SVG path string into a Compose Path.
 */
@Composable
fun rememberSvgPath(pathData: String): Path {
    return remember(pathData) {
        PathParser().parsePathString(pathData).toPath()
    }
}

/**
 * Draws a cached SVG path scaled cleanly into the Canvas bounds.
 */
fun DrawScope.drawNormalizedPath(
    path: Path,
    srcWidth: Float,
    srcHeight: Float,
    color: Color? = null,
    brush: Brush? = null
) {
    if (srcWidth <= 0f || srcHeight <= 0f) return
    val sx = size.width / srcWidth
    val sy = size.height / srcHeight
    withTransform({
        scale(scaleX = sx, scaleY = sy, pivot = Offset.Zero)
    }) {
        if (brush != null) {
            drawPath(path = path, brush = brush)
        } else if (color != null) {
            drawPath(path = path, color = color)
        }
    }
}

/**
 * Renders an expressive Figma shape ("Move" layer rotates on its axis) with an upright foreground icon group.
 */
@Composable
fun ExpressiveGroupedBadge(
    shapePathData: String,
    shapeSrcWidth: Float,
    shapeSrcHeight: Float,
    shapeColor: Color?,
    shapeBrush: Brush? = null,
    shapeRotation: Float,
    iconPaths: List<String>,
    iconColors: List<Color>,
    iconSrcWidth: Float,
    iconSrcHeight: Float,
    iconScaleFraction: Float = 0.46f,
    iconOffsetFraction: Offset = Offset.Zero,
    iconStrokeWidths: List<Float> = emptyList(),
    iconFillFlags: List<Boolean> = emptyList(),
    useEvenOddFill: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shapePath = rememberSvgPath(shapePathData)
    val parsedIcons = remember(iconPaths, useEvenOddFill) {
        iconPaths.map { d ->
            PathParser().parsePathString(d).toPath().apply {
                if (useEvenOddFill) fillType = PathFillType.EvenOdd
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Rotating background shape ("- Move")
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = shapeRotation }
        ) {
            drawNormalizedPath(
                path = shapePath,
                srcWidth = shapeSrcWidth,
                srcHeight = shapeSrcHeight,
                color = shapeColor,
                brush = shapeBrush
            )
        }

        // Upright foreground icon centered in the group
        Canvas(
            modifier = Modifier
                .fillMaxSize(iconScaleFraction)
                .graphicsLayer {
                    translationX = size.width * iconOffsetFraction.x
                    translationY = size.height * iconOffsetFraction.y
                }
        ) {
            val sx = size.width / iconSrcWidth
            val sy = size.height / iconSrcHeight
            val scale = min(sx, sy)
            val dx = (size.width - iconSrcWidth * scale) / 2f
            val dy = (size.height - iconSrcHeight * scale) / 2f
            withTransform({
                translate(dx, dy)
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            }) {
                parsedIcons.forEachIndexed { index, path ->
                    val tint = iconColors.getOrElse(index) { Color.Black }
                    val shouldFill = iconFillFlags.getOrElse(index) { true }
                    val strokeW = iconStrokeWidths.getOrElse(index) { 0f }
                    if (shouldFill) {
                        drawPath(path = path, color = tint)
                    }
                    if (strokeW > 0f) {
                        drawPath(
                            path = path,
                            color = tint,
                            style = Stroke(
                                width = strokeW,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen 3 Instagram badge that smoothly morphs between the Figma Fan shape and a 12-petal Expressive Flower
 * using androidx.graphics:graphics-shapes Morph.
 */
@Composable
fun FanToFlowerMorphBadge(
    morphProgress: Float,
    rotationDegrees: Float,
    modifier: Modifier = Modifier
) {
    val fanPoly = remember {
        // 12-vertex representation of a 4-blade asymmetrical rounded fan so Morph interpolates smoothly into 12-petal flower
        RoundedPolygon.star(
            numVerticesPerRadius = 12,
            radius = 1f,
            innerRadius = 0.86f,
            rounding = CornerRounding(0.32f, smoothing = 0.6f)
        ).let {
            // Build an expressive 4-lobed fan/cookie silhouette with 12-petal compatibility
            RoundedPolygon.star(
                numVerticesPerRadius = 4,
                radius = 1f,
                innerRadius = 0.64f,
                rounding = CornerRounding(0.36f, smoothing = 0.8f)
            )
        }
    }
    val flowerPoly = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 12,
            radius = 1f,
            innerRadius = 0.78f,
            rounding = CornerRounding(0.24f, smoothing = 0.5f)
        )
    }
    val morph = remember(fanPoly, flowerPoly) { Morph(fanPoly, flowerPoly) }
    val igIconPaths = remember {
        OnboardingSvgPaths.instagramIconPaths.map { d ->
            PathParser().parsePathString(d).toPath().apply {
                fillType = PathFillType.EvenOdd
            }
        }
    }
    val androidPath = remember { android.graphics.Path() }
    val matrix = remember { Matrix() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotationDegrees }
        ) {
            androidPath.reset()
            morph.toPath(morphProgress.coerceIn(0f, 1f), androidPath)
            val radius = min(size.width, size.height) * 0.48f
            matrix.reset()
            matrix.setScale(radius, radius)
            matrix.postTranslate(size.width / 2f, size.height / 2f)
            androidPath.transform(matrix)

            val brush = if (morphProgress > 0.55f) {
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD54F),
                        Color(0xFFFB8A2E),
                        Color(0xFFE2425C),
                        Color(0xFFB13589)
                    )
                )
            } else null

            if (brush != null) {
                drawPath(path = androidPath.asComposePath(), brush = brush)
            } else {
                drawPath(path = androidPath.asComposePath(), color = OnboardingInstagramAmber)
            }
        }

        // Upright Instagram glyph inside the morphing badge
        Canvas(modifier = Modifier.fillMaxSize(0.42f)) {
            val scale = min(
                size.width / OnboardingSvgPaths.instagramIconWidth,
                size.height / OnboardingSvgPaths.instagramIconHeight
            )
            val dx = (size.width - OnboardingSvgPaths.instagramIconWidth * scale) / 2f
            val dy = (size.height - OnboardingSvgPaths.instagramIconHeight * scale) / 2f
            withTransform({
                translate(dx, dy)
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            }) {
                val iconColor = if (morphProgress > 0.55f) Color.White else Color(0xFF0F0F0F)
                igIconPaths.forEach { drawPath(path = it, color = iconColor) }
            }
        }
    }
}

/**
 * Material 3 Expressive ContainedLoadingIndicator built with androidx.graphics:graphics-shapes.
 * Renders a filled circular container housing a smoothly morphing & rotating RoundedPolygon sequence.
 */
@Composable
fun ContainedExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    indicatorColor: Color = Color.White,
    containerColor: Color = Color(0xFF1F2B5E),
    paddingFraction: Float = 0.20f
) {
    val polygons = remember {
        listOf(
            // SoftBurst
            RoundedPolygon.star(
                numVerticesPerRadius = 10,
                radius = 1f,
                innerRadius = 0.68f,
                rounding = CornerRounding(0.24f, smoothing = 0.4f)
            ),
            // Cookie9Sided
            RoundedPolygon.star(
                numVerticesPerRadius = 9,
                radius = 1f,
                innerRadius = 0.78f,
                rounding = CornerRounding(0.28f, smoothing = 0.5f)
            ),
            // Pentagon
            RoundedPolygon(
                numVertices = 5,
                radius = 0.94f,
                rounding = CornerRounding(0.26f, smoothing = 0.6f)
            ),
            // Flower / Sunny
            RoundedPolygon.star(
                numVerticesPerRadius = 12,
                radius = 1f,
                innerRadius = 0.76f,
                rounding = CornerRounding(0.22f, smoothing = 0.5f)
            ),
            // Cookie4Sided (Clover)
            RoundedPolygon.star(
                numVerticesPerRadius = 4,
                radius = 1f,
                innerRadius = 0.62f,
                rounding = CornerRounding(0.32f, smoothing = 0.7f)
            ),
            // Circle
            RoundedPolygon.circle(numVertices = 10, radius = 0.90f)
        )
    }
    val morphs = remember(polygons) {
        polygons.indices.map { i ->
            Morph(polygons[i], polygons[(i + 1) % polygons.size])
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "M3ExpressiveContainedLoader")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = polygons.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LoaderShapePhase"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LoaderRotation"
    )

    val androidPath = remember { android.graphics.Path() }
    val matrix = remember { Matrix() }

    Canvas(modifier = modifier) {
        val containerRadius = min(size.width, size.height) * 0.5f
        drawCircle(
            color = containerColor,
            radius = containerRadius,
            center = Offset(size.width / 2f, size.height / 2f)
        )

        val idx = phase.toInt().coerceIn(0, morphs.lastIndex)
        val localProgress = (phase - idx).coerceIn(0f, 1f)
        val easedProgress = FastOutSlowInEasing.transform(localProgress)

        androidPath.reset()
        morphs[idx].toPath(easedProgress, androidPath)

        val shapeRadius = containerRadius * (1f - paddingFraction * 1.45f)
        matrix.reset()
        matrix.setRotate(rotation)
        matrix.postScale(shapeRadius, shapeRadius)
        matrix.postTranslate(size.width / 2f, size.height / 2f)
        androidPath.transform(matrix)

        drawPath(path = androidPath.asComposePath(), color = indicatorColor)
    }
}

/**
 * Full-screen expanding Expressive Flower transition overlay triggered when Instagram is tapped on Screen 3.
 */
@Composable
fun ExpressiveFlowerFillOverlay(
    progress: Float,
    originFraction: Offset,
    modifier: Modifier = Modifier
) {
    if (progress <= 0f) return
    val flowerPoly = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 12,
            radius = 1f,
            innerRadius = 0.78f,
            rounding = CornerRounding(0.26f, smoothing = 0.6f)
        )
    }
    val androidPath = remember { android.graphics.Path() }
    val matrix = remember { Matrix() }

    Canvas(modifier = modifier.fillMaxSize()) {
        androidPath.reset()
        flowerPoly.toPath(androidPath)
        val maxDim = maxOf(size.width, size.height)
        val startScale = maxDim * 0.08f
        val endScale = maxDim * 1.85f
        val currentScale = startScale + (endScale - startScale) * progress
        val cx = size.width * originFraction.x + (size.width * 0.5f - size.width * originFraction.x) * progress
        val cy = size.height * originFraction.y + (size.height * 0.5f - size.height * originFraction.y) * progress

        matrix.reset()
        matrix.setRotate(progress * 95f)
        matrix.postScale(currentScale, currentScale)
        matrix.postTranslate(cx, cy)
        androidPath.transform(matrix)

        drawPath(
            path = androidPath.asComposePath(),
            brush = InstagramFullFillBrush,
            alpha = (progress * 1.4f).coerceIn(0f, 1f)
        )
    }
}

/**
 * Expressive Arrow burst/shower overlay triggered when WhatsApp is tapped on Screen 3.
 */
@Composable
fun ExpressiveArrowShowerOverlay(
    progress: Float,
    modifier: Modifier = Modifier
) {
    if (progress <= 0f) return
    val arrowPath = rememberSvgPath(OnboardingSvgPaths.arrowShapePath)
    val items = remember {
        listOf(
            Triple(0.24f, 0.58f, 130f),
            Triple(0.72f, 0.44f, 96f),
            Triple(0.48f, 0.76f, 118f),
            Triple(0.18f, 0.30f, 84f),
            Triple(0.82f, 0.74f, 110f),
            Triple(0.52f, 0.22f, 92f),
            Triple(0.50f, 0.50f, 180f)
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val alpha = if (progress < 0.8f) (progress / 0.3f).coerceIn(0f, 1f) else ((1f - progress) / 0.2f).coerceIn(0f, 1f)
        items.forEachIndexed { index, (fx, fy, baseDp) ->
            val stagger = (progress * 1.35f - index * 0.06f).coerceIn(0f, 1f)
            if (stagger > 0f) {
                val targetSize = baseDp.dp.toPx() * (0.4f + 1.15f * stagger)
                val cx = size.width * fx
                val cy = size.height * (fy + 0.18f * (1f - stagger))
                val rot = (index * 37f) + stagger * 140f
                val sx = targetSize / OnboardingSvgPaths.arrowShapeWidth
                val sy = targetSize / OnboardingSvgPaths.arrowShapeHeight
                withTransform({
                    translate(cx, cy)
                    rotate(rot)
                    translate(-targetSize / 2f, -targetSize / 2f)
                    scale(sx, sy, pivot = Offset.Zero)
                }) {
                    drawPath(
                        path = arrowPath,
                        color = OnboardingWhatsAppArrowNeon.copy(alpha = alpha * 0.92f)
                    )
                }
            }
        }
    }
}

/**
 * Screen 1 giant Foreground Overlay featuring Blue Sky diagonal starfield,
 * rotating Outer-Yellow, Inner-Shuttle, and rotating Stars-All sparkles.
 */
@Composable
fun Screen1WasmHeroOverlay(
    outerYellowRotation: Float,
    starsRotation: Float,
    modifier: Modifier = Modifier
) {
    val outerYellowPath = rememberSvgPath(OnboardingSvgPaths.logoOuterYellowPath)
    val innerShuttlePath = rememberSvgPath(OnboardingSvgPaths.logoInnerShuttlePath)

    Canvas(modifier = modifier.fillMaxSize()) {
        val sx = size.width / 412f
        val sy = size.height / 917f
        val u = min(sx, sy)

        // Blue Sky angled starfield shard matching Figma [x=-260..229, y=275..973]
        val shardPath = Path().apply {
            moveTo(-99.27f * sx, 589f * sy)
            lineTo(-260.5f * sx, 438f * sy)
            lineTo(170.0f * sx, 276.6f * sy)
            lineTo(185.6f * sx, 275.1f * sy)
            lineTo(229.2f * sx, 292.5f * sy)
            lineTo(11.5f * sx, 973f * sy)
            close()
        }
        drawPath(
            path = shardPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF758AEF), Color(0xFF434F89)),
                startY = 275f * sy,
                endY = 900f * sy
            )
        )
        drawPath(
            path = shardPath,
            color = Color(0xFF758AEF).copy(alpha = 0.5f),
            style = Stroke(width = 3.dp.toPx())
        )

        // Stars-All - Move (twinkling/rotating 4-point sparkle stars inside the Blue Sky shard)
        val starCoords = listOf(
            Triple(71.5f, 662.5f, 22f),
            Triple(8.7f, 367.1f, 13f),
            Triple(131.5f, 464.9f, 16f),
            Triple(62.7f, 520.3f, 12f),
            Triple(58.2f, 434.7f, 17f),
            Triple(26.7f, 601.3f, 13f),
            Triple(101.5f, 568.5f, 13f),
            Triple(108.5f, 391.5f, 13f),
            Triple(11.5f, 470.5f, 22f),
            Triple(31.5f, 818.5f, 22f),
            Triple(40.5f, 727.5f, 13f)
        )
        starCoords.forEachIndexed { idx, (x, y, r) ->
            drawFourPointStar(
                center = Offset(x * sx, y * sy),
                radius = r * u,
                rotationDeg = starsRotation + idx * 18f,
                color = Color.White.copy(alpha = 0.78f)
            )
        }

        // Outer-Yellow - Move (rotates continuously on its axis!)
        val oyW = 292.9f * u
        val oyH = 289.7f * u
        val oyCx = 211.3f * sx
        val oyCy = 269.2f * sy
        withTransform({
            translate(oyCx, oyCy)
            rotate(outerYellowRotation)
            translate(-oyW / 2f, -oyH / 2f)
            scale(oyW / OnboardingSvgPaths.logoOuterYellowWidth, oyH / OnboardingSvgPaths.logoOuterYellowHeight, pivot = Offset.Zero)
        }) {
            drawPath(path = outerYellowPath, color = OnboardingOuterYellow)
        }

        // Inner-Shuttle (upright gradient triangle)
        val inW = 140.9f * u
        val inH = 149.3f * u
        val inCx = 205.1f * sx
        val inCy = 275.9f * sy
        withTransform({
            translate(inCx - inW / 2f, inCy - inH / 2f)
            scale(inW / OnboardingSvgPaths.logoInnerShuttleWidth, inH / OnboardingSvgPaths.logoInnerShuttleHeight, pivot = Offset.Zero)
        }) {
            drawPath(
                path = innerShuttlePath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF758AEF), Color(0xFF062362)),
                    start = Offset(OnboardingSvgPaths.logoInnerShuttleWidth, 0f),
                    end = Offset(0f, OnboardingSvgPaths.logoInnerShuttleHeight)
                )
            )
        }
    }
}

/**
 * Screen 5 "WASM - Logo" rounded square app icon card with rotating Outer-Yellow on its axis.
 */
@Composable
fun Screen5WasmLogoCard(
    outerYellowRotation: Float,
    cardSize: Dp,
    modifier: Modifier = Modifier
) {
    val outerYellowPath = rememberSvgPath(OnboardingSvgPaths.logoOuterYellowPath)
    val innerShuttlePath = rememberSvgPath(OnboardingSvgPaths.logoInnerShuttlePath)
    val cornerRadius = cardSize * (37.48f / 253f)

    Box(
        modifier = modifier
            .size(cardSize)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(cornerRadius), clip = false)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF758AEF), Color(0xFF062362)),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = size.width / 253f

            // Blue Sky angled shard inside the 253x253 card
            val shard = Path().apply {
                moveTo(-42.2f * s, 288.1f * s)
                lineTo(-42.2f * s, 175.6f * s)
                lineTo(102.6f * s, 120.1f * s)
                lineTo(111.0f * s, 119.3f * s)
                lineTo(134.5f * s, 128.7f * s)
                lineTo(92.3f * s, 288.1f * s)
                close()
            }
            drawPath(
                path = shard,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF758AEF), Color(0xFF434F89)),
                    startY = 119f * s,
                    endY = 288f * s
                )
            )
            drawPath(
                path = shard,
                color = Color(0xFF758AEF).copy(alpha = 0.5f),
                style = Stroke(width = 2.dp.toPx())
            )

            // Stars inside the logo card
            val cardStars = listOf(
                Triple(48.5f, 166.2f, 4.6f),
                Triple(95.4f, 201.8f, 6.5f),
                Triple(67.2f, 219.4f, 4.1f),
                Triple(23.5f, 191.2f, 7.8f),
                Triple(65.7f, 189.7f, 6.2f),
                Triple(54.7f, 247.5f, 4.6f),
                Triple(20.4f, 231.9f, 7.8f),
                Triple(42.2f, 213.1f, 4.6f),
                Triple(89.1f, 238.1f, 4.6f)
            )
            cardStars.forEach { (cx, cy, r) ->
                drawFourPointStar(
                    center = Offset(cx * s, cy * s),
                    radius = r * s,
                    rotationDeg = 0f,
                    color = Color.White.copy(alpha = 0.80f)
                )
            }

            // Outer-Yellow rotating on its axis!
            val oyW = 164.8f * s
            val oyH = 163.0f * s
            val oyCx = 128.5f * s
            val oyCy = 116.0f * s
            withTransform({
                translate(oyCx, oyCy)
                rotate(outerYellowRotation)
                translate(-oyW / 2f, -oyH / 2f)
                scale(oyW / OnboardingSvgPaths.logoOuterYellowWidth, oyH / OnboardingSvgPaths.logoOuterYellowHeight, pivot = Offset.Zero)
            }) {
                drawPath(path = outerYellowPath, color = OnboardingOuterYellow)
            }

            // Inner-Shuttle
            val inW = 79.3f * s
            val inH = 84.0f * s
            val inCx = 125.0f * s
            val inCy = 119.8f * s
            withTransform({
                translate(inCx - inW / 2f, inCy - inH / 2f)
                scale(inW / OnboardingSvgPaths.logoInnerShuttleWidth, inH / OnboardingSvgPaths.logoInnerShuttleHeight, pivot = Offset.Zero)
            }) {
                drawPath(
                    path = innerShuttlePath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF758AEF), Color(0xFF062362)),
                        start = Offset(OnboardingSvgPaths.logoInnerShuttleWidth, 0f),
                        end = Offset(0f, OnboardingSvgPaths.logoInnerShuttleHeight)
                    )
                )
            }
        }
    }
}

private fun DrawScope.drawFourPointStar(
    center: Offset,
    radius: Float,
    rotationDeg: Float,
    color: Color
) {
    val path = Path().apply {
        val inner = radius * 0.28f
        for (i in 0 until 8) {
            val angle = (i * 45.0 + rotationDeg) * PI / 180.0
            val r = if (i % 2 == 0) radius else inner
            val x = center.x + (r * cos(angle)).toFloat()
            val y = center.y + (r * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path = path, color = color)
}
