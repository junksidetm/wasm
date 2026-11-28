package com.mrdartsidetm.wasm.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object GalleryDownloader {

    /**
     * Saves a photo, video, or GIF file into the user's Android device Gallery / Photos app.
     * Uses Scoped Storage on Android 10+ (API 29+) and MediaStore with MediaScannerConnection on older releases.
     */
    fun saveToGallery(context: Context, file: File, mediaType: String? = null): Boolean {
        if (!file.exists() || file.length() == 0L) return false

        val fileName = file.name
        val isVideo = mediaType == "video" ||
                fileName.endsWith(".mp4", ignoreCase = true) ||
                fileName.endsWith(".mkv", ignoreCase = true) ||
                fileName.endsWith(".3gp", ignoreCase = true)
        val isGif = mediaType == "gif" || fileName.endsWith(".gif", ignoreCase = true)

        val mimeType = when {
            isVideo -> "video/mp4"
            isGif -> "image/gif"
            fileName.endsWith(".png", ignoreCase = true) -> "image/png"
            fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
            else -> "image/jpeg"
        }

        val resolver = context.contentResolver

        val collectionUri = if (isVideo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        }

        val relativeSubDir = if (isVideo) "Movies/Wasm" else "Pictures/Wasm"
        val timestamp = System.currentTimeMillis()
        val displayName = "Wasm_${timestamp}_$fileName"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeSubDir)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        return try {
            val itemUri = resolver.insert(collectionUri, values) ?: return false

            resolver.openOutputStream(itemUri)?.use { out ->
                file.inputStream().use { input ->
                    input.copyTo(out)
                }
            } ?: run {
                resolver.delete(itemUri, null, null)
                return false
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            } else {
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mimeType), null)
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Saves an image with an optional clockwise rotation applied (`rotationDegrees` multiple of 90).
     */
    fun saveImageToGallery(context: Context, file: File, rotationDegrees: Int = 0): Boolean {
        val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
        if (normalizedRotation == 0 || !file.exists()) {
            return saveToGallery(context, file, mediaType = "photo")
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath) ?: return saveToGallery(context, file, "photo")
        val matrix = Matrix().apply { postRotate(normalizedRotation.toFloat()) }
        val rotated = try {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        } catch (_: Exception) {
            decoded
        }
        val tempFile = File(context.cacheDir, "rotated_${System.currentTimeMillis()}_${file.nameWithoutExtension}.jpg")
        return try {
            tempFile.outputStream().use { out ->
                rotated.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            if (rotated !== decoded) rotated.recycle()
            decoded.recycle()
            saveToGallery(context, tempFile, mediaType = "photo")
        } finally {
            tempFile.delete()
        }
    }

    /**
     * Downloads Audio, GIF, or Sticker files into public `Downloads/!WASM/` directory.
     * Uses Scoped Storage `MediaStore.Downloads` on API 29+ and public Downloads dir on API 26–28.
     */
    fun saveToWasmDownloads(context: Context, file: File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        val mimeType = AttachmentOpenHelper.mimeTypeFor(file.name)
        val displayName = file.name

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/!WASM")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            return try {
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
                resolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                } ?: run {
                    resolver.delete(uri, null, null)
                    return false
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } catch (_: Exception) {
                false
            }
        } else {
            return try {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "!WASM").apply { mkdirs() }
                var dest = File(targetDir, displayName)
                if (dest.exists()) {
                    dest = File(targetDir, "${System.currentTimeMillis()}_$displayName")
                }
                file.inputStream().use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf(mimeType), null)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Batch saves multiple media files to Gallery and returns the count of successfully saved files.
     */
    fun saveBatchToGallery(context: Context, files: List<Pair<String, File>>): Int {
        var count = 0
        for ((type, file) in files) {
            if (saveToGallery(context, file, type)) {
                count++
            }
        }
        return count
    }
}
