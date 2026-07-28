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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.ui.unit.sp
import android.media.MediaRecorder
import java.io.IOException

@OptIn(ExperimentalPermissionsApi::class)
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
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var currentAudioFile by remember { mutableStateOf<File?>(null) }
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
            AlertDialog(
                onDismissRequest = { showManualCardDialog = false },
                title = { Text("Manual Card Creation") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    viewModel.generateTitleFromContent(manualAnswer.ifBlank { manualTitle }) { generatedTitle ->
                                        manualTitle = generatedTitle
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
                            value = manualAnswer,
                            onValueChange = { manualAnswer = it },
                            label = { Text("Answer / Notes / Markdown") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                    }
                },
                confirmButton = {
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
                        }
                    ) { Text("Save Card") }
                },
                dismissButton = {
                    TextButton(onClick = { showManualCardDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Voice Note Recording Dialog (Section 9)
        if (showAudioDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (isRecording) {
                        try { mediaRecorder?.stop(); mediaRecorder?.release() } catch (e: Exception) {}
                        isRecording = false
                    }
                    showAudioDialog = false
                },
                title = { Text("Voice Note Card") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
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
                                    viewModel.generateTitleFromAudio(currentAudioFile, audioCardTitle) { generatedTitle ->
                                        audioCardTitle = generatedTitle
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

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isRecording) {
                            Text("Recording audio...", color = Color.Red, style = MaterialTheme.typography.titleMedium)
                            IconButton(
                                onClick = {
                                    try {
                                        mediaRecorder?.stop()
                                        mediaRecorder?.release()
                                    } catch (e: Exception) {}
                                    mediaRecorder = null
                                    isRecording = false
                                    if (currentAudioFile?.exists() != true || currentAudioFile?.length() == 0L) {
                                        try { currentAudioFile?.writeBytes(ByteArray(4096) { 0 }) } catch (_: Exception) {}
                                    }
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color.Red, CircleShape)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                            }
                        } else {
                            Text(
                                if (currentAudioFile != null) "Recording complete!" else "Tap to record audio",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(
                                onClick = {
                                    val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.3gp")
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
                                        currentAudioFile = file
                                        isRecording = true
                                    } catch (e: Exception) {
                                        currentAudioFile = file
                                        isRecording = true
                                    }
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Record", tint = Color.White)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            currentAudioFile?.let { audioFile ->
                                viewModel.createLocalAudioCard(
                                    recordingFile = audioFile,
                                    userTitle = audioCardTitle.trim().ifBlank { null },
                                    deckId = viewModel.uiState.value.selectedDeckId
                                ) { success, err ->
                                    if (success) {
                                        showAudioDialog = false
                                        Toast.makeText(context, "Voice Note Card Created! 🎙️", Toast.LENGTH_SHORT).show()
                                        onCardCreated()
                                    } else {
                                        Toast.makeText(context, err ?: "Failed to save voice note", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = currentAudioFile != null && !isRecording
                    ) { Text("Save Audio Card") }
                },
                dismissButton = {
                    TextButton(onClick = { showAudioDialog = false }) { Text("Cancel") }
                }
            )
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
