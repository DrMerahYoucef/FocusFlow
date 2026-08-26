package com.example.ui.revisions

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.db.entity.RevisionNoteEntity
import com.example.ui.screen.revisions.AudioCardPlayerView
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.NeumorphicColors
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenNoteReaderDialog(
    note: RevisionNoteEntity,
    deckName: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isDark = LocalIsDarkTheme.current

    var textScale by remember { mutableFloatStateOf(1.0f) }
    var imageScale by remember { mutableFloatStateOf(1.0f) }
    var showZoomHint by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val scrollState = rememberScrollState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = NeumorphicColors.TextPrimary
                            )
                            if (deckName.isNotBlank()) {
                                Text(
                                    text = deckName.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeumorphicColors.Primary,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("fullscreen_exit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.exit_full_screen),
                                tint = NeumorphicColors.TextPrimary
                            )
                        }
                    },
                    actions = {
                        // Zoom Controls Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.45f else 0.7f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            // Zoom Out (-)
                            IconButton(
                                onClick = {
                                    textScale = (textScale - 0.15f).coerceIn(0.7f, 3.5f)
                                    imageScale = (imageScale - 0.2f).coerceIn(0.8f, 4.0f)
                                },
                                modifier = Modifier.size(32.dp).testTag("zoom_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = stringResource(R.string.zoom_out),
                                    modifier = Modifier.size(16.dp),
                                    tint = NeumorphicColors.TextPrimary
                                )
                            }

                            // Zoom Percentage badge / Reset button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (textScale != 1.0f) NeumorphicColors.Primary.copy(alpha = 0.2f) else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        textScale = 1.0f
                                        imageScale = 1.0f
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("zoom_percentage_badge")
                            ) {
                                Text(
                                    text = "${(textScale * 100).roundToInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (textScale != 1.0f) NeumorphicColors.Primary else NeumorphicColors.TextSecondary
                                )
                            }

                            // Zoom In (+)
                            IconButton(
                                onClick = {
                                    textScale = (textScale + 0.15f).coerceIn(0.7f, 3.5f)
                                    imageScale = (imageScale + 0.2f).coerceIn(0.8f, 4.0f)
                                },
                                modifier = Modifier.size(32.dp).testTag("zoom_in_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.zoom_in),
                                    modifier = Modifier.size(16.dp),
                                    tint = NeumorphicColors.TextPrimary
                                )
                            }
                        }

                        // Copy Text Action
                        IconButton(
                            onClick = {
                                val textToCopy = note.plainTextPreview.ifBlank { note.contentMarkdown }
                                clipboardManager.setText(AnnotatedString(textToCopy))
                                Toast.makeText(context, "Copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Text",
                                tint = NeumorphicColors.TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NeumorphicColors.Background,
                        titleContentColor = NeumorphicColors.TextPrimary,
                        navigationIconContentColor = NeumorphicColors.TextPrimary
                    )
                )
            },
            containerColor = NeumorphicColors.Background
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Two-finger pinch gesture detector on the whole content container
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                if (zoom != 1.0f) {
                                    textScale = (textScale * zoom).coerceIn(0.7f, 3.5f)
                                    imageScale = (imageScale * zoom).coerceIn(0.8f, 4.0f)
                                }
                            }
                        }
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .testTag("fullscreen_note_content_container")
                ) {
                    val imageFile = note.mediaFilePath?.let { File(it) }
                    val isImage = note.mediaType == "IMAGE" && imageFile != null && imageFile.exists()
                    val isAudio = note.mediaType == "AUDIO" && imageFile != null && imageFile.exists()

                    // IMAGE Card in Fullscreen
                    if (isImage) {
                        val bitmap = remember(note.mediaFilePath) {
                            BitmapFactory.decodeFile(imageFile!!.absolutePath)
                        }
                        if (bitmap != null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer(
                                            scaleX = imageScale,
                                            scaleY = imageScale
                                        )
                                ) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Card Photo Fullscreen",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 200.dp, max = 500.dp)
                                    )
                                }
                            }
                        }
                    }

                    // AUDIO Card in Fullscreen
                    if (isAudio) {
                        AudioCardPlayerView(
                            audioFile = imageFile!!,
                            context = context,
                            autoPlay = false
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Structured Text Content with Pinch Zoom Font Scaling
                    val baseFontSize = 17.sp
                    val scaledFontSize = (baseFontSize.value * textScale).sp

                    val hasBlocks = note.contentBlocksJson.isNotBlank() && note.contentBlocksJson != "[]"
                    val plainText = note.plainTextPreview.ifBlank { note.contentMarkdown }.trim()
                    val showPlainText = !hasBlocks && plainText.isNotBlank() &&
                            !plainText.equals("Photo Card", ignoreCase = true) &&
                            !plainText.equals("Voice Note Card", ignoreCase = true)

                    if (hasBlocks) {
                        NoteBlocksRenderer(
                            blocksJson = note.contentBlocksJson,
                            fontSize = scaledFontSize,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (showPlainText) {
                        HighlightedMarkdownWithTables(
                            markdown = plainText,
                            fontSize = scaledFontSize,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }

                // Floating Bottom Pinch-to-Zoom Helper Pill
                AnimatedVisibility(
                    visible = showZoomHint,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.90f else 0.95f),
                        shadowElevation = 6.dp,
                        tonalElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "🤏 Pinch to zoom • ${(textScale * 100).roundToInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = NeumorphicColors.TextPrimary
                            )
                            if (textScale != 1.0f || imageScale != 1.0f) {
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = {
                                        textScale = 1.0f
                                        imageScale = 1.0f
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = "Reset",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeumorphicColors.Primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
