package com.mrdartsidetm.wasm.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

/**
 * Verifies attachment extraction, media vs external document boundaries, and 1-on-1 DM sender
 * detection across the real export structure of `WhatsApp Chat with Abhijeet 😎.zip`.
 */
class WhatsAppParserAbhijeetExportTest {

    private val waAbhijeetZip = File("D:/code/temp/Chats/WhatsApp Chat with Abhijeet 😎.zip")

    @Test
    fun testParsesAllMediaAndExternalAttachmentVariantsFromExportLines() {
        val sampleLines = listOf(
            "29/11/2026, 12:02\u202Fam - Messages and calls are end-to-end encrypted. Only people in this chat can read, listen to, or share them. *Learn more*",
            "29/11/2026, 12:02\u202Fam - Darkside Studio: TESTING GROUND",
            "29/11/2026, 12:04\u202Fam - Darkside Studio: VID-20261129-WA0000.mp4 (file attached)",
            "29/11/2026, 12:04\u202Fam - Darkside Studio: STK-20261129-WA0001.webp (file attached)",
            "29/11/2026, 12:04\u202Fam - Darkside Studio: PTT-20261129-WA0002.opus (file attached)",
            "29/11/2026, 12:05\u202Fam - Darkside Studio: IMG-20261129-WA0003.jpg (file attached)",
            "29/11/2026, 12:08\u202Fam - Abhijeet 😎: Midway Deflation & Veno-Occlusive Lock Guide.pdf (file attached)",
            "29/11/2026, 12:09\u202Fam - Abhijeet 😎: LongShot-PDF-v1.1.0.apk (file attached)",
            "29/11/2026, 12:09\u202Fam - Abhijeet 😎: UUID1.zip (file attached)"
        )

        val messages = WhatsAppParser.parse(sampleLines)
        assertEquals(9, messages.size)
        assertTrue(messages[0].isSystemMessage)

        val mediaNames = messages.drop(2).map { WhatsAppParser.extractMediaName(it.content) }
        assertEquals(
            listOf(
                "VID-20261129-WA0000.mp4",
                "STK-20261129-WA0001.webp",
                "PTT-20261129-WA0002.opus",
                "IMG-20261129-WA0003.jpg",
                "Midway Deflation & Veno-Occlusive Lock Guide.pdf",
                "LongShot-PDF-v1.1.0.apk",
                "UUID1.zip"
            ),
            mediaNames
        )

        // Two human participants => 1-on-1 DM (not a group chat)
        val humanSenders = messages.filter { !it.isSystemMessage }.map { it.sender }.distinct()
        assertEquals(listOf("Darkside Studio", "Abhijeet 😎"), humanSenders)

        // Verify MIME mapping for external attachments
        assertEquals("application/pdf", AttachmentOpenHelper.mimeTypeFor("Midway Deflation & Veno-Occlusive Lock Guide.pdf"))
        assertEquals("application/vnd.android.package-archive", AttachmentOpenHelper.mimeTypeFor("LongShot-PDF-v1.1.0.apk"))
        assertEquals("application/zip", AttachmentOpenHelper.mimeTypeFor("UUID1.zip"))
    }

    @Test
    fun testRealAbhijeetZipArchiveWhenPresentLocally() {
        if (!waAbhijeetZip.exists()) return

        ZipFile(waAbhijeetZip).use { zip ->
            assertEquals(DetectedArchivePlatform.WHATSAPP_ZIP, ZipArchiveDetector.detectFromFile(waAbhijeetZip))
            val txtEntry = zip.entries().asSequence().firstOrNull { it.name.endsWith(".txt", ignoreCase = true) }
            assertTrue(txtEntry != null)
            val lines = zip.getInputStream(txtEntry!!).bufferedReader(Charsets.UTF_8).readLines()
            val messages = WhatsAppParser.parse(lines)
            assertTrue(messages.size >= 15)
            assertEquals(
                DateOrderPreference.DAY_FIRST,
                ChatSearchFilter.inferDateOrder(messages.map { it.timestamp })
            )
        }
    }
}
