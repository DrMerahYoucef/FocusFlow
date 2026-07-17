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

    var batterySaverActive by rememberSaveable { mutableStateOf(false) }
    var isBatterySaverVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(batterySaverActive) {
        if (batterySaverActive) {
            isBatterySaverVisible = true
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
                    onClick = { batterySaverActive = true },
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

        if (batterySaverActive) {
            Dialog(
                onDismissRequest = {
                    coroutineScope.launch {
                        isBatterySaverVisible = false
                        delay(300)
                        batterySaverActive = false
                    }
                },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false
                )
            ) {
                val dialogView = LocalView.current
                val dialogWindow = remember(dialogView) {
                    var parent = dialogView.parent
                    var windowProvider: androidx.compose.ui.window.DialogWindowProvider? = null
                    while (parent != null) {
                        if (parent is androidx.compose.ui.window.DialogWindowProvider) {
                            windowProvider = parent
                            break
                        }
                        parent = parent.parent
                    }
                    val window = windowProvider?.window
                    if (window != null) {
                        window
                    } else {
                        var context = dialogView.context
                        while (context is android.content.ContextWrapper) {
                            if (context is android.app.Activity) {
                                break
                            }
                            context = context.baseContext
                        }
                        (context as? android.app.Activity)?.window
                    }
                }

                val ctx = LocalContext.current
                val batteryLevel = remember { mutableStateOf(100) }

                DisposableEffect(dialogWindow) {
                    // 5 - Listen to real battery percentage
                    val receiver = object : android.content.BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                            val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                            if (level != -1 && scale != -1) {
                                batteryLevel.value = (level * 100 / scale.toFloat()).toInt()
                            }
                        }
                    }
                    ctx.registerReceiver(receiver, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))

                    if (dialogWindow != null) {
                        // 2 - Immersive absolute fullscreen (no system or navigation bars)
                        dialogWindow.setLayout(
                            android.view.WindowManager.LayoutParams.MATCH_PARENT,
                            android.view.WindowManager.LayoutParams.MATCH_PARENT
                        )
                        dialogWindow.setBackgroundDrawableResource(android.R.color.transparent)
                        dialogWindow.setDimAmount(0f)
                        
                        // Keep screen on
                        dialogWindow.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                        // Set full screen layout flags to cover notch / status bar and draw behind status/nav bars
                        dialogWindow.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)

                        val controller = androidx.core.view.WindowCompat.getInsetsController(dialogWindow, dialogWindow.decorView)
                        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }

                    onDispose {
                        try {
                            ctx.unregisterReceiver(receiver)
                        } catch (e: Exception) {
                            // ignore
                        }
                        if (dialogWindow != null) {
                            dialogWindow.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            dialogWindow.clearFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                        }
                    }
                }

                // 4 - Animate entering and exiting
                AnimatedVisibility(
                    visible = isBatterySaverVisible,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        coroutineScope.launch {
                                            isBatterySaverVisible = false
                                            delay(300)
                                            batterySaverActive = false
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val activeAudioName = if (radioPlaying && currentStation != null) {
                            "Radio: ${currentStation?.name}"
                        } else if (state.currentAmbientId != "none") {
                            val ambientLabel = when (state.currentAmbientId) {
                                "rain" -> "Rain 🌧️"
                                "white_noise" -> "White Noise 🌫️"
                                "campfire" -> "Campfire 🔥"
                                "stream" -> "Stream 🌊"
                                "space" -> "Space Ambient 🌌"
                                else -> "Ambient"
                            }
                            "Playing: $ambientLabel"
                        } else {
                            "No Audio Playing"
                        }

                        val configuration = LocalConfiguration.current
                        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

                        if (isLandscape) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 48.dp, vertical = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Column: Timer info & active audio
                                Column(
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    // Top status line inside Column (OLED text removed, battery percentage added)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "BATTERY SAVER ACTIVE 🔋",
                                            color = Color(0x60FFFFFF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "${batteryLevel.value}%",
                                            color = Color(0x80FFFFFF),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }

                                    // Center big remaining time
                                    Column(
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = state.phase.label.uppercase(),
                                            color = Color(0xB0FFFFFF),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 3.sp
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = remainingText,
                                            color = Color.White,
                                            style = MaterialTheme.typography.displayLarge.copy(
                                                fontSize = 72.sp,
                                                fontWeight = FontWeight.Thin
                                            )
                                        )
                                    }

                                    // Active audio status
                                    Text(
                                        text = activeAudioName,
                                        color = Color(0x60FFFFFF),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Divider
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(1.dp)
                                        .padding(vertical = 16.dp)
                                        .background(Color(0x1AFFFFFF))
                                )

                                // Right Column: Controls & Stats
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Quick Stats
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "COMPLETED",
                                                color = Color(0x50FFFFFF),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                            Text(
                                                text = "${state.sessionCount} sessions",
                                                color = Color(0xD0FFFFFF),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "FOCUS TIME",
                                                color = Color(0x50FFFFFF),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                            val totalMins = state.totalFocusSecs / 60
                                            Text(
                                                text = "$totalMins min",
                                                color = Color(0xD0FFFFFF),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    // 3 - Control Buttons: Play/Pause, Stop, Skip
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Play / Pause Button
                                        val isRunning = state.isRunning
                                        androidx.compose.material3.IconButton(
                                            onClick = {
                                                if (isRunning) {
                                                    viewModel.pauseTimer()
                                                } else {
                                                    viewModel.startTimer()
                                                }
                                            },
                                            modifier = Modifier
                                                .size(54.dp)
                                                .border(1.dp, Color(0x40FFFFFF), CircleShape)
                                                .background(Color(0x10FFFFFF), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isRunning) "Pause Timer" else "Play Timer",
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        // Stop Button
                                        androidx.compose.material3.IconButton(
                                            onClick = { viewModel.stopTimer() },
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

                                        // Skip Button
                                        androidx.compose.material3.IconButton(
                                            onClick = { viewModel.skipPhase() },
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

                                    // Hint
                                    Text(
                                        text = "Double tap anywhere to return",
                                        color = Color(0x40FFFFFF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                        } else {
                            // PORTRAIT LAYOUT
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp, vertical = 32.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Top Header
                                Row(
                                    modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "BATTERY SAVER ACTIVE 🔋",
                                        color = Color(0x60FFFFFF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "${batteryLevel.value}%",
                                        color = Color(0x80FFFFFF),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                // Center Timer
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = state.phase.label.uppercase(),
                                        color = Color(0xB0FFFFFF),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 3.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = remainingText,
                                        color = Color.White,
                                        style = MaterialTheme.typography.displayLarge.copy(
                                            fontSize = 80.sp,
                                            fontWeight = FontWeight.Thin
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = activeAudioName,
                                        color = Color(0x60FFFFFF),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                // Stats section
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "COMPLETED",
                                            color = Color(0x50FFFFFF),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "${state.sessionCount} sessions",
                                            color = Color(0xD0FFFFFF),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
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
                                            letterSpacing = 1.sp
                                        )
                                        val totalMins = state.totalFocusSecs / 60
                                        Text(
                                            text = "$totalMins min",
                                            color = Color(0xD0FFFFFF),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Control Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isRunning = state.isRunning
                                    androidx.compose.material3.IconButton(
                                        onClick = {
                                            if (isRunning) {
                                                viewModel.pauseTimer()
                                            } else {
                                                viewModel.startTimer()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(54.dp)
                                            .border(1.dp, Color(0x40FFFFFF), CircleShape)
                                            .background(Color(0x10FFFFFF), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isRunning) "Pause Timer" else "Play Timer",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    androidx.compose.material3.IconButton(
                                        onClick = { viewModel.stopTimer() },
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
                                        onClick = { viewModel.skipPhase() },
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

                                // Hint text
                                Text(
                                    text = "Double tap anywhere to return",
                                    color = Color(0x40FFFFFF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(bottom = 8.dp).navigationBarsPadding()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
