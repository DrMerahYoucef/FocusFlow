package com.example.ui.revisions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp
import com.example.data.db.entity.RevisionNoteEntity
import com.example.data.repository.NoteBlock
import com.example.data.repository.NoteBlocksSerializer
import com.example.data.repository.NoteHighlight
import java.util.UUID

/**
 * Data model for an inline highlight span with start, end, and color.
 */
data class RichHighlight(
    val id: String = UUID.randomUUID().toString(),
    val start: Int,
    val end: Int,
    val color: String = "amber" // "amber", "green", "blue", "red", "purple", "pink", "cyan"
)

/**
 * Data model for bold or italic style ranges.
 */
data class RichStyleSpan(
    val start: Int,
    val end: Int
)

/**
 * Complete document state holding clean text and all rich formatting metadata.
 */
data class RichDocumentData(
    val cleanText: String,
    val lineHeadings: List<Int>, // 0 = Normal, 1 = H1, 2 = H2, 3 = H3, 4 = H4
    val highlights: List<RichHighlight>,
    val bolds: List<RichStyleSpan>,
    val italics: List<RichStyleSpan>
)

object RichTextEditorEngine {

    /**
     * Color palette for text highlights matching Word & Markdown palettes.
     */
    fun getHighlightBackground(colorName: String, isDark: Boolean): Color {
        return when (colorName.lowercase()) {
            "green", "emerald", "vert" -> if (isDark) Color(0xFF065F46).copy(alpha = 0.75f) else Color(0xFFBBF7D0).copy(alpha = 0.95f)
            "blue", "bleu", "sky", "cyan" -> if (isDark) Color(0xFF1E40AF).copy(alpha = 0.75f) else Color(0xFFBFDBFE).copy(alpha = 0.95f)
            "red", "rouge", "rose", "pink" -> if (isDark) Color(0xFF9F1239).copy(alpha = 0.75f) else Color(0xFFFECDD3).copy(alpha = 0.95f)
            "purple", "violet", "lavender" -> if (isDark) Color(0xFF581C87).copy(alpha = 0.75f) else Color(0xFFE9D5FF).copy(alpha = 0.95f)
            else -> if (isDark) Color(0xFF78350F).copy(alpha = 0.75f) else Color(0xFFFEF08A).copy(alpha = 0.95f) // Amber / Yellow
        }
    }

    fun getHighlightTextColor(colorName: String, isDark: Boolean): Color {
        if (isDark) return Color(0xFFFFFFFF)
        return when (colorName.lowercase()) {
            "green", "emerald", "vert" -> Color(0xFF064E3B)
            "blue", "bleu", "sky" -> Color(0xFF1E3A8A)
            "red", "rouge", "rose", "pink" -> Color(0xFF881337)
            "purple", "violet" -> Color(0xFF4C1D95)
            else -> Color(0xFF713F12) // Amber / Yellow
        }
    }

    fun getHeadingColor(level: Int, isDark: Boolean): Color {
        return when (level) {
            1 -> if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8) // Royal Blue
            2 -> if (isDark) Color(0xFF34D399) else Color(0xFF047857) // Mint Teal
            3 -> if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9) // Purple
            4 -> if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309) // Amber Orange
            else -> if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B) // Normal text
        }
    }

    /**
     * Parses an existing raw markdown string and/or NoteBlocks JSON into clean text
     * and visual formatting spans (headings, highlights, bold, italic).
     */
    fun parseFromNote(note: RevisionNoteEntity): RichDocumentData {
        val rawMarkdown = note.contentMarkdown.trim()
        val blocksJson = note.contentBlocksJson.trim()

        if (rawMarkdown.isNotBlank()) {
            return parseMarkdownToDocument(rawMarkdown)
        }

        // If markdown is empty, check structured blocks
        if (blocksJson.isNotBlank() && blocksJson != "[]") {
            try {
                val blocks = NoteBlocksSerializer.fromJson(blocksJson)
                val reconstructedMd = StringBuilder()
                blocks.forEachIndexed { i, block ->
                    if (i > 0) reconstructedMd.append("\n\n")
                    when (block) {
                        is NoteBlock.TextBlock -> {
                            var content = block.content
                            block.highlights.forEach { h ->
                                if (h.text.isNotBlank() && content.contains(h.text, ignoreCase = true)) {
                                    val colorTag = if (h.color.lowercase() == "amber") "" else "color:${h.color.lowercase()}=="
                                    val regex = Regex(Regex.escape(h.text), RegexOption.IGNORE_CASE)
                                    content = regex.replace(content) { m -> "==$colorTag${m.value}==" }
                                }
                            }
                            reconstructedMd.append(content)
                        }
                        is NoteBlock.TableBlock -> {
                            if (block.headers.isNotEmpty()) {
                                reconstructedMd.append("| ").append(block.headers.joinToString(" | ")).append(" |\n")
                                reconstructedMd.append("| ").append(block.headers.joinToString(" | ") { "---" }).append(" |\n")
                                block.rows.forEach { row ->
                                    reconstructedMd.append("| ").append(row.joinToString(" | ")).append(" |\n")
                                }
                            }
                        }
                    }
                }
                return parseMarkdownToDocument(reconstructedMd.toString().trim())
            } catch (_: Exception) {}
        }

        val plain = note.plainTextPreview.ifBlank { "Contenu de la carte..." }
        return RichDocumentData(
            cleanText = plain,
            lineHeadings = listOf(0),
            highlights = emptyList(),
            bolds = emptyList(),
            italics = emptyList()
        )
    }

    /**
     * Parses markdown text line by line, stripping symbols (#, ==, **)
     * while recording exact span boundaries on the clean text.
     */
    fun parseMarkdownToDocument(markdown: String): RichDocumentData {
        val lines = markdown.replace("\r\n", "\n").replace("\r", "\n").lines()
        val cleanLines = mutableListOf<String>()
        val lineHeadings = mutableListOf<Int>()
        val highlights = mutableListOf<RichHighlight>()
        val bolds = mutableListOf<RichStyleSpan>()
        val italics = mutableListOf<RichStyleSpan>()

        var currentCleanOffset = 0

        for (line in lines) {
            // Check heading level
            var headingLevel = 0
            var textWithoutHeading = line
            when {
                line.startsWith("#### ") -> {
                    headingLevel = 4
                    textWithoutHeading = line.removePrefix("#### ")
                }
                line.startsWith("### ") -> {
                    headingLevel = 3
                    textWithoutHeading = line.removePrefix("### ")
                }
                line.startsWith("## ") -> {
                    headingLevel = 2
                    textWithoutHeading = line.removePrefix("## ")
                }
                line.startsWith("# ") -> {
                    headingLevel = 1
                    textWithoutHeading = line.removePrefix("# ")
                }
            }
            lineHeadings.add(headingLevel)

            // Parse inline tokens: ==color:x==term==, ==term==, **bold**, *italic*
            val cleanLineBuilder = StringBuilder()
            var cursor = 0
            val lineLen = textWithoutHeading.length

            // Regex matching tokens in order
            val tokenRegex = Regex("""(==(?:color:([a-zA-Z]+)==)?(.*?)=*=|\*\*(.*?)\*\*|\*(.*?)\*|~~(.*?)~~|`([^`]+)`)""")
            val matches = tokenRegex.findAll(textWithoutHeading).toList()

            for (match in matches) {
                val matchStart = match.range.first
                val matchEnd = match.range.last + 1

                // Append any plain text before this match
                if (matchStart > cursor) {
                    val plainPrefix = textWithoutHeading.substring(cursor, matchStart)
                    cleanLineBuilder.append(plainPrefix)
                }

                val fullMatch = match.value
                when {
                    fullMatch.startsWith("==") -> {
                        // Highlight
                        val color = match.groupValues.getOrNull(2)?.lowercase()?.ifBlank { "amber" } ?: "amber"
                        val content = match.groupValues.getOrNull(3) ?: ""
                        val cleanTerm = content.replace(Regex("""[*_`#~]"""), "")
                        val startOffset = currentCleanOffset + cleanLineBuilder.length
                        cleanLineBuilder.append(cleanTerm)
                        val endOffset = currentCleanOffset + cleanLineBuilder.length
                        if (cleanTerm.isNotEmpty()) {
                            highlights.add(RichHighlight(start = startOffset, end = endOffset, color = color))
                        }
                    }
                    fullMatch.startsWith("**") -> {
                        // Bold
                        val content = match.groupValues.getOrNull(4) ?: ""
                        val startOffset = currentCleanOffset + cleanLineBuilder.length
                        cleanLineBuilder.append(content)
                        val endOffset = currentCleanOffset + cleanLineBuilder.length
                        if (content.isNotEmpty()) {
                            bolds.add(RichStyleSpan(start = startOffset, end = endOffset))
                        }
                    }
                    fullMatch.startsWith("*") -> {
                        // Italic
                        val content = match.groupValues.getOrNull(5) ?: ""
                        val startOffset = currentCleanOffset + cleanLineBuilder.length
                        cleanLineBuilder.append(content)
                        val endOffset = currentCleanOffset + cleanLineBuilder.length
                        if (content.isNotEmpty()) {
                            italics.add(RichStyleSpan(start = startOffset, end = endOffset))
                        }
                    }
                    else -> {
                        val content = match.groupValues.getOrNull(6) ?: match.groupValues.getOrNull(7) ?: fullMatch
                        cleanLineBuilder.append(content)
                    }
                }

                cursor = matchEnd
            }

            // Append any remaining text in line
            if (cursor < lineLen) {
                cleanLineBuilder.append(textWithoutHeading.substring(cursor))
            }

            val finalCleanLine = cleanLineBuilder.toString()
            cleanLines.add(finalCleanLine)
            currentCleanOffset += finalCleanLine.length + 1 // +1 for the newline
        }

        val totalCleanText = cleanLines.joinToString("\n")
        return RichDocumentData(
            cleanText = totalCleanText,
            lineHeadings = lineHeadings,
            highlights = highlights,
            bolds = bolds,
            italics = italics
        )
    }

    /**
     * Converts clean text + formatting spans back to valid Markdown syntax
     * with headings (#), highlights (==color:x==term==), and bold/italic.
     */
    fun serializeToMarkdown(
        cleanText: String,
        lineHeadings: List<Int>,
        highlights: List<RichHighlight>,
        bolds: List<RichStyleSpan>,
        italics: List<RichStyleSpan>
    ): String {
        val lines = cleanText.split('\n')
        val result = StringBuilder()
        var currentOffset = 0

        lines.forEachIndexed { lineIdx, lineText ->
            if (lineIdx > 0) result.append("\n")

            val headingLevel = lineHeadings.getOrNull(lineIdx) ?: 0
            val headingPrefix = when (headingLevel) {
                1 -> "# "
                2 -> "## "
                3 -> "### "
                4 -> "#### "
                else -> ""
            }
            result.append(headingPrefix)

            val lineStart = currentOffset
            val lineEnd = currentOffset + lineText.length

            // Find all highlights in this line
            val lineHighlights = highlights.filter { h ->
                h.start < lineEnd && h.end > lineStart
            }.map { h ->
                val localStart = (h.start - lineStart).coerceIn(0, lineText.length)
                val localEnd = (h.end - lineStart).coerceIn(0, lineText.length)
                RichHighlight(h.id, localStart, localEnd, h.color)
            }.filter { it.start < it.end }

            // Find all bolds in this line
            val lineBolds = bolds.filter { b ->
                b.start < lineEnd && b.end > lineStart
            }.map { b ->
                val localStart = (b.start - lineStart).coerceIn(0, lineText.length)
                val localEnd = (b.end - lineStart).coerceIn(0, lineText.length)
                RichStyleSpan(localStart, localEnd)
            }.filter { it.start < it.end }

            // Construct formatted line
            val formattedLine = buildFormattedLine(lineText, lineHighlights, lineBolds)
            result.append(formattedLine)

            currentOffset += lineText.length + 1 // +1 for newline
        }

        return result.toString().trim()
    }

    private fun buildFormattedLine(
        text: String,
        highlights: List<RichHighlight>,
        bolds: List<RichStyleSpan>
    ): String {
        if (highlights.isEmpty() && bolds.isEmpty()) return text

        // Collect all boundary points
        val boundaries = sortedSetOf<Int>()
        boundaries.add(0)
        boundaries.add(text.length)
        highlights.forEach {
            boundaries.add(it.start.coerceIn(0, text.length))
            boundaries.add(it.end.coerceIn(0, text.length))
        }
        bolds.forEach {
            boundaries.add(it.start.coerceIn(0, text.length))
            boundaries.add(it.end.coerceIn(0, text.length))
        }

        val points = boundaries.toList()
        val lineBuilder = StringBuilder()

        for (i in 0 until points.size - 1) {
            val segStart = points[i]
            val segEnd = points[i + 1]
            if (segStart >= segEnd) continue

            val segmentText = text.substring(segStart, segEnd)
            val activeHighlight = highlights.find { it.start <= segStart && it.end >= segEnd }
            val activeBold = bolds.find { it.start <= segStart && it.end >= segEnd }

            var formattedSegment = segmentText
            if (activeBold != null && formattedSegment.isNotBlank()) {
                formattedSegment = "**$formattedSegment**"
            }
            if (activeHighlight != null && formattedSegment.isNotBlank()) {
                val colorTag = if (activeHighlight.color.lowercase() == "amber") "" else "color:${activeHighlight.color.lowercase()}=="
                formattedSegment = "==$colorTag$formattedSegment=="
            }

            lineBuilder.append(formattedSegment)
        }

        return lineBuilder.toString()
    }

    /**
     * Builds an AnnotatedString with true WYSIWYG colors, font sizes, and backgrounds
     * for displaying inside the Compose TextField.
     */
    fun buildVisualAnnotatedString(
        text: String,
        lineHeadings: List<Int>,
        highlights: List<RichHighlight>,
        bolds: List<RichStyleSpan>,
        italics: List<RichStyleSpan>,
        isDark: Boolean
    ): AnnotatedString {
        val builder = AnnotatedString.Builder(text)
        val lines = text.split('\n')
        var offset = 0

        // 1. Line-level Heading Styles
        lines.forEachIndexed { idx, line ->
            val lineStart = offset
            val lineEnd = offset + line.length
            val headingLevel = lineHeadings.getOrNull(idx) ?: 0

            if (headingLevel in 1..4 && lineEnd > lineStart) {
                val headingColor = getHeadingColor(headingLevel, isDark)
                val fontSize = when (headingLevel) {
                    1 -> 20.sp
                    2 -> 18.sp
                    3 -> 16.sp
                    4 -> 15.sp
                    else -> 15.sp
                }
                builder.addStyle(
                    SpanStyle(
                        color = headingColor,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    lineStart,
                    lineEnd
                )
            }
            offset += line.length + 1
        }

        // 2. Bold Spans
        bolds.forEach { b ->
            val start = b.start.coerceIn(0, text.length)
            val end = b.end.coerceIn(0, text.length)
            if (start < end) {
                builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
            }
        }

        // 3. Italic Spans
        italics.forEach { itSpan ->
            val start = itSpan.start.coerceIn(0, text.length)
            val end = itSpan.end.coerceIn(0, text.length)
            if (start < end) {
                builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
            }
        }

        // 4. Character Highlight Backgrounds (Word Style)
        highlights.forEach { h ->
            val start = h.start.coerceIn(0, text.length)
            val end = h.end.coerceIn(0, text.length)
            if (start < end) {
                val bgColor = getHighlightBackground(h.color, isDark)
                val txtColor = getHighlightTextColor(h.color, isDark)
                builder.addStyle(
                    SpanStyle(
                        background = bgColor,
                        color = txtColor,
                        fontWeight = FontWeight.SemiBold
                    ),
                    start,
                    end
                )
            }
        }

        return builder.toAnnotatedString()
    }

    /**
     * Adjusts spans when the user types or deletes text in the editor.
     */
    fun adjustSpansOnTextChange(
        oldText: String,
        newText: String,
        currentHighlights: List<RichHighlight>,
        currentBolds: List<RichStyleSpan>,
        currentItalics: List<RichStyleSpan>,
        currentLineHeadings: List<Int>
    ): Pair<Triple<List<RichHighlight>, List<RichStyleSpan>, List<RichStyleSpan>>, List<Int>> {
        val oldLen = oldText.length
        val newLen = newText.length
        val delta = newLen - oldLen

        // Compute edit position
        var editStart = 0
        while (editStart < oldLen && editStart < newLen && oldText[editStart] == newText[editStart]) {
            editStart++
        }

        // Adjust highlights
        val newHighlights = currentHighlights.mapNotNull { h ->
            when {
                // Span is completely before the edit -> unchanged
                h.end <= editStart -> h
                // Span is completely after the edit -> shift by delta
                h.start >= editStart -> {
                    val newStart = (h.start + delta).coerceIn(0, newLen)
                    val newEnd = (h.end + delta).coerceIn(0, newLen)
                    if (newStart < newEnd) h.copy(start = newStart, end = newEnd) else null
                }
                // Edit occurs inside the span -> expand / contract span
                else -> {
                    val newEnd = (h.end + delta).coerceIn(h.start + 1, newLen)
                    if (h.start < newEnd) h.copy(end = newEnd) else null
                }
            }
        }

        // Adjust bolds
        val newBolds = currentBolds.mapNotNull { b ->
            when {
                b.end <= editStart -> b
                b.start >= editStart -> {
                    val newStart = (b.start + delta).coerceIn(0, newLen)
                    val newEnd = (b.end + delta).coerceIn(0, newLen)
                    if (newStart < newEnd) b.copy(start = newStart, end = newEnd) else null
                }
                else -> {
                    val newEnd = (b.end + delta).coerceIn(b.start + 1, newLen)
                    if (b.start < newEnd) b.copy(end = newEnd) else null
                }
            }
        }

        // Adjust italics
        val newItalics = currentItalics.mapNotNull { itSpan ->
            when {
                itSpan.end <= editStart -> itSpan
                itSpan.start >= editStart -> {
                    val newStart = (itSpan.start + delta).coerceIn(0, newLen)
                    val newEnd = (itSpan.end + delta).coerceIn(0, newLen)
                    if (newStart < newEnd) itSpan.copy(start = newStart, end = newEnd) else null
                }
                else -> {
                    val newEnd = (itSpan.end + delta).coerceIn(itSpan.start + 1, newLen)
                    if (itSpan.start < newEnd) itSpan.copy(end = newEnd) else null
                }
            }
        }

        // Adjust line headings count to match new line count
        val newLineCount = newText.split('\n').size
        val newLineHeadings = MutableList(newLineCount) { idx ->
            currentLineHeadings.getOrNull(idx) ?: 0
        }

        return Pair(Triple(newHighlights, newBolds, newItalics), newLineHeadings)
    }

    /**
     * Applies a highlight color to the selected text range (or word at cursor),
     * or removes it if color is null.
     */
    fun applyHighlightToRange(
        text: String,
        selection: TextRange,
        color: String?,
        currentHighlights: List<RichHighlight>
    ): Pair<List<RichHighlight>, TextRange> {
        var start = selection.min.coerceIn(0, text.length)
        var end = selection.max.coerceIn(0, text.length)

        // If no selection, expand to the current word under cursor
        if (start == end && text.isNotEmpty()) {
            val cursor = start
            var wordStart = cursor
            while (wordStart > 0 && !text[wordStart - 1].isWhitespace()) {
                wordStart--
            }
            var wordEnd = cursor
            while (wordEnd < text.length && !text[wordEnd].isWhitespace()) {
                wordEnd++
            }
            if (wordStart < wordEnd) {
                start = wordStart
                end = wordEnd
            }
        }

        if (start >= end) return Pair(currentHighlights, selection)

        // Filter out overlapping highlights in this range
        val filtered = currentHighlights.filterNot { h ->
            (h.start < end && h.end > start)
        }.toMutableList()

        if (color != null) {
            filtered.add(RichHighlight(start = start, end = end, color = color))
        }

        return Pair(filtered, TextRange(start, end))
    }

    /**
     * Toggles Heading level (0..4) on the line containing the selection.
     */
    fun toggleHeadingOnLine(
        text: String,
        selection: TextRange,
        level: Int,
        currentLineHeadings: List<Int>
    ): List<Int> {
        val lines = text.split('\n')
        val selStart = selection.min.coerceIn(0, text.length)

        // Find which line index contains selStart
        var offset = 0
        var targetLineIdx = 0
        for (i in lines.indices) {
            val nextOffset = offset + lines[i].length + 1
            if (selStart in offset..nextOffset || (i == lines.size - 1)) {
                targetLineIdx = i
                break
            }
            offset = nextOffset
        }

        val updated = currentLineHeadings.toMutableList()
        while (updated.size < lines.size) {
            updated.add(0)
        }

        val currentLevel = updated.getOrNull(targetLineIdx) ?: 0
        updated[targetLineIdx] = if (currentLevel == level) 0 else level

        return updated
    }

    /**
     * Toggles bold on the selected range.
     */
    fun toggleBoldOnRange(
        text: String,
        selection: TextRange,
        currentBolds: List<RichStyleSpan>
    ): List<RichStyleSpan> {
        val start = selection.min.coerceIn(0, text.length)
        val end = selection.max.coerceIn(0, text.length)
        if (start >= end) return currentBolds

        val isAlreadyBold = currentBolds.any { it.start <= start && it.end >= end }
        return if (isAlreadyBold) {
            currentBolds.filterNot { it.start <= start && it.end >= end }
        } else {
            currentBolds + RichStyleSpan(start, end)
        }
    }

    /**
     * Toggles italic on the selected range.
     */
    fun toggleItalicOnRange(
        text: String,
        selection: TextRange,
        currentItalics: List<RichStyleSpan>
    ): List<RichStyleSpan> {
        val start = selection.min.coerceIn(0, text.length)
        val end = selection.max.coerceIn(0, text.length)
        if (start >= end) return currentItalics

        val isAlreadyItalic = currentItalics.any { it.start <= start && it.end >= end }
        return if (isAlreadyItalic) {
            currentItalics.filterNot { it.start <= start && it.end >= end }
        } else {
            currentItalics + RichStyleSpan(start, end)
        }
    }
}

/**
 * VisualTransformation that renders rich styles directly onto clean text
 * with 1:1 identity offset mapping (no cursor jumping or desync!).
 */
class WordRichTextVisualTransformation(
    private val isDark: Boolean,
    private val lineHeadings: List<Int>,
    private val highlights: List<RichHighlight>,
    private val bolds: List<RichStyleSpan>,
    private val italics: List<RichStyleSpan>
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val styled = RichTextEditorEngine.buildVisualAnnotatedString(
            text = text.text,
            lineHeadings = lineHeadings,
            highlights = highlights,
            bolds = bolds,
            italics = italics,
            isDark = isDark
        )
        return TransformedText(styled, OffsetMapping.Identity)
    }
}
