package com.example.ui.screen.revisions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.HighlightedMarkdownText
import com.example.ui.theme.NeumorphicColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionNoteDetailScreen(
    noteId: String,
    viewModel: RevisionsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val note = state.allNotes.find { it.id == noteId }

    var showDeleteCardDialog by remember { mutableStateOf(false) }

    if (showDeleteCardDialog && note != null) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_card_title),
            body = stringResource(R.string.delete_card_body),
            onConfirm = {
                viewModel.deleteNote(note)
                onBack()
            },
            onDismiss = { showDeleteCardDialog = false }
        )
    }

    if (note == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Card") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                )
            },
            containerColor = NeumorphicColors.Background
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = note.title,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteCardDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete_card_title),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NeumorphicColors.Background,
                    titleContentColor = NeumorphicColors.TextPrimary
                )
            )
        },
        containerColor = NeumorphicColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NeumorphicColors.SurfaceLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = note.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    HighlightedMarkdownText(
                        markdown = note.contentMarkdown,
                        fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
