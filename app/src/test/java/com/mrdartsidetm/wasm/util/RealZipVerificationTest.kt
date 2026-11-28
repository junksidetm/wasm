package com.mrdartsidetm.wasm.util

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

/**
 * Verifies end-to-end parsing, platform auto-detection, and storage category classification
 * against the real user-provided chat exports when available locally.
 */
class RealZipVerificationTest {

    private val igZipFile = File("D:/code/temp/Chats/instagram-kajal.23.sinha-2026-09-14-sxQHlayj.zip")
    private val waZipFile = File("D:/code/temp/Chats/WhatsApp Chat with Deepanjali COC.zip")

    @Test
    fun testRealInstagramZipDetectionAndParsing() {
        if (!igZipFile.exists()) return

        ZipFile(igZipFile).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toList()
            assertEquals(DetectedArchivePlatform.INSTAGRAM_ZIP, ZipArchiveDetector.detectFromFile(igZipFile))

            val startHereEntry = zip.getEntry("start_here.html")
            assertNotNull(startHereEntry)
            val account = InstagramHtmlParser.parseAccountInfo(
                zip.getInputStream(startHereEntry).bufferedReader(Charsets.UTF_8).readText(),
                igZipFile.name
            )
            assertEquals("kajal.23.sinha", account.accountName)
            assertEquals("Kajal Sinha", account.displayName)

            val chatsEntry = zip.getEntry("your_instagram_activity/messages/chats.html")
            assertNotNull(chatsEntry)
            val chats = InstagramHtmlParser.parseChatsList(
                zip.getInputStream(chatsEntry).bufferedReader(Charsets.UTF_8).readText()
            )
            assertTrue("Expected chats in real Instagram export", chats.isNotEmpty())

            val msgEntry = zip.getEntry("your_instagram_activity/messages/inbox/1405380277523017/message_1.html")
            assertNotNull(msgEntry)
            val messages = InstagramHtmlParser.parseConversationMessages(
                htmlContent = zip.getInputStream(msgEntry).bufferedReader(Charsets.UTF_8).readText(),
                conversationId = "1405380277523017",
                accountOwnerDisplayName = account.displayName
            )
            assertTrue("Expected messages in sample Instagram thread", messages.size > 100)
            // Chronological order (oldest first -> newest last)
            assertTrue(messages.first().epochTime <= messages.last().epochTime)

            // Verify folder-aware media classification for extensionless photos and audio/*.mp4
            val extensionlessPhoto = names.firstOrNull { it.contains("/photos/") && !it.substringAfterLast('/').contains('.') }
            if (extensionlessPhoto != null) {
                assertEquals(
                    StorageMediaCategory.IMAGES,
                    StorageMediaHelper.classifyFile(File(extensionlessPhoto))
                )
            }
            val audioMp4 = names.firstOrNull { it.contains("/audio/") && it.endsWith(".mp4") }
            if (audioMp4 != null) {
                assertEquals(
                    StorageMediaCategory.VOICE_NOTES,
                    StorageMediaHelper.classifyFile(File(audioMp4))
                )
            }
            val videoMp4 = names.firstOrNull { it.contains("/videos/") && it.endsWith(".mp4") }
            if (videoMp4 != null) {
                assertEquals(
                    StorageMediaCategory.VIDEOS,
                    StorageMediaHelper.classifyFile(File(videoMp4))
                )
            }
        }
    }

    @Test
    fun testRealWhatsAppZipDetectionAndParsing() {
        if (!waZipFile.exists()) return

        ZipFile(waZipFile).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toList()
            assertEquals(DetectedArchivePlatform.WHATSAPP_ZIP, ZipArchiveDetector.detectFromFile(waZipFile))

            val txtEntry = zip.entries().asSequence().firstOrNull { it.name.endsWith(".txt", ignoreCase = true) }
            assertNotNull(txtEntry)
            val lines = zip.getInputStream(txtEntry).bufferedReader(Charsets.UTF_8).readLines()
            val messages = WhatsAppParser.parse(lines)
            assertTrue("Expected parsed WhatsApp messages", messages.isNotEmpty())
        }
    }
}
