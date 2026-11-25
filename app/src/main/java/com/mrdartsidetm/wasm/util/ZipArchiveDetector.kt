package com.mrdartsidetm.wasm.util

import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

enum class DetectedArchivePlatform {
    WHATSAPP_TXT,
    WHATSAPP_ZIP,
    INSTAGRAM_ZIP,
    UNKNOWN
}

/**
 * Inspects a chat export file (.txt or .zip) to determine whether it is a WhatsApp or Instagram export.
 */
object ZipArchiveDetector {

    fun detectFromFile(file: File, originalFileName: String? = null): DetectedArchivePlatform {
        val name = (originalFileName ?: file.name).lowercase()
        if (name.endsWith(".txt")) {
            return DetectedArchivePlatform.WHATSAPP_TXT
        }
        return file.inputStream().use { stream ->
            detectFromZipStream(stream, name)
        }
    }

    fun detectFromZipStream(inputStream: InputStream, fileNameHint: String? = null): DetectedArchivePlatform {
        var hasInstagramMarkers = false
        var hasTextChatEntry = false

        try {
            val zipStream = ZipInputStream(inputStream)
            var entry = zipStream.nextEntry
            while (entry != null) {
                val entryName = entry.name.lowercase()
                if (!entry.isDirectory && !entryName.contains("__macosx")) {
                    if (isInstagramArchiveEntry(entryName)) {
                        hasInstagramMarkers = true
                        break
                    }
                    if (entryName.endsWith(".txt")) {
                        hasTextChatEntry = true
                    }
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
        } catch (_: Exception) {
            // Fallback to filename hint if stream is not a valid ZIP
        }

        if (hasInstagramMarkers) return DetectedArchivePlatform.INSTAGRAM_ZIP
        if (hasTextChatEntry) return DetectedArchivePlatform.WHATSAPP_ZIP

        val hint = fileNameHint?.lowercase().orEmpty()
        if (hint.contains("instagram")) return DetectedArchivePlatform.INSTAGRAM_ZIP
        if (hint.contains("whatsapp") || hint.endsWith(".txt")) return DetectedArchivePlatform.WHATSAPP_TXT
        return DetectedArchivePlatform.UNKNOWN
    }

    private fun isInstagramArchiveEntry(entryName: String): Boolean {
        return entryName == "start_here.html" ||
            entryName.endsWith("/start_here.html") ||
            entryName == "chats.html" ||
            entryName.endsWith("/chats.html") ||
            entryName.contains("your_instagram_activity/messages/") ||
            entryName.contains("messages/inbox/") ||
            entryName.contains("messages/message_requests/") ||
            (entryName.startsWith("messages/") && (entryName.endsWith(".html") || entryName.endsWith(".json")))
    }
}
