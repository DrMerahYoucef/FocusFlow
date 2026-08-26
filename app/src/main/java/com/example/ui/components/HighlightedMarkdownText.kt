package com.example.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.data.repository.NoteHighlight
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.NeumorphicColors

@Composable
fun HighlightedMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = NeumorphicColors.TextPrimary,
    fontSize: TextUnit = 16.sp,
    highlights: List<NoteHighlight> = emptyList()
) {
    val isDark = LocalIsDarkTheme.current
    val annotatedString = remember(markdown, color, highlights, isDark) {
        parseMarkdownWithHighlights(
            input = markdown,
            defaultTextColor = color,
            highlights = highlights,
            isDark = isDark
        )
    }
    Text(
        text = annotatedString,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        lineHeight = (fontSize.value * 1.45).sp
    )
}

/**
 * Robust markdown and highlight parser for Compose AnnotatedString.
 * Converts bold, italic, inline code, strikethrough, markdown highlights (==...==),
 * and structured NoteHighlights into AnnotatedString SpanStyles without leaking syntax characters.
 */
fun parseMarkdownWithHighlights(
    input: String,
    defaultTextColor: Color = Color.Unspecified,
    highlights: List<NoteHighlight> = emptyList(),
    isDark: Boolean = false
): AnnotatedString {
    if (input.isBlank()) return AnnotatedString("")

    // 1. Gather all highlights from markdown tags (==color:x==term== or ==term==) and explicit list
    val allHighlights = mutableListOf<NoteHighlight>()
    allHighlights.addAll(highlights.filter { it.text.isNotBlank() })

    val mdHighlightRegex = Regex("==(?:color:(amber|green|blue|red)==)?(.*?)==", RegexOption.DOT_MATCHES_ALL)
    mdHighlightRegex.findAll(input).forEach { match ->
        val colorName = match.groupValues.getOrNull(1)?.lowercase()?.ifBlank { "amber" } ?: "amber"
        val term = match.groupValues.getOrNull(2) ?: ""
        if (term.isNotBlank()) {
            allHighlights.add(NoteHighlight(term, colorName))
        }
    }

    // Clean highlight texts from markdown tokens if any so they match against plain text
    val cleanHighlightTargets = allHighlights.map { h ->
        val cleanedText = stripMarkdownTokens(h.text).trim()
        NoteHighlight(if (cleanedText.isNotEmpty()) cleanedText else h.text.trim(), h.color.lowercase())
    }.filter { it.text.isNotBlank() }

    // Strip inline ==...== markers for initial formatting parsing
    var textToParse = mdHighlightRegex.replace(input) { matchResult ->
        matchResult.groupValues.getOrNull(2) ?: ""
    }

    // Normalize unclosed markdown bold/italics (e.g. "**word" without closing "**") so raw syntax does not leak
    textToParse = autoCloseOrStripUnclosedFormatting(textToParse)

    // 2. Parse inline markdown (bold, italic, code, etc.) into AnnotatedString
    val builder = AnnotatedString.Builder()
    appendFormattedMarkdown(builder, textToParse, defaultTextColor)
    val plainString = builder.toAnnotatedString().text

    // 3. Apply highlight backgrounds over the rendered clean text
    // Light mode: high saturation pastel tones with dark text
    // Dark mode: vibrant medium tones with pure white text
    val amberBg = if (isDark) Color(0xFFD97706).copy(alpha = 0.55f) else Color(0xFFFEF08A).copy(alpha = 0.90f)
    val greenBg = if (isDark) Color(0xFF059669).copy(alpha = 0.55f) else Color(0xFFBBF7D0).copy(alpha = 0.90f)
    val blueBg = if (isDark) Color(0xFF2563EB).copy(alpha = 0.55f) else Color(0xFFBFDBFE).copy(alpha = 0.90f)
    val redBg = if (isDark) Color(0xFFDC2626).copy(alpha = 0.55f) else Color(0xFFFECDD3).copy(alpha = 0.90f)
    val highlightTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)

    data class SpanMatch(val start: Int, val end: Int, val color: String)
    val matchedSpans = mutableListOf<SpanMatch>()

    cleanHighlightTargets.forEach { h ->
        val target = h.text.trim()
        if (target.length >= 2) {
            var searchStart = 0
            while (searchStart < plainString.length) {
                val idx = plainString.indexOf(target, searchStart, ignoreCase = true)
                if (idx < 0) break
                val endIdx = idx + target.length
                // Avoid overlapping duplicate spans
                if (matchedSpans.none { existing -> idx < existing.end && endIdx > existing.start }) {
                    matchedSpans.add(SpanMatch(idx, endIdx, h.color))
                }
                searchStart = idx + 1
            }
        }
    }

    matchedSpans.forEach { span ->
        val bgColor = when (span.color) {
            "green" -> greenBg
            "blue" -> blueBg
            "red" -> redBg
            else -> amberBg
        }
        builder.addStyle(
            SpanStyle(
                background = bgColor,
                color = highlightTextColor,
                fontWeight = FontWeight.Bold
            ),
            span.start,
            span.end
        )
    }

    return builder.toAnnotatedString()
}

/**
 * Strips basic markdown tokens (*, _, `, #, ~) from a string.
 */
fun stripMarkdownTokens(text: String): String {
    return text
        .replace(Regex("""[*_`#~]"""), "")
        .replace(Regex("""^==.*?==$"""), "")
        .trim()
}

/**
 * Auto-closes or cleans stray unclosed bold/italic tags so they never appear literally.
 */
private fun autoCloseOrStripUnclosedFormatting(text: String): String {
    var result = text
    // Check if ** count is odd
    val doubleAsteriskCount = Regex("""\*\*""").findAll(result).count()
    if (doubleAsteriskCount % 2 != 0) {
        // If there's an odd number of **, try to close it before punctuation or at the end
        val lastIdx = result.lastIndexOf("**")
        if (lastIdx >= 0) {
            val after = result.substring(lastIdx + 2)
            if (!after.contains("**")) {
                // Find next punctuation like ; , . or newline
                val punctIdx = after.indexOfAny(charArrayOf(';', ',', '.', '\n', ':'))
                result = if (punctIdx >= 0) {
                    result.substring(0, lastIdx + 2 + punctIdx) + "**" + result.substring(lastIdx + 2 + punctIdx)
                } else {
                    "$result**"
                }
            }
        }
    }
    return result
}

/**
 * Parses markdown inline styles (bold, italic, code, strikethrough) into an AnnotatedString.Builder.
 */
internal fun appendFormattedMarkdown(
    builder: AnnotatedString.Builder,
    text: String,
    textColor: Color
) {
    if (text.isEmpty()) return

    // Regex matching:
    // 1: ***bold italic*** or ___bold italic___
    // 2: **bold** or __bold__
    // 3: *italic* or _italic_
    // 4: `inline code`
    // 5: ~~strikethrough~~
    val formattingRegex = Regex(
        """(?:\*\*\*(.+?)\*\*\*|___(.+?)___|\*\*(.+?)\*\*|__(.+?)__|(?<!\w)\*(.+?)\*(?!\w)|(?<!\w)_(.+?)_(?!\w)|`([^`]+)`|~~(.+?)~~)""",
        RegexOption.DOT_MATCHES_ALL
    )

    var currentIndex = 0
    val matches = formattingRegex.findAll(text).toList()

    if (matches.isEmpty()) {
        // Strip any stray unparsed markers like standalone ** or #
        val cleanPlain = text.replace(Regex("""(?<!\w)\*\*|\*\*(?!\w)"""), "")
            .replace(Regex("""^#+\s*"""), "")
        builder.append(cleanPlain)
        return
    }

    for (match in matches) {
        val start = match.range.first
        val end = match.range.last + 1

        if (start > currentIndex) {
            val between = text.substring(currentIndex, start)
                .replace(Regex("""(?<!\w)\*\*|\*\*(?!\w)"""), "")
                .replace(Regex("""^#+\s*"""), "")
            builder.append(between)
        }

        val boldItalic1 = match.groupValues[1]
        val boldItalic2 = match.groupValues[2]
        val bold1 = match.groupValues[3]
        val bold2 = match.groupValues[4]
        val italic1 = match.groupValues[5]
        val italic2 = match.groupValues[6]
        val code = match.groupValues[7]
        val strike = match.groupValues[8]

        when {
            boldItalic1.isNotEmpty() || boldItalic2.isNotEmpty() -> {
                val content = if (boldItalic1.isNotEmpty()) boldItalic1 else boldItalic2
                val style = if (textColor != Color.Unspecified) {
                    SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = textColor)
                } else {
                    SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                }
                builder.pushStyle(style)
                builder.append(content)
                builder.pop()
            }
            bold1.isNotEmpty() || bold2.isNotEmpty() -> {
                val content = if (bold1.isNotEmpty()) bold1 else bold2
                val style = if (textColor != Color.Unspecified) {
                    SpanStyle(fontWeight = FontWeight.Bold, color = textColor)
                } else {
                    SpanStyle(fontWeight = FontWeight.Bold)
                }
                builder.pushStyle(style)
                builder.append(content)
                builder.pop()
            }
            italic1.isNotEmpty() || italic2.isNotEmpty() -> {
                val content = if (italic1.isNotEmpty()) italic1 else italic2
                val style = if (textColor != Color.Unspecified) {
                    SpanStyle(fontStyle = FontStyle.Italic, color = textColor)
                } else {
                    SpanStyle(fontStyle = FontStyle.Italic)
                }
                builder.pushStyle(style)
                builder.append(content)
                builder.pop()
            }
            code.isNotEmpty() -> {
                builder.pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0x1F888888),
                        fontSize = 14.sp
                    )
                )
                builder.append(code)
                builder.pop()
            }
            strike.isNotEmpty() -> {
                builder.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                builder.append(strike)
                builder.pop()
            }
        }

        currentIndex = end
    }

    if (currentIndex < text.length) {
        val remaining = text.substring(currentIndex)
            .replace(Regex("""(?<!\w)\*\*|\*\*(?!\w)"""), "")
            .replace(Regex("""^#+\s*"""), "")
        builder.append(remaining)
    }
}
