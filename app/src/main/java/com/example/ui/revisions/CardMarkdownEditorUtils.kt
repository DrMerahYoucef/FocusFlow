package com.example.ui.revisions

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.db.entity.RevisionNoteEntity
import com.example.data.repository.NoteBlock
import com.example.data.repository.NoteBlocksSerializer
import com.example.data.repository.NoteHighlight

object CardMarkdownEditorUtils {

    /**
     * Extracts or prepares editable markdown string from a RevisionNoteEntity.
     * Reconstructs full markdown syntax (including ==color:x==term== highlights and headings)
     * if the note was previously stored as structured blocks.
     */
    fun prepareMarkdownForEditing(note: RevisionNoteEntity): String {
        // If contentMarkdown is present and contains actual markdown structure or highlights, prefer it
        val rawMarkdown = note.contentMarkdown.trim()
        if (rawMarkdown.isNotBlank() && (rawMarkdown.contains("#") || rawMarkdown.contains("==") || rawMarkdown.contains("**") || rawMarkdown.contains("*") || rawMarkdown.contains("•"))) {
            return rawMarkdown
        }

        // If blocks are available, reconstruct cleanly
        if (note.contentBlocksJson.isNotBlank() && note.contentBlocksJson != "[]") {
            try {
                val blocks = NoteBlocksSerializer.fromJson(note.contentBlocksJson)
                val reconstructed = StringBuilder()
                blocks.forEachIndexed { idx, block ->
                    if (idx > 0) reconstructed.append("\n\n")
                    when (block) {
                        is NoteBlock.TextBlock -> {
                            var content = block.content
                            // If block has explicit highlights and content doesn't yet have == tags, wrap highlight terms
                            if (block.highlights.isNotEmpty() && !content.contains("==")) {
                                block.highlights.forEach { h ->
                                    if (h.text.isNotBlank() && content.contains(h.text, ignoreCase = true)) {
                                        val colorTag = if (h.color.lowercase() == "amber") "" else "color:${h.color.lowercase()}=="
                                        val regex = Regex(Regex.escape(h.text), RegexOption.IGNORE_CASE)
                                        content = regex.replace(content) { match ->
                                            "==$colorTag${match.value}=="
                                        }
                                    }
                                }
                            }
                            reconstructed.append(content)
                        }
                        is NoteBlock.TableBlock -> {
                            if (block.headers.isNotEmpty()) {
                                reconstructed.append("| ").append(block.headers.joinToString(" | ")).append(" |\n")
                                reconstructed.append("| ").append(block.headers.joinToString(" | ") { "---" }).append(" |\n")
                                block.rows.forEach { row ->
                                    reconstructed.append("| ").append(row.joinToString(" | ")).append(" |\n")
                                }
                            }
                        }
                    }
                }
                val result = reconstructed.toString().trim()
                if (result.isNotBlank()) return result
            } catch (e: Exception) {
                // Ignore fallback to plain text
            }
        }

        return note.plainTextPreview.ifBlank { note.contentMarkdown }
    }

    /**
     * Converts the edited markdown back into a fully synchronized RevisionNoteEntity,
     * maintaining markdown files, clean plain text previews, and high-fidelity structured NoteBlocks
     * with color highlights.
     */
    fun serializeEditedContentToNote(
        originalNote: RevisionNoteEntity,
        newTitle: String,
        editedMarkdown: String
    ): RevisionNoteEntity {
        val trimmedMarkdown = editedMarkdown.trim()
        val segments = parseContentSegments(trimmedMarkdown)
        
        // Extract all highlights from ==color:x==term== or ==term==
        val mdHighlightRegex = Regex("==(?:color:([a-zA-Z]+)==)?(.*?)==", RegexOption.DOT_MATCHES_ALL)
        val extractedHighlights = mutableListOf<NoteHighlight>()
        mdHighlightRegex.findAll(trimmedMarkdown).forEach { match ->
            val color = match.groupValues.getOrNull(1)?.lowercase()?.ifBlank { "amber" } ?: "amber"
            val text = match.groupValues.getOrNull(2)?.trim() ?: ""
            if (text.isNotBlank()) {
                val cleanTerm = text.replace(Regex("""[*_`#~]"""), "").trim()
                if (cleanTerm.isNotBlank()) {
                    extractedHighlights.add(NoteHighlight(cleanTerm, color))
                }
            }
        }

        val blocks = mutableListOf<NoteBlock>()
        segments.forEach { segment ->
            when (segment) {
                is ContentSegment.MarkdownText -> {
                    if (segment.content.isNotBlank()) {
                        blocks.add(
                            NoteBlock.TextBlock(
                                content = segment.content,
                                highlights = extractedHighlights
                            )
                        )
                    }
                }
                is ContentSegment.Table -> {
                    blocks.add(NoteBlock.TableBlock(headers = segment.headers, rows = segment.rows))
                }
            }
        }

        if (blocks.isEmpty() && trimmedMarkdown.isNotBlank()) {
            blocks.add(NoteBlock.TextBlock(content = trimmedMarkdown, highlights = extractedHighlights))
        }

        val blocksJson = NoteBlocksSerializer.toJson(blocks)
        val plainText = stripMarkdownAndHighlights(trimmedMarkdown)

        return originalNote.copy(
            title = newTitle.trim().ifBlank { originalNote.title },
            contentMarkdown = trimmedMarkdown,
            plainTextPreview = plainText,
            contentBlocksJson = blocksJson,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Strips headers, bold, italics, highlights, and formatting markers for clean plain text preview.
     */
    fun stripMarkdownAndHighlights(markdown: String): String {
        return markdown
            .replace(Regex("==(?:color:[a-zA-Z]+==)?(.*?)==", RegexOption.DOT_MATCHES_ALL)) { it.groupValues[1] }
            .replace(Regex("""^#{1,6}\s*""", RegexOption.MULTILINE), "")
            .replace(Regex("""\*\*(.*?)\*\*""", RegexOption.DOT_MATCHES_ALL)) { it.groupValues[1] }
            .replace(Regex("""\*(.*?)\*""", RegexOption.DOT_MATCHES_ALL)) { it.groupValues[1] }
            .replace(Regex("""~~(.*?)~~""", RegexOption.DOT_MATCHES_ALL)) { it.groupValues[1] }
            .replace(Regex("""`([^`]+)`""")) { it.groupValues[1] }
            .trim()
    }

    /**
     * Applies heading hierarchy (# H1, ## H2, ### H3, #### H4, or 0 for paragraph)
     * to the line(s) around the cursor or current selection.
     */
    fun applyHeading(current: TextFieldValue, level: Int): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val selStart = selection.min.coerceIn(0, text.length)
        val selEnd = selection.max.coerceIn(0, text.length)

        // Find line start and line end boundaries
        val lineStart = text.lastIndexOf('\n', (selStart - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', selEnd).let { if (it == -1) text.length else it }

        val targetSection = text.substring(lineStart, lineEnd)
        val lines = targetSection.split('\n')

        val prefix = when (level) {
            1 -> "# "
            2 -> "## "
            3 -> "### "
            4 -> "#### "
            else -> ""
        }

        val modifiedLines = lines.map { line ->
            // Strip any existing heading hashes or bullet symbols
            val cleaned = line
                .replace(Regex("""^(\s*)#{1,6}\s*"""), "$1")
                .replace(Regex("""^(\s*)[•\-\*]\s*"""), "$1")
                .replace(Regex("""^(\s*)\d+[\.\)]\s*"""), "$1")

            if (prefix.isNotEmpty()) {
                "$prefix$cleaned"
            } else {
                cleaned
            }
        }

        val newTargetSection = modifiedLines.joinToString("\n")
        val newText = text.substring(0, lineStart) + newTargetSection + text.substring(lineEnd)
        val newCursor = (lineStart + newTargetSection.length).coerceIn(0, newText.length)

        return TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
    }

    /**
     * Applies inline formatting (e.g. bold "**", italic "*", strikethrough "~~")
     * If text is selected, wraps it or toggles unwrapping.
     * If no text is selected, inserts tags and places cursor in the middle.
     */
    fun applyInlineFormatting(
        current: TextFieldValue,
        prefix: String,
        suffix: String = prefix
    ): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val selStart = selection.min.coerceIn(0, text.length)
        val selEnd = selection.max.coerceIn(0, text.length)

        if (selStart < selEnd) {
            val selected = text.substring(selStart, selEnd)
            // If already wrapped with prefix and suffix, unwrap
            if (selected.startsWith(prefix) && selected.endsWith(suffix) && selected.length >= prefix.length + suffix.length) {
                val unwrapped = selected.substring(prefix.length, selected.length - suffix.length)
                val newText = text.substring(0, selStart) + unwrapped + text.substring(selEnd)
                return TextFieldValue(
                    text = newText,
                    selection = TextRange(selStart, selStart + unwrapped.length)
                )
            } else {
                val wrapped = "$prefix$selected$suffix"
                val newText = text.substring(0, selStart) + wrapped + text.substring(selEnd)
                return TextFieldValue(
                    text = newText,
                    selection = TextRange(selStart, selStart + wrapped.length)
                )
            }
        } else {
            // No selection: insert tags at cursor
            val newText = text.substring(0, selStart) + prefix + suffix + text.substring(selStart)
            val newCursor = selStart + prefix.length
            return TextFieldValue(
                text = newText,
                selection = TextRange(newCursor)
            )
        }
    }

    /**
     * Applies or modifies highlight coloring (amber, green, blue, red, purple)
     * using the markdown syntax ==color:x==term== or ==term==.
     * If color is null, strips highlight formatting from selection.
     */
    fun applyHighlight(
        current: TextFieldValue,
        color: String? = "amber"
    ): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val selStart = selection.min.coerceIn(0, text.length)
        val selEnd = selection.max.coerceIn(0, text.length)

        if (selStart < selEnd) {
            var selected = text.substring(selStart, selEnd)
            // Strip any existing highlight wrapper
            selected = selected.replace(Regex("^==(?:color:[a-zA-Z]+==)?"), "").replace(Regex("==$"), "")

            if (color == null) {
                // Clear highlight
                val newText = text.substring(0, selStart) + selected + text.substring(selEnd)
                return TextFieldValue(
                    text = newText,
                    selection = TextRange(selStart, selStart + selected.length)
                )
            } else {
                val tag = if (color.lowercase() == "amber") "==" else "==color:${color.lowercase()}=="
                val wrapped = "$tag$selected=="
                val newText = text.substring(0, selStart) + wrapped + text.substring(selEnd)
                return TextFieldValue(
                    text = newText,
                    selection = TextRange(selStart, selStart + wrapped.length)
                )
            }
        } else {
            // No selection: find word boundaries around cursor
            var wordStart = selStart
            while (wordStart > 0 && !text[wordStart - 1].isWhitespace() && text[wordStart - 1] !in listOf('\n', '=', '*', '#', '|')) {
                wordStart--
            }
            var wordEnd = selStart
            while (wordEnd < text.length && !text[wordEnd].isWhitespace() && text[wordEnd] !in listOf('\n', '=', '*', '#', '|')) {
                wordEnd++
            }

            if (wordEnd > wordStart) {
                var selected = text.substring(wordStart, wordEnd)
                selected = selected.replace(Regex("^==(?:color:[a-zA-Z]+==)?"), "").replace(Regex("==$"), "")
                if (color == null) {
                    val newText = text.substring(0, wordStart) + selected + text.substring(wordEnd)
                    return TextFieldValue(text = newText, selection = TextRange(wordStart + selected.length))
                } else {
                    val tag = if (color.lowercase() == "amber") "==" else "==color:${color.lowercase()}=="
                    val wrapped = "$tag$selected=="
                    val newText = text.substring(0, wordStart) + wrapped + text.substring(wordEnd)
                    return TextFieldValue(text = newText, selection = TextRange(wordStart + wrapped.length))
                }
            } else {
                // Insert blank highlight tag template
                if (color != null) {
                    val tag = if (color.lowercase() == "amber") "==" else "==color:${color.lowercase()}=="
                    val template = "${tag}mot=="
                    val newText = text.substring(0, selStart) + template + text.substring(selStart)
                    return TextFieldValue(
                        text = newText,
                        selection = TextRange(selStart + tag.length, selStart + tag.length + 3)
                    )
                }
                return current
            }
        }
    }

    /**
     * Toggles bullet list item prefix (• ) for selected lines.
     */
    fun applyBulletList(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val selStart = selection.min.coerceIn(0, text.length)
        val selEnd = selection.max.coerceIn(0, text.length)

        val lineStart = text.lastIndexOf('\n', (selStart - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', selEnd).let { if (it == -1) text.length else it }

        val targetSection = text.substring(lineStart, lineEnd)
        val lines = targetSection.split('\n')

        val modifiedLines = lines.map { line ->
            if (line.trimStart().startsWith("• ") || line.trimStart().startsWith("- ")) {
                line.replaceFirst(Regex("""^(\s*)[•\-]\s*"""), "$1")
            } else {
                "• " + line.replace(Regex("""^(\s*)#{1,6}\s*"""), "$1").replace(Regex("""^(\s*)\d+[\.\)]\s*"""), "$1")
            }
        }

        val newTargetSection = modifiedLines.joinToString("\n")
        val newText = text.substring(0, lineStart) + newTargetSection + text.substring(lineEnd)
        val newCursor = (lineStart + newTargetSection.length).coerceIn(0, newText.length)

        return TextFieldValue(text = newText, selection = TextRange(newCursor))
    }

    /**
     * Toggles numbered list item prefix (1. ) for selected lines.
     */
    fun applyNumberedList(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val selStart = selection.min.coerceIn(0, text.length)
        val selEnd = selection.max.coerceIn(0, text.length)

        val lineStart = text.lastIndexOf('\n', (selStart - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', selEnd).let { if (it == -1) text.length else it }

        val targetSection = text.substring(lineStart, lineEnd)
        val lines = targetSection.split('\n')

        var counter = 1
        val modifiedLines = lines.map { line ->
            if (Regex("""^\s*\d+[\.\)]\s*""").containsMatchIn(line)) {
                line.replaceFirst(Regex("""^(\s*)\d+[\.\)]\s*"""), "$1")
            } else {
                val res = "${counter}. " + line.replace(Regex("""^(\s*)#{1,6}\s*"""), "$1").replace(Regex("""^(\s*)[•\-]\s*"""), "$1")
                counter++
                res
            }
        }

        val newTargetSection = modifiedLines.joinToString("\n")
        val newText = text.substring(0, lineStart) + newTargetSection + text.substring(lineEnd)
        val newCursor = (lineStart + newTargetSection.length).coerceIn(0, newText.length)

        return TextFieldValue(text = newText, selection = TextRange(newCursor))
    }
}
