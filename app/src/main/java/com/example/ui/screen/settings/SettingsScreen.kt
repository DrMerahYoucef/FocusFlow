package com.example.ui.screen.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.neumorphicShadow
import com.example.ui.theme.NeumorphicColors

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

        // Section: Upcoming Exams
        Text(
            text = "EXAM WORKLOAD",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = NeumorphicColors.TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Start
        )

        val examDao = remember { com.example.FocusFlowApplication.instance.database.examDao() }
        val examList by examDao.getAllExams().collectAsState(initial = emptyList())
        val examCount = examList.size

        NeumorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    navController?.navigate("exams")
                },
            cornerRadius = 16.dp,
            elevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
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

        Spacer(modifier = Modifier.height(24.dp))

        // Section: Durations
        Text(
            text = "TIMER INTERVALS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = NeumorphicColors.TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Start
        )

        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.3f))

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

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.3f))

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

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.3f))

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

        Spacer(modifier = Modifier.height(24.dp))

        // Section: System Behaviors
        Text(
            text = "SYSTEM SETTINGS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = NeumorphicColors.TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Start
        )

        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Block Notifications
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

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.3f))

                // Vibrate on complete
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

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.3f))

                // Ambient Sound Change Interval stepper
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

        Spacer(modifier = Modifier.height(24.dp))

        // Section: App Blocker
        Text(
            text = "APP BLOCKER",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = NeumorphicColors.TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Start
        )

        val db = remember { com.example.FocusFlowApplication.instance.database }
        val blockedList by db.blockedAppDao().getAllBlocked().collectAsState(initial = emptyList())
        val blockedCount = blockedList.size

        NeumorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    navController?.navigate("app_blocker")
                },
            cornerRadius = 16.dp,
            elevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
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

        Spacer(modifier = Modifier.height(24.dp))

        // Section: Data & Backup
        Text(
            text = "DATA & BACKUP",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = NeumorphicColors.TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Start
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
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

        // Section: Wallpapers
        Text(
            text = "FOREST WALLPAPER",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = NeumorphicColors.TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 12.dp),
            textAlign = TextAlign.Start
        )

        var isWallpaperApplying by remember { mutableStateOf(false) }

        val dbSessionCount by remember {
            com.example.FocusFlowApplication.instance.sessionRepository.getSessionCount(0L, Long.MAX_VALUE)
        }.collectAsState(initial = 0)

        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.3f))

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

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.3f))

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
                    Switch(
                        checked = state.autoSyncWallpaper,
                        onCheckedChange = { checked ->
                            viewModel.updateAutoSyncWallpaper(checked)
                            if (checked) {
                                // Instantly trigger sync
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

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.3f))

                if (isWallpaperApplying) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeumorphicColors.Accent)
                    }
                } else {
                    NeumorphicButton(
                        label = "Apply Current Forest Now",
                        icon = androidx.compose.material.icons.Icons.Default.Eco,
                        accentColor = NeumorphicColors.Accent,
                        onClick = {
                            if (!state.wallpaperHomeScreen && !state.wallpaperLockScreen) {
                                Toast.makeText(context, "Please select at least one screen!", Toast.LENGTH_SHORT).show()
                                return@NeumorphicButton
                            }
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

        // Section: Account / Logout
        Text(
            text = "ACCOUNT",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = NeumorphicColors.TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 12.dp),
            textAlign = TextAlign.Start
        )

        NeumorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    com.google.firebase.Firebase.auth.signOut()
                    navController?.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                },
            cornerRadius = 16.dp,
            elevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Logout,
                    contentDescription = "Logout icon",
                    tint = Color(0xFFFF6584),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Sign Out / Log Out",
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF6584),
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NeumorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    isDeleteAccountConfirmOpen = true
                },
            cornerRadius = 16.dp,
            elevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Block,
                    contentDescription = "Delete Account icon",
                    tint = Color(0xFFFF4D4D),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Delete My Account",
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF4D4D),
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section: System Update Center
        Text(
            text = "SYSTEM SELF-UPDATE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = NeumorphicColors.TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Start
        )

        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FocusFlow Version",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Current built: v${com.example.BuildConfig.VERSION_NAME} (Code: ${com.example.BuildConfig.VERSION_CODE})",
                            fontSize = 11.sp,
                            color = NeumorphicColors.TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Version information details",
                        tint = NeumorphicColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.3f))

                // Custom matching UI for each of the states in com.example.service.UpdateState
                when (updateState) {
                    is com.example.service.UpdateState.Idle -> {
                        Text(
                            text = "Manually request and fetch the latest FocusFlow version directly outside standard stores.",
                            fontSize = 12.sp,
                            color = NeumorphicColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
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
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = NeumorphicColors.Primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Checking Remote Config parameters...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicColors.TextPrimary
                            )
                        }
                    }
                    is com.example.service.UpdateState.UpToDate -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Up-to-Date Icon Check",
                                    tint = NeumorphicColors.Success,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Your application is fully up to date! 🚀",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeumorphicColors.TextPrimary
                                )
                            }
                            TextButton(onClick = { viewModel.checkForUpdates() }) {
                                Text("Check Again", color = NeumorphicColors.Primary)
                            }
                        }
                    }
                    is com.example.service.UpdateState.UpdateAvailable -> {
                        val available = updateState as com.example.service.UpdateState.UpdateAvailable
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "New Version Available! (vCode: ${available.latestVersionCode})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicColors.Primary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "A newer FocusFlow deployment package is ready in Firebase Storage. Click below to begin downloading.",
                                fontSize = 11.sp,
                                color = NeumorphicColors.TextSecondary,
                                textAlign = TextAlign.Center
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
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val percentText = if (progress >= 0f) "${(progress * 100).toInt()}%" else "Indeterminate"
                            Text(
                                text = "Downloading Update: $percentText",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicColors.TextPrimary
                            )
                            if (progress >= 0f) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = NeumorphicColors.Primary,
                                    trackColor = NeumorphicColors.SurfaceDark.copy(alpha = 0.3f)
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = NeumorphicColors.Primary,
                                    trackColor = NeumorphicColors.SurfaceDark.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                    is com.example.service.UpdateState.ReadyToInstall -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "APK verified and ready",
                                    tint = NeumorphicColors.Success,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Update downloaded & verified successfully!",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeumorphicColors.TextPrimary
                                )
                            }
                            Text(
                                text = "Secure FileProvider authority is successfully created. Grant package installation if requested by the OS.",
                                fontSize = 11.sp,
                                color = NeumorphicColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                NeumorphicButton(
                                    label = "Install Update",
                                    icon = Icons.Default.CloudDownload,
                                    accentColor = NeumorphicColors.Success,
                                    onClick = { viewModel.installUpdate() },
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { viewModel.resetUpdateState() },
                                    modifier = Modifier.weight(0.4f)
                                ) {
                                    Text("Discard", color = Color(0xFFFF6584))
                                }
                            }
                        }
                    }
                    is com.example.service.UpdateState.Error -> {
                        val errMsg = (updateState as com.example.service.UpdateState.Error).message
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error icon details",
                                    tint = NeumorphicColors.Accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Update Check Failed",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeumorphicColors.TextPrimary
                                )
                            }
                            Text(
                                text = errMsg,
                                fontSize = 11.sp,
                                color = NeumorphicColors.TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                NeumorphicButton(
                                    label = "Retry Check",
                                    icon = Icons.Default.Refresh,
                                    accentColor = NeumorphicColors.Primary,
                                    onClick = { viewModel.checkForUpdates() },
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { viewModel.resetUpdateState() },
                                    modifier = Modifier.weight(0.4f)
                                ) {
                                    Text("Dismiss", color = NeumorphicColors.TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Section: Developer Credentials
        Text(
            text = "AUTHOR & DEVELOPER",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = NeumorphicColors.TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Start
        )

        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile & Bio
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
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Text(
                    text = "Tel: +213558460474",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphicColors.Primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
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

        Spacer(modifier = Modifier.height(100.dp)) // Safe padding for bottom items
    }

    // Reset Confirmation Dialog dialog
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
            putExtra(Intent.EXTRA_SUBJECT, "FocusFlow Sessions Report")
            putExtra(Intent.EXTRA_TEXT, csvText)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Save focus statistics report via:"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to export data: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
