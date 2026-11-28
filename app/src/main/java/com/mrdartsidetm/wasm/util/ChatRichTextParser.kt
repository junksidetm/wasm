package com.mrdartsidetm.wasm.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mrdartsidetm.wasm.ui.theme.GoogleSansCodeFontFamily
import java.util.regex.Pattern

/**
 * Parses WhatsApp & Instagram inline rich formatting tokens strictly within a single continuous line,
 * and highlights clickable URLs (including schemeless domains like `github.com`),
 * 10-digit phone numbers (with optional country code or `0` prefix), and 4-digit / 6-digit codes.
 */
object ChatRichTextParser {

    const val TAG_URL = "wasm_url"
    const val TAG_PHONE = "wasm_phone"
    const val TAG_CODE = "wasm_code"

    private data class ActiveFormat(
        val bold: Boolean = false,
        val italic: Boolean = false,
        val strike: Boolean = false
    )

    private val urlPattern = Pattern.compile(
        """(?i)(?:https?://[^\s<>()]+|www\.[a-z0-9-]+(?:\.[a-z0-9-]+)+(?:/[^\s<>()]*)?|(?<![@/.\w-])[a-z0-9-]+(?:\.[a-z0-9-]+)*\.(?:com|org|net|io|dev|ai|app|in|co|uk|edu|gov|me|tv|be|info|biz|gg|sh|ly|tech|xyz|online|site|club|store|live|cloud)(?::\d{2,5})?(?:/[^\s<>()]*)?(?![\w-])(?!\.[a-z0-9]))"""
    )

    private val phonePattern = Pattern.compile(
        """(?:\+\d{1,3}[\s-]?\d{5}[\s-]?\d{5}|\+\d{1,3}[\s-]?\d{10}|(?<![\d+.:/-])0\d{10}|(?<![\d+.:/-])\d{10})(?!\d)(?![.:/-]\d)"""
    )

    private val codePattern = Pattern.compile(
        """(?<![\w+.:/-])(?:\d{6}|\d{4})(?![\w:/-])(?!\.\d)"""
    )

    fun parseInlineSingleLine(
        text: String,
        isDark: Boolean,
        textColor: Color,
        monoFontFamily: FontFamily = GoogleSansCodeFontFamily
    ): AnnotatedString {
        val builder = AnnotatedString.Builder()
        val codeBackground = if (isDark) Color(0x33FFFFFF) else Color(0x1A000000)
        val monoRanges = mutableListOf<IntRange>()
        val lines = text.split('\n')

        lines.forEachIndexed { lineIdx, line ->
            parseLine(
                segment = line,
                active = ActiveFormat(),
                builder = builder,
                textColor = textColor,
                codeBackground = codeBackground,
                monoFontFamily = monoFontFamily,
                monoRanges = monoRanges
            )
            if (lineIdx < lines.lastIndex) {
                builder.append('\n')
            }
        }

        val rendered = builder.toAnnotatedString().text
        val entityColor = if (isDark) Color(0xFF64B5F6) else Color(0xFF0B57D0)
        val occupiedRanges = mutableListOf<IntRange>()
        occupiedRanges.addAll(monoRanges)

        fun isRangeFree(start: Int, endExclusive: Int): Boolean {
            return occupiedRanges.none { r -> start <= r.last && endExclusive > r.first }
        }

        // 1. Detect URLs (explicit https://, www., and schemeless domains)
        val urlMatcher = urlPattern.matcher(rendered)
        while (urlMatcher.find()) {
            var start = urlMatcher.start()
            var end = urlMatcher.end()
            while (end > start && rendered[end - 1] in listOf('.', ',', ')', ']', '!', '?')) {
                end--
            }
            if (end > start && isRangeFree(start, end)) {
                val matched = rendered.substring(start, end)
                builder.addStyle(
                    SpanStyle(
                        color = entityColor,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    ),
                    start,
                    end
                )
                builder.addStringAnnotation(TAG_URL, matched, start, end)
                occupiedRanges.add(start until end)
            }
        }

        // 2. Detect 10-digit phone numbers (with optional +<country> or 0 prefix)
        val phoneMatcher = phonePattern.matcher(rendered)
        while (phoneMatcher.find()) {
            val start = phoneMatcher.start()
            val end = phoneMatcher.end()
            if (end > start && isRangeFree(start, end)) {
                val matched = rendered.substring(start, end)
                builder.addStyle(
                    SpanStyle(
                        color = entityColor,
                        fontWeight = FontWeight.Bold
                    ),
                    start,
                    end
                )
                builder.addStringAnnotation(TAG_PHONE, matched, start, end)
                occupiedRanges.add(start until end)
            }
        }

        // 3. Detect standalone 4-digit and 6-digit codes
        val codeMatcher = codePattern.matcher(rendered)
        while (codeMatcher.find()) {
            val start = codeMatcher.start()
            val end = codeMatcher.end()
            if (end > start && isRangeFree(start, end)) {
                val matched = rendered.substring(start, end)
                builder.addStyle(
                    SpanStyle(
                        color = entityColor,
                        fontWeight = FontWeight.Bold
                    ),
                    start,
                    end
                )
                builder.addStringAnnotation(TAG_CODE, matched, start, end)
                occupiedRanges.add(start until end)
            }
        }

        return builder.toAnnotatedString()
    }

    private fun spanStyleFor(active: ActiveFormat, textColor: Color): SpanStyle {
        return SpanStyle(
            fontWeight = if (active.bold) FontWeight.Bold else null,
            fontStyle = if (active.italic) FontStyle.Italic else null,
            textDecoration = if (active.strike) TextDecoration.LineThrough else null,
            color = textColor
        )
    }

    private fun parseLine(
        segment: String,
        active: ActiveFormat,
        builder: AnnotatedString.Builder,
        textColor: Color,
        codeBackground: Color,
        monoFontFamily: FontFamily,
        monoRanges: MutableList<IntRange>
    ) {
        var i = 0
        while (i < segment.length) {
            // Triple backtick monospace on the same line: ```text```
            if (segment.startsWith("```", i)) {
                val closeIdx = segment.indexOf("```", i + 3)
                if (closeIdx > i + 3) {
                    val inner = segment.substring(i + 3, closeIdx)
                    val start = builder.length
                    builder.append(inner)
                    builder.addStyle(
                        SpanStyle(
                            fontFamily = monoFontFamily,
                            background = codeBackground,
                            color = textColor
                        ),
                        start,
                        builder.length
                    )
                    monoRanges.add(start until builder.length)
                    i = closeIdx + 3
                    continue
                } else {
                    // Emit unclosed triple backticks literally so they cannot pair with a single backtick
                    builder.append("```")
                    i += 3
                    continue
                }
            }

            // Single backtick inline code on the same line: `text`
            if (segment[i] == '`') {
                val closeIdx = segment.indexOf('`', i + 1)
                if (closeIdx > i + 1 && !segment.startsWith("```", closeIdx)) {
                    val inner = segment.substring(i + 1, closeIdx)
                    val start = builder.length
                    builder.append(inner)
                    builder.addStyle(
                        SpanStyle(
                            fontFamily = monoFontFamily,
                            background = codeBackground,
                            color = textColor
                        ),
                        start,
                        builder.length
                    )
                    monoRanges.add(start until builder.length)
                    i = closeIdx + 1
                    continue
                }
            }

            // Bold: *text*
            if (segment[i] == '*') {
                val closeIdx = findClosingDelimiterOnLine(segment, i, '*')
                if (closeIdx != -1) {
                    val inner = segment.substring(i + 1, closeIdx)
                    val start = builder.length
                    val next = active.copy(bold = true)
                    parseLine(inner, next, builder, textColor, codeBackground, monoFontFamily, monoRanges)
                    if (builder.length > start) {
                        builder.addStyle(spanStyleFor(next, textColor), start, builder.length)
                    }
                    i = closeIdx + 1
                    continue
                }
            }

            // Italic / Slant: _text_
            if (segment[i] == '_') {
                val closeIdx = findClosingDelimiterOnLine(segment, i, '_')
                if (closeIdx != -1) {
                    val inner = segment.substring(i + 1, closeIdx)
                    val start = builder.length
                    val next = active.copy(italic = true)
                    parseLine(inner, next, builder, textColor, codeBackground, monoFontFamily, monoRanges)
                    if (builder.length > start) {
                        builder.addStyle(spanStyleFor(next, textColor), start, builder.length)
                    }
                    i = closeIdx + 1
                    continue
                }
            }

            // Strikethrough: ~text~
            if (segment[i] == '~') {
                val closeIdx = findClosingDelimiterOnLine(segment, i, '~')
                if (closeIdx != -1) {
                    val inner = segment.substring(i + 1, closeIdx)
                    val start = builder.length
                    val next = active.copy(strike = true)
                    parseLine(inner, next, builder, textColor, codeBackground, monoFontFamily, monoRanges)
                    if (builder.length > start) {
                        builder.addStyle(spanStyleFor(next, textColor), start, builder.length)
                    }
                    i = closeIdx + 1
                    continue
                }
            }

            // Plain character (enclosing delimiter recursion applies SpanStyle over the full span)
            builder.append(segment[i])
            i++
        }
    }

    private fun findClosingDelimiterOnLine(line: String, startIndex: Int, delimiter: Char): Int {
        if (startIndex + 1 >= line.length || line[startIndex + 1].isWhitespace()) return -1
        for (j in (startIndex + 1) until line.length) {
            if (line[j] == delimiter && j > startIndex + 1 && !line[j - 1].isWhitespace()) {
                return j
            }
        }
        return -1
    }
}
