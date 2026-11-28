package com.mrdartsidetm.wasm.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

/**
 * Handles external intents cleanly using [FileProvider] for non-native document/archive/APK
 * attachments, plus external browser URLs and dialer intents from chat messages.
 */
object AttachmentOpenHelper {

    fun contentUriFor(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun mimeTypeFor(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "pdf" -> "application/pdf"
            "apk" -> "application/vnd.android.package-archive"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"
            "vcf" -> "text/x-vcard"
            "txt", "log" -> "text/plain"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "mp4", "mkv", "3gp", "webm" -> "video/*"
            "mp3", "m4a", "opus", "ogg", "aac", "wav" -> "audio/*"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        }
    }

    /**
     * Opens any non-media file (.pdf, .apk, .zip, .vcf, office docs, etc.) in an external Android app.
     * Returns true if an activity chooser / viewer could be launched.
     */
    fun openFileExternally(context: Context, file: File): Boolean {
        if (!file.exists()) return false
        val uri = try {
            contentUriFor(context, file)
        } catch (_: Exception) {
            return false
        }
        val mime = mimeTypeFor(file.name)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(viewIntent)
            true
        } catch (_: ActivityNotFoundException) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            return try {
                val chooser = Intent.createChooser(shareIntent, file.name).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                true
            } catch (_: Exception) {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun openExternalUrl(context: Context, rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        val normalized = if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun dialPhoneNumber(context: Context, rawNumber: String): Boolean {
        val cleaned = rawNumber.trim().replace(" ", "").replace("-", "")
        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
