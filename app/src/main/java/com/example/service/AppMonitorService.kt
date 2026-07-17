package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.FocusFlowApplication
import com.example.MainActivity
import com.example.FocusOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppMonitorService : Service() {

    private val db by lazy { FocusFlowApplication.instance.database }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var blockedPackages: Set<String> = emptySet()
    private var pollingJob: Job? = null
    private var lastBlockedApp: String? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    // Immediate check on screen wake — don't wait for next poll cycle
                    scope.launch {
                        delay(300L) // small delay for system to settle
                        val isTimerRunning = PomodoroTimerService.isRunning && PomodoroTimerService.state.value.phase == Phase.FOCUS
                        if (!isTimerRunning) return@launch
                        val foregroundPkg = getForegroundApp()
                        if (foregroundPkg != null && foregroundPkg in blockedPackages) {
                            lastBlockedApp = foregroundPkg
                            showFocusOverlay(foregroundPkg)
                        }
                    }
                }
                "com.focusflow.RESET_BLOCKER_STATE" -> {
                    // Reset tracker so overlay can re-trigger immediately if needed
                    lastBlockedApp = null
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(MONITOR_NOTIF_ID, buildMonitorNotification())
        refreshBlockList()
        startPolling()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction("com.focusflow.RESET_BLOCKER_STATE")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }

        return START_STICKY
    }

    private fun refreshBlockList() {
        scope.launch {
            try {
                blockedPackages = db.blockedAppDao().getLaunchBlockedPackages().toSet()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                delay(1000L) // 1000ms is safe and battery-friendly with UsageEvents
                val isTimerRunning = PomodoroTimerService.isRunning && PomodoroTimerService.state.value.phase == Phase.FOCUS
                if (!isTimerRunning) {
                    lastBlockedApp = null
                    continue
                }
                
                val foregroundPkg = getForegroundApp()
                if (foregroundPkg != null && foregroundPkg in blockedPackages) {
                    // Debounce: Only show overlay if it's a different app or we reset
                    if (foregroundPkg != lastBlockedApp) {
                        lastBlockedApp = foregroundPkg
                        showFocusOverlay(foregroundPkg)
                    }
                } else {
                    // Reset when user is on an allowed app or FocusFlow itself
                    if (foregroundPkg == packageName || (foregroundPkg != null && foregroundPkg !in blockedPackages)) {
                        lastBlockedApp = null
                    }
                }
            }
        }
    }

    private fun getForegroundApp(): String? {
        val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        
        // Look back 5 seconds to ensure we don't miss rapid events
        val events = usm.queryEvents(now - 5000L, now)
        val event = UsageEvents.Event()
        var lastForegroundPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // MOVE_TO_FOREGROUND catches BOTH fresh launches AND resumes from Recents
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastForegroundPackage = event.packageName
            }
        }
        
        return lastForegroundPackage
    }

    private fun showFocusOverlay(blockedPkg: String) {
        if (!Settings.canDrawOverlays(this)) return
        val intent = Intent(this, FocusOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("blocked_package", blockedPkg)
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Monitor"
            val descriptionText = "Monitors app launches during focus session"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildMonitorNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Blocker Active")
            .setContentText("Monitoring app usage during focus.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        pollingJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val MONITOR_NOTIF_ID = 1002
        private const val CHANNEL_ID = "focusflow_monitor_channel"

        fun start(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            context.stopService(intent)
        }
    }
}
