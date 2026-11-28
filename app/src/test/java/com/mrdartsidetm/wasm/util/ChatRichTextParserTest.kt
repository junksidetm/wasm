package com.mrdartsidetm.wasm.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mrdartsidetm.wasm.ui.theme.GoogleSansCodeFontFamily
import org.junit.Assert.*
import org.junit.Test

class ChatRichTextParserTest {

    @Test
    fun testSingleLineFormattingAllTokens() {
        val annotated = ChatRichTextParser.parseInlineSingleLine(
            text = "*8* _8_ ~8~ ```8``` `9`",
            isDark = false,
            textColor = Color.Black,
            monoFontFamily = FontFamily.Monospace
        )
        assertEquals("8 8 8 8 9", annotated.text)

        val boldSpans = annotated.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        val italicSpans = annotated.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
        val strikeSpans = annotated.spanStyles.filter { it.item.textDecoration == TextDecoration.LineThrough }
        val monoSpans = annotated.spanStyles.filter { it.item.fontFamily == FontFamily.Monospace }

        assertTrue(boldSpans.any { annotated.text.substring(it.start, it.end) == "8" })
        assertTrue(italicSpans.any { annotated.text.substring(it.start, it.end) == "8" })
        assertTrue(strikeSpans.any { annotated.text.substring(it.start, it.end) == "8" })
        assertEquals(2, monoSpans.size)
        assertEquals("8", annotated.text.substring(monoSpans[0].start, monoSpans[0].end))
        assertEquals("9", annotated.text.substring(monoSpans[1].start, monoSpans[1].end))
    }

    @Test
    fun testDefaultMonoUsesGoogleSansCode() {
        val annotated = ChatRichTextParser.parseInlineSingleLine(
            text = "```val x = 8```",
            isDark = false,
            textColor = Color.Black
        )
        assertEquals("val x = 8", annotated.text)
        assertTrue(annotated.spanStyles.any { it.item.fontFamily == GoogleSansCodeFontFamily })
    }

    @Test
    fun testNewlineBreaksInlineFormatting() {
        val multiLine = "*bold start\nbold end* _slant\nend_ ~strike\nend~ ```code\nend``` `inline\nend`"
        val annotated = ChatRichTextParser.parseInlineSingleLine(
            text = multiLine,
            isDark = false,
            textColor = Color.Black,
            monoFontFamily = FontFamily.Monospace
        )
        // Delimiters remain literal and no bold/italic/strike/mono span crosses a line break
        assertEquals(multiLine, annotated.text)
        assertTrue(annotated.spanStyles.none { it.item.fontWeight == FontWeight.Bold })
        assertTrue(annotated.spanStyles.none { it.item.fontStyle == FontStyle.Italic })
        assertTrue(annotated.spanStyles.none { it.item.textDecoration == TextDecoration.LineThrough })
        assertTrue(annotated.spanStyles.none { it.item.fontFamily == FontFamily.Monospace })
    }

    @Test
    fun testMixedMultiLineWithSingleLineFormattedSegments() {
        val text = "Header *one*\n*unclosed\nSecond ~line~ here"
        val annotated = ChatRichTextParser.parseInlineSingleLine(
            text = text,
            isDark = true,
            textColor = Color.White
        )
        assertEquals("Header one\n*unclosed\nSecond line here", annotated.text)
        val boldSpans = annotated.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        val strikeSpans = annotated.spanStyles.filter { it.item.textDecoration == TextDecoration.LineThrough }
        assertEquals(1, boldSpans.size)
        assertEquals("one", annotated.text.substring(boldSpans[0].start, boldSpans[0].end))
        assertEquals(1, strikeSpans.size)
        assertEquals("line", annotated.text.substring(strikeSpans[0].start, strikeSpans[0].end))
    }
}
