package com.mrdartsidetm.wasm.util

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure pixel/coordinate math for 1:1 square avatar cropping so screen gestures and exported
 * square bitmaps match identically and can be unit-tested cleanly on the JVM.
 */
object AvatarCropMath {

    data class SquareCropRect(
        val left: Int,
        val top: Int,
        val size: Int
    )

    fun effectiveDimensions(
        rawWidth: Int,
        rawHeight: Int,
        rotationDegrees: Int
    ): Pair<Int, Int> {
        val norm = ((rotationDegrees % 360) + 360) % 360
        return if (norm == 90 || norm == 270) {
            rawHeight to rawWidth
        } else {
            rawWidth to rawHeight
        }
    }

    /**
     * Minimum scale needed so the rotated image covers the square viewfinder of [cropSidePx]
     * with no empty margins at zoom = 1f.
     */
    fun baseScaleToCoverSquare(
        effWidth: Float,
        effHeight: Float,
        cropSidePx: Float
    ): Float {
        if (effWidth <= 0f || effHeight <= 0f || cropSidePx <= 0f) return 1f
        return max(cropSidePx / effWidth, cropSidePx / effHeight)
    }

    /**
     * Clamps pan offsets so the displayed image always completely covers the 1:1 square frame.
     */
    fun clampPanOffset(
        offsetX: Float,
        offsetY: Float,
        effWidth: Float,
        effHeight: Float,
        cropSidePx: Float,
        displayScale: Float
    ): Pair<Float, Float> {
        val maxDx = max(0f, (effWidth * displayScale - cropSidePx) / 2f)
        val maxDy = max(0f, (effHeight * displayScale - cropSidePx) / 2f)
        return offsetX.coerceIn(-maxDx, maxDx) to offsetY.coerceIn(-maxDy, maxDy)
    }

    /**
     * Computes the exact 1:1 square crop region in post-rotation bitmap pixel coordinates.
     */
    fun computeSourceSquareRect(
        effWidth: Int,
        effHeight: Int,
        cropSidePx: Float,
        displayScale: Float,
        offsetX: Float,
        offsetY: Float
    ): SquareCropRect {
        if (effWidth <= 0 || effHeight <= 0 || displayScale <= 0f || cropSidePx <= 0f) {
            val side = minOf(max(1, effWidth), max(1, effHeight))
            return SquareCropRect(0, 0, side)
        }
        val (clampedX, clampedY) = clampPanOffset(
            offsetX = offsetX,
            offsetY = offsetY,
            effWidth = effWidth.toFloat(),
            effHeight = effHeight.toFloat(),
            cropSidePx = cropSidePx,
            displayScale = displayScale
        )
        val srcSizeFloat = cropSidePx / displayScale
        val rawLeft = effWidth / 2f - clampedX / displayScale - srcSizeFloat / 2f
        val rawTop = effHeight / 2f - clampedY / displayScale - srcSizeFloat / 2f

        val maxSide = minOf(effWidth, effHeight)
        val side = srcSizeFloat.roundToInt().coerceIn(1, maxSide)
        val left = rawLeft.roundToInt().coerceIn(0, effWidth - side)
        val top = rawTop.roundToInt().coerceIn(0, effHeight - side)
        return SquareCropRect(left = left, top = top, size = side)
    }

    /**
     * Maps EXIF orientation tag (`android.media.ExifInterface.ORIENTATION_*` integer values 1..8)
     * into clockwise rotation degrees and horizontal flip flag.
     */
    fun exifOrientationTransform(orientation: Int): Pair<Float, Boolean> {
        return when (orientation) {
            2 -> 0f to true     // FLIP_HORIZONTAL
            3 -> 180f to false  // ROTATE_180
            4 -> 180f to true   // FLIP_VERTICAL
            5 -> 90f to true    // TRANSPOSE
            6 -> 90f to false   // ROTATE_90
            7 -> 270f to true   // TRANSVERSE
            8 -> 270f to false  // ROTATE_270
            else -> 0f to false // NORMAL
        }
    }
}
