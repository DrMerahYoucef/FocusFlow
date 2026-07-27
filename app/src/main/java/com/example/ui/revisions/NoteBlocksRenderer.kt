package com.example.ui.revisions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.NoteBlock
import com.example.data.repository.NoteBlocksSerializer
import com.example.data.repository.NoteHighlight
import com.example.ui.components.appendFormattedMarkdown

// --- The new authoritative renderer -------------------------------------------------------
// Deserializes RevisionNoteEntity.contentBlocksJson and renders each block directly. There is
// no markdown string to reparse — a "table" block's headers/rows came straight from Gemini's
// JSON response and are handed to NativeTableRenderer as-is. This is what replaces
// HighlightedMarkdownWithTables for any note captured going forward; that older component (and
// its pipe-boundary parsing) only still matters for notes captured before this change, which
// only have contentMarkdown and no contentBlocksJson.
@Composable
fun NoteBlocksRenderer(
    blocksJson: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp
) {
    val blocks = remember(blocksJson) { NoteBlocksSerializer.fromJson(blocksJson) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        blocks.forEach { block ->
            when (block) {
                is NoteBlock.TextBlock ->
                    if (block.content.isNotBlank()) {
                        Text(
                            text = remember(block) { buildHighlightedAnnotatedText(block.content, block.highlights) },
                            fontSize = fontSize,
                            color = com.example.ui.theme.NeumorphicColors.TextPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                is NoteBlock.TableBlock ->
                    NativeTableRenderer(
                        headers = block.headers,
                        rows = block.rows,
                        fontSize = fontSize,
                        modifier = Modifier.fillMaxWidth()
                    )
            }
        }
    }
}

private data class HighlightRange(val start: Int, val end: Int, val color: String)

// Builds one AnnotatedString for a text block: highlight backgrounds are applied first (based
// on each highlight's exact position in this specific block's text — no string-replace markup
// involved), then bold/italic markdown is parsed within and around those ranges.
private fun buildHighlightedAnnotatedText(content: String, highlights: List<NoteHighlight>): AnnotatedString {
    return buildAnnotatedString {
        if (highlights.isEmpty()) {
            appendFormattedMarkdown(content, Color.Unspecified)
            return@buildAnnotatedString
        }

        val ranges = mutableListOf<HighlightRange>()
        highlights.forEach { h ->
            if (h.text.isBlank()) return@forEach
            val idx = content.indexOf(h.text)
            if (idx < 0) return@forEach
            val candidate = HighlightRange(idx, idx + h.text.length, h.color)
            // Skip a highlight that overlaps one already accepted — never let two highlight
            // spans collide on the same text.
            if (ranges.none { existing -> candidate.start < existing.end && candidate.end > existing.start }) {
                ranges.add(candidate)
            }
        }
        ranges.sortBy { it.start }

        var cursor = 0
        ranges.forEach { r ->
            if (r.start > cursor) appendFormattedMarkdown(content.substring(cursor, r.start), Color.Unspecified)

            val bgColor = when (r.color.lowercase()) {
                "green" -> Color(0xFFC8E6C9)
                "blue" -> Color(0xFFBBDEFB)
                "red" -> Color(0xFFFFCDD2)
                else -> Color(0xFFFFECB3)
            }
            pushStyle(SpanStyle(background = bgColor, color = Color(0xFF1A1A1A), fontWeight = FontWeight.Bold))
            appendFormattedMarkdown(content.substring(r.start, r.end), Color(0xFF1A1A1A))
            pop()

            cursor = r.end
        }
        if (cursor < content.length) appendFormattedMarkdown(content.substring(cursor), Color.Unspecified)
    }
}
