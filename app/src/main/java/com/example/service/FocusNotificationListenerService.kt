package com.example.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.FocusFlowApplication
import com.example.data.repository.NotificationSummary
import com.example.data.repository.NotificationSummaryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.example.data.repository.ConversationRepository
import com.example.data.repository.ConversationSummaryManager
import com.example.util.TrackedAppsStorage

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
        populateActiveNotifications()
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
            populateActiveNotifications()
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

    fun populateActiveNotifications() {
        try {
            val active = activeNotifications ?: return
            for (sbn in active) {
                processAndRecordNotification(sbn)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processAndRecordNotification(sbn: StatusBarNotification) {
        try {
            if (sbn.packageName == packageName) return

            val trackedApps = TrackedAppsStorage.getTrackedApps(applicationContext)
            if (trackedApps.isNotEmpty() && sbn.packageName !in trackedApps) return

            val extras = sbn.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            val appLabel = try {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(sbn.packageName, 0)
                ).toString()
            } catch (e: Exception) { sbn.packageName }

            val messagingStyle = androidx.core.app.NotificationCompat.MessagingStyle
                .extractMessagingStyleFromNotification(sbn.notification)
            val messageLines: List<String> = messagingStyle?.messages
                ?.mapNotNull { it.text?.toString() }
                ?.filter { it.isNotBlank() }
                ?: listOfNotNull(text.ifBlank { null })

            if (title.isBlank() && messageLines.isEmpty()) return

            val senderName = title.ifBlank { appLabel }

            ConversationRepository.addMessages(
                packageName = sbn.packageName,
                appName = appLabel,
                sender = senderName,
                newMessages = messageLines
            )

            val lastLine = messageLines.lastOrNull() ?: text.take(120)
            val entry = NotificationSummary(
                appName = appLabel,
                packageName = sbn.packageName,
                sender = senderName,
                messageResume = lastLine,
                postedAt = sbn.postTime
            )
            NotificationSummaryRepository.addOrUpdate(entry)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ConversationSummaryManager.summarizePendingThreads(applicationContext)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        processAndRecordNotification(sbn)

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

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        try {
            NotificationSummaryRepository.remove(sbn.packageName, sbn.id)
        } catch (e: Exception) {
            e.printStackTrace()
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
                inst.populateActiveNotifications()
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

        fun refreshNotifications(context: Context) {
            val inst = instance
            if (inst != null) {
                inst.populateActiveNotifications()
            } else {
                refresh(context)
            }
        }
    }
}

