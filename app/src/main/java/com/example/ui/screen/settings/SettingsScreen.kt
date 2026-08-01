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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.neumorphicShadow
import com.example.ui.theme.NeumorphicColors
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.example.data.backup.BackupTaskState
import com.example.data.backup.BackupTaskType
import com.example.data.backup.CardBackupStateHolder
import com.example.data.backup.CardTypeFilter
import com.example.data.repository.ImportMode
import com.example.service.CardBackupService
import androidx.compose.material.icons.filled.Wallpaper
import com.example.service.DayNightLiveWallpaperService
import com.example.widget.ExamCountdownWidgetReceiver
import com.example.widget.ExamMatrixWidgetReceiver
import java.io.File
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

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
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isResetConfirmOpen by remember { mutableStateOf(false) }
    var isDeleteAccountConfirmOpen by remember { mutableStateOf(false) }

    // State of each expandable section
    var isExamsExpanded by remember { mutableStateOf(false) }
    var isTimerIntervalsExpanded by remember { mutableStateOf(false) }
    var isSystemSettingsExpanded by remember { mutableStateOf(false) }
    var isAppBlockerExpanded by remember { mutableStateOf(false) }
    var isDataBackupExpanded by remember { mutableStateOf(false) }
    var isWallpaperExpanded by remember { mutableStateOf(false) }
    var isAccountExpanded by remember { mutableStateOf(false) }
    var isUpdateExpanded by remember { mutableStateOf(false) }
    var isDeveloperExpanded by remember { mutableStateOf(false) }

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
            .background(androidx.compose.ui.graphics.Color.Transparent)
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

                HorizontalDivider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.2f))

                Text(
                    text = "ADD WIDGET TO PHONE HOME SCREEN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = NeumorphicColors.TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                val mgr = android.appwidget.AppWidgetManager.getInstance(context)
                                if (mgr.isRequestPinAppWidgetSupported) {
                                    val provider = android.content.ComponentName(context, ExamCountdownWidgetReceiver::class.java)
                                    mgr.requestPinAppWidget(provider, null, null)
                                    Toast.makeText(context, "Adding Countdown & Stats Widget...", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Pinning widgets not supported by launcher.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Long-press home screen to pick widgets.", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Primary)
                    ) {
                        Icon(imageVector = Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stats Widget", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                val mgr = android.appwidget.AppWidgetManager.getInstance(context)
                                if (mgr.isRequestPinAppWidgetSupported) {
                                    val provider = android.content.ComponentName(context, ExamMatrixWidgetReceiver::class.java)
                                    mgr.requestPinAppWidget(provider, null, null)
                                    Toast.makeText(context, "Adding Matrix Calendar Widget...", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Pinning widgets not supported by launcher.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Long-press home screen to pick widgets.", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Matrix Widget", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
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
        var isSrsSettingsExpanded by remember { mutableStateOf(false) }
        var isExportOptionsDialogOpen by remember { mutableStateOf(false) }
        var selectedExportCardType by remember { mutableStateOf(CardTypeFilter.ALL) }
        var selectedExportDeckId by remember { mutableStateOf("ALL") }

        var isImportConfirmDialogOpen by remember { mutableStateOf(false) }
        var selectedImportUri by remember { mutableStateOf<Uri?>(null) }
        var selectedImportMode by remember { mutableStateOf(ImportMode.MERGE) }

        val revisionsViewModel: com.example.ui.screen.revisions.RevisionsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        val srsUiState by revisionsViewModel.uiState.collectAsState()
        val backupState by CardBackupStateHolder.state.collectAsState()

        val saveDocumentLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/octet-stream")
        ) { destinationUri ->
            destinationUri?.let { uri ->
                if (backupState is BackupTaskState.ExportCompleted) {
                    val exportedFile = (backupState as BackupTaskState.ExportCompleted).backupFile
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            exportedFile.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                        Toast.makeText(context, "Backup saved to phone!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error saving backup file: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        val backupPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                selectedImportUri = it
                isImportConfirmDialogOpen = true
            }
        }

        ExpandableSection(
            title = "REVISIONS & GEMINI API KEY",
            icon = Icons.Default.AutoAwesome,
            isExpanded = isSrsSettingsExpanded,
            onHeaderClick = { isSrsSettingsExpanded = !isSrsSettingsExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Gemini API Key Field
                var localGeminiKey by remember(srsUiState.srsSettings.geminiApiKey) { mutableStateOf(srsUiState.srsSettings.geminiApiKey) }

                Text(
                    text = "Gemini API Key (OCR Cards)",
                    fontWeight = FontWeight.Bold,
                    color = NeumorphicColors.TextPrimary,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = localGeminiKey,
                    onValueChange = { localGeminiKey = it },
                    label = { Text("Gemini API Key (AI Studio)") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NeumorphicColors.TextPrimary,
                        unfocusedTextColor = NeumorphicColors.TextPrimary,
                        focusedBorderColor = NeumorphicColors.Primary,
                        unfocusedBorderColor = NeumorphicColors.SurfaceDark.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    NeumorphicButton(
                        label = "Save Key Locally",
                        icon = Icons.Default.Save,
                        onClick = {
                            revisionsViewModel.setGeminiApiKey(localGeminiKey)
                            Toast.makeText(context, "Gemini API key saved locally!", Toast.LENGTH_SHORT).show()
                        },
                        accentColor = NeumorphicColors.Primary
                    )
                }

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))

                // --- Inner Small Expandable: Gemini Model Selection & Model Verification ---
                var isModelsSectionExpanded by remember { mutableStateOf(false) }
                var modelTestStatuses by remember { mutableStateOf<Map<String, com.example.data.repository.GeminiModelTestStatus>>(emptyMap()) }
                var isVerifyingModels by remember { mutableStateOf(false) }
                var isAddModelDialogOpen by remember { mutableStateOf(false) }
                var newModelInput by remember { mutableStateOf("") }

                Card(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NeumorphicColors.Background.copy(alpha = 0.7f)),
                    border = BorderStroke(1.dp, NeumorphicColors.SurfaceDark.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Expandable Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isModelsSectionExpanded = !isModelsSectionExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = NeumorphicColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Gemini Models & API Compatibility",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = NeumorphicColors.TextPrimary
                                    )
                                    Text(
                                        text = "Active: ${srsUiState.srsSettings.selectedGeminiModel}",
                                        fontSize = 11.sp,
                                        color = NeumorphicColors.Primary
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isModelsSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isModelsSectionExpanded) "Collapse" else "Expand",
                                tint = NeumorphicColors.TextSecondary
                            )
                        }

                        if (isModelsSectionExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Test / Verify Models action column
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Select model & test API key compatibility:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NeumorphicColors.TextSecondary
                                )

                                NeumorphicButton(
                                    label = if (isVerifyingModels) "Verifying Models..." else "Check All Models",
                                    icon = if (isVerifyingModels) Icons.Default.Sync else Icons.Default.FactCheck,
                                    onClick = {
                                        val keyToTest = localGeminiKey.ifBlank { srsUiState.srsSettings.geminiApiKey }
                                        if (keyToTest.isBlank() || keyToTest == "MY_GEMINI_API_KEY") {
                                            Toast.makeText(context, "Please enter a valid Gemini API Key first!", Toast.LENGTH_SHORT).show()
                                        } else if (!isVerifyingModels) {
                                            scope.launch {
                                                isVerifyingModels = true
                                                val currentModels = srsUiState.srsSettings.availableGeminiModels
                                                modelTestStatuses = currentModels.associateWith { com.example.data.repository.GeminiModelTestStatus.UNTESTED }

                                                var approvedCount = 0
                                                for (modelId in currentModels) {
                                                    modelTestStatuses = modelTestStatuses + (modelId to com.example.data.repository.GeminiModelTestStatus.TESTING)
                                                    val passed = com.example.data.repository.verifyGeminiModel(keyToTest, modelId)
                                                    if (passed) approvedCount++
                                                    modelTestStatuses = modelTestStatuses + (modelId to if (passed) com.example.data.repository.GeminiModelTestStatus.APPROVED else com.example.data.repository.GeminiModelTestStatus.FAILED)
                                                }
                                                isVerifyingModels = false
                                                Toast.makeText(context, "Model Check Complete: $approvedCount/${currentModels.size} approved!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    accentColor = NeumorphicColors.Primary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Available Models List
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                srsUiState.srsSettings.availableGeminiModels.forEach { modelId ->
                                    val isSelected = srsUiState.srsSettings.selectedGeminiModel == modelId
                                    val testStatus = modelTestStatuses[modelId] ?: com.example.data.repository.GeminiModelTestStatus.UNTESTED

                                    Surface(
                                        onClick = { revisionsViewModel.setSelectedGeminiModel(modelId) },
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                        color = if (isSelected) NeumorphicColors.Primary.copy(alpha = 0.12f) else NeumorphicColors.SurfaceDark.copy(alpha = 0.05f),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) NeumorphicColors.Primary else Color.Transparent
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { revisionsViewModel.setSelectedGeminiModel(modelId) },
                                                    colors = RadioButtonDefaults.colors(selectedColor = NeumorphicColors.Primary)
                                                )
                                                Text(
                                                    text = modelId,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 12.sp,
                                                    color = NeumorphicColors.TextPrimary
                                                )
                                                if (isSelected) {
                                                    Surface(
                                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                                        color = NeumorphicColors.Primary,
                                                        modifier = Modifier.padding(start = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = "Active",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Status Icon (Green check for approved, Red wrong for failed)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                when (testStatus) {
                                                    com.example.data.repository.GeminiModelTestStatus.TESTING -> {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(16.dp),
                                                            strokeWidth = 2.dp,
                                                            color = NeumorphicColors.Primary
                                                        )
                                                    }
                                                    com.example.data.repository.GeminiModelTestStatus.APPROVED -> {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = "Approved",
                                                            tint = Color(0xFF4CAF50),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    com.example.data.repository.GeminiModelTestStatus.FAILED -> {
                                                        Icon(
                                                            imageVector = Icons.Default.Cancel,
                                                            contentDescription = "Failed",
                                                            tint = Color(0xFFE53935),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    com.example.data.repository.GeminiModelTestStatus.UNTESTED -> {}
                                                }

                                                // Delete custom model option if not default
                                                if (srsUiState.srsSettings.availableGeminiModels.size > 1 &&
                                                    !listOf("gemini-2.5-flash", "gemini-1.5-flash").contains(modelId)
                                                ) {
                                                    IconButton(
                                                        onClick = { revisionsViewModel.removeCustomGeminiModel(modelId) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Remove model",
                                                            tint = NeumorphicColors.TextSecondary.copy(alpha = 0.5f),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Button to add custom new model
                            OutlinedButton(
                                onClick = { isAddModelDialogOpen = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeumorphicColors.Primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add New Model to List", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                if (isAddModelDialogOpen) {
                    AlertDialog(
                        onDismissRequest = { isAddModelDialogOpen = false },
                        containerColor = NeumorphicColors.DialogBackground,
                        titleContentColor = NeumorphicColors.TextPrimary,
                        textContentColor = NeumorphicColors.TextSecondary,
                        title = { Text("Add Custom Gemini Model", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NeumorphicColors.TextPrimary) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Enter the model ID (e.g. gemini-2.0-flash-exp, gemini-exp-1206):",
                                    fontSize = 12.sp,
                                    color = NeumorphicColors.TextSecondary
                                )
                                OutlinedTextField(
                                    value = newModelInput,
                                    onValueChange = { newModelInput = it },
                                    placeholder = { Text("gemini-2.0-flash", color = NeumorphicColors.TextSecondary.copy(alpha = 0.5f)) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = NeumorphicColors.TextPrimary,
                                        unfocusedTextColor = NeumorphicColors.TextPrimary,
                                        focusedBorderColor = NeumorphicColors.Primary,
                                        unfocusedBorderColor = NeumorphicColors.SurfaceDark.copy(alpha = 0.3f),
                                        focusedLabelColor = NeumorphicColors.Primary,
                                        unfocusedLabelColor = NeumorphicColors.TextSecondary,
                                        cursorColor = NeumorphicColors.Primary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (newModelInput.isNotBlank()) {
                                        revisionsViewModel.addCustomGeminiModel(newModelInput.trim())
                                        newModelInput = ""
                                        isAddModelDialogOpen = false
                                        Toast.makeText(context, "New model added & selected!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Primary)
                            ) {
                                Text("Add Model", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { isAddModelDialogOpen = false }) {
                                Text("Cancel", color = NeumorphicColors.TextSecondary)
                            }
                        }
                    )
                }

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))

                // Custom Prompt Override Section
                var showAdjustPromptDialog by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OCR System Prompt",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (srsUiState.srsSettings.customPromptOverride != null) "Custom prompt active" else "Default prompt active",
                            fontSize = 11.sp,
                            color = if (srsUiState.srsSettings.customPromptOverride != null) NeumorphicColors.Primary else NeumorphicColors.TextSecondary
                        )
                    }

                    NeumorphicButton(
                        label = "Adjust Prompt",
                        icon = Icons.Default.Edit,
                        onClick = { showAdjustPromptDialog = true },
                        accentColor = NeumorphicColors.Primary
                    )
                }

                if (showAdjustPromptDialog) {
                    val defaultPrompt = com.example.data.repository.GeminiOcrEngine.VERBATIM_PROMPT
                    var editingPrompt by remember {
                        mutableStateOf(srsUiState.srsSettings.customPromptOverride ?: defaultPrompt)
                    }

                    Dialog(
                        onDismissRequest = { showAdjustPromptDialog = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(16.dp)
                                .navigationBarsPadding()
                                .imePadding(),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                                color = NeumorphicColors.Background,
                                shadowElevation = 12.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 580.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { showAdjustPromptDialog = false }) {
                                                Icon(Icons.Default.Close, contentDescription = "Close", tint = NeumorphicColors.TextPrimary)
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Adjust System Prompt",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp,
                                                color = NeumorphicColors.TextPrimary
                                            )
                                        }

                                        TextButton(
                                            onClick = {
                                                editingPrompt = defaultPrompt
                                                revisionsViewModel.updateSrsSettings(srsUiState.srsSettings.copy(customPromptOverride = null))
                                                Toast.makeText(context, "Original prompt restored!", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restore", color = NeumorphicColors.TextSecondary, fontSize = 12.sp)
                                        }
                                    }

                                    Text(
                                        text = "Edit the instruction prompt used by Gemini when reading your cards and notes:",
                                        fontSize = 12.sp,
                                        color = NeumorphicColors.TextSecondary
                                    )

                                    OutlinedTextField(
                                        value = editingPrompt,
                                        onValueChange = { editingPrompt = it },
                                        label = { Text("System Prompt") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = NeumorphicColors.TextPrimary,
                                            unfocusedTextColor = NeumorphicColors.TextPrimary,
                                            focusedBorderColor = NeumorphicColors.Primary,
                                            unfocusedBorderColor = NeumorphicColors.SurfaceDark.copy(alpha = 0.3f),
                                            focusedContainerColor = NeumorphicColors.SurfaceLight,
                                            unfocusedContainerColor = NeumorphicColors.SurfaceLight
                                        ),
                                        minLines = 6,
                                        maxLines = 10,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f, fill = false)
                                            .heightIn(min = 160.dp, max = 280.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { showAdjustPromptDialog = false },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Cancel")
                                        }

                                        Button(
                                            onClick = {
                                                val finalPrompt = if (editingPrompt.trim() == defaultPrompt.trim()) null else editingPrompt.trim().ifBlank { null }
                                                revisionsViewModel.updateSrsSettings(srsUiState.srsSettings.copy(customPromptOverride = finalPrompt))
                                                Toast.makeText(context, "New prompt saved!", Toast.LENGTH_SHORT).show()
                                                showAdjustPromptDialog = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Primary),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Save New Prompt", fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))

                // Daily limits
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Max new cards / day",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Daily limit of new revision cards",
                            fontSize = 11.sp,
                            color = NeumorphicColors.TextSecondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(onClick = {
                            if (srsUiState.srsSettings.newCardsPerDay > 5) {
                                revisionsViewModel.updateSrsSettings(srsUiState.srsSettings.copy(newCardsPerDay = srsUiState.srsSettings.newCardsPerDay - 5))
                            }
                        }) {
                            Text("-", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.Primary)
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    color = NeumorphicColors.Primary.copy(alpha = 0.12f),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${srsUiState.srsSettings.newCardsPerDay}",
                                fontWeight = FontWeight.ExtraBold,
                                color = NeumorphicColors.TextPrimary,
                                fontSize = 15.sp
                            )
                        }
                        IconButton(onClick = {
                            if (srsUiState.srsSettings.newCardsPerDay < 100) {
                                revisionsViewModel.updateSrsSettings(srsUiState.srsSettings.copy(newCardsPerDay = srsUiState.srsSettings.newCardsPerDay + 5))
                            }
                        }) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NeumorphicColors.Primary)
                        }
                    }
                }

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))

                // Notifications toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Daily Revision Reminders", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeumorphicColors.TextPrimary)
                        Text(text = "Reminder at ${"%02d".format(srsUiState.srsSettings.reminderHour)}:${"%02d".format(srsUiState.srsSettings.reminderMinute)} when cards are due", fontSize = 11.sp, color = NeumorphicColors.TextSecondary)
                    }
                    Switch(
                        checked = srsUiState.srsSettings.notificationsEnabled,
                        onCheckedChange = { checked ->
                            revisionsViewModel.updateSrsSettings(srsUiState.srsSettings.copy(notificationsEnabled = checked))
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeumorphicColors.Primary)
                    )
                }
            }
        }

        // Export Options Dialog
        if (isExportOptionsDialogOpen) {
            AlertDialog(
                onDismissRequest = { isExportOptionsDialogOpen = false },
                containerColor = NeumorphicColors.DialogBackground,
                titleContentColor = NeumorphicColors.TextPrimary,
                textContentColor = NeumorphicColors.TextSecondary,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = NeumorphicColors.Primary)
                        Text("Export Card Backup (.focuscards)", color = NeumorphicColors.TextPrimary)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Select Card Types to Export:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeumorphicColors.TextPrimary)

                        CardTypeFilter.values().forEach { filter ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedExportCardType = filter }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedExportCardType == filter,
                                    onClick = { selectedExportCardType = filter },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = NeumorphicColors.Primary,
                                        unselectedColor = NeumorphicColors.TextSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(filter.label, fontSize = 13.sp, color = NeumorphicColors.TextPrimary)
                            }
                        }

                        Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.2f))

                        Text("Select Deck:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeumorphicColors.TextPrimary)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedExportDeckId = "ALL" }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedExportDeckId == "ALL",
                                onClick = { selectedExportDeckId = "ALL" },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = NeumorphicColors.Primary,
                                    unselectedColor = NeumorphicColors.TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("All Decks (${srsUiState.totalCount} cards)", fontSize = 13.sp, color = NeumorphicColors.TextPrimary)
                        }

                        srsUiState.decks.forEach { deck ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedExportDeckId = deck.id }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedExportDeckId == deck.id,
                                    onClick = { selectedExportDeckId = deck.id },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = NeumorphicColors.Primary,
                                        unselectedColor = NeumorphicColors.TextSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(deck.name, fontSize = 13.sp, color = NeumorphicColors.TextPrimary)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isExportOptionsDialogOpen = false
                            CardBackupService.startExport(
                                context = context,
                                cardTypeFilter = selectedExportCardType,
                                deckIdFilter = selectedExportDeckId
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Primary)
                    ) {
                        Text("Start Export", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isExportOptionsDialogOpen = false }) {
                        Text("Cancel", color = NeumorphicColors.TextSecondary)
                    }
                }
            )
        }

        // Import Confirmation Dialog
        if (isImportConfirmDialogOpen && selectedImportUri != null) {
            AlertDialog(
                onDismissRequest = { isImportConfirmDialogOpen = false },
                containerColor = NeumorphicColors.DialogBackground,
                titleContentColor = NeumorphicColors.TextPrimary,
                textContentColor = NeumorphicColors.TextSecondary,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = NeumorphicColors.Accent)
                        Text("Restore Cards Backup", color = NeumorphicColors.TextPrimary)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Select Import Mode:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeumorphicColors.TextPrimary)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedImportMode = ImportMode.MERGE }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedImportMode == ImportMode.MERGE,
                                onClick = { selectedImportMode = ImportMode.MERGE },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = NeumorphicColors.Primary,
                                    unselectedColor = NeumorphicColors.TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Merge (Recommended)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = NeumorphicColors.TextPrimary)
                                Text("Add imported cards & keep existing history", fontSize = 11.sp, color = NeumorphicColors.TextSecondary)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedImportMode = ImportMode.REPLACE_ALL }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedImportMode == ImportMode.REPLACE_ALL,
                                onClick = { selectedImportMode = ImportMode.REPLACE_ALL },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = NeumorphicColors.Primary,
                                    unselectedColor = NeumorphicColors.TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Replace All Cards", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFFE53935))
                                Text("Delete current cards & replace with backup", fontSize = 11.sp, color = NeumorphicColors.TextSecondary)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val uri = selectedImportUri!!
                            isImportConfirmDialogOpen = false
                            CardBackupService.startImport(
                                context = context,
                                importUri = uri,
                                importMode = selectedImportMode
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Accent)
                    ) {
                        Text("Restore Now", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isImportConfirmDialogOpen = false }) {
                        Text("Cancel", color = NeumorphicColors.TextSecondary)
                    }
                }
            )
        }

        // 5. Section: Data & Backup management
        ExpandableSection(
            title = "DATA & BACKUP",
            icon = Icons.Default.Storage,
            isExpanded = isDataBackupExpanded,
            onHeaderClick = { isDataBackupExpanded = !isDataBackupExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Card Collection Backup (.focuscards)
                Text(text = "Card Collection Backup (.focuscards)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeumorphicColors.TextPrimary)
                Text(
                    text = "Bundle all markdown text, images, voice notes, and review history into a single portable backup file.",
                    fontSize = 11.sp,
                    color = NeumorphicColors.TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeumorphicButton(
                        label = "Create Backup",
                        icon = Icons.Default.CloudUpload,
                        onClick = {
                            isExportOptionsDialogOpen = true
                        },
                        modifier = Modifier.weight(1f),
                        accentColor = NeumorphicColors.Primary
                    )

                    NeumorphicButton(
                        label = "Restore Backup",
                        icon = Icons.Default.CloudDownload,
                        onClick = {
                            backupPickerLauncher.launch("*/*")
                        },
                        modifier = Modifier.weight(1f),
                        accentColor = NeumorphicColors.Accent
                    )
                }

                // Active Backup/Restore Process or Result Card
                when (val state = backupState) {
                    is BackupTaskState.Running -> {
                        NeumorphicCard(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.5.dp,
                                        color = NeumorphicColors.Primary
                                    )
                                    Text(
                                        text = if (state.type == BackupTaskType.EXPORT) "Creating Card Backup..." else "Restoring Flashcards...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = NeumorphicColors.TextPrimary
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "${(state.progress * 100).toInt()}%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = NeumorphicColors.Primary
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = NeumorphicColors.Primary,
                                    trackColor = NeumorphicColors.SurfaceDark.copy(alpha = 0.2f)
                                )

                                Text(
                                    text = state.statusMessage,
                                    fontSize = 11.sp,
                                    color = NeumorphicColors.TextSecondary
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = NeumorphicColors.Primary
                                    )
                                    Text(
                                        text = "Process continues in background even if phone is locked.",
                                        fontSize = 10.sp,
                                        color = NeumorphicColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    is BackupTaskState.ExportCompleted -> {
                        NeumorphicCard(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50)
                                    )
                                    Text(
                                        text = "Backup Ready!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = NeumorphicColors.TextPrimary
                                    )
                                }

                                Text(
                                    text = state.result.summaryText,
                                    fontSize = 12.sp,
                                    color = NeumorphicColors.TextSecondary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            saveDocumentLauncher.launch(state.backupFile.name)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Primary)
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val authority = "${context.packageName}.fileprovider"
                                                val contentUri = FileProvider.getUriForFile(context, authority, state.backupFile)
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "application/octet-stream"
                                                    putExtra(Intent.EXTRA_STREAM, contentUri)
                                                    putExtra(Intent.EXTRA_SUBJECT, "Focus Island Cards Backup (.focuscards)")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Send Card Backup:"))
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Send", fontSize = 12.sp)
                                    }

                                    TextButton(
                                        onClick = { CardBackupStateHolder.reset() }
                                    ) {
                                        Text("Dismiss", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    is BackupTaskState.ImportCompleted -> {
                        NeumorphicCard(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50)
                                    )
                                    Text(
                                        text = "Import Successful!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = NeumorphicColors.TextPrimary
                                    )
                                }

                                Text(
                                    text = state.result.summaryText,
                                    fontSize = 12.sp,
                                    color = NeumorphicColors.TextSecondary
                                )

                                Button(
                                    onClick = { CardBackupStateHolder.reset() },
                                    modifier = Modifier.align(Alignment.End),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Primary)
                                ) {
                                    Text("Done", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    is BackupTaskState.Error -> {
                        NeumorphicCard(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = Color.Red
                                    )
                                    Text(
                                        text = if (state.type == BackupTaskType.EXPORT) "Export Error" else "Import Error",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.Red
                                    )
                                }

                                Text(
                                    text = state.errorMessage,
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )

                                TextButton(
                                    onClick = { CardBackupStateHolder.reset() },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Dismiss", color = Color.Red, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    else -> {}
                }

                Divider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.1f))

                // Clear Database / Reset Data
                Text(text = "Database Reset", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeumorphicColors.TextPrimary)
                NeumorphicButton(
                    label = "Reset All Local Data",
                    icon = Icons.Default.DeleteSweep,
                    onClick = { isResetConfirmOpen = true },
                    modifier = Modifier.fillMaxWidth(),
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

                Button(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                putExtra(
                                    android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                    android.content.ComponentName(context, DayNightLiveWallpaperService::class.java)
                                )
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = android.content.Intent(android.app.WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                                context.startActivity(intent)
                            } catch (e2: Exception) {
                                Toast.makeText(context, "Live wallpaper chooser opened.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E5B37))
                ) {
                    Icon(imageVector = Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Set Live Auto Day/Night Wallpaper Service", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
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


