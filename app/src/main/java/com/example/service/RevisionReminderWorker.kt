package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.FocusFlowApplication
import com.example.MainActivity
import com.example.data.srs.SrsSettings
import java.util.Calendar
import java.util.concurrent.TimeUnit

const val REVISION_CHANNEL_ID = "revisions_reminder_channel"
const val REVISION_NOTIFICATION_ID = 20261

class RevisionReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = FocusFlowApplication.instance.revisionRepository
            val dueCount = repository.getDueCount()

            if (dueCount > 0) {
                val intent = Intent(applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("target_screen", "revisions")
                }

                val pendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                createNotificationChannel(applicationContext)

                val notification = NotificationCompat.Builder(applicationContext, REVISION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Flashcards Review Due 📚")
                    .setContentText("You have $dueCount flashcard(s) to review today.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()

                NotificationManagerCompat.from(applicationContext).notify(REVISION_NOTIFICATION_ID, notification)
            }
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("RevisionReminderWorker", "Failed to check or send reminder notification", e)
            Result.failure()
        }
    }

    companion object {
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Revision Reminders"
                val descriptionText = "Notifications for due flashcards"
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(REVISION_CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun scheduleRevisionReminder(context: Context, settings: SrsSettings) {
            if (!settings.notificationsEnabled) {
                WorkManager.getInstance(context).cancelUniqueWork("revision_reminder")
                return
            }

            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, settings.reminderHour)
                set(Calendar.MINUTE, settings.reminderMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }

            val delayMs = target.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<RevisionReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "revision_reminder",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
