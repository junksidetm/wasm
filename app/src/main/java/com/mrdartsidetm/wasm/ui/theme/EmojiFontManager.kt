package com.mrdartsidetm.wasm.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Central registry of selectable and runtime-updatable emoji font packs.
 * Defaults to [WHATSAPP] across the app and WhatsApp chats; switchable to [FACEBOOK], [IOS], or
 * [SYSTEM] in Font Selection, and any pack can be updated cleanly by replacing/importing a `.ttf`.
 */
enum class EmojiPackOption(
    val id: String,
    val displayName: String,
    val bundledAssetPath: String?,
    val bundledVersion: String,
    val description: String
) {
    WHATSAPP(
        id = "whatsapp",
        displayName = "WhatsApp Emojis",
        bundledAssetPath = "fonts/whatsapp_emoji.ttf",
        bundledVersion = "2.26.8.72",
        description = "Default WhatsApp emoji set"
    ),
    FACEBOOK(
        id = "facebook",
        displayName = "Facebook Emojis",
        bundledAssetPath = "fonts/facebook_emoji.ttf",
        bundledVersion = "16.0",
        description = "Facebook & Messenger emoji style"
    ),
    IOS(
        id = "ios",
        displayName = "iOS Emojis",
        bundledAssetPath = "fonts/ios_emoji.ttf",
        bundledVersion = "Bundled",
        description = "Apple-style glossy emoji glyphs"
    ),
    SYSTEM(
        id = "system",
        displayName = "System Default",
        bundledAssetPath = null,
        bundledVersion = "Android OS",
        description = "Use device built-in emoji font"
    );

    companion object {
        val DEFAULT: EmojiPackOption = WHATSAPP

        fun fromId(id: String): EmojiPackOption =
            values().firstOrNull { it.id == id } ?: DEFAULT
    }
}

data class EmojiInstallResult(
    val pack: EmojiPackOption,
    val label: String,
    val sizeBytes: Long
)

object EmojiFontManager {

    private const val EMOJI_OVERRIDES_DIR = "emoji_packs"

    @Volatile
    private var cachedKey: String? = null

    @Volatile
    private var cachedPlatformFamily: Any? = null

    fun isValidFontHeader(headerBytes: ByteArray): Boolean {
        if (headerBytes.size < 4) return false
        val b0 = headerBytes[0].toInt() and 0xFF
        val b1 = headerBytes[1].toInt() and 0xFF
        val b2 = headerBytes[2].toInt() and 0xFF
        val b3 = headerBytes[3].toInt() and 0xFF

        val isSfntTrueType = (b0 == 0x00 && b1 == 0x01 && b2 == 0x00 && b3 == 0x00)
        val isOpenTypeCff = (b0 == 'O'.code && b1 == 'T'.code && b2 == 'T'.code && b3 == 'O'.code)
        val isAppleTrue = (b0 == 't'.code && b1 == 'r'.code && b2 == 'u'.code && b3 == 'e'.code)
        val isCollection = (b0 == 't'.code && b1 == 't'.code && b2 == 'c'.code && b3 == 'f'.code)
        return isSfntTrueType || isOpenTypeCff || isAppleTrue || isCollection
    }

    fun overrideFileForPack(context: Context, packId: String): File {
        val dir = File(context.filesDir, EMOJI_OVERRIDES_DIR)
        return File(dir, "${packId}_override.ttf")
    }

    fun hasOverride(context: Context, packId: String): Boolean {
        val f = overrideFileForPack(context, packId)
        return f.exists() && f.length() > 1024L
    }

    @Synchronized
    fun invalidateCache() {
        cachedKey = null
        cachedPlatformFamily = null
    }

    /**
     * Validates and installs an updated `.ttf` / `.ttc` / `.otf` emoji font into app storage
     * for [pack] so emoji packs can be updated cleanly without reinstalling the app.
     */
    fun installUpdatedEmojiFont(
        context: Context,
        pack: EmojiPackOption,
        uri: Uri,
        sourceDisplayName: String?
    ): Result<EmojiInstallResult> {
        return try {
            val dir = File(context.filesDir, EMOJI_OVERRIDES_DIR).apply { mkdirs() }
            val tempFile = File(dir, "${pack.id}_staging_${System.currentTimeMillis()}.ttf")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(IllegalStateException("Could not read selected font file."))

            if (tempFile.length() < 1024L) {
                tempFile.delete()
                return Result.failure(IllegalArgumentException("Selected file is too small to be a valid font."))
            }

            val header = ByteArray(4)
            FileInputStream(tempFile).use { it.read(header) }
            if (!isValidFontHeader(header)) {
                tempFile.delete()
                return Result.failure(IllegalArgumentException("Unsupported font format. Please choose a valid .ttf emoji font."))
            }

            // Verify Android font subsystem can actually parse the file
            val loadOk = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val font = android.graphics.fonts.Font.Builder(tempFile).build()
                    android.graphics.fonts.FontFamily.Builder(font).build()
                    true
                } else {
                    Typeface.createFromFile(tempFile) != null
                }
            } catch (_: Throwable) {
                false
            }
            if (!loadOk) {
                tempFile.delete()
                return Result.failure(IllegalArgumentException("Android could not parse this emoji font file."))
            }

            val destFile = overrideFileForPack(context, pack.id)
            if (destFile.exists()) destFile.delete()
            if (!tempFile.renameTo(destFile)) {
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
            }

            invalidateCache()

            val cleanLabel = sourceDisplayName
                ?.substringAfterLast('/')
                ?.removeSuffix(".ttf")
                ?.removeSuffix(".TTF")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "Custom TTF"

            Result.success(
                EmojiInstallResult(
                    pack = pack,
                    label = cleanLabel,
                    sizeBytes = destFile.length()
                )
            )
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun clearPackOverride(context: Context, pack: EmojiPackOption) {
        runCatching {
            val f = overrideFileForPack(context, pack.id)
            if (f.exists()) f.delete()
        }
        invalidateCache()
    }

    /**
     * Loads and caches the platform `android.graphics.fonts.FontFamily` for the active emoji pack
     * so sliding GSF axes or recomposing typography reuses the 34–38 MB bitmap TTF in memory.
     */
    fun getCachedPlatformEmojiFamily(
        context: Context,
        emojiPackId: String,
        emojiRevision: Int
    ): android.graphics.fonts.FontFamily? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val pack = EmojiPackOption.fromId(emojiPackId)
        val overrideFile = overrideFileForPack(context, pack.id).takeIf { it.exists() && it.length() > 1024L }

        if (pack == EmojiPackOption.SYSTEM && overrideFile == null) {
            return null
        }

        val cacheKey = "${pack.id}|${overrideFile?.absolutePath ?: pack.bundledAssetPath}|rev=$emojiRevision"
        val existing = cachedPlatformFamily
        if (cachedKey == cacheKey && existing is android.graphics.fonts.FontFamily) {
            return existing
        }

        return synchronized(this) {
            val again = cachedPlatformFamily
            if (cachedKey == cacheKey && again is android.graphics.fonts.FontFamily) {
                return@synchronized again
            }
            val built = try {
                val font = when {
                    overrideFile != null -> android.graphics.fonts.Font.Builder(overrideFile).build()
                    pack.bundledAssetPath != null -> android.graphics.fonts.Font.Builder(context.assets, pack.bundledAssetPath).build()
                    else -> null
                }
                font?.let { android.graphics.fonts.FontFamily.Builder(it).build() }
            } catch (_: Throwable) {
                null
            }
            if (built != null) {
                cachedKey = cacheKey
                cachedPlatformFamily = built
            }
            built
        }
    }
}
