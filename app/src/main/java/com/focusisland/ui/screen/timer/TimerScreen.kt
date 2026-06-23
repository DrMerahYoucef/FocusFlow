package com.focusisland.ui.screen.timer

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
import com.focusisland.service.Phase
import com.focusisland.ui.components.NeumorphicProgressArc
import com.focusisland.ui.theme.NeumorphicColors
import com.focusisland.ui.components.GlassButton
import com.focusisland.ui.components.GlassCard
import com.focusisland.ui.components.TreePlantedCelebration

data class SoundOption(val id: String, val name: String, val emoji: String)

@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.timerState.collectAsState()
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
            // App Title / Branding Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "FOCUS FLOW",
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

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Ambient soundscapes soundboard
            val ambientSounds = remember {
                listOf(
                    com.example.ui.screen.timer.SoundOption("none", "None", "🔇"),
                    com.example.ui.screen.timer.SoundOption("rain", "Rain", "🌧️"),
                    com.example.ui.screen.timer.SoundOption("white_noise", "Noise", "🌫️"),
                    com.example.ui.screen.timer.SoundOption("campfire", "Fire", "🔥"),
                    com.example.ui.screen.timer.SoundOption("stream", "Stream", "🌊"),
                    com.example.ui.screen.timer.SoundOption("space", "Space", "🌌")
                )
            }

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
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                    ambientSounds.forEach { option ->
                        val isSelected = state.currentAmbientId == option.id
                        
                        val backgroundColor = if (isSelected) {
                            themeColors.accent.copy(alpha = 0.18f)
                        } else {
                            Color.Transparent
                        }
                        
                        val borderDecorationModifier = if (isSelected) {
                            Modifier.border(
                                width = 1.5.dp,
                                color = themeColors.accent,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            )
                        } else {
                            Modifier.border(
                                width = 1.dp,
                                color = themeColors.divider.copy(alpha = 0.4f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .background(backgroundColor)
                                .then(borderDecorationModifier)
                                .clickable { viewModel.setAmbientSound(option.id) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = option.emoji,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = option.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) themeColors.accent else themeColors.secondaryText,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

            // Do Not Disturb permission / Status Card
            if (state.phase == Phase.FOCUS) {
                val dndActiveText = if (state.isDndActive) {
                    "🔕 Notifications Silenced (DND Active)"
                } else if (!hasDndPermission) {
                    "⚠️ DND access lacking. Click here to configure."
                } else {
                    "🔔 Notifications allowed in breaks"
                }

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
                                text = dndActiveText,
                                color = if (!hasDndPermission) themeColors.accent else themeColors.secondaryText,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.clickable {
                                    if (!hasDndPermission) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            context.startActivity(
                                                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Keep background balanced with placeholder spacing
                Spacer(modifier = Modifier.height(56.dp))
            }
        }

        if (showCelebration) {
            TreePlantedCelebration(treeNumber = celebrationTree) {
                showCelebration = false
            }
        }
    }
}
