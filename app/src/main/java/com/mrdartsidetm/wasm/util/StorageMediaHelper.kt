package com.mrdartsidetm.wasm.util

import java.io.File

/**
 * Unified storage categories matching the 7 media/file buckets rendered in
 * [com.mrdartsidetm.wasm.ui.StorageScreen] and [com.mrdartsidetm.wasm.ui.StorageDataScreen].
 */
enum class StorageMediaCategory(val label: String) {
    TEXT("Text & Chats"),
    IMAGES("Images"),
    VIDEOS("Videos"),
    VOICE_NOTES("Voice Notes"),
    GIFS("GIFs"),
    STICKERS("Stickers"),
    OTHERS("Others")
}

data class MediaCategoryBreakdown(
    val textBytes: Long = 0L,
    val imageBytes: Long = 0L,
    val videoBytes: Long = 0L,
    val voiceNoteBytes: Long = 0L,
    val gifBytes: Long = 0L,
    val stickerBytes: Long = 0L,
    val otherBytes: Long = 0L,
    val fileCount: Int = 0
) {
    val totalBytes: Long
        get() = textBytes + imageBytes + videoBytes + voiceNoteBytes + gifBytes + stickerBytes + otherBytes

    fun bytesFor(category: StorageMediaCategory): Long = when (category) {
        StorageMediaCategory.TEXT -> textBytes
        StorageMediaCategory.IMAGES -> imageBytes
        StorageMediaCategory.VIDEOS -> videoBytes
        StorageMediaCategory.VOICE_NOTES -> voiceNoteBytes
        StorageMediaCategory.GIFS -> gifBytes
        StorageMediaCategory.STICKERS -> stickerBytes
        StorageMediaCategory.OTHERS -> otherBytes
    }
}

/**
 * Classifies exported WhatsApp and Instagram media files accurately—including
 * extensionless Instagram `photos` files and `audio` `.mp4` voice clips vs `videos` `.mp4`.
 */
object StorageMediaHelper {

    fun classifyFile(file: File, relativeRoot: File? = null): StorageMediaCategory {
        val normalizedPath = (if (relativeRoot != null) {
            file.relativeToOrNull(relativeRoot)?.invariantSeparatorsPath ?: file.invariantSeparatorsPath
        } else {
            file.invariantSeparatorsPath
        }).lowercase()

        val name = file.name.lowercase()
        val ext = file.extension.lowercase()

        // 1. Folder-first classification for Instagram exports (photos often lack extensions;
        //    voice messages inside audio/ frequently use .mp4 container)
        val segments = normalizedPath.split('/')
        if (segments.any { it == "stickers" || it == "your_stickers" } ||
            name.contains("sticker") ||
            ext == "webp"
        ) {
            return StorageMediaCategory.STICKERS
        }
        if (segments.any { it == "gifs" } || ext == "gif") {
            return StorageMediaCategory.GIFS
        }
        if (segments.any { it == "audio" || it == "voice" || it == "voice_notes" }) {
            return StorageMediaCategory.VOICE_NOTES
        }
        if (segments.any { it == "videos" }) {
            return StorageMediaCategory.VIDEOS
        }
        if (segments.any { it == "photos" }) {
            // Extensionless files in photos/ are JPEG/PNG/HEIC payloads from Instagram exports
            if (ext.isEmpty() || ext in setOf("jpg", "jpeg", "png", "heic", "heif", "bmp", "avif")) {
                return StorageMediaCategory.IMAGES
            }
        }

        // 2. Extension & WhatsApp naming heuristics
        return when {
            ext in setOf("jpg", "jpeg", "png", "heic", "heif", "bmp", "avif") -> StorageMediaCategory.IMAGES
            ext in setOf("mp4", "mov", "mkv", "avi", "3gp", "webm") -> StorageMediaCategory.VIDEOS
            ext in setOf("opus", "ogg", "oga", "m4a", "mp3", "aac", "wav", "amr") ||
                name.startsWith("ptt-") -> StorageMediaCategory.VOICE_NOTES
            ext in setOf("txt", "html", "htm", "json", "csv", "vcf") -> StorageMediaCategory.TEXT
            else -> StorageMediaCategory.OTHERS
        }
    }

    fun scanDirectory(rootDir: File): MediaCategoryBreakdown {
        if (!rootDir.exists()) return MediaCategoryBreakdown()
        var textBytes = 0L
        var imageBytes = 0L
        var videoBytes = 0L
        var voiceNoteBytes = 0L
        var gifBytes = 0L
        var stickerBytes = 0L
        var otherBytes = 0L
        var count = 0

        rootDir.walkTopDown().filter { it.isFile }.forEach { file ->
            count++
            val len = file.length()
            when (classifyFile(file, rootDir)) {
                StorageMediaCategory.TEXT -> textBytes += len
                StorageMediaCategory.IMAGES -> imageBytes += len
                StorageMediaCategory.VIDEOS -> videoBytes += len
                StorageMediaCategory.VOICE_NOTES -> voiceNoteBytes += len
                StorageMediaCategory.GIFS -> gifBytes += len
                StorageMediaCategory.STICKERS -> stickerBytes += len
                StorageMediaCategory.OTHERS -> otherBytes += len
            }
        }

        return MediaCategoryBreakdown(
            textBytes = textBytes,
            imageBytes = imageBytes,
            videoBytes = videoBytes,
            voiceNoteBytes = voiceNoteBytes,
            gifBytes = gifBytes,
            stickerBytes = stickerBytes,
            otherBytes = otherBytes,
            fileCount = count
        )
    }

    /**
     * Deletes every file matching [category] inside [rootDir] and cleans up empty directories.
     * Returns total bytes reclaimed.
     */
    fun deleteCategoryInDirectory(rootDir: File, category: StorageMediaCategory): Long {
        if (!rootDir.exists()) return 0L
        var reclaimed = 0L
        rootDir.walkTopDown().filter { it.isFile }.forEach { file ->
            if (classifyFile(file, rootDir) == category) {
                val len = file.length()
                if (file.delete()) {
                    reclaimed += len
                }
            }
        }
        pruneEmptyDirectories(rootDir)
        return reclaimed
    }

    private fun pruneEmptyDirectories(rootDir: File) {
        rootDir.walkBottomUp()
            .filter { it.isDirectory && it != rootDir }
            .forEach { dir ->
                if (dir.listFiles()?.isEmpty() == true) {
                    dir.delete()
                }
            }
    }
}
