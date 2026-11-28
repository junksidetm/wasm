package com.mrdartsidetm.wasm.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for 1:1 square avatar cropper geometry, pan clamping, rotation swaps, and EXIF mapping.
 */
class AvatarCropMathTest {

    @Test
    fun testEffectiveDimensionsSwapOn90And270Rotation() {
        assertEquals(1600 to 900, AvatarCropMath.effectiveDimensions(1600, 900, 0))
        assertEquals(900 to 1600, AvatarCropMath.effectiveDimensions(1600, 900, 90))
        assertEquals(1600 to 900, AvatarCropMath.effectiveDimensions(1600, 900, 180))
        assertEquals(900 to 1600, AvatarCropMath.effectiveDimensions(1600, 900, 270))
    }

    @Test
    fun testCenterSquareCropOnLandscapeAndPortraitImages() {
        val cropBoxPx = 800f

        // 1600x800 landscape at base scale 1.0 -> centered 800x800 square at x=400, y=0
        val scaleLand = AvatarCropMath.baseScaleToCoverSquare(1600f, 800f, cropBoxPx)
        assertEquals(1f, scaleLand, 0.0001f)
        val rectLand = AvatarCropMath.computeSourceSquareRect(
            effWidth = 1600,
            effHeight = 800,
            cropSidePx = cropBoxPx,
            displayScale = scaleLand,
            offsetX = 0f,
            offsetY = 0f
        )
        assertEquals(AvatarCropMath.SquareCropRect(left = 400, top = 0, size = 800), rectLand)

        // 800x1600 portrait at base scale 1.0 -> centered 800x800 square at x=0, y=400
        val scalePort = AvatarCropMath.baseScaleToCoverSquare(800f, 1600f, cropBoxPx)
        assertEquals(1f, scalePort, 0.0001f)
        val rectPort = AvatarCropMath.computeSourceSquareRect(
            effWidth = 800,
            effHeight = 1600,
            cropSidePx = cropBoxPx,
            displayScale = scalePort,
            offsetX = 0f,
            offsetY = 0f
        )
        assertEquals(AvatarCropMath.SquareCropRect(left = 0, top = 400, size = 800), rectPort)
    }

    @Test
    fun testPanClampingNeverExposesOutOfBoundsArea() {
        val cropBoxPx = 600f
        val effW = 1200
        val effH = 600
        val baseScale = AvatarCropMath.baseScaleToCoverSquare(effW.toFloat(), effH.toFloat(), cropBoxPx)
        val displayScale = baseScale * 2f // zoomed 2x -> rendered 2400x1200

        // Extreme pan way beyond bounds should clamp strictly inside bitmap
        val rect = AvatarCropMath.computeSourceSquareRect(
            effWidth = effW,
            effHeight = effH,
            cropSidePx = cropBoxPx,
            displayScale = displayScale,
            offsetX = 10_000f,
            offsetY = -10_000f
        )
        assertTrue(rect.left >= 0)
        assertTrue(rect.top >= 0)
        assertTrue(rect.left + rect.size <= effW)
        assertTrue(rect.top + rect.size <= effH)
        assertEquals(300, rect.size)
        assertEquals(0, rect.left)
        assertEquals(300, rect.top)
    }

    @Test
    fun testExifOrientationTransformMapping() {
        assertEquals(0f to false, AvatarCropMath.exifOrientationTransform(1))
        assertEquals(90f to false, AvatarCropMath.exifOrientationTransform(6))
        assertEquals(180f to false, AvatarCropMath.exifOrientationTransform(3))
        assertEquals(270f to false, AvatarCropMath.exifOrientationTransform(8))
        assertEquals(0f to true, AvatarCropMath.exifOrientationTransform(2))
    }
}
