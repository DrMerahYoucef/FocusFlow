package com.example.ui.screen.revisions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.entity.RevisionDeckEntity
import com.example.data.db.entity.RevisionNoteEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.theme.NeumorphicColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionDeckDetailScreen(
    deckId: String,
    viewModel: RevisionsViewModel,
    onNoteClick: (String) -> Unit,
    onStartReview: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val deck = state.decks.find { it.id == deckId }
    val deckNotes = state.allNotes.filter { it.deckId == deckId }

    var showDeleteDeckDialog by remember { mutableStateOf(false) }

    if (showDeleteDeckDialog && deck != null) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_deck_title),
            body = stringResource(R.string.delete_deck_body, deck.name, deckNotes.size),
            onConfirm = {
                viewModel.deleteDeckAndNotes(deckId)
                onBack()
            },
            onDismiss = { showDeleteDeckDialog = false }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = deck?.name ?: "Deck", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDeckDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete_deck_title),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = NeumorphicColors.TextPrimary,
                    navigationIconContentColor = NeumorphicColors.TextPrimary,
                    actionIconContentColor = NeumorphicColors.TextPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onStartReview(deckId) },
                icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                text = { Text(stringResource(R.string.start_review)) },
                containerColor = NeumorphicColors.Primary,
                contentColor = androidx.compose.ui.graphics.Color.White
            )
        },
        containerColor = NeumorphicColors.Background
    ) { padding ->
        if (deckNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No cards in this deck yet",
                    color = NeumorphicColors.TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(deckNotes, key = { it.id }) { note ->
                    DeckNoteListItem(
                        note = note,
                        onClick = { onNoteClick(note.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DeckNoteListItem(
    note: RevisionNoteEntity,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val isDueToday = note.dueDate <= System.currentTimeMillis()

    Card(
        colors = CardDefaults.cardColors(containerColor = NeumorphicColors.SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                Text(
                    text = if (isDueToday) stringResource(R.string.due_today) else stringResource(R.string.due_date, dateFormat.format(Date(note.dueDate))),
                    fontSize = 12.sp,
                    color = if (isDueToday) NeumorphicColors.Accent else NeumorphicColors.TextSecondary,
                    fontWeight = if (isDueToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
