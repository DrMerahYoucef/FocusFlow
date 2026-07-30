package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.FocusFlowApplication
import com.example.MainActivity
import com.example.data.backup.BackupRestoreResult
import com.example.data.backup.BackupTaskState
import com.example.data.backup.BackupTaskType
import com.example.data.backup.CardBackupEngine
import com.example.data.backup.CardBackupStateHolder
import com.example.data.backup.CardTypeFilter
import com.example.data.repository.ImportMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CardBackupService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "CardBackupService"
        private const val CHANNEL_ID = "card_backup_channel"
        private const val NOTIFICATION_ID = 2005

        const val ACTION_START_EXPORT = "com.example.action.START_EXPORT"
        const val ACTION_START_IMPORT = "com.example.action.START_IMPORT"
        const val ACTION_CANCEL = "com.example.action.CANCEL"

        const val EXTRA_CARD_TYPE_FILTER = "extra_card_type_filter"
        const val EXTRA_DECK_ID_FILTER = "extra_deck_id_filter"
        const val EXTRA_IMPORT_URI = "extra_import_uri"
        const val EXTRA_IMPORT_MODE = "extra_import_mode"

        fun startExport(context: Context, cardTypeFilter: CardTypeFilter, deckIdFilter: String) {
            val intent = Intent(context, CardBackupService::class.java).apply {
                action = ACTION_START_EXPORT
                putExtra(EXTRA_CARD_TYPE_FILTER, cardTypeFilter.name)
                putExtra(EXTRA_DECK_ID_FILTER, deckIdFilter)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startImport(context: Context, importUri: Uri, importMode: ImportMode) {
            val intent = Intent(context, CardBackupService::class.java).apply {
                action = ACTION_START_IMPORT
                putExtra(EXTRA_IMPORT_URI, importUri.toString())
                putExtra(EXTRA_IMPORT_MODE, importMode.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START_EXPORT -> {
                val filterName = intent.getStringExtra(EXTRA_CARD_TYPE_FILTER) ?: CardTypeFilter.ALL.name
                val cardTypeFilter = try { CardTypeFilter.valueOf(filterName) } catch (e: Exception) { CardTypeFilter.ALL }
                val deckIdFilter = intent.getStringExtra(EXTRA_DECK_ID_FILTER) ?: "ALL"
                runExportTask(cardTypeFilter, deckIdFilter)
            }
            ACTION_START_IMPORT -> {
                val uriStr = intent.getStringExtra(EXTRA_IMPORT_URI)
                val modeName = intent.getStringExtra(EXTRA_IMPORT_MODE) ?: ImportMode.MERGE.name
                val importMode = try { ImportMode.valueOf(modeName) } catch (e: Exception) { ImportMode.MERGE }
                if (uriStr != null) {
                    runImportTask(Uri.parse(uriStr), importMode)
                } else {
                    stopSelf()
                }
            }
            ACTION_CANCEL -> {
                CardBackupStateHolder.updateState(BackupTaskState.Idle)
                stopForegroundService()
            }
            else -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun runExportTask(cardTypeFilter: CardTypeFilter, deckIdFilter: String) {
        val initialNotification = createProgressNotification("Starting backup export...", 0)
        startForeground(NOTIFICATION_ID, initialNotification)

        serviceScope.launch {
            val app = applicationContext as FocusFlowApplication
            val repository = app.revisionRepository

            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val backupFile = File(cacheDir, "FocusCards_Backup_$timestamp.focuscards")

                CardBackupStateHolder.updateState(
                    BackupTaskState.Running(BackupTaskType.EXPORT, 0.05f, "Preparing backup...")
                )

                val result = CardBackupEngine.createBackup(
                    context = applicationContext,
                    repository = repository,
                    cardTypeFilter = cardTypeFilter,
                    deckIdFilter = deckIdFilter,
                    outputFile = backupFile
                ) { progress, status ->
                    CardBackupStateHolder.updateState(
                        BackupTaskState.Running(BackupTaskType.EXPORT, progress, status)
                    )
                    updateNotification("Exporting: $status", (progress * 100).toInt())
                }

                CardBackupStateHolder.updateState(
                    BackupTaskState.ExportCompleted(backupFile, result)
                )

                showCompletionNotification(
                    title = "Backup Export Complete! 📦",
                    contentText = "${result.notesCount} cards exported (${result.mediaCount} media files)."
                )
            } catch (e: Exception) {
                Log.e(TAG, "Export failed", e)
                CardBackupStateHolder.updateState(
                    BackupTaskState.Error(BackupTaskType.EXPORT, e.message ?: "Export failed unexpectedly")
                )
                showCompletionNotification(
                    title = "Backup Export Failed ❌",
                    contentText = e.message ?: "An error occurred during export."
                )
            } finally {
                stopForegroundService()
            }
        }
    }

    private fun runImportTask(importUri: Uri, importMode: ImportMode) {
        val initialNotification = createProgressNotification("Starting card restoration...", 0)
        startForeground(NOTIFICATION_ID, initialNotification)

        serviceScope.launch {
            val app = applicationContext as FocusFlowApplication
            val repository = app.revisionRepository

            try {
                CardBackupStateHolder.updateState(
                    BackupTaskState.Running(BackupTaskType.IMPORT, 0.05f, "Opening backup file...")
                )

                val result = CardBackupEngine.restoreBackup(
                    context = applicationContext,
                    repository = repository,
                    inputFileUri = importUri,
                    importMode = importMode
                ) { progress, status ->
                    CardBackupStateHolder.updateState(
                        BackupTaskState.Running(BackupTaskType.IMPORT, progress, status)
                    )
                    updateNotification("Importing: $status", (progress * 100).toInt())
                }

                CardBackupStateHolder.updateState(
                    BackupTaskState.ImportCompleted(result)
                )

                showCompletionNotification(
                    title = "Cards Restored Successfully! 🎉",
                    contentText = "${result.notesCount} cards & ${result.decksCount} decks imported."
                )
            } catch (e: Exception) {
                Log.e(TAG, "Import failed", e)
                CardBackupStateHolder.updateState(
                    BackupTaskState.Error(BackupTaskType.IMPORT, e.message ?: "Import failed unexpectedly")
                )
                showCompletionNotification(
                    title = "Import Failed ❌",
                    contentText = e.message ?: "An error occurred during import."
                )
            } finally {
                stopForegroundService()
            }
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FocusIsland:CardBackupService")
            wakeLock?.acquire(15 * 60 * 1000L) // 15 min max timeout
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Card Backup & Restore Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time progress of flashcard backup export and import tasks"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createProgressNotification(contentText: String, progressPercent: Int) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Island Backup & Restore")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progressPercent, progressPercent == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(createPendingIntent())
            .build()

    private fun updateNotification(contentText: String, progressPercent: Int) {
        val notification = createProgressNotification(contentText, progressPercent)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(title: String, contentText: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent())
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun stopForegroundService() {
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceJob.cancel()
        releaseWakeLock()
        super.onDestroy()
    }
}
