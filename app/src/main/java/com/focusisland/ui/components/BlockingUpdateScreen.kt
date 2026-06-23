package com.focusisland.ui.components

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusisland.service.UpdateState
import com.focusisland.ui.screen.settings.SettingsViewModel
import com.focusisland.ui.theme.LocalIsDarkTheme
import com.focusisland.ui.theme.NeumorphicColors

@Composable
fun BlockingUpdateScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val updateState by viewModel.updateState.collectAsState()
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current

    // Override Back button to completely block the user from navigating back or bypassing the restriction
    BackHandler {
        // Do nothing - consume the back press event completely
    }

    // High contrast ambient background corresponding to overall system theme
    val bgColor = if (isDark) {
        Color(0xFF14161C).copy(alpha = 0.96f)
    } else {
        Color(0xFFD2D7DF).copy(alpha = 0.96f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        NeumorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            cornerRadius = 24.dp,
            elevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Icon with warning / update badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = NeumorphicColors.Primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "System Update Required Icon",
                        tint = NeumorphicColors.Primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "MANDATORY UPDATE REQUIRED",
                    fontWeight = FontWeight.Black,
                    color = NeumorphicColors.TextPrimary,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "To ensure security, stability, and access to all productivity features, please install the latest build of FocusFlow to continue.",
                    fontSize = 13.sp,
                    color = NeumorphicColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Divider(
                    color = if (isDark) Color(0x1F2E333F) else Color(0x1FA3B1C6),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Render matching layout based on the current active UpdateState
                when (updateState) {
                    is UpdateState.UpdateAvailable -> {
                        val available = updateState as UpdateState.UpdateAvailable
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "A newer build of FocusFlow is ready in Firebase Storage.",
                                fontSize = 12.sp,
                                color = NeumorphicColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            NeumorphicButton(
                                label = "Download & Install (Code: ${available.latestVersionCode})",
                                icon = Icons.Default.CloudDownload,
                                accentColor = NeumorphicColors.Success,
                                onClick = { viewModel.downloadUpdate(available.downloadUrl) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    is UpdateState.Downloading -> {
                        val progress = (updateState as UpdateState.Downloading).progress
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val percentText = if (progress >= 0f) "${(progress * 100).toInt()}%" else "Indeterminate"
                            Text(
                                text = "Downloading deployment package: $percentText",
                                fontSize = 13.sp,
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

                    is UpdateState.ReadyToInstall -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Install Warning Icon",
                                    tint = NeumorphicColors.Warning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Package ready for system installation",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeumorphicColors.TextPrimary
                                )
                            }

                            Text(
                                text = "Secure FileProvider cache verified. Grant 'Install Unknown Apps' permission if prompted by Android.",
                                fontSize = 11.sp,
                                color = NeumorphicColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            NeumorphicButton(
                                label = "Install Update",
                                icon = Icons.Default.CloudDownload,
                                accentColor = NeumorphicColors.Success,
                                onClick = { viewModel.installUpdate() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    is UpdateState.Error -> {
                        val errMsg = (updateState as UpdateState.Error).message
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error detail icon",
                                    tint = NeumorphicColors.Accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Update installation failed",
                                    fontSize = 14.sp,
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

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                NeumorphicButton(
                                    label = "Retry Check",
                                    icon = Icons.Default.Refresh,
                                    accentColor = NeumorphicColors.Primary,
                                    onClick = { viewModel.checkForUpdates(forceFetch = true) },
                                    modifier = Modifier.weight(1f)
                                )

                                Button(
                                    onClick = { (context as? Activity)?.finish() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeumorphicColors.Accent.copy(alpha = 0.15f),
                                        contentColor = NeumorphicColors.Accent
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(48.dp).weight(0.8f)
                                ) {
                                    Text(
                                        text = "Exit",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        // In case of Idle, Checking, or UpToDate when rendering blocking update (should not happen normally)
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NeumorphicColors.Primary)
                        }
                    }
                }
            }
        }
    }
}
