package com.mrdartsidetm.wasm.util

import com.mrdartsidetm.wasm.ui.theme.EmojiFontManager
import com.mrdartsidetm.wasm.ui.theme.EmojiPackOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

/**
 * Verifies real-world WhatsApp group chat parsing (grounded in `Jhut ke karo phuch phuch 💦💦.zip`),
 * empty-content sender attribution, bidi mention isolate stripping, and updatable emoji pack registry defaults.
 */
class WhatsAppGroupAndEmojiTest {

    private val groupZip = File("D:/code/temp/Chats/WhatsApp Chat with Jhut ke karo phuch phuch 💦💦.zip")

    @Test
    fun testGroupChatSystemMessagesEmptySenderLinesAndMentions() {
        val sampleLines = listOf(
            "09/08/2025, 4:45\u202Fpm - Aditya Singh Vasisth created group \"Jhuko thoda\".",
            "09/08/2025, 4:45\u202Fpm - Aditya Singh Vasisth added you",
            "09/08/2025, 4:45\u202Fpm - Aditya Singh Vasisth: Kya Krna h",
            "09/08/2025, 4:46\u202Fpm - Abhijeet Yadav \uD83D\uDE0E: Aur 12 ya kal ka karegey to?",
            "09/08/2025, 4:47\u202Fpm - Aditya Singh Vasisth: Soch lo <This message was edited>",
            "09/08/2025, 4:47\u202Fpm - Aditya Singh Vasisth: @\u2068Ashwin Bhai 10 C\u2069 tu ready h",
            "09/08/2025, 4:48\u202Fpm - Abhijeet Yadav \uD83D\uDE0E: ",
            "09/08/2025, 5:03\u202Fpm - Ashwin Bhai 10 C: Dukhas",
            "09/08/2025, 6:59\u202Fpm - Ashwin Bhai 10 C changed the group name from \"Jhuko mat, Barabari Karo\" to \"Jhut ke karo phuch phuch \uD83D\uDCA6\uD83D\uDCA6\".",
            "09/08/2025, 6:59\u202Fpm - You were added."
        )

        val parsed = WhatsAppParser.parse(sampleLines)
        assertEquals(10, parsed.size)

        // System messages identified cleanly
        assertTrue(parsed[0].isSystemMessage)
        assertTrue(parsed[1].isSystemMessage)
        assertTrue(parsed[8].isSystemMessage)
        assertTrue(parsed[9].isSystemMessage)

        // Mention bidi isolates stripped without touching display name
        assertEquals("@Ashwin Bhai 10 C tu ready h", parsed[5].content)
        assertFalse(parsed[5].content.contains('\u2068'))
        assertFalse(parsed[5].content.contains('\u2069'))

        // Empty-body sender line remains attributed to its sender instead of System
        assertFalse(parsed[6].isSystemMessage)
        assertEquals("Abhijeet Yadav \uD83D\uDE0E", parsed[6].sender)
        assertEquals("", parsed[6].content)

        // Group participants detected (> 2 human senders)
        val distinctHumanSenders = parsed.filter { !it.isSystemMessage }.map { it.sender }.distinct()
        assertEquals(3, distinctHumanSenders.size)
    }

    @Test
    fun testRealGroupZipArchiveWhenPresentLocally() {
        if (!groupZip.exists()) return

        ZipFile(groupZip).use { zip ->
            assertEquals(DetectedArchivePlatform.WHATSAPP_ZIP, ZipArchiveDetector.detectFromFile(groupZip))
            val txtEntry = zip.entries().asSequence().firstOrNull { it.name.endsWith(".txt", ignoreCase = true) }
            assertTrue(txtEntry != null)
            val lines = zip.getInputStream(txtEntry!!).bufferedReader(Charsets.UTF_8).readLines()
            val messages = WhatsAppParser.parse(lines)
            val humanSenders = messages.filter { !it.isSystemMessage }.map { it.sender }.distinct()
            assertEquals(3, humanSenders.size)
            assertTrue(messages.none { it.isSystemMessage && it.content.endsWith(":") })
        }
    }

    @Test
    fun testEmojiPackRegistryDefaultsAndFontHeaderValidation() {
        assertEquals(EmojiPackOption.WHATSAPP, EmojiPackOption.DEFAULT)
        assertEquals("whatsapp", EmojiPackOption.DEFAULT.id)
        assertEquals("fonts/whatsapp_emoji.ttf", EmojiPackOption.WHATSAPP.bundledAssetPath)
        assertEquals("2.26.8.72", EmojiPackOption.WHATSAPP.bundledVersion)

        assertEquals(EmojiPackOption.FACEBOOK, EmojiPackOption.fromId("facebook"))
        assertEquals("fonts/facebook_emoji.ttf", EmojiPackOption.FACEBOOK.bundledAssetPath)
        assertEquals("16.0", EmojiPackOption.FACEBOOK.bundledVersion)

        assertTrue(EmojiFontManager.isValidFontHeader(byteArrayOf(0x00, 0x01, 0x00, 0x00)))
        assertTrue(EmojiFontManager.isValidFontHeader("OTTO".toByteArray()))
        assertTrue(EmojiFontManager.isValidFontHeader("ttcf".toByteArray()))
        assertFalse(EmojiFontManager.isValidFontHeader("PK\u0003\u0004".toByteArray()))
        assertFalse(EmojiFontManager.isValidFontHeader(byteArrayOf(0x00, 0x01)))
    }
}
