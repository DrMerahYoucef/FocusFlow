package com.example.ui.screen.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.neumorphicShadow
import com.example.ui.theme.NeumorphicColors
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun ExpandableSection(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onHeaderClick: () -> Unit,
    content: @Composable () -> Unit
) {
    NeumorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        cornerRadius = 16.dp,
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHeaderClick() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NeumorphicColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = title,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = NeumorphicColors.TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = NeumorphicColors.TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Divider(
                        color = NeumorphicColors.SurfaceDark.copy(alpha = 0.15f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    navController: androidx.navigation.NavController? = null
) {
    val state by viewModel.state.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var isResetConfirmOpen by remember { mutableStateOf(false) }
    var isDeleteAccountConfirmOpen by remember { mutableStateOf(false) }

    // State of each expandable section
    var isGeminiExpanded by remember { mutableStateOf(false) }
    var isExamsExpanded by remember { mutableStateOf(false) }
    var isTimerIntervalsExpanded by remember { mutableStateOf(false) }
    var isSystemSettingsExpanded by remember { mutableStateOf(false) }
    var isAppBlockerExpanded by remember { mutableStateOf(false) }
    var isDataBackupExpanded by remember { mutableStateOf(false) }
    var isWallpaperExpanded by remember { mutableStateOf(false) }
    var isAccountExpanded by remember { mutableStateOf(false) }
    var isUpdateExpanded by remember { mutableStateOf(false) }
    var isDeveloperExpanded by remember { mutableStateOf(false) }

    val currentGeminiKey by viewModel.geminiApiKey.collectAsState()

    // Data queries
    val examDao = remember { com.example.FocusFlowApplication.instance.database.examDao() }
    val examList by examDao.getAllExams().collectAsState(initial = emptyList())
    val examCount = examList.size

    val db = remember { com.example.FocusFlowApplication.instance.database }
    val blockedList by db.blockedAppDao().getAllBlocked().collectAsState(initial = emptyList())
    val blockedCount = blockedList.size

    val dbSessionCount by remember {
        com.example.FocusFlowApplication.instance.sessionRepository.getSessionCount(0L, Long.MAX_VALUE)
    }.collectAsState(initial = 0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeumorphicColors.Background)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Uppermost Header
        Text(
            text = "DASHBOARD CONFIG",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = NeumorphicColors.TextPrimary,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        )

        // 1. Section: Exam Countdown Workload
        ExpandableSection(
            title = "EXAM WORKLOAD",
            icon = Icons.Default.CalendarMonth,
            isExpanded = isExamsExpanded,
            onHeaderClick = { isExamsExpanded = !isExamsExpanded }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController?.navigate("exams") }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Exam Countdowns",
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicColors.TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "$examCount upcoming trials",
                        fontSize = 11.sp,
                        color = NeumorphicColors.TextSecondary
                    )
                }
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Configure Exam Countdown",
                    tint = NeumorphicColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 2. Section: Timer Intervals Option Sliders / Steppers
        ExpandableSection(
            title = "TIMER INTERVALS",
            icon = Icons.Default.Timer,
            isExpanded = isTimerIntervalsExpanded,
            onHeaderClick = { isTimerIntervalsExpanded = !isTimerIntervalsExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Focus Option stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Focus Duration",
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicColors.TextPrimary,
                        fontSize = 14.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (state.focusMin > 1) {
                                    viewModel.updateFocusMin(state.focusMin - 1)
                                }
                            }
                        ) {
                            Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.Primary)
                        }
                        Text(
                            text = "${state.focusMin} min",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = NeumorphicColors.TextPrimary
                        )
                        IconButton(
                            onClick = {
                                if (state.focusMin < 120) {
                                    viewModel.updateFocusMin(state.focusMin + 1)
                                }
                            }
                        ) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.Primary)
                        }
                    }
                }

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))

                // Short break option stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Short Break",
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicColors.TextPrimary,
                        fontSize = 14.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (state.shortBreakMin > 1) {
                                    viewModel.updateShortBreakMin(state.shortBreakMin - 1)
                                }
                            }
                        ) {
                            Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.Success)
                        }
                        Text(
                            text = "${state.shortBreakMin} min",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = NeumorphicColors.TextPrimary
                        )
                        IconButton(
                            onClick = {
                                if (state.shortBreakMin < 60) {
                                    viewModel.updateShortBreakMin(state.shortBreakMin + 1)
                                }
                            }
                        ) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.Success)
                        }
                    }
                }

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))

                // Long break option stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Long Break",
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicColors.TextPrimary,
                        fontSize = 14.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (state.longBreakMin > 1) {
                                    viewModel.updateLongBreakMin(state.longBreakMin - 1)
                                }
                            }
                        ) {
                            Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.Accent)
                        }
                        Text(
                            text = "${state.longBreakMin} min",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = NeumorphicColors.TextPrimary
                        )
                        IconButton(
                            onClick = {
                                if (state.longBreakMin < 60) {
                                    viewModel.updateLongBreakMin(state.longBreakMin + 1)
                                }
                            }
                        ) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.Accent)
                        }
                    }
                }

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))

                // Rounds before long break
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rounds Count",
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicColors.TextPrimary,
                        fontSize = 14.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (state.sessionsBeforeLong > 2) {
                                    viewModel.updateSessionsBeforeLong(state.sessionsBeforeLong - 1)
                                }
                            }
                        ) {
                            Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.TextSecondary)
                        }
                        Text(
                            text = "${state.sessionsBeforeLong}",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = NeumorphicColors.TextPrimary
                        )
                        IconButton(
                            onClick = {
                                if (state.sessionsBeforeLong < 10) {
                                    viewModel.updateSessionsBeforeLong(state.sessionsBeforeLong + 1)
                                }
                            }
                        ) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.TextSecondary)
                        }
                    }
                }
            }
        }

        // 3. Section: System Settings
        ExpandableSection(
            title = "SYSTEM SETTINGS",
            icon = Icons.Default.Settings,
            isExpanded = isSystemSettingsExpanded,
            onHeaderClick = { isSystemSettingsExpanded = !isSystemSettingsExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Block Notifications (DND)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Silenced Focus (DND)",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Automated Do Not Disturb block active on Focus",
                            fontSize = 11.sp,
                            color = NeumorphicColors.TextSecondary
                        )
                    }
                    Switch(
                        checked = state.blockNotifications,
                        onCheckedChange = { viewModel.updateBlockNotifications(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeumorphicColors.Primary,
                            checkedTrackColor = NeumorphicColors.Primary.copy(alpha = 0.5f)
                        )
                    )
                }

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))

                // Vibrate feedback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vibrations Feedback",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Double pulse vibrate when a phase ends",
                            fontSize = 11.sp,
                            color = NeumorphicColors.TextSecondary
                        )
                    }
                    Switch(
                        checked = state.vibrateOnComplete,
                        onCheckedChange = { viewModel.updateVibrateOnComplete(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeumorphicColors.Primary,
                            checkedTrackColor = NeumorphicColors.Primary.copy(alpha = 0.5f)
                        )
                    )
                }

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))

                // Swipe to Navigate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Swipe to Navigate",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Swipe left/right to switch screens",
                            fontSize = 11.sp,
                            color = NeumorphicColors.TextSecondary
                        )
                    }
                    Switch(
                        checked = state.swipeToNavigate,
                        onCheckedChange = { viewModel.updateSwipeToNavigate(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeumorphicColors.Primary,
                            checkedTrackColor = NeumorphicColors.Primary.copy(alpha = 0.5f)
                        )
                    )
                }

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))

                // Ambient sound switched interval
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sound Switch Interval",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Minutes before fading to another random sound",
                            fontSize = 11.sp,
                            color = NeumorphicColors.TextSecondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (state.ambientRotationMin > 1) {
                                    viewModel.updateAmbientRotationMin(state.ambientRotationMin - 1)
                                }
                            }
                        ) {
                            Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.Primary)
                        }
                        Text(
                            text = "${state.ambientRotationMin} min",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = NeumorphicColors.TextPrimary
                        )
                        IconButton(
                            onClick = {
                                if (state.ambientRotationMin < 120) {
                                    viewModel.updateAmbientRotationMin(state.ambientRotationMin + 1)
                                }
                            }
                        ) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.Primary)
                        }
                    }
                }
            }
        }

        // 3.5. Section: Gemini API Key
        ExpandableSection(
            title = "GEMINI API KEY",
            icon = Icons.Default.Key,
            isExpanded = isGeminiExpanded,
            onHeaderClick = { isGeminiExpanded = !isGeminiExpanded }
        ) {
            GeminiApiKeySetting(
                currentKey = currentGeminiKey,
                onSave = { key ->
                    viewModel.saveGeminiApiKey(key)
                },
                onClear = {
                    viewModel.clearGeminiApiKey()
                }
            )
        }

        // 4. Section: App Blocker Setup
        ExpandableSection(
            title = "APP BLOCKER",
            icon = Icons.Default.Block,
            isExpanded = isAppBlockerExpanded,
            onHeaderClick = { isAppBlockerExpanded = !isAppBlockerExpanded }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController?.navigate("app_blocker") }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Focus App Blocker",
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicColors.TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "$blockedCount apps configured",
                        fontSize = 11.sp,
                        color = NeumorphicColors.TextSecondary
                    )
                }
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Configure App Blocker",
                    tint = NeumorphicColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 5. Section: Data & Backup management
        ExpandableSection(
            title = "DATA & BACKUP",
            icon = Icons.Default.Storage,
            isExpanded = isDataBackupExpanded,
            onHeaderClick = { isDataBackupExpanded = !isDataBackupExpanded }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NeumorphicButton(
                    label = "Export CSV",
                    icon = Icons.Default.SaveAlt,
                    onClick = {
                        viewModel.exportSessionsAsCsv { csvString ->
                            shareCsvContent(context, csvString)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    accentColor = NeumorphicColors.Primary
                )

                NeumorphicButton(
                    label = "Clear DB",
                    icon = Icons.Default.DeleteSweep,
                    onClick = { isResetConfirmOpen = true },
                    modifier = Modifier.weight(1f),
                    accentColor = NeumorphicColors.Accent
                )
            }
        }

        // 6. Section: Forest Wallpapers Custom Setup
        ExpandableSection(
            title = "FOREST WALLPAPER",
            icon = Icons.Default.Eco,
            isExpanded = isWallpaperExpanded,
            onHeaderClick = { isWallpaperExpanded = !isWallpaperExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Generate and apply your actual Focus Forest as a beautiful high-resolution wallpaper!",
                    fontSize = 12.sp,
                    color = NeumorphicColors.TextSecondary,
                    lineHeight = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Set Home Screen",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Apply on phone home screen launcher",
                            fontSize = 11.sp,
                            color = NeumorphicColors.TextSecondary
                        )
                    }
                    Switch(
                        checked = state.wallpaperHomeScreen,
                        onCheckedChange = { viewModel.updateWallpaperHomeScreen(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeumorphicColors.Primary,
                            checkedTrackColor = NeumorphicColors.Primary.copy(alpha = 0.5f)
                        )
                    )
                }

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Set Lock Screen",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Apply on phone secure lock screen",
                            fontSize = 11.sp,
                            color = NeumorphicColors.TextSecondary
                        )
                    }
                    Switch(
                        checked = state.wallpaperLockScreen,
                        onCheckedChange = { viewModel.updateWallpaperLockScreen(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeumorphicColors.Primary,
                            checkedTrackColor = NeumorphicColors.Primary.copy(alpha = 0.5f)
                        )
                    )
                }

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Sync Wallpaper ☀️/🌙",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Automatically adjust to day/night theme and grow with your tree count!",
                            fontSize = 11.sp,
                            color = NeumorphicColors.TextSecondary
                        )
                    }
                    var isWallpaperApplying by remember { mutableStateOf(false) }
                    Switch(
                        checked = state.autoSyncWallpaper,
                        onCheckedChange = { checked ->
                            viewModel.updateAutoSyncWallpaper(checked)
                            if (checked) {
                                isWallpaperApplying = true
                                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                                val isDaytimeSetting = hour in 6..17
                                com.example.ui.components.WallpaperHelper.setForestWallpaper(
                                    context = context,
                                    isDay = isDaytimeSetting,
                                    treeCount = dbSessionCount,
                                    setHomeScreen = state.wallpaperHomeScreen,
                                    setLockScreen = state.wallpaperLockScreen
                                ) { success, error ->
                                    isWallpaperApplying = false
                                    if (success) {
                                        context.getSharedPreferences("focusflow_prefs", android.content.Context.MODE_PRIVATE)
                                            .edit()
                                            .putBoolean("last_synced_daytime", isDaytimeSetting)
                                            .putInt("last_synced_tree_count", dbSessionCount)
                                            .apply()
                                        Toast.makeText(context, "Auto-Sync Enabled & Wallpaper applied! 🌲", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Auto-Sync Enabled: failed to apply initial wallpaper: $error", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeumorphicColors.Primary,
                            checkedTrackColor = NeumorphicColors.Primary.copy(alpha = 0.5f)
                        )
                    )
                }

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Location-Based Day/Night 🌍",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (state.useLocationForDayNight && state.latitude != 0.0) {
                                "Active: Sunrise/Sunset calculated for lat: ${"%.2f".format(state.latitude)}, lng: ${"%.2f".format(state.longitude)}"
                            } else {
                                "Automatically switch day/night theme based on your current local sunrise and sunset times."
                            },
                            fontSize = 11.sp,
                            color = NeumorphicColors.TextSecondary
                        )
                    }
                    
                    val locationPermissionsState = rememberMultiplePermissionsState(
                        permissions = listOf(
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )

                    var isFetchingLocation by remember { mutableStateOf(false) }

                    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
                        if (state.useLocationForDayNight && locationPermissionsState.allPermissionsGranted) {
                            isFetchingLocation = true
                            viewModel.fetchAndSaveLocation { success ->
                                isFetchingLocation = false
                                if (success) {
                                    Toast.makeText(context, "Location updated successfully! 🌲", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to get current location. Using fallback schedule.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    if (isFetchingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = NeumorphicColors.Primary
                        )
                    } else {
                        Switch(
                            checked = state.useLocationForDayNight,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (locationPermissionsState.allPermissionsGranted) {
                                        viewModel.updateUseLocationForDayNight(true)
                                        isFetchingLocation = true
                                        viewModel.fetchAndSaveLocation { success ->
                                            isFetchingLocation = false
                                            if (success) {
                                                Toast.makeText(context, "Location-based day/night activated! ☀️🌙", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Activated with fallback schedule (could not fetch location).", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        locationPermissionsState.launchMultiplePermissionRequest()
                                        viewModel.updateUseLocationForDayNight(true)
                                    }
                                } else {
                                    viewModel.updateUseLocationForDayNight(false)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeumorphicColors.Primary,
                                checkedTrackColor = NeumorphicColors.Primary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))

                var isWallpaperApplyingNow by remember { mutableStateOf(false) }
                if (isWallpaperApplyingNow) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeumorphicColors.Accent)
                    }
                } else {
                    NeumorphicButton(
                        label = "Apply Current Forest Now",
                        icon = Icons.Default.Eco,
                        accentColor = NeumorphicColors.Accent,
                        onClick = {
                            if (!state.wallpaperHomeScreen && !state.wallpaperLockScreen) {
                                Toast.makeText(context, "Please select at least one screen!", Toast.LENGTH_SHORT).show()
                                return@NeumorphicButton
                            }
                            isWallpaperApplyingNow = true
                            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                            val isDaytimeSetting = hour in 6..17

                            com.example.ui.components.WallpaperHelper.setForestWallpaper(
                                context = context,
                                isDay = isDaytimeSetting,
                                treeCount = dbSessionCount,
                                setHomeScreen = state.wallpaperHomeScreen,
                                setLockScreen = state.wallpaperLockScreen
                            ) { success, error ->
                                isWallpaperApplyingNow = false
                                if (success) {
                                    context.getSharedPreferences("focusflow_prefs", android.content.Context.MODE_PRIVATE)
                                        .edit()
                                        .putBoolean("last_synced_daytime", isDaytimeSetting)
                                        .putInt("last_synced_tree_count", dbSessionCount)
                                        .apply()
                                    Toast.makeText(context, "Forest Wallpaper Applied Successfully! 🌲", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Failed to apply wallpaper: $error", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 7. Section: Account Controls
        ExpandableSection(
            title = "ACCOUNT CONTROLS",
            icon = Icons.Default.AccountCircle,
            isExpanded = isAccountExpanded,
            onHeaderClick = { isAccountExpanded = !isAccountExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NeumorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            com.google.firebase.Firebase.auth.signOut()
                            navController?.navigate("auth") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                    cornerRadius = 12.dp,
                    elevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout icon",
                            tint = Color(0xFFFF6584),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Sign Out / Log Out",
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF6584),
                            fontSize = 14.sp
                        )
                    }
                }

                NeumorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isDeleteAccountConfirmOpen = true
                        },
                    cornerRadius = 12.dp,
                    elevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Delete Account icon",
                            tint = Color(0xFFFF4D4D),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Delete My Account",
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF4D4D),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // 8. Section: System Update Center
        ExpandableSection(
            title = "SYSTEM UPDATE CENTER",
            icon = Icons.Default.CloudDownload,
            isExpanded = isUpdateExpanded,
            onHeaderClick = { isUpdateExpanded = !isUpdateExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Focus Island Version",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Current: v${com.example.BuildConfig.VERSION_NAME} (Code: ${com.example.BuildConfig.VERSION_CODE})",
                            fontSize = 11.sp,
                            color = NeumorphicColors.TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Version details",
                        tint = NeumorphicColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))

                when (updateState) {
                    is com.example.service.UpdateState.Idle -> {
                        Text(
                            text = "Check and fetch the latest Focus Island build parameters.",
                            fontSize = 11.sp,
                            color = NeumorphicColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        NeumorphicButton(
                            label = "Check for Updates",
                            icon = Icons.Default.Refresh,
                            accentColor = NeumorphicColors.Primary,
                            onClick = { viewModel.checkForUpdates() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is com.example.service.UpdateState.Checking -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                color = NeumorphicColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Checking for updates...",
                                fontSize = 11.sp,
                                color = NeumorphicColors.TextPrimary
                            )
                        }
                    }
                    is com.example.service.UpdateState.UpToDate -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Up to date",
                                    tint = NeumorphicColors.Success,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Application is up to date!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeumorphicColors.TextPrimary
                                )
                            }
                            TextButton(onClick = { viewModel.checkForUpdates() }) {
                                Text("Check Again", color = NeumorphicColors.Primary, fontSize = 12.sp)
                            }
                        }
                    }
                    is com.example.service.UpdateState.UpdateAvailable -> {
                        val available = updateState as com.example.service.UpdateState.UpdateAvailable
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "New Version Ready (Code: ${available.latestVersionCode})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicColors.Primary
                            )
                            NeumorphicButton(
                                label = "Download & Install Update",
                                icon = Icons.Default.CloudDownload,
                                accentColor = NeumorphicColors.Success,
                                onClick = { viewModel.downloadUpdate(available.downloadUrl) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    is com.example.service.UpdateState.Downloading -> {
                        val progress = (updateState as com.example.service.UpdateState.Downloading).progress
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val percentText = if (progress >= 0f) "${(progress * 100).toInt()}%" else "..."
                            Text(
                                text = "Downloading: $percentText",
                                fontSize = 11.sp,
                                color = NeumorphicColors.TextPrimary
                            )
                            LinearProgressIndicator(
                                progress = { if (progress >= 0f) progress else 0f },
                                modifier = Modifier.fillMaxWidth(),
                                color = NeumorphicColors.Primary,
                                trackColor = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f)
                            )
                        }
                    }
                    is com.example.service.UpdateState.ReadyToInstall -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Download complete and ready to install!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicColors.Success
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                NeumorphicButton(
                                    label = "Install",
                                    icon = Icons.Default.CloudDownload,
                                    accentColor = NeumorphicColors.Success,
                                    onClick = { viewModel.installUpdate() },
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { viewModel.resetUpdateState() },
                                    modifier = Modifier.weight(0.4f)
                                ) {
                                    Text("Discard", color = Color(0xFFFF6584), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    is com.example.service.UpdateState.Error -> {
                        val errMsg = (updateState as com.example.service.UpdateState.Error).message
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Check Failed: $errMsg",
                                fontSize = 11.sp,
                                color = NeumorphicColors.Accent,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                NeumorphicButton(
                                    label = "Retry",
                                    icon = Icons.Default.Refresh,
                                    accentColor = NeumorphicColors.Primary,
                                    onClick = { viewModel.checkForUpdates() },
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { viewModel.resetUpdateState() },
                                    modifier = Modifier.weight(0.4f)
                                ) {
                                    Text("Dismiss", color = NeumorphicColors.TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 9. Section: Developer Credentials Bio
        ExpandableSection(
            title = "AUTHOR & DEVELOPER",
            icon = Icons.Default.Info,
            isExpanded = isDeveloperExpanded,
            onHeaderClick = { isDeveloperExpanded = !isDeveloperExpanded }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Dr Merah Youcef",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = NeumorphicColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Orthopedic surgeon by profession, programmer at heart.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeumorphicColors.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Tel: +213558460474",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphicColors.Primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // WhatsApp Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .neumorphicShadow(
                                cornerRadius = 12.dp,
                                elevation = 4.dp,
                                isPressed = false
                            )
                            .clickable {
                                try {
                                    val waIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/213558460474"))
                                    context.startActivity(waIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = com.example.R.drawable.ic_whatsapp),
                                contentDescription = "WhatsApp icon",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "contact me",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NeumorphicColors.TextPrimary
                            )
                        }
                    }

                    // Facebook Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .neumorphicShadow(
                                cornerRadius = 12.dp,
                                elevation = 4.dp,
                                isPressed = false
                            )
                            .clickable {
                                try {
                                    val fbIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.facebook.com/youcef.Merahh/"))
                                    context.startActivity(fbIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open Facebook: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = com.example.R.drawable.ic_facebook),
                                contentDescription = "Facebook icon",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "contact me",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NeumorphicColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp)) // Safe bottom padding
    }

    // Reset Confirmation Dialog
    if (isResetConfirmOpen) {
         val dialogContext = LocalContext.current
         AlertDialog(
             onDismissRequest = { isResetConfirmOpen = false },
             containerColor = NeumorphicColors.DialogBackground,
             title = {
                Text(
                    text = "Confirm Hard Reset",
                    fontWeight = FontWeight.Black,
                    color = NeumorphicColors.TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will irreversibly delete all recorded completed sessions, statistics, streak logs and exam countdown schedules. Continue?",
                    color = NeumorphicColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData {
                            Toast.makeText(dialogContext, "Data successfully purged!", Toast.LENGTH_SHORT).show()
                            isResetConfirmOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Accent)
                ) {
                    Text("Delete Everything", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { isResetConfirmOpen = false }) {
                    Text("Cancel", color = NeumorphicColors.TextSecondary)
                }
            }
         )
    }

    // Delete Account Confirmation Dialog
    if (isDeleteAccountConfirmOpen) {
         val dialogContext = LocalContext.current
         AlertDialog(
             onDismissRequest = { isDeleteAccountConfirmOpen = false },
             containerColor = NeumorphicColors.DialogBackground,
             title = {
                Text(
                    text = "Delete Your Account?",
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF4D4D)
                )
            },
            text = {
                Text(
                    text = "WARNING: This will permanently block and delete your user account, remove your leaderboard rank, clear your online tree count, and delete all related remote & local database data. This action cannot be undone. Are you absolutely sure?",
                    color = NeumorphicColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount { success, errorMsg ->
                            if (success) {
                                Toast.makeText(dialogContext, "Account permanently deleted.", Toast.LENGTH_LONG).show()
                                isDeleteAccountConfirmOpen = false
                                navController?.navigate("auth") {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else {
                                Toast.makeText(dialogContext, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                                isDeleteAccountConfirmOpen = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4D))
                ) {
                    Text("Delete Account Permanently", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteAccountConfirmOpen = false }) {
                    Text("Cancel", color = NeumorphicColors.TextSecondary)
                }
            }
         )
    }
}

private fun shareCsvContent(context: Context, csvText: String) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Focus Island Sessions Report")
            putExtra(Intent.EXTRA_TEXT, csvText)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Save focus statistics report via:"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to export data: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun GeminiApiKeySetting(
    currentKey: String?,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var keyInput by remember(currentKey) { mutableStateOf(currentKey ?: "") }
    var isVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Gemini API Key",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = NeumorphicColors.TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            singleLine = true,
            placeholder = { Text("Paste your Gemini API key here") },
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isVisible = !isVisible }) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isVisible) "Hide Key" else "Show Key",
                        tint = NeumorphicColors.TextSecondary
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeumorphicColors.Primary,
                unfocusedBorderColor = NeumorphicColors.SurfaceDark.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Used for AI-powered features. Your key is stored only on this device.",
            style = MaterialTheme.typography.labelSmall,
            color = NeumorphicColors.TextSecondary
        )
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    onSave(keyInput)
                    Toast.makeText(context, "Gemini API key saved", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Primary)
            ) {
                Text("Save", color = Color.White)
            }
            OutlinedButton(
                onClick = {
                    keyInput = ""
                    onClear()
                    Toast.makeText(context, "Gemini API key cleared", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("Clear", color = NeumorphicColors.TextPrimary)
            }
        }
    }
}
