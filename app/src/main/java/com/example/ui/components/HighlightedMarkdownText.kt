package com.example.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeumorphicColors

@Composable
fun HighlightedMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = NeumorphicColors.TextPrimary,
    fontSize: TextUnit = 16.sp
) {
    val annotatedString = parseMarkdownWithHighlights(markdown, defaultTextColor = color)
    Text(
        text = annotatedString,
        modifier = modifier,
        fontSize = fontSize,
        lineHeight = (fontSize.value * 1.4).sp
    )
}

fun parseMarkdownWithHighlights(
    input: String,
    defaultTextColor: Color = Color.Unspecified
): AnnotatedString {
    return buildAnnotatedString {
        var text = input

        // Process highlight syntax ==color:xxx==term== or ==term==
        // Color mapping
        val amberBg = Color(0xFFFFECB3)
        val greenBg = Color(0xFFC8E6C9)
        val blueBg = Color(0xFFBBDEFB)
        val redBg = Color(0xFFFFCDD2)

        val highlightRegex = Regex("==(?:color:(amber|green|blue|red)==)?(.*?)====", RegexOption.DOT_MATCHES_ALL)
        val highlightRegex2 = Regex("==(?:color:(amber|green|blue|red)==)?(.*?)==", RegexOption.DOT_MATCHES_ALL)

        val matches = highlightRegex2.findAll(input).toList()
        var lastIndex = 0

        if (matches.isEmpty()) {
            appendFormattedMarkdown(input, defaultTextColor)
            return@buildAnnotatedString
        }

        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1

            if (start > lastIndex) {
                appendFormattedMarkdown(input.substring(lastIndex, start), defaultTextColor)
            }

            val colorGroup = match.groupValues.getOrNull(1)?.lowercase()
            val term = match.groupValues.getOrNull(2) ?: ""

            val bgColor = when (colorGroup) {
                "green" -> greenBg
                "blue" -> blueBg
                "red" -> redBg
                else -> amberBg
            }

            pushStyle(
                SpanStyle(
                    background = bgColor,
                    color = Color(0xFF1A1A1A),
                    fontWeight = FontWeight.Bold
                )
            )
            appendFormattedMarkdown(term, Color(0xFF1A1A1A))
            pop()

            lastIndex = end
        }

        if (lastIndex < input.length) {
            appendFormattedMarkdown(input.substring(lastIndex), defaultTextColor)
        }
    }
}

private fun AnnotatedString.Builder.appendFormattedMarkdown(text: String, textColor: Color) {
    // Process **bold** and *italic* in a single pass. The alternation tries "**...**" first at
    // each position, so a genuine bold run is never mistaken for two italic runs; a lone
    // "*...*" only matches the second branch once the first has failed to match at that spot.
    // (Previous version only handled **bold** — a lone *italic* was left as literal asterisks,
    // which is the bug seen on scanned bullet-list cards.)
    val formattingRegex = Regex("\\*\\*(.+?)\\*\\*|\\*(.+?)\\*", RegexOption.DOT_MATCHES_ALL)
    var currentIndex = 0

    val matches = formattingRegex.findAll(text).toList()
    if (matches.isEmpty()) {
        append(text)
        return
    }

    for (match in matches) {
        val start = match.range.first
        val end = match.range.last + 1

        if (start > currentIndex) {
            append(text.substring(currentIndex, start))
        }

        val boldContent = match.groupValues[1]
        val italicContent = match.groupValues[2]

        if (boldContent.isNotEmpty()) {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor))
            append(boldContent)
            pop()
        } else {
            pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = textColor))
            append(italicContent)
            pop()
        }

        currentIndex = end
    }

    if (currentIndex < text.length) {
        append(text.substring(currentIndex))
    }
}
