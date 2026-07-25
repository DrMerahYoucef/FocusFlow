package com.example.ui.screen.revisions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.entity.RevisionDeckEntity
import com.example.data.db.entity.RevisionNoteEntity
import com.example.ui.components.HighlightedMarkdownText
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.NeumorphicColors

@Composable
fun RevisionsHomeScreen(
    viewModel: RevisionsViewModel,
    onAddClick: () -> Unit,
    onStartSessionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var isAddDeckDialogOpen by remember { mutableStateOf(false) }
    var newDeckName by remember { mutableStateOf("") }

    val activeDeck = state.decks.find { it.id == state.selectedDeckId } ?: state.decks.firstOrNull()
    val activeDeckNotes = state.allNotes.filter { it.deckId == (activeDeck?.id ?: "default_deck") }
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
                        text = "RÉVISIONS",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = NeumorphicColors.TextPrimary
                    )
                    Text(
                        text = "Répétition espacée SM-2",
                        fontSize = 12.sp,
                        color = NeumorphicColors.TextSecondary
                    )
                }

                IconButton(onClick = { isAddDeckDialogOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "Ajouter un paquet",
                        tint = NeumorphicColors.Primary
                    )
                }
            }

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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${activeDeckDueNotes.size} fiches à réviser",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (activeDeckDueNotes.isNotEmpty()) NeumorphicColors.Accent else NeumorphicColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sur un total de ${activeDeckNotes.size} fiches dans ${activeDeck?.name ?: "Paquet"}",
                            fontSize = 12.sp,
                            color = NeumorphicColors.TextSecondary
                        )
                    }

                    if (activeDeckDueNotes.isNotEmpty()) {
                        NeumorphicButton(
                            label = "Réviser",
                            icon = Icons.Default.PlayArrow,
                            onClick = { activeDeck?.let { onStartSessionClick(it.id) } },
                            accentColor = NeumorphicColors.Accent
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "À jour",
                            tint = NeumorphicColors.Success,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Deck selector tabs
            Text(
                text = "PAQUETS",
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
                    val deckNotesCount = state.allNotes.count { it.deckId == deck.id }
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
                            .clickable { viewModel.setSelectedDeck(deck.id) }
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
                    text = "FICHES DE CE PAQUET (${activeDeckNotes.size})",
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
                            text = "Aucune fiche dans ce paquet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = NeumorphicColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Appuyez sur le bouton '+' pour scanner un texte ou une photo",
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
                            onDelete = { viewModel.deleteNote(note) }
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
                contentDescription = "Scanner une fiche",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // Add Deck Dialog
    if (isAddDeckDialogOpen) {
        AlertDialog(
            onDismissRequest = { isAddDeckDialogOpen = false },
            title = { Text("Nouveau paquet de révision") },
            text = {
                OutlinedTextField(
                    value = newDeckName,
                    onValueChange = { newDeckName = it },
                    label = { Text("Nom du paquet (ex: Biologie)") },
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
                ) { Text("Créer") }
            },
            dismissButton = {
                TextButton(onClick = { isAddDeckDialogOpen = false }) { Text("Annuler") }
            }
        )
    }
}

@Composable
fun NoteListItem(
    note: RevisionNoteEntity,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NeumorphicColors.TextPrimary,
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Supprimer",
                            tint = NeumorphicColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = NeumorphicColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))

                HighlightedMarkdownText(
                    markdown = note.contentMarkdown,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Intervalle: ${note.intervalDays}j | Facilité: ${"%.2f".format(note.easeFactor)}",
                        fontSize = 11.sp,
                        color = NeumorphicColors.TextSecondary
                    )
                    Text(
                        text = if (note.dueDate <= System.currentTimeMillis()) "Due aujourd'hui" else "Prochaine révision programmée",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (note.dueDate <= System.currentTimeMillis()) NeumorphicColors.Accent else NeumorphicColors.Success
                    )
                }
            }
        }
    }
}
