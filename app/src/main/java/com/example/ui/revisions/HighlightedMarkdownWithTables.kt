package com.example.ui.revisions

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HighlightedMarkdownText

// --- Segment model -----------------------------------------------------------------------
// A card's contentMarkdown can now interleave plain markdown and GFM pipe tables (see
// GeminiOcrRepository's "table" blocks, which are rendered into standard "| a | b |" syntax).
// This splits that single string back into typed segments so each can be rendered with the
// component best suited for it, instead of asking the markdown library to understand tables.
sealed class ContentSegment {
    data class MarkdownText(val content: String) : ContentSegment()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : ContentSegment()
}

private val SEPARATOR_ROW_REGEX = Regex("""^\|?\s*:?-{1,}:?\s*(\|\s*:?-{1,}:?\s*)*\|?$""")

fun parseContentSegments(markdown: String): List<ContentSegment> {
    val lines = markdown.lines()
    val segments = mutableListOf<ContentSegment>()
    val textBuffer = StringBuilder()

    fun flushText() {
        if (textBuffer.isNotBlank()) {
            segments.add(ContentSegment.MarkdownText(textBuffer.toString().trim()))
        }
        textBuffer.clear()
    }

    fun splitRow(line: String): List<String> =
        line.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }

    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val looksLikeRow = line.contains("|") && line.trim().isNotEmpty()
        val nextLine = lines.getOrNull(i + 1)?.trim().orEmpty()

        if (looksLikeRow && SEPARATOR_ROW_REGEX.matches(nextLine)) {
            // Found a table: current line = header, next line = separator, following pipe
            // lines = data rows.
            flushText()
            val headers = splitRow(line)
            var j = i + 2
            val rows = mutableListOf<List<String>>()
            while (j < lines.size && lines[j].contains("|") && lines[j].trim().isNotEmpty()) {
                rows.add(splitRow(lines[j]))
                j++
            }
            segments.add(ContentSegment.Table(headers, rows))
            i = j
        } else {
            textBuffer.appendLine(line)
            i++
        }
    }
    flushText()
    return segments
}

// --- Native table composable --------------------------------------------------------------
@Composable
fun NativeTableRenderer(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp
) {
    val columnWidth = 140.dp
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .horizontalScroll(scrollState)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.small
            )
            .padding(4.dp)
    ) {
        Row {
            headers.forEach { header ->
                Text(
                    text = header,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    modifier = Modifier.width(columnWidth).padding(8.dp)
                )
            }
        }
        HorizontalDivider()
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (rowIndex % 2 == 0) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    )
            ) {
                // Pad short rows so a malformed row (fewer cells than headers) doesn't shift
                // the grid — this can happen if Gemini emits an uneven row by mistake.
                headers.indices.forEach { colIndex ->
                    Text(
                        text = row.getOrNull(colIndex).orEmpty(),
                        fontSize = fontSize,
                        modifier = Modifier.width(columnWidth).padding(8.dp)
                    )
                }
            }
            if (rowIndex != rows.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

// --- Drop-in replacement for rendering a note's full content -------------------------------
// Use this wherever the app currently calls HighlightedMarkdownText(note.contentMarkdown) —
// on the review session card and on the Note Detail reader screen. Plain text/highlight
// segments still go through the existing markdown renderer unchanged; only table segments get
// the native grid.
@Composable
fun HighlightedMarkdownWithTables(
    markdown: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp
) {
    val segments = remember(markdown) { parseContentSegments(markdown) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        segments.forEach { segment ->
            when (segment) {
                is ContentSegment.MarkdownText ->
                    if (segment.content.isNotBlank()) {
                        HighlightedMarkdownText(
                            markdown = segment.content,
                            fontSize = fontSize,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                is ContentSegment.Table ->
                    NativeTableRenderer(
                        headers = segment.headers,
                        rows = segment.rows,
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxWidth()
                    )
            }
        }
    }
}
