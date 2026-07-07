package com.example.ui.screen.timer

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.Phase
import com.example.ui.components.NeumorphicProgressArc
import com.example.ui.theme.NeumorphicColors
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.TreePlantedCelebration
import com.example.ui.components.neumorphicShadow

import com.example.ui.screen.settings.SettingsViewModel
import com.example.service.UpdateState

data class SoundOption(val id: String, val name: String, val emoji: String)

@Composable
fun UpdateBadgeBanner(
    updateState: UpdateState,
    settingsViewModel: SettingsViewModel,
    themeColors: com.example.ui.theme.AppThemeColors,
    modifier: Modifier = Modifier
) {
    val isDark = com.example.ui.theme.LocalIsDarkTheme.current
    val accentColor = when (updateState) {
        is UpdateState.ReadyToInstall -> NeumorphicColors.Success
        is UpdateState.Error -> NeumorphicColors.Accent
        is UpdateState.Downloading -> NeumorphicColors.Primary
        else -> NeumorphicColors.Primary
    }

    val bannerText = when (updateState) {
        is UpdateState.UpdateAvailable -> "Mandatory Update Available! Tap to install."
        is UpdateState.Downloading -> {
            val progress = (updateState as UpdateState.Downloading).progress
            val percent = if (progress >= 0f) "${(progress * 100).toInt()}%" else "..."
            "Downloading Update: $percent"
        }
        is UpdateState.ReadyToInstall -> "Update Ready! Tap to install and restart."
        is UpdateState.Error -> "Update failed. Tap to retry check."
        else -> ""
    }

    val bannerIcon = when (updateState) {
        is UpdateState.ReadyToInstall -> Icons.Default.CheckCircle
        is UpdateState.Error -> Icons.Default.Error
        is UpdateState.Downloading -> Icons.Default.CloudDownload
        else -> Icons.Default.CloudDownload
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .neumorphicShadow(
                cornerRadius = 12.dp,
                elevation = 4.dp,
                isPressed = false
            )
            .clickable {
                when (updateState) {
                    is UpdateState.UpdateAvailable -> {
                        settingsViewModel.downloadUpdate(updateState.downloadUrl)
                    }
                    is UpdateState.ReadyToInstall -> {
                        settingsViewModel.installUpdate()
                    }
                    is UpdateState.Error -> {
                        settingsViewModel.checkForUpdates(forceFetch = true)
                    }
                    else -> {}
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = bannerIcon,
                contentDescription = "Update Status Icon",
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = bannerText,
                color = themeColors.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.timerState.collectAsState()
    val updateState by settingsViewModel.updateState.collectAsState()
    val context = LocalContext.current

    var showCelebration by remember { mutableStateOf(false) }
    var celebrationTree by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.treePlanted.collect { treeNumber ->
            showCelebration = true
            celebrationTree = treeNumber
        }
    }

    val totalDurationMs = when (state.phase) {
        Phase.FOCUS -> state.focusDurationMs
        Phase.SHORT_BREAK -> state.shortBreakDurationMs
        Phase.LONG_BREAK -> state.longBreakDurationMs
    }

    val progress = if (totalDurationMs > 0) {
        state.remainingMs.toFloat() / totalDurationMs.toFloat()
    } else {
        1f
    }

    val minutes = (state.remainingMs / 1000) / 60
    val seconds = (state.remainingMs / 1000) % 60
    val remainingText = String.format("%02d:%02d", minutes, seconds)

    // Check if DND access is enabled
    var hasDndPermission by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        hasDndPermission = nm.isNotificationPolicyAccessGranted
    }

    val themeColors = com.example.ui.theme.LocalAppThemeColors.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (updateState is UpdateState.UpdateAvailable ||
                updateState is UpdateState.Downloading ||
                updateState is UpdateState.ReadyToInstall ||
                updateState is UpdateState.Error
            ) {
                UpdateBadgeBanner(
                    updateState = updateState,
                    settingsViewModel = settingsViewModel,
                    themeColors = themeColors
                )
            }

            // App Title / Branding Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "FOCUS ISLAND",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = themeColors.onSurface
                )
                Text(
                    text = "Streamlined tactile productivity",
                    style = MaterialTheme.typography.labelSmall,
                    color = themeColors.secondaryText,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Center Countdown Progress Arc
            NeumorphicProgressArc(
                progress = progress,
                phase = state.phase,
                remainingText = remainingText,
                modifier = Modifier.weight(1f, fill = false)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Session completion dots indicators
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Completed: ${state.sessionCount} sessions",
                    color = themeColors.secondaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sessionsTarget = state.sessionsBeforeLongBreak
                    val currentCompletedMod = if (sessionsTarget > 0) {
                        state.sessionCount % sessionsTarget
                    } else {
                        0
                    }

                    for (i in 0 until sessionsTarget) {
                        val isActive = i < currentCompletedMod
                        val dotColor = if (isActive) {
                            themeColors.accent
                        } else {
                            themeColors.divider
                        }
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Interactive control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                if (!state.isRunning) {
                    // Play / Launch Button
                    GlassButton(
                        label = "Start",
                        icon = Icons.Default.PlayArrow,
                        onClick = { viewModel.startTimer() },
                        accentColor = themeColors.accent,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    // Pause Button
                    GlassButton(
                        label = "Pause",
                        icon = Icons.Default.Pause,
                        onClick = { viewModel.pauseTimer() },
                        accentColor = NeumorphicColors.Warning,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Stop Button
                GlassButton(
                    label = "Stop",
                    icon = Icons.Default.Stop,
                    onClick = { viewModel.stopTimer() },
                    accentColor = NeumorphicColors.Accent,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                    modifier = Modifier.weight(1f)
                )

                // Skip Button (advance to break/focus immediately)
                GlassButton(
                    label = "Skip",
                    icon = Icons.Default.SkipNext,
                    onClick = { viewModel.skipPhase() },
                    accentColor = themeColors.accent,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Ambient soundscapes soundboard
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AMBIENT SOUNDSCAPE 🎧",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.secondaryText,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val activeSoundName = when (state.currentAmbientId) {
                    "rain" -> "Rain 🌧️"
                    "white_noise" -> "Noise 🌫️"
                    "campfire" -> "Fire 🔥"
                    "stream" -> "Stream 🌊"
                    "space" -> "Space 🌌"
                    else -> "Muted 🔇"
                }

                Text(
                    text = "Playing: $activeSoundName",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (state.currentAmbientId == "none") themeColors.secondaryText else themeColors.accent,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    // Button 1: Play Random
                    GlassButton(
                        label = "Play Random",
                        icon = Icons.Default.Shuffle,
                        onClick = {
                            val sounds = listOf("rain", "white_noise", "campfire", "stream", "space")
                            val currentId = state.currentAmbientId
                            val nextSounds = sounds.filter { it != currentId }
                            val nextId = if (nextSounds.isNotEmpty()) nextSounds.random() else sounds.random()
                            viewModel.setAmbientSound(nextId)
                        },
                        accentColor = themeColors.accent,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                        modifier = Modifier.weight(1f)
                    )

                    // Button 2: Stop / Mute
                    GlassButton(
                        label = "Mute",
                        icon = Icons.Default.VolumeOff,
                        onClick = {
                            viewModel.setAmbientSound("none")
                        },
                        accentColor = if (state.currentAmbientId == "none") themeColors.divider else Color(0xFFE57373),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Do Not Disturb permission / Status Card
            if (!hasDndPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 12.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "⚠️ DND access lacking. Click here to configure.",
                                color = themeColors.accent,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.clickable {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        context.startActivity(
                                            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showCelebration) {
            TreePlantedCelebration(treeNumber = celebrationTree) {
                showCelebration = false
            }
        }
    }
}
