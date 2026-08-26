package com.example.ui.revisions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.NoteBlock
import com.example.data.repository.NoteBlocksSerializer
import com.example.data.repository.NoteHighlight
import com.example.ui.components.parseMarkdownWithHighlights
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.NeumorphicColors
import com.example.ui.theme.RevisionHeadingColors

// --- Structured Note Line Classification ---------------------------------------------------
sealed class FormattedNoteLine {
    data class Heading(val level: Int, val text: String) : FormattedNoteLine()
    data class BulletList(val text: String, val indentLevel: Int = 0) : FormattedNoteLine()
    data class NumberedList(val numberStr: String, val text: String) : FormattedNoteLine()
    data class Blockquote(val text: String) : FormattedNoteLine()
    object Divider : FormattedNoteLine()
    data class Paragraph(val text: String) : FormattedNoteLine()
}

/**
 * Parses a markdown text block into structured lines and paragraphs.
 * Extracts headers (#, ##, ###, ####, #####, ######), bullet lists (- , * , • ),
 * numbered lists (1. , 2. ), blockquotes (> ), horizontal dividers, and paragraphs.
 */
fun parseNoteLines(content: String): List<FormattedNoteLine> {
    if (content.isBlank()) return emptyList()

    val lines = content.replace("\r\n", "\n").replace("\r", "\n").lines()
    val result = mutableListOf<FormattedNoteLine>()
    val paragraphBuffer = StringBuilder()

    fun flushParagraph() {
        val p = paragraphBuffer.toString().trim()
        if (p.isNotEmpty()) {
            result.add(FormattedNoteLine.Paragraph(p))
        }
        paragraphBuffer.clear()
    }

    // Matches "# Title", "## Subtitle", "### Topic", "#Title", etc.
    val headingRegex = Regex("""^(\s*#{1,6})\s*(.*)$""")
    val bulletRegex = Regex("""^(\s*)(?:[-*•+]|\(?[•*-]\)?)\s+(.*)$""")
    val numberedRegex = Regex("""^(\s*)(\d+[\.\)]|\(\d+\)|\d+-\s)\s*(.*)$""")
    val blockquoteRegex = Regex("""^>\s*(.*)$""")
    val hrRegex = Regex("""^(\s*[-*_]\s*){3,}$""")

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            flushParagraph()
            continue
        }

        // 1. Check for headings (#, ##, ###, ...)
        val headingMatch = headingRegex.matchEntire(trimmed)
        if (headingMatch != null) {
            flushParagraph()
            val hashes = headingMatch.groupValues[1].trim()
            val level = hashes.length.coerceIn(1, 6)
            var headingText = headingMatch.groupValues[2].trim()
            // Clean any trailing hashes or markdown wrap
            headingText = headingText.removeSuffix("#").trim()
            if (headingText.isNotEmpty()) {
                result.add(FormattedNoteLine.Heading(level, headingText))
                continue
            }
        }

        // 2. Check for Horizontal Rule
        if (hrRegex.matches(trimmed)) {
            flushParagraph()
            result.add(FormattedNoteLine.Divider)
            continue
        }

        // 3. Check for Blockquote
        val blockquoteMatch = blockquoteRegex.matchEntire(trimmed)
        if (blockquoteMatch != null) {
            flushParagraph()
            result.add(FormattedNoteLine.Blockquote(blockquoteMatch.groupValues[1].trim()))
            continue
        }

        // 4. Check for Bullet list
        val bulletMatch = bulletRegex.matchEntire(line)
        if (bulletMatch != null) {
            flushParagraph()
            val indent = bulletMatch.groupValues[1].length / 2
            val itemText = bulletMatch.groupValues[2].trim()
            result.add(FormattedNoteLine.BulletList(itemText, indent))
            continue
        }

        // 5. Check for Numbered list
        val numberedMatch = numberedRegex.matchEntire(line)
        if (numberedMatch != null) {
            flushParagraph()
            val numStr = numberedMatch.groupValues[2].trim()
            val itemText = numberedMatch.groupValues[3].trim()
            result.add(FormattedNoteLine.NumberedList(numStr, itemText))
            continue
        }

        // 6. Regular prose line: accumulate into current paragraph buffer
        if (paragraphBuffer.isNotEmpty()) {
            paragraphBuffer.append("\n")
        }
        paragraphBuffer.append(line)
    }
    flushParagraph()

    return result
}

// --- Authoritative Structured Note Renderer -----------------------------------------------
@Composable
fun NoteBlocksRenderer(
    blocksJson: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp
) {
    val blocks = remember(blocksJson) { NoteBlocksSerializer.fromJson(blocksJson) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is NoteBlock.TextBlock -> {
                    if (block.content.isNotBlank()) {
                        RichTextBlockRenderer(
                            content = block.content,
                            highlights = block.highlights,
                            fontSize = fontSize,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                is NoteBlock.TableBlock -> {
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
}

/**
 * Renders a text block with colored titles, subtitles, styled lists, and rich highlights.
 * Titles and subtitles are rendered without raw '#' characters, instead using distinctive
 * color palettes and background containers.
 */
@Composable
fun RichTextBlockRenderer(
    content: String,
    highlights: List<NoteHighlight> = emptyList(),
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp
) {
    val isDark = LocalIsDarkTheme.current
    val parsedLines = remember(content) { parseNoteLines(content) }
    val defaultTextColor = NeumorphicColors.TextPrimary

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        parsedLines.forEachIndexed { index, line ->
            when (line) {
                is FormattedNoteLine.Heading -> {
                    val headingAnnotated = remember(line.text, highlights, isDark) {
                        parseMarkdownWithHighlights(
                            input = line.text,
                            defaultTextColor = Color.Unspecified,
                            highlights = highlights,
                            isDark = isDark
                        )
                    }
                    when (line.level) {
                        1 -> NoteHeading1(
                            text = headingAnnotated,
                            fontSize = (fontSize.value * 1.30f).sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        2 -> NoteHeading2(
                            text = headingAnnotated,
                            fontSize = (fontSize.value * 1.16f).sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        3 -> NoteHeading3(
                            text = headingAnnotated,
                            fontSize = (fontSize.value * 1.06f).sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        4 -> NoteHeading4(
                            text = headingAnnotated,
                            fontSize = fontSize,
                            modifier = Modifier.fillMaxWidth()
                        )
                        else -> NoteHeadingSmall(
                            text = headingAnnotated,
                            level = line.level,
                            fontSize = (fontSize.value * 0.95f).sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                is FormattedNoteLine.BulletList -> {
                    val itemAnnotated = remember(line.text, highlights, isDark, defaultTextColor) {
                        parseMarkdownWithHighlights(
                            input = line.text,
                            defaultTextColor = defaultTextColor,
                            highlights = highlights,
                            isDark = isDark
                        )
                    }
                    NoteBulletItem(
                        text = itemAnnotated,
                        indentLevel = line.indentLevel,
                        fontSize = fontSize,
                        textColor = defaultTextColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is FormattedNoteLine.NumberedList -> {
                    val itemAnnotated = remember(line.text, highlights, isDark, defaultTextColor) {
                        parseMarkdownWithHighlights(
                            input = line.text,
                            defaultTextColor = defaultTextColor,
                            highlights = highlights,
                            isDark = isDark
                        )
                    }
                    NoteNumberedItem(
                        numberStr = line.numberStr,
                        text = itemAnnotated,
                        fontSize = fontSize,
                        textColor = defaultTextColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is FormattedNoteLine.Blockquote -> {
                    val quoteAnnotated = remember(line.text, highlights, isDark, defaultTextColor) {
                        parseMarkdownWithHighlights(
                            input = line.text,
                            defaultTextColor = defaultTextColor,
                            highlights = highlights,
                            isDark = isDark
                        )
                    }
                    NoteBlockquote(
                        text = quoteAnnotated,
                        fontSize = fontSize,
                        textColor = defaultTextColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is FormattedNoteLine.Divider -> {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                is FormattedNoteLine.Paragraph -> {
                    val paragraphAnnotated = remember(line.text, highlights, isDark, defaultTextColor) {
                        parseMarkdownWithHighlights(
                            input = line.text,
                            defaultTextColor = defaultTextColor,
                            highlights = highlights,
                            isDark = isDark
                        )
                    }
                    Text(
                        text = paragraphAnnotated,
                        fontSize = fontSize,
                        color = defaultTextColor,
                        lineHeight = (fontSize.value * 1.48).sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .testTag("note_paragraph_$index")
                    )
                }
            }
        }
    }
}

// --- Styled Heading Composables -----------------------------------------------------------

/**
 * Title (Heading 1 / #)
 * Prominent Royal Blue accent with left color bar and soft background pill container.
 */
@Composable
fun NoteHeading1(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 21.sp,
    color: Color = RevisionHeadingColors.getHeadingColor(1),
    bgColor: Color = RevisionHeadingColors.getHeadingBgColor(1)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 6.dp)
            .background(bgColor, shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .testTag("heading_level_1")
    ) {
        Box(
            modifier = Modifier
                .width(4.5.dp)
                .height(24.dp)
                .background(color, shape = RoundedCornerShape(2.5.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            lineHeight = (fontSize.value * 1.3).sp
        )
    }
}

/**
 * Subtitle 1 (Heading 2 / ##)
 * Mint Teal / Deep Emerald accent with left color bar and soft background pill container.
 */
@Composable
fun NoteHeading2(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 18.5.sp,
    color: Color = RevisionHeadingColors.getHeadingColor(2),
    bgColor: Color = RevisionHeadingColors.getHeadingBgColor(2)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 5.dp)
            .background(bgColor, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag("heading_level_2")
    ) {
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(20.dp)
                .background(color, shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = color,
            lineHeight = (fontSize.value * 1.3).sp
        )
    }
}

/**
 * Subtitle 2 (Heading 3 / ###)
 * Purple / Lavender accent with colored bullet indicator and clean styling.
 */
@Composable
fun NoteHeading3(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 17.sp,
    color: Color = RevisionHeadingColors.getHeadingColor(3),
    bgColor: Color = RevisionHeadingColors.getHeadingBgColor(3)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
            .background(bgColor, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag("heading_level_3")
    ) {
        Box(
            modifier = Modifier
                .size(7.5.dp)
                .background(color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = color,
            lineHeight = (fontSize.value * 1.35).sp
        )
    }
}

/**
 * Subtitle 3 (Heading 4 / ####)
 * Warm Amber / Gold accent with subtle bullet.
 */
@Composable
fun NoteHeading4(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    color: Color = RevisionHeadingColors.getHeadingColor(4)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 3.dp)
            .testTag("heading_level_4")
    ) {
        Box(
            modifier = Modifier
                .size(5.5.dp)
                .background(color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = color,
            lineHeight = (fontSize.value * 1.35).sp
        )
    }
}

@Composable
fun NoteHeadingSmall(
    text: AnnotatedString,
    level: Int,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 15.sp,
    color: Color = RevisionHeadingColors.getHeadingColor(level)
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold,
        color = color,
        lineHeight = (fontSize.value * 1.35).sp,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp)
            .testTag("heading_level_$level")
    )
}

// --- Styled List and Quote Composables ----------------------------------------------------

@Composable
fun NoteBulletItem(
    text: AnnotatedString,
    indentLevel: Int = 0,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 15.sp,
    textColor: Color = NeumorphicColors.TextPrimary,
    bulletColor: Color = RevisionHeadingColors.getHeadingColor(2)
) {
    val startPadding = (indentLevel * 12 + 2).dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startPadding, top = 2.dp, bottom = 2.dp)
            .testTag("note_bullet_item"),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.5.dp, end = 8.dp)
                .size(5.5.dp)
                .background(bulletColor, shape = CircleShape)
        )
        Text(
            text = text,
            fontSize = fontSize,
            color = textColor,
            lineHeight = (fontSize.value * 1.45).sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun NoteNumberedItem(
    numberStr: String,
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 15.sp,
    textColor: Color = NeumorphicColors.TextPrimary,
    numberColor: Color = RevisionHeadingColors.getHeadingColor(1)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 2.dp)
            .testTag("note_numbered_item"),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = numberStr,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            color = numberColor,
            modifier = Modifier
                .padding(end = 8.dp)
                .widthIn(min = 20.dp)
        )
        Text(
            text = text,
            fontSize = fontSize,
            color = textColor,
            lineHeight = (fontSize.value * 1.45).sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun NoteBlockquote(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 15.sp,
    textColor: Color = NeumorphicColors.TextPrimary,
    accentColor: Color = RevisionHeadingColors.getHeadingColor(3)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(accentColor.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("note_blockquote"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(28.dp)
                .background(accentColor, RoundedCornerShape(1.5.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = fontSize,
            fontStyle = FontStyle.Italic,
            color = textColor,
            lineHeight = (fontSize.value * 1.45).sp
        )
    }
}
