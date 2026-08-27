package com.example.ui.revisions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.entity.RevisionNoteEntity
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.NeumorphicColors
import com.example.ui.theme.RevisionHeadingColors

/**
 * Microsoft Word-style True WYSIWYG rich card editor.
 * Renders headings, highlight colors, and text styles DIRECTLY on clean text
 * without exposing raw markdown markup (no ####, no ==color:blue==, no ==, no **).
 * When saved, it automatically synchronizes to standard markdown and high-fidelity blocks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardInlineEditor(
    note: RevisionNoteEntity,
    onSave: (RevisionNoteEntity) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    var title by remember { mutableStateOf(note.title) }

    // Parse note into clean text and rich formatting spans
    val initialDoc = remember(note.id) {
        RichTextEditorEngine.parseFromNote(note)
    }

    var contentTextFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialDoc.cleanText,
                selection = TextRange(initialDoc.cleanText.length)
            )
        )
    }
    var lineHeadings by remember { mutableStateOf(initialDoc.lineHeadings) }
    var highlights by remember { mutableStateOf(initialDoc.highlights) }
    var bolds by remember { mutableStateOf(initialDoc.bolds) }
    var italics by remember { mutableStateOf(initialDoc.italics) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Word WYSIWYG, 1 = Live Preview
    val editorScrollState = rememberScrollState()

    // Determine current line heading for active toolbar button indication
    val currentLineIndex = remember(contentTextFieldValue.text, contentTextFieldValue.selection) {
        val selStart = contentTextFieldValue.selection.min.coerceIn(0, contentTextFieldValue.text.length)
        val lines = contentTextFieldValue.text.split('\n')
        var offset = 0
        var foundIdx = 0
        for (i in lines.indices) {
            val nextOffset = offset + lines[i].length + 1
            if (selStart in offset..nextOffset || i == lines.size - 1) {
                foundIdx = i
                break
            }
            offset = nextOffset
        }
        foundIdx
    }
    val activeHeadingLevel = lineHeadings.getOrNull(currentLineIndex) ?: 0

    // Construct live note for instant preview tab
    val livePreviewMarkdown = remember(contentTextFieldValue.text, lineHeadings, highlights, bolds, italics) {
        RichTextEditorEngine.serializeToMarkdown(
            cleanText = contentTextFieldValue.text,
            lineHeadings = lineHeadings,
            highlights = highlights,
            bolds = bolds,
            italics = italics
        )
    }
    val livePreviewNote = remember(title, livePreviewMarkdown) {
        CardMarkdownEditorUtils.serializeEditedContentToNote(
            originalNote = note,
            newTitle = title,
            editedMarkdown = livePreviewMarkdown
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeumorphicColors.Background)
    ) {
        // --- Top Bar: Editor Mode Header & Mode Tabs ---
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.88f else 0.98f),
            tonalElevation = 2.dp,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = NeumorphicColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Modifier la carte",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary
                        )
                    }

                    // Mode Tabs (Édition vs Aperçu)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTab == 0) NeumorphicColors.Primary.copy(alpha = 0.18f) else Color.Transparent,
                            border = if (selectedTab == 0) androidx.compose.foundation.BorderStroke(1.dp, NeumorphicColors.Primary) else null,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = 0 }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = if (selectedTab == 0) NeumorphicColors.Primary else NeumorphicColors.TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Éditer",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 0) NeumorphicColors.Primary else NeumorphicColors.TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTab == 1) NeumorphicColors.Primary.copy(alpha = 0.18f) else Color.Transparent,
                            border = if (selectedTab == 1) androidx.compose.foundation.BorderStroke(1.dp, NeumorphicColors.Primary) else null,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = 1 }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = if (selectedTab == 1) NeumorphicColors.Primary else NeumorphicColors.TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Aperçu",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 1) NeumorphicColors.Primary else NeumorphicColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Word-Style Sticky Formatting Toolbar ---
        if (selectedTab == 0) {
            WordStyleFormattingRibbon(
                currentText = contentTextFieldValue.text,
                currentSelection = contentTextFieldValue.selection,
                activeHeadingLevel = activeHeadingLevel,
                onApplyHeading = { level ->
                    lineHeadings = RichTextEditorEngine.toggleHeadingOnLine(
                        text = contentTextFieldValue.text,
                        selection = contentTextFieldValue.selection,
                        level = level,
                        currentLineHeadings = lineHeadings
                    )
                },
                onApplyHighlight = { colorName ->
                    val (updatedHighlights, newSel) = RichTextEditorEngine.applyHighlightToRange(
                        text = contentTextFieldValue.text,
                        selection = contentTextFieldValue.selection,
                        color = colorName,
                        currentHighlights = highlights
                    )
                    highlights = updatedHighlights
                    contentTextFieldValue = contentTextFieldValue.copy(selection = newSel)
                },
                onToggleBold = {
                    bolds = RichTextEditorEngine.toggleBoldOnRange(
                        text = contentTextFieldValue.text,
                        selection = contentTextFieldValue.selection,
                        currentBolds = bolds
                    )
                },
                onToggleItalic = {
                    italics = RichTextEditorEngine.toggleItalicOnRange(
                        text = contentTextFieldValue.text,
                        selection = contentTextFieldValue.selection,
                        currentItalics = italics
                    )
                },
                onInsertBullet = {
                    val selStart = contentTextFieldValue.selection.min.coerceIn(0, contentTextFieldValue.text.length)
                    val text = contentTextFieldValue.text
                    val lineStart = text.lastIndexOf('\n', (selStart - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
                    val newText = text.substring(0, lineStart) + "• " + text.substring(lineStart)
                    contentTextFieldValue = TextFieldValue(newText, TextRange(selStart + 2))
                },
                onInsertNumbered = {
                    val selStart = contentTextFieldValue.selection.min.coerceIn(0, contentTextFieldValue.text.length)
                    val text = contentTextFieldValue.text
                    val lineStart = text.lastIndexOf('\n', (selStart - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
                    val newText = text.substring(0, lineStart) + "1. " + text.substring(lineStart)
                    contentTextFieldValue = TextFieldValue(newText, TextRange(selStart + 3))
                }
            )
        }

        // --- Main Editor or Live Preview Body ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (selectedTab == 0) {
                // TRUE WORD-STYLE WYSIWYG EDITOR
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(editorScrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Card Title Field (H1 Principal)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Titre de la carte") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Title,
                                contentDescription = null,
                                tint = NeumorphicColors.Primary
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_editor_title_input"),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary
                        )
                    )

                    // Content Rich Visual Text Area
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.5.dp,
                            color = NeumorphicColors.Primary.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 320.dp, max = 800.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            if (contentTextFieldValue.text.isEmpty()) {
                                Text(
                                    text = "Tapez votre texte ici...",
                                    color = NeumorphicColors.TextSecondary.copy(alpha = 0.6f),
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )
                            }

                            // Visual Transformation renders colored headings, highlight pills & bold/italic
                            // while maintaining 100% clean underlying text
                            BasicTextField(
                                value = contentTextFieldValue,
                                onValueChange = { newValue ->
                                    val oldText = contentTextFieldValue.text
                                    val newText = newValue.text

                                    if (oldText != newText) {
                                        // Adjust span offsets cleanly
                                        val (adjustedSpans, adjustedLineHeadings) = RichTextEditorEngine.adjustSpansOnTextChange(
                                            oldText = oldText,
                                            newText = newText,
                                            currentHighlights = highlights,
                                            currentBolds = bolds,
                                            currentItalics = italics,
                                            currentLineHeadings = lineHeadings
                                        )
                                        highlights = adjustedSpans.first
                                        bolds = adjustedSpans.second
                                        italics = adjustedSpans.third
                                        lineHeadings = adjustedLineHeadings
                                    }

                                    contentTextFieldValue = newValue
                                },
                                visualTransformation = WordRichTextVisualTransformation(
                                    isDark = isDark,
                                    lineHeadings = lineHeadings,
                                    highlights = highlights,
                                    bolds = bolds,
                                    italics = italics
                                ),
                                textStyle = TextStyle(
                                    color = NeumorphicColors.TextPrimary,
                                    fontSize = 15.sp,
                                    lineHeight = 23.sp
                                ),
                                cursorBrush = SolidColor(NeumorphicColors.Primary),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("card_editor_content_input")
                            )
                        }
                    }

                    // Bottom Document Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${contentTextFieldValue.text.length} caractères • ${contentTextFieldValue.text.split('\n').size} lignes • ${highlights.size} surlignages",
                            fontSize = 11.sp,
                            color = NeumorphicColors.TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                // LIVE CARD PREVIEW TAB
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = livePreviewNote.title.ifBlank { "Sans titre" },
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicColors.TextPrimary
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            Spacer(modifier = Modifier.height(14.dp))

                            if (livePreviewNote.contentBlocksJson.isNotBlank() && livePreviewNote.contentBlocksJson != "[]") {
                                NoteBlocksRenderer(
                                    blocksJson = livePreviewNote.contentBlocksJson,
                                    fontSize = 16.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                HighlightedMarkdownWithTables(
                                    markdown = livePreviewNote.contentMarkdown.ifBlank { "Aucun contenu" },
                                    fontSize = 16.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // --- Bottom Actions Bar (X Annuler & Check Confirmer) ---
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel Button (X / Annuler)
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("card_editor_cancel_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Annuler",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Annuler",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Confirm / Save Button (Check / Confirmer)
                Button(
                    onClick = {
                        val finalMarkdown = RichTextEditorEngine.serializeToMarkdown(
                            cleanText = contentTextFieldValue.text,
                            lineHeadings = lineHeadings,
                            highlights = highlights,
                            bolds = bolds,
                            italics = italics
                        )
                        val updated = CardMarkdownEditorUtils.serializeEditedContentToNote(
                            originalNote = note,
                            newTitle = title,
                            editedMarkdown = finalMarkdown
                        )
                        onSave(updated)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("card_editor_save_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeumorphicColors.Primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirmer",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Confirmer",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Word-Style Ribbon Toolbar:
 * 1. Headings Row: Titre 1, Sous-Titre, Sujet, Section, Texte
 * 2. Highlighting & Text Styles Row: Color Palette (Jaune, Vert, Bleu, Rose, Violet, Clear) + Bold, Italic, Bullets
 */
@Composable
fun WordStyleFormattingRibbon(
    currentText: String,
    currentSelection: TextRange,
    activeHeadingLevel: Int,
    onApplyHeading: (Int) -> Unit,
    onApplyHighlight: (String?) -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onInsertBullet: () -> Unit,
    onInsertNumbered: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val rowScrollState = rememberScrollState()

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.65f else 0.90f),
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Row 1: Headings & Hierarchy Styles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rowScrollState)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Heading 1 (#)
                WordHierarchyChip(
                    label = "# Titre 1",
                    level = 1,
                    isActive = activeHeadingLevel == 1,
                    onClick = { onApplyHeading(1) }
                )

                // Heading 2 (##)
                WordHierarchyChip(
                    label = "## Sous-Titre",
                    level = 2,
                    isActive = activeHeadingLevel == 2,
                    onClick = { onApplyHeading(2) }
                )

                // Heading 3 (###)
                WordHierarchyChip(
                    label = "### Sujet",
                    level = 3,
                    isActive = activeHeadingLevel == 3,
                    onClick = { onApplyHeading(3) }
                )

                // Heading 4 (####)
                WordHierarchyChip(
                    label = "#### Section",
                    level = 4,
                    isActive = activeHeadingLevel == 4,
                    onClick = { onApplyHeading(4) }
                )

                // Normal Paragraph
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (activeHeadingLevel == 0) NeumorphicColors.Primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    border = if (activeHeadingLevel == 0) androidx.compose.foundation.BorderStroke(1.dp, NeumorphicColors.Primary) else null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onApplyHeading(0) }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "¶ Normal",
                        fontSize = 12.sp,
                        fontWeight = if (activeHeadingLevel == 0) FontWeight.Bold else FontWeight.Medium,
                        color = NeumorphicColors.TextPrimary
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(22.dp)
                        .padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Bold (B)
                FormatIconButton(
                    icon = { Text("B", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) },
                    contentDescription = "Gras",
                    onClick = onToggleBold
                )

                // Italic (I)
                FormatIconButton(
                    icon = { Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    contentDescription = "Italique",
                    onClick = onToggleItalic
                )

                // Bullet List (•)
                FormatIconButton(
                    icon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    contentDescription = "Puces",
                    onClick = onInsertBullet
                )

                // Numbered List (1.)
                FormatIconButton(
                    icon = { Text("1.", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    contentDescription = "Numéroté",
                    onClick = onInsertNumbered
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Row 2: Microsoft Word-Style Highlight Palette
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BorderColor,
                        contentDescription = null,
                        tint = NeumorphicColors.Primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text("Surlignage :", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.TextPrimary)
                }

                // Jaune / Amber
                WordHighlightChip(
                    name = "Jaune",
                    color = Color(0xFFEAB308),
                    bgColor = if (isDark) Color(0xFF78350F).copy(alpha = 0.75f) else Color(0xFFFEF08A),
                    onClick = { onApplyHighlight("amber") }
                )

                // Vert / Green
                WordHighlightChip(
                    name = "Vert",
                    color = Color(0xFF10B981),
                    bgColor = if (isDark) Color(0xFF065F46).copy(alpha = 0.75f) else Color(0xFFBBF7D0),
                    onClick = { onApplyHighlight("green") }
                )

                // Bleu / Blue
                WordHighlightChip(
                    name = "Bleu",
                    color = Color(0xFF3B82F6),
                    bgColor = if (isDark) Color(0xFF1E40AF).copy(alpha = 0.75f) else Color(0xFFBFDBFE),
                    onClick = { onApplyHighlight("blue") }
                )

                // Rose / Rouge
                WordHighlightChip(
                    name = "Rose/Rouge",
                    color = Color(0xFFEF4444),
                    bgColor = if (isDark) Color(0xFF9F1239).copy(alpha = 0.75f) else Color(0xFFFECDD3),
                    onClick = { onApplyHighlight("red") }
                )

                // Violet / Purple
                WordHighlightChip(
                    name = "Violet",
                    color = Color(0xFF8B5CF6),
                    bgColor = if (isDark) Color(0xFF581C87).copy(alpha = 0.75f) else Color(0xFFE9D5FF),
                    onClick = { onApplyHighlight("purple") }
                )

                // Clear Highlight (🧽 Effacer)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onApplyHighlight(null) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatClear,
                            contentDescription = "Effacer le surlignage",
                            modifier = Modifier.size(13.dp),
                            tint = NeumorphicColors.TextSecondary
                        )
                        Text("Effacer", fontSize = 11.sp, color = NeumorphicColors.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun WordHierarchyChip(
    label: String,
    level: Int,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val barColor = RevisionHeadingColors.getHeadingColor(level, isDark)
    val bgColor = RevisionHeadingColors.getHeadingBgColor(level, isDark)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) barColor.copy(alpha = 0.25f) else bgColor,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) barColor else barColor.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(12.dp)
                    .background(barColor, RoundedCornerShape(1.dp))
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                color = barColor
            )
        }
    }
}

@Composable
private fun WordHighlightChip(
    name: String,
    color: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.7f)),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (LocalIsDarkTheme.current) Color.White else Color(0xFF0F172A)
            )
        }
    }
}

@Composable
private fun FormatIconButton(
    icon: @Composable () -> Unit,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
    }
}
