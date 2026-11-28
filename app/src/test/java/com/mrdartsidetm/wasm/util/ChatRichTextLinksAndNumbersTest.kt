package com.mrdartsidetm.wasm.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies URL, 10-digit phone number, and 4/6-digit code annotation behavior
 * alongside WhatsApp single-line formatting markers grounded in real chat exports.
 */
class ChatRichTextLinksAndNumbersTest {

    @Test
    fun testAbhijeetExportMarkdownAndLinksAndPhones() {
        val sample = """
            *Hi*
            _Hello_
             ~Hello~
             ```Hello```


            +91 2678262728
            +910972648293
            09273718362

            github.com
            https://github.com
            www.google.com
        """.trimIndent()

        val annotated = ChatRichTextParser.parseInlineSingleLine(
            text = sample,
            isDark = false,
            textColor = Color.Black,
            monoFontFamily = FontFamily.Monospace
        )

        val urls = annotated.getStringAnnotations(ChatRichTextParser.TAG_URL, 0, annotated.length)
            .map { it.item }
        assertEquals(listOf("github.com", "https://github.com", "www.google.com"), urls)

        val phones = annotated.getStringAnnotations(ChatRichTextParser.TAG_PHONE, 0, annotated.length)
            .map { it.item }
        assertEquals(listOf("+91 2678262728", "+910972648293", "09273718362"), phones)

        // Verify inline formatting spans also coexist cleanly
        assertTrue(annotated.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(annotated.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
        assertTrue(annotated.spanStyles.any { it.item.textDecoration == TextDecoration.LineThrough })
        assertTrue(annotated.spanStyles.any { it.item.fontFamily == FontFamily.Monospace })
    }

    @Test
    fun testCodesIgnoreTimestampsFilenamesAndFiveDigitNumbers() {
        val sample = "OTP is 482910 and PIN 1234. Ignore 12345, version 2026.11, 29/11/2026, 12:04, VID-20261129-WA0000.mp4."

        val annotated = ChatRichTextParser.parseInlineSingleLine(
            text = sample,
            isDark = true,
            textColor = Color.White,
            monoFontFamily = FontFamily.Monospace
        )

        val codes = annotated.getStringAnnotations(ChatRichTextParser.TAG_CODE, 0, annotated.length)
            .map { it.item }
        assertEquals(listOf("482910", "1234"), codes)

        val phones = annotated.getStringAnnotations(ChatRichTextParser.TAG_PHONE, 0, annotated.length)
        assertTrue(phones.isEmpty())
    }

    @Test
    fun testMonospaceProtectsUrlsAndNumbersInsideCodeSpan() {
        val sample = "```github.com``` and 9876543210 plus github.com."

        val annotated = ChatRichTextParser.parseInlineSingleLine(
            text = sample,
            isDark = false,
            textColor = Color.Black,
            monoFontFamily = FontFamily.Monospace
        )

        val urls = annotated.getStringAnnotations(ChatRichTextParser.TAG_URL, 0, annotated.length)
            .map { it.item }
        // Only the URL outside the monospace span should be annotated, and trailing period stripped
        assertEquals(listOf("github.com"), urls)

        val phones = annotated.getStringAnnotations(ChatRichTextParser.TAG_PHONE, 0, annotated.length)
            .map { it.item }
        assertEquals(listOf("9876543210"), phones)
    }
}
