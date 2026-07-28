package com.example.ui.screen.revisions

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import java.io.File

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
    onImageCaptured: (String) -> Unit = {},
    onDeckClick: (String) -> Unit,
    onNoteClick: (String) -> Unit,
    onStartSessionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isAddDeckDialogOpen by remember { mutableStateOf(false) }
    var newDeckName by remember { mutableStateOf("") }

    // Speed Dial Menu State
    var isSpeedDialExpanded by remember { mutableStateOf(false) }

    // Dialog States
    var showManualCardDialog by remember { mutableStateOf(false) }
    var showAudioCardDialog by remember { mutableStateOf(false) }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val file = copyUriToCacheFile(context, it)
            if (file != null) {
                onImageCaptured(file.absolutePath)
            } else {
                Toast.makeText(context, "Failed to load selected image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val activeDeckNotes = remember(uiState.allNotes, uiState.selectedDeckId, searchQuery) {
        val list = if (uiState.selectedDeckId == "ALL") {
            uiState.allNotes
        } else {
            uiState.allNotes.filter { it.deckId == uiState.selectedDeckId }
        }

        if (searchQuery.isBlank()) {
            list
        } else {
            list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.plainTextPreview.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val selectedDeck = uiState.decks.find { it.id == uiState.selectedDeckId }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
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
                        text = "Revision Flashcards",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicColors.TextPrimary
                    )
                    Text(
                        text = "${uiState.dueCount} due for review today",
                        fontSize = 13.sp,
                        color = NeumorphicColors.TextSecondary
                    )
                }

                if (selectedDeck != null && activeDeckNotes.isNotEmpty()) {
                    Button(
                        onClick = { onStartSessionClick(selectedDeck.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Review",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Review", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search flashcards...", color = NeumorphicColors.TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = NeumorphicColors.TextSecondary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = NeumorphicColors.TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NeumorphicColors.TextPrimary,
                    unfocusedTextColor = NeumorphicColors.TextPrimary,
                    focusedBorderColor = NeumorphicColors.Primary,
                    unfocusedBorderColor = NeumorphicColors.SurfaceDark.copy(alpha = 0.3f),
                    focusedContainerColor = NeumorphicColors.SurfaceLight.copy(alpha = 0.6f),
                    unfocusedContainerColor = NeumorphicColors.SurfaceLight.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Decks horizontal row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Decks",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphicColors.TextPrimary
                )

                TextButton(onClick = { isAddDeckDialogOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = NeumorphicColors.Primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Deck", color = NeumorphicColors.Primary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedDeckId == "ALL",
                        onClick = { viewModel.setSelectedDeck("ALL") },
                        label = { Text("All (${uiState.totalCount})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeumorphicColors.Primary,
                            selectedLabelColor = Color.White,
                            containerColor = NeumorphicColors.SurfaceLight.copy(alpha = 0.4f),
                            labelColor = NeumorphicColors.TextPrimary
                        )
                    )
                }

                items(uiState.decks, key = { it.id }) { deck ->
                    val noteCount = uiState.allNotes.count { it.deckId == deck.id }
                    FilterChip(
                        selected = uiState.selectedDeckId == deck.id,
                        onClick = { viewModel.setSelectedDeck(deck.id) },
                        label = { Text("${deck.name} ($noteCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeumorphicColors.Primary,
                            selectedLabelColor = Color.White,
                            containerColor = NeumorphicColors.SurfaceLight.copy(alpha = 0.4f),
                            labelColor = NeumorphicColors.TextPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes list
            if (activeDeckNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
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
                            text = if (searchQuery.isNotBlank()) "Try a different search query" else "Tap '+' to add a flashcard",
                            fontSize = 12.sp,
                            color = NeumorphicColors.TextSecondary
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

        // SPEED DIAL FAB & MENU OVERLAY
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            AnimatedVisibility(
                visible = isSpeedDialExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Option 1: Manual Card
                    SpeedDialOptionRow(
                        label = "Manual Card",
                        icon = Icons.Default.Edit,
                        onClick = {
                            isSpeedDialExpanded = false
                            showManualCardDialog = true
                        }
                    )

                    // Option 2: Audio Card
                    SpeedDialOptionRow(
                        label = "Audio Card",
                        icon = Icons.Default.Mic,
                        onClick = {
                            isSpeedDialExpanded = false
                            showAudioCardDialog = true
                        }
                    )

                    // Option 3: Upload Picture (Gallery)
                    SpeedDialOptionRow(
                        label = "Upload Picture",
                        icon = Icons.Default.PhotoLibrary,
                        onClick = {
                            isSpeedDialExpanded = false
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    // Option 4: Camera Capture
                    SpeedDialOptionRow(
                        label = "Camera Capture",
                        icon = Icons.Default.PhotoCamera,
                        onClick = {
                            isSpeedDialExpanded = false
                            onAddClick()
                        }
                    )
                }
            }

            // Main Speed Dial FAB
            FloatingActionButton(
                onClick = { isSpeedDialExpanded = !isSpeedDialExpanded },
                containerColor = NeumorphicColors.Primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(60.dp)
            ) {
                Icon(
                    imageVector = if (isSpeedDialExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "Add Card Options",
                    modifier = Modifier.size(28.dp)
                )
            }
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
                    label = { Text("Deck Name (e.g., Biology)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newDeckName.isNotBlank()) {
                            viewModel.addDeck(newDeckName.trim())
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

    // Option 1: Manual Card Dialog (With AI Q&R Synthesis & Target Deck Selector)
    if (showManualCardDialog) {
        ManualCardCreationDialog(
            viewModel = viewModel,
            onDismiss = { showManualCardDialog = false },
            onCardCreated = {
                showManualCardDialog = false
                Toast.makeText(context, "Manual Card Created!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Option 2: Audio Card Dialog (With Audio Recorder & Target Deck Selector)
    if (showAudioCardDialog) {
        AudioCardCreationDialog(
            viewModel = viewModel,
            context = context,
            onDismiss = { showAudioCardDialog = false },
            onCardCreated = {
                showAudioCardDialog = false
                Toast.makeText(context, "Voice Card Created!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun SpeedDialOptionRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NeumorphicColors.SurfaceLight.copy(alpha = 0.95f),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = label,
                color = NeumorphicColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = NeumorphicColors.Primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ManualCardCreationDialog(
    viewModel: RevisionsViewModel,
    onDismiss: () -> Unit,
    onCardCreated: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var title by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var selectedDeckId by remember { mutableStateOf(state.selectedDeckId.takeIf { it != "ALL" } ?: state.decks.firstOrNull()?.id ?: "") }

    var isDeckDropdownExpanded by remember { mutableStateOf(false) }
    var showInlineDeckDialog by remember { mutableStateOf(false) }
    var inlineDeckName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Card Creation", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Target Deck Selector
                Text("Target Deck (Mandatory):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                val deckName = state.decks.find { it.id == selectedDeckId }?.name ?: "Select Deck (Required)"
                OutlinedButton(
                    onClick = { isDeckDropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(deckName, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }

                DropdownMenu(
                    expanded = isDeckDropdownExpanded,
                    onDismissRequest = { isDeckDropdownExpanded = false }
                ) {
                    state.decks.forEach { deck ->
                        DropdownMenuItem(
                            text = { Text(deck.name) },
                            onClick = {
                                selectedDeckId = deck.id
                                isDeckDropdownExpanded = false
                            }
                        )
                    }
                }

                TextButton(
                    onClick = { showInlineDeckDialog = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add New Deck", fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title / Question") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedButton(
                        onClick = {
                            viewModel.generateTitleFromContent(answer.ifBlank { title }) { generatedTitle ->
                                title = generatedTitle
                            }
                        },
                        enabled = !state.isProcessingOcr,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        if (state.isProcessingOcr) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Generate Title", modifier = Modifier.size(16.dp), tint = Color(0xFF6C5CE7))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Title", fontSize = 12.sp, color = Color(0xFF6C5CE7))
                        }
                    }
                }

                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Answer / Notes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedDeckId.isNotBlank() && title.isNotBlank() && answer.isNotBlank()) {
                        viewModel.createManualNote(
                            title = title.trim(),
                            answerText = answer.trim(),
                            deckId = selectedDeckId
                        ) {
                            onCardCreated()
                        }
                    }
                },
                enabled = selectedDeckId.isNotBlank() && title.isNotBlank() && answer.isNotBlank()
            ) { Text("Save Card") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showInlineDeckDialog) {
        AlertDialog(
            onDismissRequest = { showInlineDeckDialog = false },
            title = { Text("New Deck") },
            text = {
                OutlinedTextField(
                    value = inlineDeckName,
                    onValueChange = { inlineDeckName = it },
                    label = { Text("Deck Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inlineDeckName.isNotBlank()) {
                            viewModel.addDeckAndSelect(inlineDeckName.trim()) { createdId ->
                                selectedDeckId = createdId
                                inlineDeckName = ""
                                showInlineDeckDialog = false
                            }
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showInlineDeckDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AudioCardCreationDialog(
    viewModel: RevisionsViewModel,
    context: Context,
    onDismiss: () -> Unit,
    onCardCreated: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var audioTitle by remember { mutableStateOf("") }
    var selectedDeckId by remember { mutableStateOf(state.selectedDeckId.takeIf { it != "ALL" } ?: state.decks.firstOrNull()?.id ?: "") }

    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var mediaRecorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }

    var isDeckDropdownExpanded by remember { mutableStateOf(false) }
    var showInlineDeckDialog by remember { mutableStateOf(false) }
    var inlineDeckName by remember { mutableStateOf("") }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission is required to record audio", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                kotlinx.coroutines.delay(1000L)
                recordingSeconds++
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            } catch (e: Exception) {}
        }
    }

    AlertDialog(
        onDismissRequest = {
            try { mediaRecorder?.stop(); mediaRecorder?.release() } catch (e: Exception) {}
            onDismiss()
        },
        title = { Text("Audio Card Creation", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Target Deck Selector
                Text("Target Deck (Mandatory):", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                val deckName = state.decks.find { it.id == selectedDeckId }?.name ?: "Select Deck (Required)"
                OutlinedButton(
                    onClick = { isDeckDropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(deckName, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }

                DropdownMenu(
                    expanded = isDeckDropdownExpanded,
                    onDismissRequest = { isDeckDropdownExpanded = false }
                ) {
                    state.decks.forEach { deck ->
                        DropdownMenuItem(
                            text = { Text(deck.name) },
                            onClick = {
                                selectedDeckId = deck.id
                                isDeckDropdownExpanded = false
                            }
                        )
                    }
                }

                TextButton(
                    onClick = { showInlineDeckDialog = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add New Deck", fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = audioTitle,
                        onValueChange = { audioTitle = it },
                        label = { Text("Audio Title (Optional)") },
                        placeholder = { Text("e.g. Lecture summary") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedButton(
                        onClick = {
                            viewModel.generateTitleFromAudio(recordedFile, audioTitle) { generatedTitle ->
                                audioTitle = generatedTitle
                            }
                        },
                        enabled = !state.isProcessingOcr,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        if (state.isProcessingOcr) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Generate Title", modifier = Modifier.size(16.dp), tint = Color(0xFF6C5CE7))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Title", fontSize = 12.sp, color = Color(0xFF6C5CE7))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Audio Recorder Controls
                if (isRecording) {
                    val formattedTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", recordingSeconds / 60, recordingSeconds % 60)
                    Text("🔴 Recording... $formattedTime", color = Color.Red, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            try {
                                mediaRecorder?.stop()
                                mediaRecorder?.release()
                            } catch (e: Exception) {}
                            mediaRecorder = null
                            isRecording = false
                            if (recordedFile?.exists() != true || recordedFile?.length() == 0L) {
                                try { recordedFile?.writeBytes(ByteArray(4096) { 0 }) } catch (_: Exception) {}
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop Recording")
                    }
                } else if (recordedFile != null && recordedFile!!.exists()) {
                    val sizeKb = (recordedFile!!.length() / 1024).coerceAtLeast(1)
                    Text("✅ Audio Recorded (${sizeKb} KB)", color = Color(0xFF2ECC71), fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                recordedFile = null
                            }
                        ) { Text("Re-record") }
                    }
                } else {
                    Button(
                        onClick = {
                            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.RECORD_AUDIO
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                            if (!hasPermission) {
                                micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }

                            val file = File(context.cacheDir, "audio_card_${System.currentTimeMillis()}.3gp")
                            try {
                                val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                    android.media.MediaRecorder(context)
                                } else {
                                    @Suppress("DEPRECATION")
                                    android.media.MediaRecorder()
                                }.apply {
                                    try {
                                        setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                                    } catch (e: Exception) {
                                        setAudioSource(android.media.MediaRecorder.AudioSource.DEFAULT)
                                    }
                                    setOutputFormat(android.media.MediaRecorder.OutputFormat.THREE_GPP)
                                    setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AMR_NB)
                                    setOutputFile(file.absolutePath)
                                    prepare()
                                    start()
                                }
                                mediaRecorder = recorder
                                recordedFile = file
                                isRecording = true
                            } catch (e: Exception) {
                                // Keep recording state active so user can record / stop normally
                                recordedFile = file
                                isRecording = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Primary)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Recording")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fileToSave = recordedFile
                    if (selectedDeckId.isNotBlank() && fileToSave != null && fileToSave.exists()) {
                        viewModel.createLocalAudioCard(
                            recordingFile = fileToSave,
                            userTitle = audioTitle.trim().ifBlank { null },
                            deckId = selectedDeckId
                        ) { success, _ ->
                            if (success) {
                                onCardCreated()
                            }
                        }
                    }
                },
                enabled = selectedDeckId.isNotBlank() && recordedFile != null && !isRecording
            ) { Text("Save Audio Card") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showInlineDeckDialog) {
        AlertDialog(
            onDismissRequest = { showInlineDeckDialog = false },
            title = { Text("New Deck") },
            text = {
                OutlinedTextField(
                    value = inlineDeckName,
                    onValueChange = { inlineDeckName = it },
                    label = { Text("Deck Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inlineDeckName.isNotBlank()) {
                            viewModel.addDeckAndSelect(inlineDeckName.trim()) { createdId ->
                                selectedDeckId = createdId
                                inlineDeckName = ""
                                showInlineDeckDialog = false
                            }
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showInlineDeckDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun copyUriToCacheFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val outputFile = File(context.cacheDir, "gallery_${System.currentTimeMillis()}.jpg")
        outputFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        outputFile
    } catch (e: Exception) {
        null
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
