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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HighlightedMarkdownText
import com.example.ui.components.parseMarkdownWithHighlights

// --- Segment model -----------------------------------------------------------------------
sealed class ContentSegment {
    data class MarkdownText(val content: String) : ContentSegment()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : ContentSegment()
}

private val SEPARATOR_ROW_REGEX = Regex("""^\|?\s*:?-{1,}:?\s*(\|\s*:?-{1,}:?\s*)*\|?$""")

// A real cell's own line-wraps never happen to land with a "|" immediately before AND after a
// newline — that specific pattern only occurs at a genuine row boundary. This is what lets us
// tell "flattened multi-row pipe text" apart from a normal paragraph that happens to contain a
// stray "|" character.
private val ROW_BOUNDARY_REGEX = Regex("""\|\s*\n\s*\|""")
private val INTERNAL_NEWLINE_REGEX = Regex("""\s*\n\s*""")

fun parseContentSegments(markdown: String): List<ContentSegment> {
    val lines = markdown.lines()
    val segments = mutableListOf<ContentSegment>()
    val textBuffer = StringBuilder()

    fun flushText() {
        val text = textBuffer.toString().trim()
        if (text.isNotBlank()) {
            // Fallback net: Gemini sometimes returns an entire table as one "text" block
            // containing raw "| a | b |" rows instead of using the "table" block type. Before
            // accepting this as plain prose, check whether it's actually a flattened table.
            val fallbackTable = tryParseFlatPipeTable(text)
            segments.add(fallbackTable ?: ContentSegment.MarkdownText(text))
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
            // Clean GFM table: header line, then a "---" separator, then data rows.
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

// --- Fallback: a table flattened into one blob of "|"-separated text with no separator row --
// Row boundaries are found via "|\n|" (a pipe immediately followed by a newline and another
// pipe); everything between two boundaries is one row, split further into cells on "|". The
// first reconstructed row becomes the header. Bails out (returns null) unless every row has the
// same number of cells — better to show slightly-ugly plain text than a misaligned grid.
private fun tryParseFlatPipeTable(text: String): ContentSegment.Table? {
    if (!text.contains("|")) return null
    val boundaryCount = ROW_BOUNDARY_REGEX.findAll(text).count()
    if (boundaryCount < 2) return null // need at least 3 rows' worth of boundaries to be confident

    val rowChunks = text.trim().split(ROW_BOUNDARY_REGEX)
    val rows = rowChunks.map { chunk ->
        chunk.trim()
            .removePrefix("|")
            .removeSuffix("|")
            .split("|")
            .map { cell -> cell.trim().replace(INTERNAL_NEWLINE_REGEX, " ").trim() }
    }

    val columnCount = rows.firstOrNull()?.size ?: return null
    if (columnCount < 2) return null
    if (rows.any { it.size != columnCount }) return null // inconsistent columns — don't guess

    return ContentSegment.Table(headers = rows.first(), rows = rows.drop(1))
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
    val textColor = com.example.ui.theme.NeumorphicColors.TextPrimary

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
                    text = remember(header, textColor) { parseMarkdownWithHighlights(header, defaultTextColor = textColor) },
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    color = textColor,
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
                headers.indices.forEach { colIndex ->
                    val cellText = row.getOrNull(colIndex).orEmpty()
                    Text(
                        text = remember(cellText, textColor) { parseMarkdownWithHighlights(cellText, defaultTextColor = textColor) },
                        fontSize = fontSize,
                        color = textColor,
                        modifier = Modifier.width(columnWidth).padding(8.dp)
                    )
                }
            }
            if (rowIndex != rows.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

// --- Drop-in replacement for rendering a note's full content -------------------------------
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
