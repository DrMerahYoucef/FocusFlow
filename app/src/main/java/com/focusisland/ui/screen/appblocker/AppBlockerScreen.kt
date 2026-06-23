package com.focusisland.ui.screen.appblocker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.focusisland.ui.components.NeumorphicCard
import com.focusisland.ui.components.neumorphicShadow
import com.focusisland.ui.theme.NeumorphicColors

@Composable
fun AppBlockerScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: AppBlockerViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val blockedList by viewModel.blockedApps.collectAsState()
    val allApps by viewModel.installedApps.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    
    var hasNotifPerm by remember { mutableStateOf(false) }
    var hasUsagePerm by remember { mutableStateOf(false) }
    var hasOverlayPerm by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotifPerm = checkNotificationListenerPermission(context)
                hasUsagePerm = checkUsageStatsPermission(context)
                hasOverlayPerm = checkOverlayPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val filteredApps by remember(allApps, blockedList, searchQuery) {
        derivedStateOf {
            allApps.filter {
                searchQuery.isEmpty() || it.appName.contains(searchQuery, ignoreCase = true)
            }.sortedWith(compareByDescending<InstalledAppInfo> { info ->
                blockedList.any { it.packageName == info.packageName }
            }.thenBy { it.appName })
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(NeumorphicColors.Background)
            .statusBarsPadding(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .neumorphicShadow(cornerRadius = 12.dp, elevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go Back",
                        tint = NeumorphicColors.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "App Blocker",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphicColors.TextPrimary
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(NeumorphicColors.Background)
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    elevation = 4.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Focus App Blocker",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Choose applications to silence and block during active focus sessions. If you try to open them, they will be intercepted with a reminder to stay on task.",
                            fontSize = 12.sp,
                            color = NeumorphicColors.TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Permissions checklist section
            item {
                Text(
                    text = "REQUIRED PERMISSIONS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphicColors.TextSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    elevation = 5.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PermissionRow(
                            title = "Notification Access",
                            description = "Allows FocusFlow to block app push alerts.",
                            isGranted = hasNotifPerm,
                            onGrantClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Search Notification Access in system settings to grant.", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                        
                        HorizontalDivider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.2f))

                        PermissionRow(
                            title = "Usage Stats Access",
                            description = "Allows detecting when a blocked app opens.",
                            isGranted = hasUsagePerm,
                            onGrantClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Search Usage Access in system settings to grant.", Toast.LENGTH_LONG).show()
                                }
                            }
                        )

                        HorizontalDivider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.2f))

                        PermissionRow(
                            title = "Draw Over Apps (Overlay)",
                            description = "Allows displaying the lock status screen.",
                            isGranted = hasOverlayPerm,
                            onGrantClick = {
                                try {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Search Overlay/Draw Over Apps in system settings to grant.", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                }
            }

            // Search Filter
            item {
                Text(
                    text = "CONFIGURED APPLICATIONS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphicColors.TextSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter installed apps...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NeumorphicColors.TextPrimary,
                        unfocusedTextColor = NeumorphicColors.TextPrimary,
                        focusedLabelColor = NeumorphicColors.Primary,
                        unfocusedLabelColor = NeumorphicColors.TextSecondary,
                        focusedBorderColor = NeumorphicColors.Primary,
                        unfocusedBorderColor = NeumorphicColors.TextSecondary.copy(alpha = 0.5f),
                        focusedPlaceholderColor = NeumorphicColors.TextSecondary,
                        unfocusedPlaceholderColor = NeumorphicColors.TextSecondary.copy(alpha = 0.7f)
                    ),
                    singleLine = true
                )
            }

            // Search Results/App lists
            if (filteredApps.isEmpty()) {
                item {
                    Text(
                        text = if (searchQuery.isEmpty()) "Loading apps list..." else "No matching apps found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeumorphicColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            } else {
                items(filteredApps, key = { it.packageName }) { appInfo ->
                    val blockCfg = blockedList.find { it.packageName == appInfo.packageName }

                    val isNotifBlocked = blockCfg?.blockNotifications ?: false
                    val isLaunchBlocked = blockCfg?.blockLaunch ?: false

                    AppConfigCard(
                        appInfo = appInfo,
                        isNotifBlocked = isNotifBlocked,
                        onNotifBlockedChange = { block ->
                            viewModel.toggleNotifBlock(appInfo.packageName, appInfo.appName, block)
                        },
                        isLaunchBlocked = isLaunchBlocked,
                        onLaunchBlockedChange = { block ->
                            viewModel.toggleLaunchBlock(appInfo.packageName, appInfo.appName, block)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = NeumorphicColors.TextPrimary,
                fontSize = 14.sp
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = NeumorphicColors.TextSecondary
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))

        if (isGranted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Permission Granted",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(28.dp)
            )
        } else {
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeumorphicColors.Accent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Grant →", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AppConfigCard(
    appInfo: InstalledAppInfo,
    isNotifBlocked: Boolean,
    onNotifBlockedChange: (Boolean) -> Unit,
    isLaunchBlocked: Boolean,
    onLaunchBlockedChange: (Boolean) -> Unit
) {
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        elevation = 3.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                appInfo.icon?.let { drawable ->
                    AndroidView(
                        factory = { ctx ->
                            ImageView(ctx).apply {
                                scaleType = ImageView.ScaleType.FIT_CENTER
                            }
                        },
                        update = { imageView ->
                            imageView.setImageDrawable(drawable)
                        },
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = appInfo.appName,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicColors.TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = appInfo.packageName,
                        fontSize = 10.sp,
                        color = NeumorphicColors.TextSecondary
                    )
                }
            }

            HorizontalDivider(color = NeumorphicColors.SurfaceDark.copy(alpha = 0.15f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isNotifBlocked) NeumorphicColors.Primary else NeumorphicColors.TextSecondary
                    )
                    Text(
                        text = "Block notifications",
                        fontSize = 12.sp,
                        color = if (isNotifBlocked) NeumorphicColors.TextPrimary else NeumorphicColors.TextSecondary
                    )
                }
                Switch(
                    checked = isNotifBlocked,
                    onCheckedChange = onNotifBlockedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeumorphicColors.Primary,
                        checkedTrackColor = NeumorphicColors.Primary.copy(alpha = 0.5f)
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isLaunchBlocked) NeumorphicColors.Primary else NeumorphicColors.TextSecondary
                    )
                    Text(
                        text = "Block application launch",
                        fontSize = 12.sp,
                        color = if (isLaunchBlocked) NeumorphicColors.TextPrimary else NeumorphicColors.TextSecondary
                    )
                }
                Switch(
                    checked = isLaunchBlocked,
                    onCheckedChange = onLaunchBlockedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeumorphicColors.Primary,
                        checkedTrackColor = NeumorphicColors.Primary.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

private fun checkNotificationListenerPermission(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver, "enabled_notification_listeners"
    )
    return enabledListeners?.contains(context.packageName) == true
}

private fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun checkOverlayPermission(context: Context): Boolean =
    Settings.canDrawOverlays(context)
