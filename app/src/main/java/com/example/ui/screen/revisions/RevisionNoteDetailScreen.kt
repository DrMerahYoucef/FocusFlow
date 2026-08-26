package com.example.ui.screen.revisions

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.R
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.revisions.FullScreenNoteReaderDialog
import com.example.ui.revisions.HighlightedMarkdownWithTables
import com.example.ui.revisions.NoteBlocksRenderer
import com.example.ui.theme.NeumorphicColors
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionNoteDetailScreen(
    noteId: String,
    viewModel: RevisionsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val state by viewModel.uiState.collectAsState()
    val note = state.allNotes.find { it.id == noteId }

    var showDeleteCardDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showMoveDeckDialog by remember { mutableStateOf(false) }
    var showZoomImageDialog by remember { mutableStateOf(false) }
    var showFullScreenReader by remember { mutableStateOf(false) }
    var cardTextScale by remember { mutableFloatStateOf(1.0f) }

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
                    IconButton(
                        onClick = { showFullScreenReader = true },
                        modifier = Modifier.testTag("detail_fullscreen_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = stringResource(R.string.full_screen),
                            tint = NeumorphicColors.Primary
                        )
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Card",
                            tint = NeumorphicColors.Primary
                        )
                    }
                    IconButton(onClick = { showMoveDeckDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = "Move to Deck",
                            tint = NeumorphicColors.Primary
                        )
                    }
                    IconButton(onClick = { showDeleteCardDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete_card_title),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = NeumorphicColors.TextPrimary,
                    navigationIconContentColor = NeumorphicColors.TextPrimary
                )
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            com.example.ui.components.NeumorphicCard(
                cornerRadius = 20.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val deckName = state.decks.find { it.id == note.deckId }?.name ?: "Deck"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = deckName.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = NeumorphicColors.Primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• " + formatDueIn(note.dueDate),
                                fontSize = 12.sp,
                                color = NeumorphicColors.TextSecondary
                            )
                        }

                        // Card Full Screen Button
                        FilledTonalIconButton(
                            onClick = { showFullScreenReader = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("card_fullscreen_button"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = NeumorphicColors.Primary.copy(alpha = 0.12f),
                                contentColor = NeumorphicColors.Primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = stringResource(R.string.full_screen),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = note.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // IMAGE Card View
                    val imageFile = note.mediaFilePath?.let { File(it) }
                    if (note.mediaType == "IMAGE" && imageFile != null && imageFile.exists()) {
                        val bitmap = remember(note.mediaFilePath) {
                            BitmapFactory.decodeFile(imageFile.absolutePath)
                        }

                        if (bitmap != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .clickable { showZoomImageDialog = true }
                                ) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Card Photo",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap photo to zoom",
                                    fontSize = 11.sp,
                                    color = NeumorphicColors.TextSecondary
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = { saveImageToGallery(context, imageFile) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = { shareMediaFile(context, imageFile, "image/jpeg") },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Send", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else if (note.mediaType == "AUDIO" && imageFile != null && imageFile.exists()) {
                        // AUDIO Card View
                        AudioCardPlayerView(
                            audioFile = imageFile,
                            context = context
                        )
                    } else {
                        // TEXT Card View with 2-finger pinch zoom
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, _, zoom, _ ->
                                        if (zoom != 1.0f) {
                                            cardTextScale = (cardTextScale * zoom).coerceIn(0.7f, 3.5f)
                                        }
                                    }
                                }
                        ) {
                            val currentFontSize = (16 * cardTextScale).sp
                            if (note.contentBlocksJson.isBlank() || note.contentBlocksJson == "[]") {
                                HighlightedMarkdownWithTables(
                                    markdown = note.plainTextPreview.ifBlank { note.contentMarkdown },
                                    fontSize = currentFontSize,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                NoteBlocksRenderer(
                                    blocksJson = note.contentBlocksJson,
                                    fontSize = currentFontSize,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (cardTextScale != 1.0f) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Zoom: ${(cardTextScale * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    color = NeumorphicColors.TextSecondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                TextButton(
                                    onClick = { cardTextScale = 1.0f },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("Reset", fontSize = 11.sp, color = NeumorphicColors.Primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                val textToCopy = note.plainTextPreview.ifBlank { note.contentMarkdown }
                                clipboardManager.setText(AnnotatedString(textToCopy))
                                Toast.makeText(context, "Text copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Text")
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Zoomable Image Dialog
    if (showZoomImageDialog && note?.mediaFilePath != null) {
        val file = File(note.mediaFilePath)
        val bitmap = remember(note.mediaFilePath) { BitmapFactory.decodeFile(file.absolutePath) }
        if (bitmap != null) {
            Dialog(
                onDismissRequest = { showZoomImageDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                var scale by remember { mutableStateOf(1f) }
                var offsetX by remember { mutableStateOf(0f) }
                var offsetY by remember { mutableStateOf(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    if (scale > 1f) {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Zoomable Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                        )
                    }

                    // Close Button in top corner
                    IconButton(
                        onClick = { showZoomImageDialog = false },
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(16.dp)
                            .align(Alignment.TopEnd)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    // Fullscreen Zoomable Note Reader Dialog
    if (showFullScreenReader && note != null) {
        val deckName = state.decks.find { it.id == note.deckId }?.name ?: ""
        FullScreenNoteReaderDialog(
            note = note,
            deckName = deckName,
            onDismiss = { showFullScreenReader = false }
        )
    }

    // Edit Card Dialog with Rich Text Highlighting & Underline
    if (showEditDialog && note != null) {
        EditCardDialog(
            note = note,
            onDismiss = { showEditDialog = false },
            onSave = { updatedTitle, updatedContent ->
                val updatedNote = note.copy(
                    title = updatedTitle,
                    plainTextPreview = updatedContent,
                    contentMarkdown = updatedContent,
                    contentBlocksJson = com.example.data.repository.NoteBlocksSerializer.toJson(
                        listOf(com.example.data.repository.NoteBlock.TextBlock(content = updatedContent))
                    )
                )
                viewModel.updateNote(updatedNote)
                showEditDialog = false
                Toast.makeText(context, "Card updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Move to Deck Dialog with Inline Deck Creation
    if (showMoveDeckDialog && note != null) {
        MoveToDeckDialog(
            currentDeckId = note.deckId,
            decks = state.decks,
            onDismiss = { showMoveDeckDialog = false },
            onDeckSelected = { selectedDeckId ->
                viewModel.moveNoteToDeck(note.id, selectedDeckId)
                showMoveDeckDialog = false
                Toast.makeText(context, "Moved card to new deck!", Toast.LENGTH_SHORT).show()
            },
            onCreateNewDeck = { newName ->
                viewModel.addDeckAndSelect(newName) { newId ->
                    viewModel.moveNoteToDeck(note.id, newId)
                    showMoveDeckDialog = false
                    Toast.makeText(context, "Created deck and moved card!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun AudioCardPlayerView(
    audioFile: File,
    context: Context,
    autoPlay: Boolean = false
) {
    var isPlaying by remember(audioFile) { mutableStateOf(autoPlay) }
    var currentPosition by remember(audioFile) { mutableStateOf(0) }
    var duration by remember(audioFile) { mutableStateOf(1) }

    val mediaPlayer = remember(audioFile) {
        MediaPlayer().apply {
            try {
                setDataSource(audioFile.absolutePath)
                prepare()
                duration = this.duration.coerceAtLeast(1)
                setOnCompletionListener {
                    isPlaying = false
                    currentPosition = 0
                }
                if (autoPlay) {
                    start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(audioFile) {
        onDispose {
            try {
                mediaPlayer.stop()
                mediaPlayer.release()
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying && mediaPlayer.isPlaying) {
            currentPosition = mediaPlayer.currentPosition
            kotlinx.coroutines.delay(200)
        }
        if (!mediaPlayer.isPlaying) {
            isPlaying = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NeumorphicColors.SurfaceLight.copy(alpha = 0.4f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = {
                    if (isPlaying) {
                        mediaPlayer.pause()
                        isPlaying = false
                    } else {
                        mediaPlayer.start()
                        isPlaying = true
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .background(NeumorphicColors.Primary, CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = {
                        currentPosition = it.toInt()
                        mediaPlayer.seekTo(it.toInt())
                    },
                    valueRange = 0f..duration.toFloat(),
                    colors = SliderDefaults.colors(thumbColor = NeumorphicColors.Primary, activeTrackColor = NeumorphicColors.Primary)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMillis(currentPosition.toLong()),
                        fontSize = 11.sp,
                        color = NeumorphicColors.TextSecondary
                    )
                    Text(
                        text = formatMillis(duration.toLong()),
                        fontSize = 11.sp,
                        color = NeumorphicColors.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { saveAudioToDownloads(context, audioFile) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save", fontSize = 12.sp)
            }

            Button(
                onClick = { shareMediaFile(context, audioFile, "audio/3gpp") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Send", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun EditCardDialog(
    note: com.example.data.db.entity.RevisionNoteEntity,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var contentText by remember { mutableStateOf(note.plainTextPreview.ifBlank { note.contentMarkdown }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeumorphicColors.DialogBackground,
        titleContentColor = NeumorphicColors.TextPrimary,
        textContentColor = NeumorphicColors.TextSecondary,
        title = { Text("Edit Card", fontWeight = FontWeight.Bold, color = NeumorphicColors.TextPrimary) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Question") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    label = { Text("Answer / Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title.trim(), contentText) }) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = NeumorphicColors.TextSecondary) }
        }
    )
}

@Composable
fun MoveToDeckDialog(
    currentDeckId: String,
    decks: List<com.example.data.db.entity.RevisionDeckEntity>,
    onDismiss: () -> Unit,
    onDeckSelected: (String) -> Unit,
    onCreateNewDeck: (String) -> Unit
) {
    var selectedId by remember { mutableStateOf(currentDeckId) }
    var isCreatingDeck by remember { mutableStateOf(false) }
    var newDeckName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeumorphicColors.DialogBackground,
        titleContentColor = NeumorphicColors.TextPrimary,
        textContentColor = NeumorphicColors.TextSecondary,
        title = { Text("Move Card to Deck", color = NeumorphicColors.TextPrimary) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!isCreatingDeck) {
                    decks.forEach { deck ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedId = deck.id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedId == deck.id,
                                onClick = { selectedId = deck.id },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = NeumorphicColors.Primary,
                                    unselectedColor = NeumorphicColors.TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(deck.name, fontWeight = FontWeight.Medium, color = NeumorphicColors.TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { isCreatingDeck = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add New Deck")
                    }
                } else {
                    OutlinedTextField(
                        value = newDeckName,
                        onValueChange = { newDeckName = it },
                        label = { Text("New Deck Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isCreatingDeck) {
                        if (newDeckName.isNotBlank()) {
                            onCreateNewDeck(newDeckName.trim())
                        }
                    } else {
                        onDeckSelected(selectedId)
                    }
                }
            ) {
                Text(if (isCreatingDeck) "Create & Move" else "Move Card")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (isCreatingDeck) isCreatingDeck = false else onDismiss()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

fun saveImageToGallery(context: Context, imageFile: File) {
    try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "card_photo_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/RevisionCards")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                imageFile.inputStream().copyTo(out)
            }
            Toast.makeText(context, "Saved to Gallery! 📷", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun saveAudioToDownloads(context: Context, audioFile: File) {
    try {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "card_audio_${System.currentTimeMillis()}.3gp")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/3gpp")
            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/RevisionCards")
        }
        val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                audioFile.inputStream().copyTo(out)
            }
            Toast.makeText(context, "Saved to Downloads! 🎵", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to save audio: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun shareMediaFile(context: Context, mediaFile: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            mediaFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Card Media"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun formatMillis(millis: Long): String {
    val totalSec = millis / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}
