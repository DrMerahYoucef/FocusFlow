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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.service.AudioCaptureManager
import com.example.ui.theme.NeumorphicColors
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.io.File

enum class CaptureMode { PHOTO, AUDIO }

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CaptureScreen(
    viewModel: RevisionsViewModel,
    onImageCaptured: (String) -> Unit,
    onAudioCaptured: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var captureMode by remember { mutableStateOf(CaptureMode.PHOTO) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

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

    val imageCapture = remember { ImageCapture.Builder().build() }
    val audioCaptureManager = remember { AudioCaptureManager(context) }

    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingSeconds = 0
            while (isRecordingAudio) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (captureMode == CaptureMode.PHOTO) {
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
        } else {
            // Audio Recording Mode UI
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121212)),
                contentAlignment = Alignment.Center
            ) {
                if (!audioPermissionState.status.isGranted) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Permission micro requise pour l'enregistrement", color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { audioPermissionState.launchPermissionRequest() }) {
                            Text("Accorder l'accès micro")
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isRecordingAudio) {
                            val infiniteTransition = rememberInfiniteTransition()
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulse"
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .alpha(alpha)
                                        .background(Color.Red, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Enregistrement en cours...",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = String.format("%02d:%02d", recordingSeconds / 60, recordingSeconds % 60),
                                color = Color.White,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = NeumorphicColors.Primary,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Enregistrement Vocal",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "L'IA générera automatiquement une question pour tester ce cours",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Top Navigation Bar
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

            // Mode Selector Switch
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(4.dp)
            ) {
                FilterChip(
                    selected = captureMode == CaptureMode.PHOTO,
                    onClick = { if (!isRecordingAudio) captureMode = CaptureMode.PHOTO },
                    label = { Text("Photo") },
                    leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                FilterChip(
                    selected = captureMode == CaptureMode.AUDIO,
                    onClick = { if (!isRecordingAudio) captureMode = CaptureMode.AUDIO },
                    label = { Text("Vocal") },
                    leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null) }
                )
            }

            if (captureMode == CaptureMode.PHOTO) {
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
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        // Bottom Controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        ) {
            if (captureMode == CaptureMode.PHOTO) {
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
            } else {
                if (audioPermissionState.status.isGranted) {
                    if (!isRecordingAudio) {
                        FloatingActionButton(
                            onClick = {
                                try {
                                    audioCaptureManager.startRecording()
                                    isRecordingAudio = true
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erreur démarrage enregistrement: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            containerColor = NeumorphicColors.Primary,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Enregistrer", modifier = Modifier.size(36.dp))
                        }
                    } else {
                        FloatingActionButton(
                            onClick = {
                                try {
                                    val tempFile = audioCaptureManager.stopRecording()
                                    isRecordingAudio = false
                                    viewModel.captureNoteFromAudio(tempFile) { success ->
                                        if (success) {
                                            Toast.makeText(context, "Fiche créée avec succès ! ✨", Toast.LENGTH_SHORT).show()
                                            onAudioCaptured()
                                        }
                                    }
                                } catch (e: Exception) {
                                    isRecordingAudio = false
                                    Toast.makeText(context, "Erreur arrêt enregistrement: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            containerColor = Color.Red,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Arrêter", modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }
        }

        // Loading Overlay while processing audio / question
        if (uiState.isProcessingOcr) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NeumorphicColors.SurfaceLight),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = NeumorphicColors.Primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Génération de la question...",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "L'IA analyse le support pour créer une question d'apprentissage...",
                            fontSize = 12.sp,
                            color = NeumorphicColors.TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
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
