package com.example.ui.components

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.entity.RevisionMediaType
import com.example.data.db.entity.RevisionNoteEntity
import com.example.ui.theme.NeumorphicColors
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun CardFrontView(
    note: RevisionNoteEntity,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize().padding(16.dp)
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
            text = note.question,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = NeumorphicColors.TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))
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
                text = "Appuyez pour révéler le support (réponse)",
                fontSize = 12.sp,
                color = NeumorphicColors.TextSecondary
            )
        }
    }
}

@Composable
fun CardBackView(
    note: RevisionNoteEntity,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SUPPORT ORIGINAL (RÉPONSE)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = NeumorphicColors.Accent
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = note.question,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = NeumorphicColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = NeumorphicColors.TextSecondary.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when (note.mediaType) {
                RevisionMediaType.IMAGE -> {
                    val bitmap = remember(note.mediaFilePath) {
                        try {
                            if (File(note.mediaFilePath).exists()) {
                                BitmapFactory.decodeFile(note.mediaFilePath)
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Original photo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Text(
                            text = "Photo introuvable",
                            color = NeumorphicColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
                RevisionMediaType.AUDIO -> {
                    AudioPlayerControls(filePath = note.mediaFilePath)
                }
            }
        }
    }
}

@Composable
fun AudioPlayerControls(
    filePath: String,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }

    val mediaPlayer = remember(filePath) {
        try {
            if (File(filePath).exists()) {
                MediaPlayer().apply {
                    setDataSource(filePath)
                    prepare()
                }
            } else null
        } catch (e: Exception) {
            android.util.Log.e("AudioPlayerControls", "Failed to initialize MediaPlayer", e)
            null
        }
    }

    DisposableEffect(mediaPlayer) {
        mediaPlayer?.setOnCompletionListener {
            isPlaying = false
            currentPosition = 0
        }
        onDispose {
            try {
                mediaPlayer?.apply {
                    if (isPlaying) stop()
                    release()
                }
            } catch (e: Exception) {
                // Ignore dispose error
            }
        }
    }

    LaunchedEffect(mediaPlayer) {
        mediaPlayer?.let {
            duration = it.duration
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying && mediaPlayer != null) {
            try {
                currentPosition = mediaPlayer.currentPosition
            } catch (e: Exception) {
                break
            }
            delay(200)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = NeumorphicColors.SurfaceLight),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth().padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = null,
                tint = NeumorphicColors.Primary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (mediaPlayer == null) {
                Text(
                    text = "Enregistrement vocal introuvable",
                    fontSize = 14.sp,
                    color = NeumorphicColors.TextSecondary
                )
            } else {
                Text(
                    text = "Note Vocale",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphicColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = NeumorphicColors.Primary,
                    trackColor = NeumorphicColors.SurfaceDark.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTimeMs(currentPosition),
                        fontSize = 12.sp,
                        color = NeumorphicColors.TextSecondary
                    )
                    Text(
                        text = formatTimeMs(duration),
                        fontSize = 12.sp,
                        color = NeumorphicColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                        .size(56.dp)
                        .background(NeumorphicColors.Primary, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

private fun formatTimeMs(ms: Int): String {
    val totalSeconds = (ms / 1000)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
