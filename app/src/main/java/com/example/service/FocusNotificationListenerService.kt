package com.example.service

import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.FocusFlowApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FocusNotificationListenerService : NotificationListenerService() {

    private val db by lazy { FocusFlowApplication.instance.database }
    private var blockedPackages: Set<String> = emptySet()

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onListenerConnected() {
        instance = this
        refreshBlockList()
    }

    override fun onListenerDisconnected() {
        if (instance == this) {
            instance = null
        }
    }

    override fun onDestroy() {
        if (instance == this) {
            instance = null
        }
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_REFRESH) {
            refreshBlockList()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    // Refresh which apps to block
    fun refreshBlockList() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                blockedPackages = db.blockedAppDao()
                    .getNotifBlockedPackages()
                    .toSet()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val isTimerRunning = PomodoroTimerService.isRunning && PomodoroTimerService.state.value.phase == Phase.FOCUS
        if (!isTimerRunning) return          // only active during focus
        if (sbn.packageName in blockedPackages) {
            try {
                cancelNotification(sbn.key)                    // silently remove it
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.example.service.REFRESH_BLOCK_LIST"
        
        @Volatile
        private var instance: FocusNotificationListenerService? = null

        fun refresh(context: Context) {
            val inst = instance
            if (inst != null) {
                inst.refreshBlockList()
            } else {
                val intent = Intent(context, FocusNotificationListenerService::class.java).apply {
                    action = ACTION_REFRESH
                }
                try {
                    context.startService(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
