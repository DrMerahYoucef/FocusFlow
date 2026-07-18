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
fun BatterySaverOverlay(
    onDismiss: () -> Unit,
    batteryLevel: Int,
    remainingTimeFormatted: String,
    radioStationName: String?,
    radioStationThumbnail: String?,
    radioIsPlaying: Boolean,
    currentAmbientId: String,
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

    val powerManager = remember(context) { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isSystemPowerSaveMode = remember(powerManager) { powerManager?.isPowerSaveMode == true }

    var totalDragY by remember { mutableFloatStateOf(0f) }

    val ambientIds = listOf("rain", "white_noise", "campfire", "stream", "space")
    val ambientLabels = mapOf(
        "rain" to "Rain 🌧️",
        "white_noise" to "White Noise 🌫️",
        "campfire" to "Campfire 🔥",
        "stream" to "Stream 🌊",
        "space" to "Space 🌌"
    )
    val currentAmbientLabel = ambientLabels[currentAmbientId] ?: "Rain 🌧️"

    @Composable
    fun AudioModeSelector(modifier: Modifier = Modifier) {
        val isRadioAvailable = !radioStationName.isNullOrBlank()
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
                if (isRadioAvailable) {
                    val isRadioActive = radioIsPlaying
                    val radioBorderColor = if (isRadioActive) Color(0x6081C784) else Color(0x15FFFFFF)
                    val radioBgColor = if (isRadioActive) Color(0x2081C784) else Color(0x05FFFFFF)
                    val radioTextColor = if (isRadioActive) Color(0xDF81C784) else Color(0x70FFFFFF)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(radioBgColor)
                            .border(1.dp, radioBorderColor, RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectRadio()
                                try {
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                } catch (e: Exception) {}
                            }
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (!radioStationThumbnail.isNullOrEmpty()) {
                            coil.compose.SubcomposeAsyncImage(
                                model = radioStationThumbnail,
                                contentDescription = "Radio thumbnail",
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .border(0.5.dp, Color(0x30FFFFFF), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = "Radio",
                                tint = radioTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = radioStationName ?: "Radio",
                            color = radioTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 2. Ambient Option
                val isAmbientActive = currentAmbientId != "none"
                val ambientBorderColor = if (isAmbientActive) Color(0x6064B5F6) else Color(0x15FFFFFF)
                val ambientBgColor = if (isAmbientActive) Color(0x2064B5F6) else Color(0x05FFFFFF)
                val ambientTextColor = if (isAmbientActive) Color(0xDF64B5F6) else Color(0x70FFFFFF)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(if (isRadioAvailable) 1.2f else 1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ambientBgColor)
                        .border(1.dp, ambientBorderColor, RoundedCornerShape(8.dp))
                        .clickable {
                            val nextId = if (isAmbientActive) {
                                val currentIndex = ambientIds.indexOf(currentAmbientId)
                                if (currentIndex == -1 || currentIndex == ambientIds.lastIndex) {
                                    ambientIds.first()
                                } else {
                                    ambientIds[currentIndex + 1]
                                }
                            } else {
                                "rain"
                            }
                            onSelectAmbient(nextId)
                            try {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                            } catch (e: Exception) {}
                        }
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Ambient",
                        tint = ambientTextColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAmbientActive) currentAmbientLabel else "Ambient",
                        color = ambientTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                // 3. No Audio Option
                val isSilenceActive = !radioIsPlaying && currentAmbientId == "none"
                val silenceBorderColor = if (isSilenceActive) Color(0x60E57373) else Color(0x15FFFFFF)
                val silenceBgColor = if (isSilenceActive) Color(0x20E57373) else Color(0x05FFFFFF)
                val silenceTextColor = if (isSilenceActive) Color(0xDFE57373) else Color(0x70FFFFFF)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(if (isRadioAvailable) 0.8f else 1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(silenceBgColor)
                        .border(1.dp, silenceBorderColor, RoundedCornerShape(8.dp))
                        .clickable {
                            onSelectNoAudio()
                            try {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                            } catch (e: Exception) {}
                        }
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeMute,
                        contentDescription = "Silence",
                        tint = silenceTextColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Silent",
                        color = silenceTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BATTERY SAVER ACTIVE",
                        color = Color(0x35FFFFFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.alpha(textAlphaMultiplier)
                    )
                    Text(
                        text = "$batteryLevel%",
                        color = Color(0x25FFFFFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.alpha(textAlphaMultiplier)
                    )
                }

                // Middle section: Left circular timer, Right stats and controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Circular Timer
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Large outer circular border
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .border(1.5.dp, Color(0x25FFFFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = when (phaseLabel.uppercase()) {
                                        "FOCUS" -> "FOCUS SESSION"
                                        "SHORT BREAK" -> "SHORT BREAK"
                                        "LONG BREAK" -> "LONG BREAK"
                                        else -> phaseLabel.uppercase()
                                    },
                                    color = Color(0x50FFFFFF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    modifier = Modifier.alpha(textAlphaMultiplier)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = remainingTimeFormatted,
                                    color = Color.White,
                                    fontSize = 52.sp,
                                    fontWeight = FontWeight.Light,
                                    modifier = Modifier.alpha(textAlphaMultiplier)
                                )
                            }
                        }
                    }

                    // Right: Stats & Controls Column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Session Overview & Stats Block
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "SESSION OVERVIEW",
                                color = Color(0x35FFFFFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier
                                    .padding(bottom = 6.dp)
                                    .alpha(textAlphaMultiplier)
                            )

                            val totalMins = totalFocusSecs / 60
                            val dailyGoalSecs = 3600
                            val progressPercent = ((totalFocusSecs.toFloat() / dailyGoalSecs.toFloat()) * 100).toInt().coerceIn(0, 100)

                            Row {
                                Text("FOCUS TIME: ", color = Color(0x35FFFFFF), fontSize = 14.sp, fontWeight = FontWeight.Normal)
                                Text("$totalMins min", color = Color(0x95FFFFFF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Row {
                                Text("SESSIONS: ", color = Color(0x35FFFFFF), fontSize = 14.sp, fontWeight = FontWeight.Normal)
                                Text("$sessionCount", color = Color(0x95FFFFFF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Row {
                                Text("DAILY GOAL: ", color = Color(0x35FFFFFF), fontSize = 14.sp, fontWeight = FontWeight.Normal)
                                Text("1 hr", color = Color(0x95FFFFFF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Row {
                                Text("Daily Goal progress: ", color = Color(0x35FFFFFF), fontSize = 14.sp, fontWeight = FontWeight.Normal)
                                Text("$progressPercent%", color = Color(0x95FFFFFF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Media and Control Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pause/Play
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        lastInteractionTime = System.currentTimeMillis()
                                        onPlayPauseClick()
                                        try {
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                        } catch (e: Exception) {}
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(1.dp, Color(0x25FFFFFF), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause Timer" else "Play Timer",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isPlaying) "Pause" else "Resume",
                                    color = Color(0x45FFFFFF),
                                    fontSize = 11.sp
                                )
                            }

                            // End Session / Stop
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        lastInteractionTime = System.currentTimeMillis()
                                        onStopClick()
                                        try {
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                        } catch (e: Exception) {}
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(1.dp, Color(0x25FFFFFF), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop Timer",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "End Session",
                                    color = Color(0x45FFFFFF),
                                    fontSize = 11.sp
                                )
                            }

                            // Next Track / Skip
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        lastInteractionTime = System.currentTimeMillis()
                                        onSkipClick()
                                        try {
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                        } catch (e: Exception) {}
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(1.dp, Color(0x25FFFFFF), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Skip Phase",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Next Track",
                                    color = Color(0x45FFFFFF),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Elegant Landscape Selector
                AudioModeSelector(modifier = Modifier.padding(top = 10.dp))
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

                // Center Timer with Circular Progress Ring and Breathing text
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .size(280.dp)
                ) {
                    Canvas(modifier = Modifier.size(260.dp)) {
                        drawCircle(
                            color = Color(0x10FFFFFF),
                            style = Stroke(width = 6.dp.toPx())
                        )
                        drawArc(
                            color = Color(0x30FFFFFF),
                            startAngle = -90f,
                            sweepAngle = 360f * (1f - progress),
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx())
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

                // Custom Audio mode selector in Portrait
                AudioModeSelector(modifier = Modifier.padding(bottom = 16.dp))

                // Stats section
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

                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.IconButton(
                        onClick = {
                            lastInteractionTime = System.currentTimeMillis()
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
                            lastInteractionTime = System.currentTimeMillis()
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
                            lastInteractionTime = System.currentTimeMillis()
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

                // Hint text
                Text(
                    text = "Double tap anywhere to return",
                    color = Color(0x40FFFFFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .navigationBarsPadding()
                        .alpha(textAlphaMultiplier)
                )
            }
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

    // Capture the playing station only if it is actively playing when opening
    var capturedStation by remember { mutableStateOf<com.example.data.RadioStation?>(null) }
    var hasCaptured by remember { mutableStateOf(false) }

    LaunchedEffect(currentStation, radioPlaying) {
        if (!hasCaptured) {
            if (radioPlaying && currentStation != null) {
                capturedStation = currentStation
                hasCaptured = true
            } else if (!radioPlaying) {
                capturedStation = null
                hasCaptured = true
            }
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

    BatterySaverOverlay(
        onDismiss = onDismiss,
        batteryLevel = batteryLevel.value,
        remainingTimeFormatted = remainingText,
        radioStationName = capturedStation?.name,
        radioStationThumbnail = capturedStation?.logoUrl,
        radioIsPlaying = radioPlaying && capturedStation != null,
        currentAmbientId = state.currentAmbientId,
        onSelectRadio = {
            capturedStation?.let { station ->
                radioViewModel.selectStation(station, context)
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


