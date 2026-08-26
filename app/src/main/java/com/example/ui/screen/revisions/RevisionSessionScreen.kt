package com.example.ui.screen.revisions

import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Fullscreen
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.db.entity.RevisionNoteEntity
import com.example.data.srs.ReviewGrade
import com.example.ui.components.NeumorphicCard
import com.example.ui.revisions.FullScreenNoteReaderDialog
import com.example.ui.revisions.HighlightedMarkdownWithTables
import com.example.ui.revisions.NoteBlocksRenderer
import com.example.ui.theme.NeumorphicColors
import java.io.File

@Composable
fun RevisionSessionScreen(
    deckId: String,
    viewModel: RevisionsViewModel,
    onFinishSession: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    val dueNotes = remember(state.dueNotes, deckId, state.allNotes) {
        val filtered = if (deckId.isBlank() || deckId == "default_deck" || deckId == "ALL") {
            state.dueNotes
        } else {
            state.dueNotes.filter { it.deckId == deckId }
        }
        if (filtered.isEmpty()) {
            if (deckId.isBlank() || deckId == "default_deck" || deckId == "ALL") {
                state.allNotes
            } else {
                state.allNotes.filter { it.deckId == deckId }
            }
        } else {
            filtered
        }
    }

    val totalInSession = remember(dueNotes.size) { dueNotes.size }
    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var zoomImageFile by remember { mutableStateOf<File?>(null) }
    var showFullScreenReader by remember { mutableStateOf(false) }
    var sessionTextScale by remember { mutableFloatStateOf(1.0f) }
    val context = LocalContext.current

    val currentNote = dueNotes.getOrNull(currentIndex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = NeumorphicColors.TextPrimary
                    )
                }

                if (totalInSession > 0 && currentNote != null) {
                    Text(
                        text = "Card ${currentIndex + 1} / $totalInSession",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NeumorphicColors.TextSecondary
                    )
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentNote == null || dueNotes.isEmpty()) {
                // Completed Session View
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    NeumorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        cornerRadius = 24.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NeumorphicColors.Success,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Session complete!",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = NeumorphicColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (deckId == "ALL") "You've reviewed every card scheduled across all decks." else "You've reviewed every card scheduled for this deck.",
                                fontSize = 14.sp,
                                color = NeumorphicColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onFinishSession,
                                colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Primary),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Back to decks", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            } else {
                // Active Card Session View
                val rotation by animateFloatAsState(
                    targetValue = if (isFlipped) 180f else 0f,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                    label = "cardFlip"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp)
                ) {
                    NeumorphicCard(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationY = rotation
                                cameraDistance = 12 * density
                            }
                            .clickable { isFlipped = !isFlipped },
                        cornerRadius = 24.dp,
                        elevation = 8.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (rotation <= 90f) {
                                // Front Side
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "QUESTION",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 2.sp,
                                            color = NeumorphicColors.Primary
                                        )

                                        IconButton(
                                            onClick = { showFullScreenReader = true },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("session_question_fullscreen_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fullscreen,
                                                contentDescription = stringResource(R.string.full_screen),
                                                tint = NeumorphicColors.Primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = currentNote.title,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeumorphicColors.TextPrimary,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(NeumorphicColors.SurfaceLight.copy(alpha = 0.3f))
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Flip,
                                            contentDescription = null,
                                            tint = NeumorphicColors.TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Tap to reveal the answer",
                                            fontSize = 12.sp,
                                            color = NeumorphicColors.TextSecondary
                                        )
                                    }
                                }
                            } else {
                                // Back Side (Rendered flipped back with 2-finger pinch zoom)
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(Unit) {
                                            detectTransformGestures { _, _, zoom, _ ->
                                                if (zoom != 1.0f) {
                                                    sessionTextScale = (sessionTextScale * zoom).coerceIn(0.7f, 3.5f)
                                                }
                                            }
                                        }
                                        .verticalScroll(rememberScrollState())
                                        .graphicsLayer { rotationY = 180f },
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ANSWER",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 2.sp,
                                            color = NeumorphicColors.Accent
                                        )

                                        IconButton(
                                            onClick = { showFullScreenReader = true },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("session_answer_fullscreen_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fullscreen,
                                                contentDescription = stringResource(R.string.full_screen),
                                                tint = NeumorphicColors.Primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = currentNote.title,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeumorphicColors.TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = NeumorphicColors.TextSecondary.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.height(16.dp))

                                    val mediaFile = remember(currentNote.mediaFilePath) {
                                        currentNote.mediaFilePath?.let { File(it) }
                                    }
                                    val isImage = (currentNote.mediaType == "IMAGE" ||
                                        (currentNote.mediaFilePath != null && (currentNote.mediaFilePath.endsWith(".jpg", true) || currentNote.mediaFilePath.endsWith(".png", true) || currentNote.mediaFilePath.endsWith(".jpeg", true)))) && mediaFile != null && mediaFile.exists()

                                    val isAudio = (currentNote.mediaType == "AUDIO" ||
                                        (currentNote.mediaFilePath != null && (currentNote.mediaFilePath.endsWith(".m4a", true) || currentNote.mediaFilePath.endsWith(".mp3", true) || currentNote.mediaFilePath.endsWith(".wav", true) || currentNote.mediaFilePath.endsWith(".3gp", true) || currentNote.mediaFilePath.endsWith(".aac", true)))) && mediaFile != null && mediaFile.exists()

                                    if (isImage) {
                                        val bitmap = remember(currentNote.mediaFilePath) {
                                            BitmapFactory.decodeFile(mediaFile!!.absolutePath)
                                        }
                                        if (bitmap != null) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(16.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(220.dp)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .clickable { zoomImageFile = mediaFile }
                                                ) {
                                                    Image(
                                                        bitmap = bitmap.asImageBitmap(),
                                                        contentDescription = "Card Photo",
                                                        contentScale = ContentScale.Fit,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "Tap photo to zoom",
                                                    fontSize = 11.sp,
                                                    color = NeumorphicColors.TextSecondary
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                            }
                                        }
                                    } else if (isAudio) {
                                        AudioCardPlayerView(
                                            audioFile = mediaFile!!,
                                            context = context,
                                            autoPlay = true
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    val hasBlocks = currentNote.contentBlocksJson.isNotBlank() && currentNote.contentBlocksJson != "[]"
                                    val plainText = currentNote.plainTextPreview.trim()
                                    val showPlainText = !hasBlocks && plainText.isNotBlank() &&
                                            !plainText.equals("Photo Card", ignoreCase = true) &&
                                            !plainText.equals("Voice Note Card", ignoreCase = true)

                                    val currentFontSize = (15 * sessionTextScale).sp

                                    if (hasBlocks) {
                                        NoteBlocksRenderer(
                                            blocksJson = currentNote.contentBlocksJson,
                                            fontSize = currentFontSize,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else if (showPlainText) {
                                        HighlightedMarkdownWithTables(
                                            markdown = plainText,
                                            fontSize = currentFontSize,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Answer Rating Buttons (Shown when card is flipped or user ready)
                if (isFlipped) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // AGAIN
                        RatingButton(
                            label = "Again",
                            color = Color(0xFFE53935),
                            onClick = {
                                viewModel.submitReviewGrade(currentNote, ReviewGrade.AGAIN)
                                isFlipped = false
                                if (currentIndex < dueNotes.size - 1) currentIndex++
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // HARD
                        RatingButton(
                            label = "Hard",
                            color = Color(0xFFFB8C00),
                            onClick = {
                                viewModel.submitReviewGrade(currentNote, ReviewGrade.HARD)
                                isFlipped = false
                                if (currentIndex < dueNotes.size - 1) currentIndex++
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // GOOD
                        RatingButton(
                            label = "Good",
                            color = Color(0xFF1E88E5),
                            onClick = {
                                viewModel.submitReviewGrade(currentNote, ReviewGrade.GOOD)
                                isFlipped = false
                                if (currentIndex < dueNotes.size - 1) currentIndex++
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // EASY
                        RatingButton(
                            label = "Easy",
                            color = Color(0xFF43A047),
                            onClick = {
                                viewModel.submitReviewGrade(currentNote, ReviewGrade.EASY)
                                isFlipped = false
                                if (currentIndex < dueNotes.size - 1) currentIndex++
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Button(
                        onClick = { isFlipped = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Primary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Show answer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        if (zoomImageFile != null && zoomImageFile!!.exists()) {
            val bitmap = remember(zoomImageFile) { BitmapFactory.decodeFile(zoomImageFile!!.absolutePath) }
            Dialog(onDismissRequest = { zoomImageFile = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.8f)
                        .background(Color.Black.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Zoomed Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        if (showFullScreenReader && currentNote != null) {
            val deckName = state.decks.find { it.id == currentNote.deckId }?.name ?: ""
            FullScreenNoteReaderDialog(
                note = currentNote,
                deckName = deckName,
                onDismiss = { showFullScreenReader = false }
            )
        }
    }
}

@Composable
fun RatingButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )
    }
}
