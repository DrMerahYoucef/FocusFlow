package com.example.ui.screen.revisions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.entity.RevisionDeckEntity
import com.example.data.db.entity.RevisionNoteEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.NeumorphicColors

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight

fun formatDueIn(dueDateMillis: Long): String {
    val diffMs = dueDateMillis - System.currentTimeMillis()
    if (diffMs <= 0) return "Due now"
    val diffSec = diffMs / 1000
    val diffMin = diffSec / 60
    val diffHours = diffMin / 60
    val diffDays = diffHours / 24
    val diffMonths = diffDays / 30

    return when {
        diffMonths >= 1 -> "Due in $diffMonths month${if (diffMonths > 1) "s" else ""}"
        diffDays >= 1 -> "Due in $diffDays day${if (diffDays > 1) "s" else ""}"
        diffHours >= 1 -> "Due in $diffHours hour${if (diffHours > 1) "s" else ""}"
        else -> "Due in $diffMin minute${if (diffMin > 1) "s" else ""}"
    }
}

@Composable
fun RevisionsHomeScreen(
    viewModel: RevisionsViewModel,
    onAddClick: () -> Unit,
    onDeckClick: (String) -> Unit,
    onNoteClick: (String) -> Unit,
    onStartSessionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var isAddDeckDialogOpen by remember { mutableStateOf(false) }
    var newDeckName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val activeDeck = state.decks.find { it.id == state.selectedDeckId } ?: state.decks.firstOrNull()
    val activeDeckNotesAll = state.allNotes.filter { it.deckId == (activeDeck?.id ?: "default_deck") }
    val activeDeckNotes = remember(activeDeckNotesAll, searchQuery) {
        if (searchQuery.isBlank()) activeDeckNotesAll
        else activeDeckNotesAll.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.plainTextPreview.contains(searchQuery, ignoreCase = true)
        }
    }
    val activeDeckDueNotes = state.dueNotes.filter { it.deckId == (activeDeck?.id ?: "default_deck") }

    val primaryColor = NeumorphicColors.Primary
    val surfaceColor = NeumorphicColors.SurfaceLight
    val surfaceDarkColor = NeumorphicColors.SurfaceDark

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NeumorphicColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.revisions_title).uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = NeumorphicColors.TextPrimary
                    )
                    Text(
                        text = "SM-2 Spaced Repetition",
                        fontSize = 12.sp,
                        color = NeumorphicColors.TextSecondary
                    )
                }

                IconButton(onClick = { isAddDeckDialogOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "New Deck",
                        tint = NeumorphicColors.Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar (Section 7)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search cards by title or text...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = NeumorphicColors.TextSecondary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = NeumorphicColors.TextSecondary)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NeumorphicColors.TextPrimary,
                    unfocusedTextColor = NeumorphicColors.TextPrimary,
                    focusedBorderColor = NeumorphicColors.Primary,
                    unfocusedBorderColor = NeumorphicColors.SurfaceDark.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Card (Due cards count)
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                elevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeDeck?.let { onDeckClick(it.id) } }
                    ) {
                        Text(
                            text = "${activeDeckDueNotes.size} cards due",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (activeDeckDueNotes.isNotEmpty()) NeumorphicColors.Accent else NeumorphicColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${activeDeckNotesAll.size} total cards in ${activeDeck?.name ?: "Deck"}",
                            fontSize = 12.sp,
                            color = NeumorphicColors.TextSecondary
                        )
                    }

                    if (activeDeckDueNotes.isNotEmpty()) {
                        NeumorphicButton(
                            label = stringResource(R.string.revisions_title),
                            icon = Icons.Default.PlayArrow,
                            onClick = { activeDeck?.let { onStartSessionClick(it.id) } },
                            accentColor = NeumorphicColors.Accent
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Up to date",
                            tint = NeumorphicColors.Success,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Deck selector tabs
            Text(
                text = "DECKS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = NeumorphicColors.TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(state.decks) { deck ->
                    val isSelected = deck.id == state.selectedDeckId
                    val deckDueCount = state.dueNotes.count { it.deckId == deck.id }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) primaryColor.copy(alpha = 0.15f) else surfaceColor,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) primaryColor else surfaceDarkColor.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.setSelectedDeck(deck.id)
                                onDeckClick(deck.id)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(deck.colorHex)))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = deck.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isSelected) NeumorphicColors.Primary else NeumorphicColors.TextPrimary
                            )
                            if (deckDueCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = NeumorphicColors.Accent,
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$deckDueCount",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Notes list header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DECK CARDS (${activeDeckNotes.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = NeumorphicColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (activeDeckNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            tint = NeumorphicColors.TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No cards found" else "No cards in this deck",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = NeumorphicColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try another search term" else "Tap '+' to scan or add a card",
                            fontSize = 12.sp,
                            color = NeumorphicColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(activeDeckNotes, key = { it.id }) { note ->
                        NoteListItem(
                            note = note,
                            onClick = { onNoteClick(note.id) }
                        )
                    }
                }
            }
        }

        // Floating Action Button (+)
        FloatingActionButton(
            onClick = onAddClick,
            containerColor = NeumorphicColors.Primary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(60.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Scan or add a card",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // Add Deck Dialog
    if (isAddDeckDialogOpen) {
        AlertDialog(
            onDismissRequest = { isAddDeckDialogOpen = false },
            title = { Text("New Deck") },
            text = {
                OutlinedTextField(
                    value = newDeckName,
                    onValueChange = { newDeckName = it },
                    label = { Text("Deck name (e.g., Biology)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newDeckName.isNotBlank()) {
                            viewModel.addDeck(newDeckName)
                            newDeckName = ""
                            isAddDeckDialogOpen = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { isAddDeckDialogOpen = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun NoteListItem(
    note: RevisionNoteEntity,
    onClick: () -> Unit
) {
    val dueText = formatDueIn(note.dueDate)
    val isDue = note.dueDate <= System.currentTimeMillis()

    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        elevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NeumorphicColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.mediaType != null) {
                        Icon(
                            imageVector = if (note.mediaType == "AUDIO") Icons.Default.Mic else Icons.Default.Image,
                            contentDescription = note.mediaType,
                            tint = NeumorphicColors.Primary,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = dueText,
                        fontSize = 12.sp,
                        fontWeight = if (isDue) FontWeight.Bold else FontWeight.Normal,
                        color = if (isDue) NeumorphicColors.Accent else NeumorphicColors.TextSecondary
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "View card",
                tint = NeumorphicColors.TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
