package com.mrdartsidetm.wasm.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mrdartsidetm.wasm.ui.theme.GoogleSansCodeFontFamily

/**
 * Parses WhatsApp & Instagram inline rich formatting tokens strictly within a single continuous line.
 * Any formatting marker (`*`, `_`, `~`, `` ` ``, ```` ``` ````) will NOT match across a newline (`\n`).
 */
object ChatRichTextParser {

    private data class ActiveFormat(
        val bold: Boolean = false,
        val italic: Boolean = false,
        val strike: Boolean = false
    )

    fun parseInlineSingleLine(
        text: String,
        isDark: Boolean,
        textColor: Color,
        monoFontFamily: FontFamily = GoogleSansCodeFontFamily
    ): AnnotatedString {
        val builder = AnnotatedString.Builder()
        val codeBackground = if (isDark) Color(0x33FFFFFF) else Color(0x1A000000)
        val lines = text.split('\n')

        lines.forEachIndexed { lineIdx, line ->
            parseLine(
                segment = line,
                active = ActiveFormat(),
                builder = builder,
                textColor = textColor,
                codeBackground = codeBackground,
                monoFontFamily = monoFontFamily
            )
            if (lineIdx < lines.lastIndex) {
                builder.append('\n')
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
        monoFontFamily: FontFamily
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
                    i = closeIdx + 3
                    continue
                }
            }

            // Single backtick inline code on the same line: `text`
            if (segment[i] == '`') {
                val closeIdx = segment.indexOf('`', i + 1)
                if (closeIdx > i + 1) {
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
                    parseLine(inner, next, builder, textColor, codeBackground, monoFontFamily)
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
                    parseLine(inner, next, builder, textColor, codeBackground, monoFontFamily)
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
                    parseLine(inner, next, builder, textColor, codeBackground, monoFontFamily)
                    if (builder.length > start) {
                        builder.addStyle(spanStyleFor(next, textColor), start, builder.length)
                    }
                    i = closeIdx + 1
                    continue
                }
            }

            // Plain character
            val charStart = builder.length
            builder.append(segment[i])
            if (active.bold || active.italic || active.strike) {
                builder.addStyle(spanStyleFor(active, textColor), charStart, builder.length)
            }
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
