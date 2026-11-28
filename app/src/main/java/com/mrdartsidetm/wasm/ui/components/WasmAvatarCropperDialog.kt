package com.mrdartsidetm.wasm.ui.components

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mrdartsidetm.wasm.ui.motion.performWasmTickHaptic
import com.mrdartsidetm.wasm.ui.onboarding.ContainedExpressiveLoadingIndicator
import com.mrdartsidetm.wasm.util.AvatarCropMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Pure native Jetpack Compose 1:1 square avatar cropper modal with pinch-to-zoom,
 * drag-to-pan, 90-degree rotation, circular preview reticle inside the square frame,
 * and M3 Expressive zoom slider.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasmAvatarCropperDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCroppedFile: (File) -> Unit,
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var sourceBitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(imageUri) { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        val loaded = withContext(Dispatchers.IO) {
            decodeOrientedBitmap(context.contentResolver, imageUri, maxDimension = 2048)
        }
        if (loaded == null) {
            loadFailed = true
            onError("Could not read selected image")
            onDismiss()
        } else {
            sourceBitmap = loaded
        }
    }

    var rotationDegrees by remember(imageUri) { mutableIntStateOf(0) }
    var zoom by remember(imageUri) { mutableFloatStateOf(1f) }
    var offsetX by remember(imageUri) { mutableFloatStateOf(0f) }
    var offsetY by remember(imageUri) { mutableFloatStateOf(0f) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            val bmp = sourceBitmap
            if (bmp == null && !loadFailed) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ContainedExpressiveLoadingIndicator(size = 48.dp)
                }
                return@Surface
            }
            if (bmp == null) return@Surface

            val (effW, effH) = remember(bmp, rotationDegrees) {
                AvatarCropMath.effectiveDimensions(bmp.width, bmp.height, rotationDegrees)
            }
            val cropSidePx = remember(viewportSize) {
                if (viewportSize.width <= 0 || viewportSize.height <= 0) 0f
                else min(viewportSize.width, viewportSize.height) * 0.82f
            }
            val baseScale = remember(effW, effH, cropSidePx) {
                AvatarCropMath.baseScaleToCoverSquare(effW.toFloat(), effH.toFloat(), cropSidePx)
            }
            val displayScale = baseScale * zoom

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { if (!isSaving) onDismiss() },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Crop Square Avatar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "1:1 square · pinch & drag",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }

                    IconButton(
                        onClick = {
                            performWasmTickHaptic(view)
                            rotationDegrees = 0
                            zoom = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        enabled = !isSaving,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Crop")
                    }
                }

                // Interactive Crop Viewport
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                        .onSizeChanged { viewportSize = it }
                        .pointerInput(bmp, rotationDegrees, cropSidePx, baseScale) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                if (cropSidePx <= 0f) return@detectTransformGestures
                                val nextZoom = (zoom * gestureZoom).coerceIn(1f, 5f)
                                val nextScale = baseScale * nextZoom
                                val (clampedX, clampedY) = AvatarCropMath.clampPanOffset(
                                    offsetX = offsetX + pan.x,
                                    offsetY = offsetY + pan.y,
                                    effWidth = effW.toFloat(),
                                    effHeight = effH.toFloat(),
                                    cropSidePx = cropSidePx,
                                    displayScale = nextScale
                                )
                                zoom = nextZoom
                                offsetX = clampedX
                                offsetY = clampedY
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val imageBitmap = remember(bmp) { bmp.asImageBitmap() }

                    // Draw bitmap at exact intrinsic size scaled by displayScale around center
                    if (viewportSize.width > 0 && viewportSize.height > 0) {
                        val density = LocalDensity.current
                        val rawWidthDp = with(density) { bmp.width.toDp() }
                        val rawHeightDp = with(density) { bmp.height.toDp() }
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Avatar Crop Preview",
                            contentScale = ContentScale.None,
                            modifier = Modifier
                                .requiredSize(width = rawWidthDp, height = rawHeightDp)
                                .graphicsLayer {
                                    translationX = offsetX
                                    translationY = offsetY
                                    rotationZ = rotationDegrees.toFloat()
                                    scaleX = displayScale
                                    scaleY = displayScale
                                }
                        )
                    }

                    // Scrim + 1:1 Square Viewfinder + Rule-of-Thirds + Circular Avatar Guide
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (cropSidePx <= 0f) return@Canvas
                        val left = (size.width - cropSidePx) / 2f
                        val top = (size.height - cropSidePx) / 2f
                        val squareRect = Rect(left, top, left + cropSidePx, top + cropSidePx)

                        val scrimPath = Path().apply {
                            fillType = PathFillType.EvenOdd
                            addRect(Rect(Offset.Zero, size))
                            addRect(squareRect)
                        }
                        drawPath(scrimPath, color = Color.Black.copy(alpha = 0.68f))

                        // Subtle circular guide inside the 1:1 square showing how avatars mask it
                        drawCircle(
                            color = Color.White.copy(alpha = 0.42f),
                            radius = cropSidePx / 2f,
                            center = Offset(left + cropSidePx / 2f, top + cropSidePx / 2f),
                            style = Stroke(
                                width = 1.25.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                            )
                        )

                        // Rule-of-thirds lines
                        val third = cropSidePx / 3f
                        val gridColor = Color.White.copy(alpha = 0.25f)
                        for (i in 1..2) {
                            drawLine(
                                color = gridColor,
                                start = Offset(left + third * i, top),
                                end = Offset(left + third * i, top + cropSidePx),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = gridColor,
                                start = Offset(left, top + third * i),
                                end = Offset(left + cropSidePx, top + third * i),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Square border
                        drawRect(
                            color = Color.White.copy(alpha = 0.85f),
                            topLeft = Offset(left, top),
                            size = Size(cropSidePx, cropSidePx),
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // Expressive corner brackets
                        val cornerLen = 22.dp.toPx()
                        val cornerStroke = 3.5.dp.toPx()
                        val r = left + cropSidePx
                        val b = top + cropSidePx
                        // Top-left
                        drawLine(Color.White, Offset(left, top), Offset(left + cornerLen, top), cornerStroke)
                        drawLine(Color.White, Offset(left, top), Offset(left, top + cornerLen), cornerStroke)
                        // Top-right
                        drawLine(Color.White, Offset(r - cornerLen, top), Offset(r, top), cornerStroke)
                        drawLine(Color.White, Offset(r, top), Offset(r, top + cornerLen), cornerStroke)
                        // Bottom-left
                        drawLine(Color.White, Offset(left, b - cornerLen), Offset(left, b), cornerStroke)
                        drawLine(Color.White, Offset(left, b), Offset(left + cornerLen, b), cornerStroke)
                        // Bottom-right
                        drawLine(Color.White, Offset(r - cornerLen, b), Offset(r, b), cornerStroke)
                        drawLine(Color.White, Offset(r, b - cornerLen), Offset(r, b), cornerStroke)
                    }
                }

                // Bottom Expressive Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF121212))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Zoom",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                        WasmExpressiveSlider(
                            value = zoom,
                            onValueChange = { nextZoom ->
                                val clampedZ = nextZoom.coerceIn(1f, 5f)
                                val nextScale = baseScale * clampedZ
                                val (cx, cy) = AvatarCropMath.clampPanOffset(
                                    offsetX = offsetX,
                                    offsetY = offsetY,
                                    effWidth = effW.toFloat(),
                                    effHeight = effH.toFloat(),
                                    cropSidePx = cropSidePx,
                                    displayScale = nextScale
                                )
                                zoom = clampedZ
                                offsetX = cx
                                offsetY = cy
                            },
                            valueRange = 1f..5f,
                            anchorValue = 1f,
                            trackHeight = 14.dp,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                            thumbColor = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format("%.1fx", zoom),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FilledTonalButton(
                            onClick = {
                                performWasmTickHaptic(view)
                                rotationDegrees = (rotationDegrees + 90) % 360
                                offsetX = 0f
                                offsetY = 0f
                                zoom = 1f
                            },
                            enabled = !isSaving,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.14f),
                                contentColor = Color.White
                            ),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Rotate")
                        }

                        Button(
                            onClick = {
                                if (isSaving || cropSidePx <= 0f) return@Button
                                isSaving = true
                                performWasmTickHaptic(view)
                                scope.launch {
                                    val croppedFile = withContext(Dispatchers.IO) {
                                        exportSquareCropJpeg(
                                            context = context,
                                            source = bmp,
                                            rotationDegrees = rotationDegrees,
                                            cropSidePx = cropSidePx,
                                            displayScale = displayScale,
                                            offsetX = offsetX,
                                            offsetY = offsetY
                                        )
                                    }
                                    isSaving = false
                                    if (croppedFile != null) {
                                        onCroppedFile(croppedFile)
                                    } else {
                                        onError("Failed to crop avatar image")
                                    }
                                }
                            },
                            enabled = !isSaving && cropSidePx > 0f,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("Use Square Avatar", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

private fun decodeOrientedBitmap(
    contentResolver: ContentResolver,
    uri: Uri,
    maxDimension: Int
): Bitmap? {
    return try {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOpts)
        }
        val rawW = boundsOpts.outWidth
        val rawH = boundsOpts.outHeight
        if (rawW <= 0 || rawH <= 0) return null

        var inSampleSize = 1
        while ((rawW / inSampleSize) > maxDimension || (rawH / inSampleSize) > maxDimension) {
            inSampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val rawBmp = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return null

        val orientation = try {
            contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Throwable) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val (deg, flipX) = AvatarCropMath.exifOrientationTransform(orientation)
        if (deg == 0f && !flipX) return rawBmp

        val matrix = Matrix().apply {
            if (flipX) postScale(-1f, 1f)
            if (deg != 0f) postRotate(deg)
        }
        val oriented = Bitmap.createBitmap(rawBmp, 0, 0, rawBmp.width, rawBmp.height, matrix, true)
        if (oriented !== rawBmp) rawBmp.recycle()
        oriented
    } catch (_: Throwable) {
        null
    }
}

private fun exportSquareCropJpeg(
    context: Context,
    source: Bitmap,
    rotationDegrees: Int,
    cropSidePx: Float,
    displayScale: Float,
    offsetX: Float,
    offsetY: Float
): File? {
    var rotatedBmp: Bitmap? = null
    var croppedBmp: Bitmap? = null
    var scaledBmp: Bitmap? = null
    return try {
        val normRot = ((rotationDegrees % 360) + 360) % 360
        val effectiveBmp = if (normRot != 0) {
            val matrix = Matrix().apply { postRotate(normRot.toFloat()) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true).also {
                rotatedBmp = it
            }
        } else {
            source
        }

        val rect = AvatarCropMath.computeSourceSquareRect(
            effWidth = effectiveBmp.width,
            effHeight = effectiveBmp.height,
            cropSidePx = cropSidePx,
            displayScale = displayScale,
            offsetX = offsetX,
            offsetY = offsetY
        )

        croppedBmp = Bitmap.createBitmap(
            effectiveBmp,
            rect.left,
            rect.top,
            rect.size,
            rect.size
        )

        val targetEdge = rect.size.coerceIn(256, 1024)
        val finalBmp = if (croppedBmp.width != targetEdge || croppedBmp.height != targetEdge) {
            Bitmap.createScaledBitmap(croppedBmp, targetEdge, targetEdge, true).also {
                scaledBmp = it
            }
        } else {
            croppedBmp
        }

        val cacheDir = File(context.cacheDir, "cropped_avatars").apply { mkdirs() }
        val outFile = File(cacheDir, "avatar_sq_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { fos ->
            finalBmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
        }
        outFile.takeIf { it.exists() && it.length() > 0L }
    } catch (_: Throwable) {
        null
    } finally {
        scaledBmp?.let { if (!it.isRecycled && it !== croppedBmp) it.recycle() }
        croppedBmp?.let { if (!it.isRecycled && it !== rotatedBmp && it !== source) it.recycle() }
        rotatedBmp?.let { if (!it.isRecycled && it !== source) it.recycle() }
    }
}
