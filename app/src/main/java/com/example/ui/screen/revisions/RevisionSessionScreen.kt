package com.example.ui.screen.revisions

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.entity.RevisionNoteEntity
import com.example.data.srs.ReviewGrade
import com.example.ui.components.NeumorphicCard
import com.example.ui.revisions.HighlightedMarkdownWithTables
import com.example.ui.revisions.NoteBlocksRenderer
import com.example.ui.theme.NeumorphicColors

@Composable
fun RevisionSessionScreen(
    deckId: String,
    viewModel: RevisionsViewModel,
    onFinishSession: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    val dueNotes = remember(state.dueNotes, deckId) {
        if (deckId.isBlank() || deckId == "default_deck") {
            state.dueNotes
        } else {
            state.dueNotes.filter { it.deckId == deckId }
        }
    }

    val totalInSession = remember(dueNotes.size) { dueNotes.size }
    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

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
                                text = "You've reviewed every card scheduled for this deck.",
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
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = "QUESTION",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp,
                                        color = NeumorphicColors.Primary
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = currentNote.title,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeumorphicColors.TextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(32.dp))
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
                                // Back Side (Rendered flipped back)
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { rotationY = 180f },
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = "ANSWER",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp,
                                        color = NeumorphicColors.Accent
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = currentNote.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeumorphicColors.TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = NeumorphicColors.TextSecondary.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (currentNote.contentBlocksJson.isBlank() || currentNote.contentBlocksJson == "[]") {
                                        HighlightedMarkdownWithTables(
                                            markdown = currentNote.plainTextPreview,
                                            fontSize = 16.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        NoteBlocksRenderer(
                                            blocksJson = currentNote.contentBlocksJson,
                                            fontSize = 16.sp,
                                            modifier = Modifier.weight(1f)
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
