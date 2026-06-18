package com.example.ui.screen.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.NeumorphicColors

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var isResetConfirmOpen by remember { mutableStateOf(false) }

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
                // Focus Option
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Focus Duration",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${state.focusMin} min",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.Primary,
                            fontSize = 14.sp
                        )
                    }
                    Slider(
                        value = state.focusMin.toFloat(),
                        onValueChange = { viewModel.updateFocusMin(it.toInt()) },
                        valueRange = 5f..90f,
                        steps = 17,
                        colors = SliderDefaults.colors(
                            thumbColor = NeumorphicColors.Primary,
                            activeTrackColor = NeumorphicColors.Primary
                        )
                    )
                }

                // Short break option
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Short Break",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${state.shortBreakMin} min",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.Success,
                            fontSize = 14.sp
                        )
                    }
                    Slider(
                        value = state.shortBreakMin.toFloat(),
                        onValueChange = { viewModel.updateShortBreakMin(it.toInt()) },
                        valueRange = 1f..25f,
                        steps = 24,
                        colors = SliderDefaults.colors(
                            thumbColor = NeumorphicColors.Success,
                            activeTrackColor = NeumorphicColors.Success
                        )
                    )
                }

                // Long break option
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Long Break",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${state.longBreakMin} min",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.Accent,
                            fontSize = 14.sp
                        )
                    }
                    Slider(
                        value = state.longBreakMin.toFloat(),
                        onValueChange = { viewModel.updateLongBreakMin(it.toInt()) },
                        valueRange = 5f..45f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = NeumorphicColors.Accent,
                            activeTrackColor = NeumorphicColors.Accent
                        )
                    )
                }

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

        Spacer(modifier = Modifier.height(100.dp)) // Safe padding for bottom items
    }

    // Reset Confirmation Dialog dialog
    if (isResetConfirmOpen) {
        val dialogContext = LocalContext.current
        AlertDialog(
            onDismissRequest = { isResetConfirmOpen = false },
            containerColor = Color(0xFFE0E5EC),
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
