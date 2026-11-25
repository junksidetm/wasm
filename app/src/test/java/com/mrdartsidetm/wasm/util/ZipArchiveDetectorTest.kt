package com.mrdartsidetm.wasm.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipArchiveDetectorTest {

    @Test
    fun detectsWhatsAppTxtFile() {
        val tempFile = File.createTempFile("wa_chat", ".txt")
        try {
            tempFile.writeText("12/05/2024, 10:30 AM - Alice: Hello")
            assertEquals(
                DetectedArchivePlatform.WHATSAPP_TXT,
                ZipArchiveDetector.detectFromFile(tempFile, "WhatsApp Chat with Alice.txt")
            )
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun detectsSyntheticWhatsAppZip() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("WhatsApp Chat with Alice.txt"))
            zos.write("12/05/2024, 10:30 AM - Alice: Hello".toByteArray())
            zos.closeEntry()
        }
        val detected = ZipArchiveDetector.detectFromZipStream(
            ByteArrayInputStream(baos.toByteArray()),
            "export.zip"
        )
        assertEquals(DetectedArchivePlatform.WHATSAPP_ZIP, detected)
    }

    @Test
    fun detectsSyntheticInstagramZip() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("your_instagram_activity/messages/chats.html"))
            zos.write("<html></html>".toByteArray())
            zos.closeEntry()
        }
        val detected = ZipArchiveDetector.detectFromZipStream(
            ByteArrayInputStream(baos.toByteArray()),
            "export.zip"
        )
        assertEquals(DetectedArchivePlatform.INSTAGRAM_ZIP, detected)
    }

    @Test
    fun detectsRealUserTestArchivesWhenPresentOnDisk() {
        val waZip = File("D:\\code\\temp\\Chats\\WhatsApp Chat with Deepanjali COC.zip")
        if (waZip.exists()) {
            assertEquals(
                DetectedArchivePlatform.WHATSAPP_ZIP,
                ZipArchiveDetector.detectFromFile(waZip)
            )
            // Also verify parser works on the extracted .txt inside the real WhatsApp ZIP
            java.util.zip.ZipFile(waZip).use { zip ->
                val txtEntry = zip.entries().asSequence().first { it.name.endsWith(".txt", ignoreCase = true) }
                val lines = zip.getInputStream(txtEntry).bufferedReader().readLines()
                val parsed = WhatsAppParser.parse(lines)
                assertTrue("Expected parsed WhatsApp messages from real archive", parsed.isNotEmpty())
            }
        }

        val igZip = File("D:\\code\\temp\\Chats\\instagram-kajal.23.sinha-2026-09-14-sxQHlayj.zip")
        if (igZip.exists()) {
            assertEquals(
                DetectedArchivePlatform.INSTAGRAM_ZIP,
                ZipArchiveDetector.detectFromFile(igZip)
            )
        }
    }
}
