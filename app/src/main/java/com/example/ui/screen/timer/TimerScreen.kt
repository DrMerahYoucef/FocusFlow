package com.example.ui.screen.timer

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import android.os.PowerManager

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
    onNavigateToBatterySaver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.timerState.collectAsState()
    val updateState by settingsViewModel.updateState.collectAsState()
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    var mainScreenLastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        mainScreenLastInteraction = System.currentTimeMillis()
    }

    // Auto re-arm battery saver after 20 seconds of no interaction if focus session is running
    LaunchedEffect(mainScreenLastInteraction, state.isRunning) {
        if (state.isRunning) {
            delay(20000) // 20s auto re-arm
            onNavigateToBatterySaver()
        }
    }
    val radioViewModel: com.example.ui.screen.radio.RadioViewModel = viewModel()
    val radioPlaying by radioViewModel.isPlaying.collectAsState()
    val currentStation by radioViewModel.currentStation.collectAsState()

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        mainScreenLastInteraction = System.currentTimeMillis()
                    }
                }
            }
    ) {
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(40.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
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

                androidx.compose.material3.IconButton(
                    onClick = { onNavigateToBatterySaver() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(themeColors.inputBackground)
                        .border(1.dp, themeColors.divider, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = "Battery Saver Mode",
                        tint = themeColors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
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

@Composable
fun NeumorphicMonoButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = androidx.compose.ui.platform.LocalView.current
    val shape = RoundedCornerShape(12.dp)
    
    val borderCol = if (isActive) Color(0x35FFFFFF) else Color(0x10FFFFFF)
    val bgCol = if (isActive) Color(0xFF0F0F0F) else Color(0xFF1C1C1C)
    val contentColor = if (isActive) Color.White else Color(0x70FFFFFF)
    
    Box(
        modifier = modifier
            .height(44.dp)
            .shadow(
                elevation = if (isActive) 0.dp else 4.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black,
                spotColor = Color.Black
            )
            .drawBehind {
                if (!isActive) {
                    drawRoundRect(
                        color = Color(0x15FFFFFF),
                        topLeft = Offset(-1.5.dp.toPx(), -1.5.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width + 1.dp.toPx(), size.height + 1.dp.toPx()),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0x45000000),
                        topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                        size = size,
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )
                } else {
                    drawRoundRect(
                        color = Color(0x25000000),
                        topLeft = Offset(0f, 0f),
                        size = size,
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )
                }
            }
            .background(bgCol, shape)
            .border(1.dp, borderCol, shape)
            .clickable {
                onClick()
                try {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                } catch (e: Exception) {}
            }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NeumorphicAudioInfoBar(
    currentStationName: String?,
    radioIsPlaying: Boolean,
    currentAmbientId: String,
    currentAmbientLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = androidx.compose.ui.platform.LocalView.current
    val shape = RoundedCornerShape(14.dp)
    
    val (icon, title, subtitle) = when {
        radioIsPlaying && !currentStationName.isNullOrBlank() -> {
            Triple(Icons.Default.Radio, currentStationName, "Streaming Live Radio")
        }
        currentAmbientId != "none" -> {
            Triple(Icons.Default.MusicNote, currentAmbientLabel, "Ambient Sound Loop")
        }
        else -> {
            Triple(Icons.Default.VolumeMute, "Silence Active", "No background audio")
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = 4.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black,
                spotColor = Color.Black
            )
            .drawBehind {
                drawRoundRect(
                    color = Color(0x12FFFFFF),
                    topLeft = Offset(-1.5.dp.toPx(), -1.5.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width + 1.dp.toPx(), size.height + 1.dp.toPx()),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0x45000000),
                    topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                )
            }
            .background(Color(0xFF181818), shape)
            .border(1.dp, Color(0x0EFFFFFF), shape)
            .clickable {
                onClick()
                try {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                } catch (e: Exception) {}
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF101010), CircleShape)
                    .border(1.dp, Color(0x10FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = Color(0x80FFFFFF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text(
                text = "▼",
                color = Color(0x60FFFFFF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StationRowItem(
    station: com.example.data.RadioStation,
    isFav: Boolean,
    isCurrent: Boolean,
    onSelect: () -> Unit,
    onToggleFav: () -> Unit
) {
    val view = androidx.compose.ui.platform.LocalView.current
    val shape = RoundedCornerShape(10.dp)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isCurrent) 0.dp else 2.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black,
                spotColor = Color.Black
            )
            .drawBehind {
                if (!isCurrent) {
                    drawRoundRect(
                        color = Color(0x0EFFFFFF),
                        topLeft = Offset(-1.dp.toPx(), -1.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width + 0.5.dp.toPx(), size.height + 0.5.dp.toPx()),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0x30000000),
                        topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                        size = size,
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                }
            }
            .background(if (isCurrent) Color(0xFF0F0F0F) else Color(0xFF1B1B1B), shape)
            .border(1.dp, if (isCurrent) Color(0x25FFFFFF) else Color(0x08FFFFFF), shape)
            .clickable {
                onSelect()
                try {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                } catch (e: Exception) {}
            }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF0F0F0F), CircleShape)
                    .border(0.5.dp, Color(0x10FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = null,
                    tint = if (isCurrent) Color.White else Color(0x60FFFFFF),
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    color = if (isCurrent) Color.White else Color(0xDFFFFFFF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = station.country.ifEmpty { "🌍 Global" },
                    color = Color(0x50FFFFFF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            androidx.compose.material3.IconButton(
                onClick = {
                    onToggleFav()
                    try {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    } catch (e: Exception) {}
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFav) Color.White else Color(0x40FFFFFF),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AmbientRowItem(
    label: String,
    isCurrent: Boolean,
    onSelect: () -> Unit
) {
    val view = androidx.compose.ui.platform.LocalView.current
    val shape = RoundedCornerShape(10.dp)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isCurrent) 0.dp else 2.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black,
                spotColor = Color.Black
            )
            .drawBehind {
                if (!isCurrent) {
                    drawRoundRect(
                        color = Color(0x0EFFFFFF),
                        topLeft = Offset(-1.dp.toPx(), -1.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width + 0.5.dp.toPx(), size.height + 0.5.dp.toPx()),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0x30000000),
                        topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                        size = size,
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                }
            }
            .background(if (isCurrent) Color(0xFF0F0F0F) else Color(0xFF1B1B1B), shape)
            .border(1.dp, if (isCurrent) Color(0x25FFFFFF) else Color(0x08FFFFFF), shape)
            .clickable {
                onSelect()
                try {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                } catch (e: Exception) {}
            }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF0F0F0F), CircleShape)
                    .border(0.5.dp, Color(0x10FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isCurrent) Color.White else Color(0x60FFFFFF),
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            
            Text(
                text = label,
                color = if (isCurrent) Color.White else Color(0xDFFFFFFF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            
            if (isCurrent) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Active",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun AudioModeSelector(
    textAlphaMultiplier: Float,
    radioIsPlaying: Boolean,
    currentAmbientId: String,
    currentAmbientLabel: String,
    radioStationName: String?,
    ambientIds: List<String>,
    onSelectRadio: () -> Unit,
    onSelectAmbient: (String) -> Unit,
    onSelectNoAudio: () -> Unit,
    onShowAudioPopup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().alpha(textAlphaMultiplier)
    ) {
        Text(
            text = "AUDIO PLAYBACK",
            color = Color(0x35FFFFFF),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            // 1. Radio Option
            val isRadioActive = radioIsPlaying
            NeumorphicMonoButton(
                text = "Radio",
                icon = Icons.Default.Radio,
                isActive = isRadioActive,
                onClick = {
                    if (radioStationName.isNullOrBlank()) {
                        onShowAudioPopup()
                    } else {
                        onSelectRadio()
                    }
                },
                modifier = Modifier.weight(1f)
            )

            // 2. Ambient Option
            val isAmbientActive = currentAmbientId != "none"
            NeumorphicMonoButton(
                text = if (isAmbientActive) currentAmbientLabel else "Ambient",
                icon = Icons.Default.MusicNote,
                isActive = isAmbientActive,
                onClick = {
                    if (!isAmbientActive) {
                        onSelectAmbient(if (currentAmbientId != "none") currentAmbientId else "rain")
                    } else {
                        val currentIndex = ambientIds.indexOf(currentAmbientId)
                        val nextId = if (currentIndex == -1 || currentIndex == ambientIds.lastIndex) {
                            ambientIds.first()
                        } else {
                            ambientIds[currentIndex + 1]
                        }
                        onSelectAmbient(nextId)
                    }
                },
                modifier = Modifier.weight(1f)
            )

            // 3. No Audio Option
            val isSilenceActive = !radioIsPlaying && currentAmbientId == "none"
            NeumorphicMonoButton(
                text = "Silent",
                icon = Icons.Default.VolumeMute,
                isActive = isSilenceActive,
                onClick = { onSelectNoAudio() },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        NeumorphicAudioInfoBar(
            currentStationName = radioStationName,
            radioIsPlaying = radioIsPlaying,
            currentAmbientId = currentAmbientId,
            currentAmbientLabel = currentAmbientLabel,
            onClick = { onShowAudioPopup() },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun LandscapeBatterySaverLayout(
    batteryLevel: Int,
    textAlphaMultiplier: Float,
    remainingTimeFormatted: String,
    phaseLabel: String,
    arcColor: Color,
    animatedProgress: Float,
    radioIsPlaying: Boolean,
    radioStationName: String?,
    currentAmbientId: String,
    currentAmbientLabel: String,
    isSystemPowerSaveMode: Boolean,
    sessionCount: Int,
    totalFocusSecs: Long,
    isPlaying: Boolean,
    ambientIds: List<String>,
    onPlayPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    onSkipClick: () -> Unit,
    onSelectRadio: () -> Unit,
    onSelectAmbient: (String) -> Unit,
    onSelectNoAudio: () -> Unit,
    onShowAudioPopup: () -> Unit,
    onInteraction: () -> Unit,
    view: android.view.View
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT SIDE: Big Timer
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 5.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = diameter / 2f,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = arcColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                        size = androidx.compose.ui.geometry.Size(diameter, diameter),
                        topLeft = androidx.compose.ui.geometry.Offset((size.width - diameter)/2f, (size.height - diameter)/2f)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = phaseLabel.uppercase(),
                        color = Color(0xB0FFFFFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.alpha(textAlphaMultiplier)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = remainingTimeFormatted,
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.alpha(textAlphaMultiplier)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when {
                            radioIsPlaying -> "Radio: ${radioStationName ?: "Playing"}"
                            currentAmbientId != "none" -> currentAmbientLabel
                            else -> "Silence 🔇"
                        },
                        color = Color(0x60FFFFFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.alpha(textAlphaMultiplier),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // RIGHT SIDE: Header, Stats, Audio Selection, Controls, Hint
        Column(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BATTERY SAVER ACTIVE 🔋",
                    color = if (isSystemPowerSaveMode) Color(0x6081C784) else Color(0x60FFFFFF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.alpha(textAlphaMultiplier)
                )
                Text(
                    text = "Battery: $batteryLevel%",
                    color = Color(0x40FFFFFF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.alpha(textAlphaMultiplier)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "COMPLETED",
                        color = Color(0x40FFFFFF),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.alpha(textAlphaMultiplier)
                    )
                    Text(
                        text = "$sessionCount sessions",
                        color = Color(0xC0FFFFFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.alpha(textAlphaMultiplier)
                    )
                }

                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .width(1.dp)
                        .background(Color(0x15FFFFFF))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "FOCUS TIME",
                        color = Color(0x40FFFFFF),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.alpha(textAlphaMultiplier)
                    )
                    val totalMins = totalFocusSecs / 60
                    Text(
                        text = "$totalMins min",
                        color = Color(0xC0FFFFFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.alpha(textAlphaMultiplier)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    val isRadioActive = radioIsPlaying
                    NeumorphicMonoButton(
                        text = "Radio",
                        icon = Icons.Default.Radio,
                        isActive = isRadioActive,
                        onClick = {
                            if (radioStationName.isNullOrBlank()) {
                                onShowAudioPopup()
                            } else {
                                onSelectRadio()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    val isSilenceActive = !radioIsPlaying && currentAmbientId == "none"
                    NeumorphicMonoButton(
                        text = "Silent",
                        icon = Icons.Default.VolumeMute,
                        isActive = isSilenceActive,
                        onClick = { onSelectNoAudio() },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val isAmbientActive = currentAmbientId != "none"
                NeumorphicMonoButton(
                    text = if (isAmbientActive) currentAmbientLabel else "Ambient Sounds",
                    icon = Icons.Default.MusicNote,
                    isActive = isAmbientActive,
                    onClick = {
                        if (!isAmbientActive) {
                            onSelectAmbient(if (currentAmbientId != "none") currentAmbientId else "rain")
                        } else {
                            val currentIndex = ambientIds.indexOf(currentAmbientId)
                            val nextId = if (currentIndex == -1 || currentIndex == ambientIds.lastIndex) {
                                ambientIds.first()
                            } else {
                                ambientIds[currentIndex + 1]
                            }
                            onSelectAmbient(nextId)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.IconButton(
                    onClick = {
                        onInteraction()
                        onPlayPauseClick()
                        try {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                        } catch (e: Exception) {}
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, Color(0x30FFFFFF), CircleShape)
                        .background(Color(0x0AFFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause Timer" else "Play Timer",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                androidx.compose.material3.IconButton(
                    onClick = {
                        onInteraction()
                        onStopClick()
                        try {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                        } catch (e: Exception) {}
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, Color(0x30FFFFFF), CircleShape)
                        .background(Color(0x0AFFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Timer",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                androidx.compose.material3.IconButton(
                    onClick = {
                        onInteraction()
                        onSkipClick()
                        try {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                        } catch (e: Exception) {}
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, Color(0x30FFFFFF), CircleShape)
                        .background(Color(0x0AFFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip Phase",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            val infiniteTransition = rememberInfiniteTransition(label = "indicator")
            val chevronTranslationY by infiniteTransition.animateFloat(
                initialValue = 3f,
                targetValue = -4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "chevronTranslationY"
            )
            val chevronAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "chevronAlpha"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(textAlphaMultiplier)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Color(0xFF9482FF).copy(alpha = chevronAlpha),
                    modifier = Modifier
                        .size(18.dp)
                        .offset(y = chevronTranslationY.dp)
                )
                Text(
                    text = "Swipe up for stats & milestones 📊",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Double tap to return",
                    color = Color(0x25FFFFFF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun PortraitBatterySaverLayout(
    batteryLevel: Int,
    textAlphaMultiplier: Float,
    remainingTimeFormatted: String,
    phaseLabel: String,
    arcColor: Color,
    animatedProgress: Float,
    radioIsPlaying: Boolean,
    radioStationName: String?,
    currentAmbientId: String,
    currentAmbientLabel: String,
    isSystemPowerSaveMode: Boolean,
    sessionCount: Int,
    totalFocusSecs: Long,
    isPlaying: Boolean,
    ambientIds: List<String>,
    onPlayPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    onSkipClick: () -> Unit,
    onSelectRadio: () -> Unit,
    onSelectAmbient: (String) -> Unit,
    onSelectNoAudio: () -> Unit,
    onShowAudioPopup: () -> Unit,
    onInteraction: () -> Unit,
    view: android.view.View
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BATTERY SAVER ACTIVE 🔋",
                color = if (isSystemPowerSaveMode) Color(0x6081C784) else Color(0x60FFFFFF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(textAlphaMultiplier)
            )
            Text(
                text = "$batteryLevel%",
                color = Color(0x80FFFFFF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.alpha(textAlphaMultiplier)
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .size(280.dp)
        ) {
            Canvas(modifier = Modifier.size(260.dp)) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    style = Stroke(width = 6.dp.toPx())
                )
                drawArc(
                    color = arcColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = phaseLabel.uppercase(),
                    color = Color(0xB0FFFFFF),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    modifier = Modifier.alpha(textAlphaMultiplier)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = remainingTimeFormatted,
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Thin
                    ),
                    modifier = Modifier.alpha(textAlphaMultiplier)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when {
                        radioIsPlaying -> "Radio: ${radioStationName ?: "Playing"}"
                        currentAmbientId != "none" -> currentAmbientLabel
                        else -> "Silence 🔇"
                    },
                    color = Color(0x60FFFFFF),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(textAlphaMultiplier)
                )
            }
        }

        AudioModeSelector(
            textAlphaMultiplier = textAlphaMultiplier,
            radioIsPlaying = radioIsPlaying,
            currentAmbientId = currentAmbientId,
            currentAmbientLabel = currentAmbientLabel,
            radioStationName = radioStationName,
            ambientIds = ambientIds,
            onSelectRadio = onSelectRadio,
            onSelectAmbient = onSelectAmbient,
            onSelectNoAudio = onSelectNoAudio,
            onShowAudioPopup = onShowAudioPopup,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "COMPLETED",
                    color = Color(0x50FFFFFF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.alpha(textAlphaMultiplier)
                )
                Text(
                    text = "$sessionCount sessions",
                    color = Color(0xD0FFFFFF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.alpha(textAlphaMultiplier)
                )
            }

            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp)
                    .background(Color(0x1AFFFFFF))
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "FOCUS TIME",
                    color = Color(0x50FFFFFF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.alpha(textAlphaMultiplier)
                )
                val totalMins = totalFocusSecs / 60
                Text(
                    text = "$totalMins min",
                    color = Color(0xD0FFFFFF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.alpha(textAlphaMultiplier)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.IconButton(
                onClick = {
                    onInteraction()
                    onPlayPauseClick()
                    try {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                    } catch (e: Exception) {}
                },
                modifier = Modifier
                    .size(54.dp)
                    .border(1.dp, Color(0x40FFFFFF), CircleShape)
                    .background(Color(0x10FFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause Timer" else "Play Timer",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            androidx.compose.material3.IconButton(
                onClick = {
                    onInteraction()
                    onStopClick()
                    try {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                    } catch (e: Exception) {}
                },
                modifier = Modifier
                    .size(54.dp)
                    .border(1.dp, Color(0x40FFFFFF), CircleShape)
                    .background(Color(0x10FFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop Timer",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            androidx.compose.material3.IconButton(
                onClick = {
                    onInteraction()
                    onSkipClick()
                    try {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                    } catch (e: Exception) {}
                },
                modifier = Modifier
                    .size(54.dp)
                    .border(1.dp, Color(0x40FFFFFF), CircleShape)
                    .background(Color(0x10FFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Skip Phase",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        val infiniteTransition = rememberInfiniteTransition(label = "indicator")
        val chevronTranslationY by infiniteTransition.animateFloat(
            initialValue = 4f,
            targetValue = -6f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "chevronTranslationY"
        )
        val chevronAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "chevronAlpha"
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .navigationBarsPadding()
                .alpha(textAlphaMultiplier)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = Color(0xFF9482FF).copy(alpha = chevronAlpha),
                modifier = Modifier
                    .size(24.dp)
                    .offset(y = chevronTranslationY.dp)
            )
            Text(
                text = "Swipe up to view statistics & milestones 📊",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Double tap anywhere to return",
                color = Color(0x30FFFFFF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun AudioSelectorDialog(
    visible: Boolean,
    allStations: List<com.example.data.RadioStation>,
    favouriteIds: Set<String>,
    currentAmbientId: String,
    currentAmbientLabel: String,
    radioIsPlaying: Boolean,
    radioStationName: String?,
    ambientIds: List<String>,
    ambientLabels: Map<String, String>,
    onToggleFavourite: (com.example.data.RadioStation) -> Unit,
    onSelectRadioStation: (com.example.data.RadioStation) -> Unit,
    onSelectAmbient: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { onDismissRequest() },
            contentAlignment = Alignment.Center
        ) {
            val popupShape = RoundedCornerShape(20.dp)
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 440.dp)
                    .fillMaxHeight(0.75f)
                    .clickable(enabled = false) {}
                    .shadow(
                        elevation = 8.dp,
                        shape = popupShape,
                        clip = false,
                        ambientColor = Color.Black,
                        spotColor = Color.Black
                    )
                    .drawBehind {
                        drawRoundRect(
                            color = Color(0x18FFFFFF),
                            topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(size.width + 1.dp.toPx(), size.height + 1.dp.toPx()),
                            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                        )
                        drawRoundRect(
                            color = Color(0x70000000),
                            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                            size = size,
                            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                        )
                    }
                    .background(Color(0xFF141414), popupShape)
                    .border(1.dp, Color(0x12FFFFFF), popupShape)
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AUDIO SELECTOR",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        androidx.compose.material3.IconButton(
                            onClick = { onDismissRequest() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0x80FFFFFF)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    var activeTab by remember { mutableStateOf("radio") }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tabs = listOf(
                            "radio" to "All Radio",
                            "favorites" to "Favorites",
                            "ambient" to "Ambient"
                        )
                        tabs.forEach { (id, label) ->
                            val isTabActive = activeTab == id
                            val chipShape = RoundedCornerShape(10.dp)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .shadow(
                                        elevation = if (isTabActive) 0.dp else 2.dp,
                                        shape = chipShape,
                                        clip = false,
                                        ambientColor = Color.Black,
                                        spotColor = Color.Black
                                    )
                                    .drawBehind {
                                        if (!isTabActive) {
                                            drawRoundRect(
                                                color = Color(0x10FFFFFF),
                                                topLeft = Offset(-1.dp.toPx(), -1.dp.toPx()),
                                                size = androidx.compose.ui.geometry.Size(size.width + 0.5.dp.toPx(), size.height + 0.5.dp.toPx()),
                                                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                                            )
                                            drawRoundRect(
                                                color = Color(0x35000000),
                                                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                                                size = size,
                                                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                                            )
                                        }
                                    }
                                    .background(
                                        if (isTabActive) Color(0xFF0C0C0C) else Color(0xFF1D1D1D),
                                        chipShape
                                    )
                                    .border(
                                        1.dp,
                                        if (isTabActive) Color(0x25FFFFFF) else Color(0x0AFFFFFF),
                                        chipShape
                                    )
                                    .clickable { activeTab = id }
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isTabActive) Color.White else Color(0x80FFFFFF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (activeTab) {
                            "radio" -> {
                                if (allStations.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No stations found", color = Color(0x40FFFFFF), fontSize = 12.sp)
                                    }
                                } else {
                                    androidx.compose.foundation.lazy.LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(allStations.size) { index ->
                                            val station = allStations[index]
                                            val isFav = favouriteIds.contains(station.id)
                                            val isCurrentlyPlaying = radioIsPlaying && radioStationName == station.name
                                            
                                            StationRowItem(
                                                station = station,
                                                isFav = isFav,
                                                isCurrent = isCurrentlyPlaying,
                                                onSelect = {
                                                    onSelectRadioStation(station)
                                                },
                                                onToggleFav = { onToggleFavourite(station) }
                                            )
                                        }
                                    }
                                }
                            }
                            "favorites" -> {
                                val favStations = allStations.filter { favouriteIds.contains(it.id) }
                                if (favStations.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No favorite stations yet", color = Color(0x40FFFFFF), fontSize = 12.sp, textAlign = TextAlign.Center)
                                    }
                                } else {
                                    androidx.compose.foundation.lazy.LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(favStations.size) { index ->
                                            val station = favStations[index]
                                            val isCurrentlyPlaying = radioIsPlaying && radioStationName == station.name
                                            
                                            StationRowItem(
                                                station = station,
                                                isFav = true,
                                                isCurrent = isCurrentlyPlaying,
                                                onSelect = {
                                                    onSelectRadioStation(station)
                                                },
                                                onToggleFav = { onToggleFavourite(station) }
                                            )
                                        }
                                    }
                                }
                            }
                            "ambient" -> {
                                androidx.compose.foundation.lazy.LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(ambientIds.size) { index ->
                                        val ambientId = ambientIds[index]
                                        val label = ambientLabels[ambientId] ?: ambientId
                                        val isCurrent = currentAmbientId == ambientId
                                        
                                        AmbientRowItem(
                                            label = label,
                                            isCurrent = isCurrent,
                                            onSelect = {
                                                onSelectAmbient(ambientId)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BatterySaverOverlay(
    onDismiss: () -> Unit,
    batteryLevel: Int,
    remainingTimeFormatted: String,
    radioStationName: String?,
    radioStationThumbnail: String?,
    radioIsPlaying: Boolean,
    currentAmbientId: String,
    allStations: List<com.example.data.RadioStation>,
    favouriteIds: Set<String>,
    onToggleFavourite: (com.example.data.RadioStation) -> Unit,
    onSelectRadioStation: (com.example.data.RadioStation) -> Unit,
    onSelectRadio: () -> Unit,
    onSelectAmbient: (String) -> Unit,
    onSelectNoAudio: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    onSkipClick: () -> Unit,
    isPlaying: Boolean,
    sessionCount: Int,
    totalFocusSecs: Long,
    phaseLabel: String,
    progress: Float
) {
    val context = LocalContext.current
    val view = LocalView.current
    val window = remember(view) {
        var parent = view.parent
        var windowProvider: androidx.compose.ui.window.DialogWindowProvider? = null
        while (parent != null) {
            if (parent is androidx.compose.ui.window.DialogWindowProvider) {
                windowProvider = parent
                break
            }
            parent = parent.parent
        }
        windowProvider?.window ?: (context as? android.app.Activity)?.window
    }

    // Keep screen on, hide status/nav bars, hide them with transient gestures
    DisposableEffect(window, view) {
        window?.let { win ->
            win.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            win.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            win.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)

            // Detect if inside Dialog to set match parent layout
            var isDialog = false
            var p = view.parent
            while (p != null) {
                if (p is androidx.compose.ui.window.DialogWindowProvider) {
                    isDialog = true
                    break
                }
                p = p.parent
            }

            if (isDialog) {
                win.setLayout(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.MATCH_PARENT
                )
                win.setBackgroundDrawableResource(android.R.color.transparent)
                win.setDimAmount(0f)

                val lp = view.layoutParams
                if (lp != null) {
                    lp.width = android.view.WindowManager.LayoutParams.MATCH_PARENT
                    lp.height = android.view.WindowManager.LayoutParams.MATCH_PARENT
                    view.layoutParams = lp
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                win.attributes = win.attributes.apply {
                    layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            win.statusBarColor = android.graphics.Color.TRANSPARENT
            win.navigationBarColor = android.graphics.Color.TRANSPARENT

            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(win, false)

            val controller = androidx.core.view.WindowCompat.getInsetsController(win, win.decorView)
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            window?.let { win ->
                win.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                win.clearFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                win.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                
                val controller = androidx.core.view.WindowCompat.getInsetsController(win, win.decorView)
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // 1. Brightness Slider State
    var currentBrightness by remember { mutableFloatStateOf(0.8f) }
    LaunchedEffect(window) {
        window?.let {
            val lp = it.attributes
            lp.screenBrightness = 0.8f
            it.attributes = lp
        }
    }

    // 2. Idle Dimming State
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isIdleState by remember { mutableStateOf(false) }
    LaunchedEffect(lastInteractionTime) {
        isIdleState = false
        delay(10000) // 10s idle delay
        isIdleState = true
    }

    // 3. Swipe-up Peek Gestures State
    var showPeekStrip by remember { mutableStateOf(false) }
    LaunchedEffect(showPeekStrip) {
        if (showPeekStrip) {
            delay(3000) // auto-hide after 3s
            showPeekStrip = false
        }
    }

    var showAnalyticsSheet by remember { mutableStateOf(false) }
    var todaySessions by remember { mutableStateOf(emptyList<com.example.data.db.entity.SessionEntity>()) }
    val todaySessionsFlow = remember {
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEnd = todayStart + 86400000L - 1
        com.example.FocusFlowApplication.instance.sessionRepository.getSessionsInRange(todayStart, todayEnd)
    }
    LaunchedEffect(todaySessionsFlow) {
        todaySessionsFlow.collect {
            todaySessions = it
        }
    }

    // 4. Milestone Pulse Animation state
    val percentCompleted = ((1f - progress) * 100).toInt()
    var lastMilestoneTriggered by remember { mutableIntStateOf(0) }
    var showMilestoneFlash by remember { mutableStateOf(false) }

    LaunchedEffect(percentCompleted) {
        val milestone = when {
            percentCompleted >= 75 -> 75
            percentCompleted >= 50 -> 50
            percentCompleted >= 25 -> 25
            else -> 0
        }
        if (milestone > 0 && milestone != lastMilestoneTriggered) {
            lastMilestoneTriggered = milestone
            showMilestoneFlash = true
            try {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(showMilestoneFlash) {
        if (showMilestoneFlash) {
            delay(1000)
            showMilestoneFlash = false
        }
    }

    val flashAlpha by animateFloatAsState(
        targetValue = if (showMilestoneFlash) 0.15f else 0.0f,
        animationSpec = tween(500),
        label = "flash"
    )

    // 5. Infinite breathing gradient ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingAlpha"
    )
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    // 6. Idle dim animation
    val textAlphaMultiplier by animateFloatAsState(
        targetValue = if (isIdleState) 0.5f else 1.0f,
        animationSpec = tween(300),
        label = "idleDim"
    )

    val arcColor = when {
        phaseLabel.uppercase().contains("FOCUS") -> Color(0xFF9482FF) // Lavender violet
        phaseLabel.uppercase().contains("SHORT") -> Color(0xFF8BCA9A) // Green
        phaseLabel.uppercase().contains("LONG") -> Color(0xFFFF829C) // Red
        else -> Color(0xFF9482FF)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "saver_progress"
    )

    val powerManager = remember(context) { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isSystemPowerSaveMode = remember(powerManager) { powerManager?.isPowerSaveMode == true }

    var totalDragY by remember { mutableFloatStateOf(0f) }
    var showAudioPopup by remember { mutableStateOf(false) }

    val ambientIds = listOf("rain", "white_noise", "campfire", "stream", "space")
    val ambientLabels = mapOf(
        "rain" to "Rain 🌧️",
        "white_noise" to "White Noise 🌫️",
        "campfire" to "Campfire 🔥",
        "stream" to "Stream 🌊",
        "space" to "Space 🌌"
    )
    val currentAmbientLabel = ambientLabels[currentAmbientId] ?: "Rain 🌧️"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onDoubleTap = {
                        try {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                        } catch (e: Exception) {}
                        onDismiss()
                    },
                    onLongPress = {
                        lastInteractionTime = System.currentTimeMillis()
                        onPlayPauseClick()
                        try {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                        } catch (e: Exception) {}
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { totalDragY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        lastInteractionTime = System.currentTimeMillis()
                        totalDragY += dragAmount.y
                        if (totalDragY < -100f) {
                            if (!showAnalyticsSheet) {
                                showAnalyticsSheet = true
                                try {
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                } catch (e: Exception) {}
                            }
                            showPeekStrip = true
                        }
                    },
                    onDragEnd = {
                        totalDragY = 0f
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Breathing radial background pulse (subtle, AMOLED safe)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x0AFFFFFF), Color.Transparent),
                            center = center,
                            radius = (size.minDimension / 1.5f) * breathingScale
                        ),
                        alpha = breathingAlpha
                    )
                }
        )

        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            LandscapeBatterySaverLayout(
                batteryLevel = batteryLevel,
                textAlphaMultiplier = textAlphaMultiplier,
                remainingTimeFormatted = remainingTimeFormatted,
                phaseLabel = phaseLabel,
                arcColor = arcColor,
                animatedProgress = animatedProgress,
                radioIsPlaying = radioIsPlaying,
                radioStationName = radioStationName,
                currentAmbientId = currentAmbientId,
                currentAmbientLabel = currentAmbientLabel,
                isSystemPowerSaveMode = isSystemPowerSaveMode,
                sessionCount = sessionCount,
                totalFocusSecs = totalFocusSecs,
                isPlaying = isPlaying,
                ambientIds = ambientIds,
                onPlayPauseClick = onPlayPauseClick,
                onStopClick = onStopClick,
                onSkipClick = onSkipClick,
                onSelectRadio = onSelectRadio,
                onSelectAmbient = onSelectAmbient,
                onSelectNoAudio = onSelectNoAudio,
                onShowAudioPopup = { showAudioPopup = true },
                onInteraction = { lastInteractionTime = System.currentTimeMillis() },
                view = view
            )
        } else {
            PortraitBatterySaverLayout(
                batteryLevel = batteryLevel,
                textAlphaMultiplier = textAlphaMultiplier,
                remainingTimeFormatted = remainingTimeFormatted,
                phaseLabel = phaseLabel,
                arcColor = arcColor,
                animatedProgress = animatedProgress,
                radioIsPlaying = radioIsPlaying,
                radioStationName = radioStationName,
                currentAmbientId = currentAmbientId,
                currentAmbientLabel = currentAmbientLabel,
                isSystemPowerSaveMode = isSystemPowerSaveMode,
                sessionCount = sessionCount,
                totalFocusSecs = totalFocusSecs,
                isPlaying = isPlaying,
                ambientIds = ambientIds,
                onPlayPauseClick = onPlayPauseClick,
                onStopClick = onStopClick,
                onSkipClick = onSkipClick,
                onSelectRadio = onSelectRadio,
                onSelectAmbient = onSelectAmbient,
                onSelectNoAudio = onSelectNoAudio,
                onShowAudioPopup = { showAudioPopup = true },
                onInteraction = { lastInteractionTime = System.currentTimeMillis() },
                view = view
            )
        }

        // --- Brightness Dim Slider (Bar on the right side) ---
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(40.dp)
                .align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        lastInteractionTime = System.currentTimeMillis()
                        val delta = -dragAmount / 600f
                        val nextBright = (currentBrightness + delta).coerceIn(0.01f, 1.0f)
                        currentBrightness = nextBright
                        window?.let { w ->
                            val lp = w.attributes
                            lp.screenBrightness = nextBright
                            w.attributes = lp
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.3f)
                    .width(2.dp)
                    .background(Color(0x15FFFFFF))
                    .align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(currentBrightness)
                        .fillMaxWidth()
                        .background(Color(0x40FFFFFF))
                        .align(Alignment.BottomCenter)
                )
            }
        }

        // --- Translucent Swipe-up Peek Strip ---
        AnimatedVisibility(
            visible = showPeekStrip,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(CircleShape)
                    .background(Color(0xE0101010))
                    .border(1.dp, Color(0x25FFFFFF), CircleShape)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Next milestone: ${if (percentCompleted < 25) "25%" else if (percentCompleted < 50) "50%" else if (percentCompleted < 75) "75%" else "100%"}",
                        color = Color(0xB0FFFFFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "All Notifications Dimmed 🤫",
                        color = Color(0x70FFFFFF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        // --- Soft Milestone Flash Overlay ---
        if (flashAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha))
            )
        }

        // --- Custom Neomorphic Monochromatic Dialog Box ---
        AudioSelectorDialog(
            visible = showAudioPopup,
            allStations = allStations,
            favouriteIds = favouriteIds,
            currentAmbientId = currentAmbientId,
            currentAmbientLabel = currentAmbientLabel,
            radioIsPlaying = radioIsPlaying,
            radioStationName = radioStationName,
            ambientIds = ambientIds,
            ambientLabels = ambientLabels,
            onToggleFavourite = onToggleFavourite,
            onSelectRadioStation = onSelectRadioStation,
            onSelectAmbient = onSelectAmbient,
            onDismissRequest = { showAudioPopup = false }
        )

        FlowAnalyticsSheet(
            visible = showAnalyticsSheet,
            onDismiss = { showAnalyticsSheet = false },
            percentCompleted = percentCompleted,
            todaySessions = todaySessions
        )
    }
}

@Composable
fun FlowAnalyticsSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    percentCompleted: Int,
    todaySessions: List<com.example.data.db.entity.SessionEntity>
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            val sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clickable(enabled = false) {}
                    .background(Color(0xFF0F0F12), sheetShape)
                    .border(1.dp, Color(0x20FFFFFF), sheetShape)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color(0x30FFFFFF), CircleShape)
                        .align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FLOW ANALYTICS 📊",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Today's Focus Statistics & Achievements",
                            color = Color(0x70FFFFFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    androidx.compose.material3.IconButton(
                        onClick = { onDismiss() },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0x10FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Analytics",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    item {
                        val nextMilestone = when {
                            percentCompleted < 25 -> 25
                            percentCompleted < 50 -> 50
                            percentCompleted < 75 -> 75
                            else -> 100
                        }
                        
                        val prevMilestone = when (nextMilestone) {
                            25 -> 0
                            50 -> 25
                            75 -> 50
                            else -> 75
                        }

                        val milestoneProgress = ((percentCompleted - prevMilestone) / 25f).coerceIn(0f, 1f)
                        val inspirationalText = when (nextMilestone) {
                            25 -> "Keep it up! Your first milestone is just around the corner."
                            50 -> "Halfway there! Your concentration is outstanding today."
                            75 -> "You're in the deep focus zone. Just a bit more!"
                            else -> "Incredible job! Push through to finish the whole session."
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x06FFFFFF), RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0x12FFFFFF), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Milestone",
                                            tint = Color(0xFFFDCB6E),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "NEXT MILESTONE: $nextMilestone%",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    
                                    Text(
                                        text = "${percentCompleted}% / ${nextMilestone}%",
                                        color = Color(0xFFA29BFE),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .background(Color(0x10FFFFFF), CircleShape)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((percentCompleted / 100f).coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFFA29BFE), Color(0xFF74B9FF))
                                                ),
                                                CircleShape
                                            )
                                    )
                                }

                                Text(
                                    text = inspirationalText,
                                    color = Color(0x95FFFFFF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    item {
                        val completedCountToday = todaySessions.count { it.completed }
                        val totalFocusSecsToday = todaySessions.filter { it.completed }.sumOf { it.durationSeconds.toLong() }
                        val totalFocusMinsToday = totalFocusSecsToday / 60
                        val pointsToday = todaySessions.filter { it.completed }.sumOf { it.focusScore }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0x06FFFFFF), RoundedCornerShape(14.dp))
                                    .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "SESSIONS",
                                    color = Color(0x50FFFFFF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$completedCountToday",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .background(Color(0x06FFFFFF), RoundedCornerShape(14.dp))
                                    .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "FOCUS TIME",
                                    color = Color(0x50FFFFFF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val displayTime = if (totalFocusMinsToday >= 60) {
                                    "${totalFocusMinsToday / 60}h ${totalFocusMinsToday % 60}m"
                                } else {
                                    "${totalFocusMinsToday}m"
                                }
                                Text(
                                    text = displayTime,
                                    color = Color(0xFF55E6C1),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0x06FFFFFF), RoundedCornerShape(14.dp))
                                    .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "POINTS",
                                    color = Color(0x50FFFFFF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+$pointsToday",
                                    color = Color(0xFFFFD23F),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    item {
                        val calendar = remember { java.util.Calendar.getInstance() }
                        var nightMins = 0f
                        var morningMins = 0f
                        var afternoonMins = 0f
                        var eveningMins = 0f

                        todaySessions.filter { it.completed }.forEach { session ->
                            calendar.timeInMillis = session.date
                            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                            val mins = session.durationSeconds / 60f
                            when (hour) {
                                in 0..5 -> nightMins += mins
                                in 6..11 -> morningMins += mins
                                in 12..17 -> afternoonMins += mins
                                else -> eveningMins += mins
                            }
                        }

                        val maxVal = maxOf(nightMins, morningMins, afternoonMins, eveningMins, 25f)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x06FFFFFF), RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0x12FFFFFF), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "TODAY'S ACTIVITY BLOCKS",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            val blocks = listOf(
                                Triple("Morning (6 AM - 12 PM)", morningMins, Color(0xFFFF9F43)),
                                Triple("Afternoon (12 PM - 6 PM)", afternoonMins, Color(0xFF0984E3)),
                                Triple("Evening (6 PM - 12 AM)", eveningMins, Color(0xFF6C5CE7)),
                                Triple("Night (12 AM - 6 AM)", nightMins, Color(0xFF00CEC9))
                            )

                            blocks.forEach { (label, mins, color) ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = label,
                                            color = Color(0xB0FFFFFF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${mins.toInt()} mins",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    val ratio = (mins / maxVal).coerceIn(0f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .background(Color(0x0AFFFFFF), CircleShape)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(ratio)
                                                .fillMaxHeight()
                                                .background(color, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val completedSessions = todaySessions.filter { it.completed }
                    if (completedSessions.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x06FFFFFF), RoundedCornerShape(16.dp))
                                    .border(1.dp, Color(0x12FFFFFF), RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "TODAY'S SESSION LOGS",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                val sdf = remember { java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()) }

                                completedSessions.forEachIndexed { index, session ->
                                    val timeStr = sdf.format(java.util.Date(session.date))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(Color(0xFF55E6C1), CircleShape)
                                            )
                                            Text(
                                                text = "Session #${completedSessions.size - index}",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = "$timeStr — ${session.durationSeconds / 60} min",
                                            color = Color(0x70FFFFFF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                    if (index < completedSessions.lastIndex) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(Color(0x0AFFFFFF))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BatterySaverScreen(
    viewModel: TimerViewModel,
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.timerState.collectAsState()
    val context = LocalContext.current
    
    // Track battery level
    val batteryLevel = remember { mutableStateOf(100) }
    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    batteryLevel.value = (level * 100 / scale.toFloat()).toInt()
                }
            }
        }
        context.registerReceiver(receiver, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {}
        }
    }

    val radioViewModel: com.example.ui.screen.radio.RadioViewModel = viewModel()
    val radioPlaying by radioViewModel.isPlaying.collectAsState()
    val currentStation by radioViewModel.currentStation.collectAsState()

    // Keep the captured station in sync with the active or last selected station
    var capturedStation by remember { mutableStateOf<com.example.data.RadioStation?>(null) }

    LaunchedEffect(currentStation) {
        if (currentStation != null) {
            capturedStation = currentStation
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

    val allStations by radioViewModel.displayedStations.collectAsState()
    val favouriteIds by radioViewModel.favouriteIds.collectAsState()

    BatterySaverOverlay(
        onDismiss = onDismiss,
        batteryLevel = batteryLevel.value,
        remainingTimeFormatted = remainingText,
        radioStationName = capturedStation?.name,
        radioStationThumbnail = capturedStation?.logoUrl,
        radioIsPlaying = radioPlaying && capturedStation != null,
        currentAmbientId = state.currentAmbientId,
        allStations = allStations,
        favouriteIds = favouriteIds,
        onToggleFavourite = { station ->
            radioViewModel.toggleFavourite(station)
        },
        onSelectRadioStation = { station ->
            capturedStation = station
            radioViewModel.selectStation(station, context)
            viewModel.setAmbientSound("none")
        },
        onSelectRadio = {
            capturedStation?.let { station ->
                radioViewModel.selectStation(station, context)
            } ?: run {
                if (allStations.isNotEmpty()) {
                    val defaultStation = allStations.first()
                    capturedStation = defaultStation
                    radioViewModel.selectStation(defaultStation, context)
                }
            }
            viewModel.setAmbientSound("none")
        },
        onSelectAmbient = { ambientId ->
            viewModel.setAmbientSound(ambientId)
            radioViewModel.pausePlayback(context)
        },
        onSelectNoAudio = {
            viewModel.setAmbientSound("none")
            radioViewModel.pausePlayback(context)
        },
        onPlayPauseClick = {
            if (state.isRunning) {
                viewModel.pauseTimer()
            } else {
                viewModel.startTimer()
            }
        },
        onStopClick = { viewModel.stopTimer() },
        onSkipClick = { viewModel.skipPhase() },
        isPlaying = state.isRunning,
        sessionCount = state.sessionCount,
        totalFocusSecs = state.totalFocusSecs,
        phaseLabel = state.phase.label,
        progress = progress
    )
}


