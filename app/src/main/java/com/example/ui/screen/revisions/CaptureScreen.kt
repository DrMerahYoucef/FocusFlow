package com.example.ui.screen.revisions

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.media.MediaRecorder
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.io.IOException

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalPermissionsApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    viewModel: RevisionsViewModel,
    onImageCaptured: (String) -> Unit,
    onCardCreated: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsState()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    var showManualCardDialog by remember { mutableStateOf(false) }
    var manualTitle by remember { mutableStateOf("") }
    var manualAnswer by remember { mutableStateOf("") }

    var showAudioDialog by remember { mutableStateOf(false) }
    val audioRecordingState by com.example.service.AudioRecordingService.recordingState.collectAsState()
    val isRecording = audioRecordingState.isRecording
    val recordingSeconds = audioRecordingState.elapsedSeconds
    val currentAudioFile = audioRecordingState.recordedFile
    var audioCardTitle by remember { mutableStateOf("") }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val cachedFile = copyUriToCacheFile(context, it)
            cachedFile?.let { file ->
                onImageCaptured(file.absolutePath)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (cameraPermissionState.status.isGranted) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("CaptureScreen", "Camera binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Permission caméra requise", color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text("Accorder l'accès")
                    }
                }
            }
        }

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = Color.White)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { showManualCardDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Saisie manuelle", tint = Color.White)
                }

                IconButton(
                    onClick = {
                        if (!audioPermissionState.status.isGranted) {
                            audioPermissionState.launchPermissionRequest()
                        } else {
                            showAudioDialog = true
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice Note Card", tint = Color.White)
                }

                IconButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Galerie", tint = Color.White)
                }
            }
        }

        if (!state.hasApiKey) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = 95.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(18.dp))
                    Text(
                        "Gemini API key not set. Captured photos will be created as local image cards.",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Bottom Capture Controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    val photoFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                onImageCaptured(photoFile.absolutePath)
                            }

                            override fun onError(exc: ImageCaptureException) {
                                Toast.makeText(context, "Erreur de capture: ${exc.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                },
                containerColor = Color.White,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.Camera, contentDescription = "Capturer", modifier = Modifier.size(36.dp))
            }
        }

        // Manual Card Dialog (Section 10)
        if (showManualCardDialog) {
            Dialog(
                onDismissRequest = { showManualCardDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 580.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Manual Card Creation",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { showManualCardDialog = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }

                            // Form Content
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = manualTitle,
                                        onValueChange = { manualTitle = it },
                                        label = { Text("Card Title / Question") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            if (state.hasApiKey) {
                                                viewModel.generateTitleFromContent(manualAnswer.ifBlank { manualTitle }) { generatedTitle ->
                                                    manualTitle = generatedTitle
                                                }
                                            } else {
                                                Toast.makeText(context, "Gemini API key is required for AI Title generation.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = state.hasApiKey && !state.isProcessingOcr,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                                    ) {
                                        if (state.isProcessingOcr) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = "Generate Title", modifier = Modifier.size(16.dp), tint = if (state.hasApiKey) Color(0xFF6C5CE7) else Color.Gray)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (state.hasApiKey) "AI Title" else "AI Title (Key Required)", fontSize = 12.sp, color = if (state.hasApiKey) Color(0xFF6C5CE7) else Color.Gray)
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = manualAnswer,
                                    onValueChange = { manualAnswer = it },
                                    label = { Text("Answer / Notes / Markdown") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            }

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showManualCardDialog = false },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Cancel") }

                                Button(
                                    onClick = {
                                        if (manualTitle.isNotBlank() && manualAnswer.isNotBlank()) {
                                            viewModel.createManualNote(
                                                title = manualTitle.trim(),
                                                answerText = manualAnswer.trim(),
                                                deckId = viewModel.uiState.value.selectedDeckId
                                            ) {
                                                showManualCardDialog = false
                                                Toast.makeText(context, "Manual Card Created!", Toast.LENGTH_SHORT).show()
                                                onCardCreated()
                                            }
                                        }
                                    },
                                    enabled = manualTitle.isNotBlank() && manualAnswer.isNotBlank(),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Save Card") }
                            }
                        }
                    }
                }
            }
        }

        // Voice Note Recording Dialog (Section 9)
        if (showAudioDialog) {
            Dialog(
                onDismissRequest = {
                    if (isRecording) {
                        com.example.service.AudioRecordingService.stopRecording(context)
                    }
                    showAudioDialog = false
                },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 560.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Voice Note Card",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = {
                                    if (isRecording) {
                                        com.example.service.AudioRecordingService.stopRecording(context)
                                    }
                                    showAudioDialog = false
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }

                            // Content
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = audioCardTitle,
                                        onValueChange = { audioCardTitle = it },
                                        label = { Text("Audio Title (Optional)") },
                                        placeholder = { Text("e.g. Lecture summary") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            if (state.hasApiKey) {
                                                viewModel.generateTitleFromAudio(currentAudioFile, audioCardTitle) { generatedTitle ->
                                                    audioCardTitle = generatedTitle
                                                }
                                            } else {
                                                Toast.makeText(context, "Gemini API key is required for AI Title generation.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = state.hasApiKey && !state.isProcessingOcr,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                                    ) {
                                        if (state.isProcessingOcr) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = "Generate Title", modifier = Modifier.size(16.dp), tint = if (state.hasApiKey) Color(0xFF6C5CE7) else Color.Gray)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (state.hasApiKey) "AI Title" else "AI Title (Key Required)", fontSize = 12.sp, color = if (state.hasApiKey) Color(0xFF6C5CE7) else Color.Gray)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (isRecording) {
                                    val formattedTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", recordingSeconds / 60, recordingSeconds % 60)
                                    Text("Recording audio... $formattedTime", color = Color.Red, style = MaterialTheme.typography.titleMedium)
                                    Text("Recording stays active in background", fontSize = 11.sp, color = Color.Gray)
                                    IconButton(
                                        onClick = {
                                            com.example.service.AudioRecordingService.stopRecording(context)
                                        },
                                        modifier = Modifier
                                            .size(72.dp)
                                            .background(Color.Red, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                                    }
                                } else {
                                    Text(
                                        if (currentAudioFile != null && currentAudioFile.exists()) "Recording complete! (${(currentAudioFile.length() / 1024).coerceAtLeast(1)} KB)" else "Tap to record background audio",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    IconButton(
                                        onClick = {
                                            if (!audioPermissionState.status.isGranted) {
                                                audioPermissionState.launchPermissionRequest()
                                            } else {
                                                com.example.service.AudioRecordingService.startRecording(context)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(72.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = "Record", tint = Color.White)
                                    }
                                }
                            }

                            // Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (isRecording) {
                                            com.example.service.AudioRecordingService.stopRecording(context)
                                        }
                                        showAudioDialog = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Cancel") }

                                Button(
                                    onClick = {
                                        currentAudioFile?.let { audioFile ->
                                            viewModel.createLocalAudioCard(
                                                recordingFile = audioFile,
                                                userTitle = audioCardTitle.trim().ifBlank { null },
                                                deckId = viewModel.uiState.value.selectedDeckId
                                            ) { success, err ->
                                                if (success) {
                                                    com.example.service.AudioRecordingService.clearLastRecording()
                                                    showAudioDialog = false
                                                    Toast.makeText(context, "Voice Note Card Created! 🎙️", Toast.LENGTH_SHORT).show()
                                                    onCardCreated()
                                                } else {
                                                    Toast.makeText(context, err ?: "Failed to save voice note", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    enabled = currentAudioFile != null && currentAudioFile.exists() && !isRecording,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Save Audio Card") }
                            }
                        }
                    }
                }
            }
        }
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
        android.util.Log.e("CaptureScreen", "Failed to copy URI to cache file", e)
        null
    }
}
