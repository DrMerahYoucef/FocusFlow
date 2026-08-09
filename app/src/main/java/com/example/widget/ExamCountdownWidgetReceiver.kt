package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.db.entity.SessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ExamCountdownWidgetReceiver : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleDayNightAlarm(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        scheduleDayNightAlarm(context)
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        scheduleDayNightAlarm(context)
        if (intent.action == Intent.ACTION_WALLPAPER_CHANGED ||
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            intent.action == "com.example.ACTION_WIDGET_AUTO_UPDATE"
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ExamCountdownWidgetReceiver::class.java))
            updateAllWidgets(context, manager, ids)
        }
    }

    private fun updateAllWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return

        val database = AppDatabase.getDatabase(context)
        val examDao = database.examDao()
        val sessionDao = database.sessionDao()

        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        CoroutineScope(Dispatchers.IO).launch {
            val exams = examDao.getUpcomingExams(todayCalendar.timeInMillis)
            val completedSessions = sessionDao.getAllSessionsList().filter { it.completed }

            // Compute statistics
            val totalSeconds = completedSessions.sumOf { it.durationSeconds }
            val totalFocusHours = totalSeconds / 3600.0
            val totalPoints = completedSessions.sumOf { it.focusScore }
            val sessionsCount = completedSessions.size
            val currentStreak = calculateStreak(completedSessions)

            // Extract wallpaper palette
            val palette = WallpaperColorExtractor.extract(context)
            val textColor = palette.textColor
            val secondaryTextColor = ColorUtils.setAlphaComponent(textColor, 0x99)
            val accentColor = if (textColor == Color.WHITE) Color.parseColor("#60A5FA") else Color.parseColor("#2563EB")

            val transparentBitmap = getTransparentWidgetBitmap()

            for (i in 0 until appWidgetIds.size) {
                val widgetId = appWidgetIds[i]
                val views = RemoteViews(context.packageName, R.layout.widget_countdown_layout)

                // Assign transparent background bitmap
                views.setImageViewBitmap(R.id.widget_background, transparentBitmap)
                views.setInt(R.id.widget_overlay, "setBackgroundColor", Color.TRANSPARENT)

                // Headers
                views.setTextColor(R.id.widget_title_label, textColor)
                views.setTextColor(R.id.widget_stats_title_label, textColor)
                views.setTextColor(R.id.widget_no_exams_text, secondaryTextColor)

                // Left Column Exam Items
                views.setTextColor(R.id.widget_exam1_name, textColor)
                views.setTextColor(R.id.widget_exam1_days, accentColor)
                views.setTextColor(R.id.widget_exam2_name, secondaryTextColor)
                views.setTextColor(R.id.widget_exam2_days, accentColor)
                views.setTextColor(R.id.widget_exam3_name, secondaryTextColor)
                views.setTextColor(R.id.widget_exam3_days, accentColor)

                // Stats Labels & Values
                views.setTextColor(R.id.widget_stat_focus_value, textColor)
                views.setTextColor(R.id.widget_stat_focus_label, secondaryTextColor)
                views.setTextColor(R.id.widget_stat_points_value, textColor)
                views.setTextColor(R.id.widget_stat_points_label, secondaryTextColor)
                views.setTextColor(R.id.widget_stat_sessions_value, textColor)
                views.setTextColor(R.id.widget_stat_sessions_label, secondaryTextColor)
                views.setTextColor(R.id.widget_stat_streak_value, textColor)
                views.setTextColor(R.id.widget_stat_streak_label, secondaryTextColor)

                // Icon Tints (Adaptive wallpaper text contrast)
                views.setInt(R.id.widget_icon_exams_header, "setColorFilter", palette.iconTint)
                views.setInt(R.id.widget_icon_stats_header, "setColorFilter", palette.iconTint)
                views.setInt(R.id.widget_icon_focus, "setColorFilter", palette.iconTint)
                views.setInt(R.id.widget_icon_points, "setColorFilter", palette.iconTint)
                views.setInt(R.id.widget_icon_sessions, "setColorFilter", palette.iconTint)
                views.setInt(R.id.widget_icon_streak, "setColorFilter", palette.iconTint)

                // Divider
                views.setInt(R.id.widget_vertical_divider, "setBackgroundColor", palette.dividerColor)

                // Format and bind statistic numbers
                val focusStr = if (totalFocusHours >= 10.0) {
                    val fullHrs = totalFocusHours.toInt()
                    "${fullHrs}h"
                } else {
                    val totalMinutes = (totalSeconds / 60)
                    "${totalMinutes}m"
                }

                views.setTextViewText(R.id.widget_stat_focus_value, focusStr)
                views.setTextViewText(R.id.widget_stat_points_value, "$totalPoints")
                views.setTextViewText(R.id.widget_stat_sessions_value, "$sessionsCount")
                views.setTextViewText(R.id.widget_stat_streak_value, "${currentStreak}d")

                // Bind list of exams (up to 3 slots)
                if (exams.isEmpty()) {
                    views.setViewVisibility(R.id.widget_no_exams_text, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_exams_list_container, View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_no_exams_text, View.GONE)
                    views.setViewVisibility(R.id.widget_exams_list_container, View.VISIBLE)

                    // Slot 1
                    if (exams.size > 0) {
                        views.setViewVisibility(R.id.widget_exam1_container, View.VISIBLE)
                        val ex = exams[0]
                        views.setTextViewText(R.id.widget_exam1_name, ex.name)
                        views.setTextViewText(R.id.widget_exam1_days, getDaysLeftStringSpecial(ex.examDate, todayCalendar.timeInMillis))
                    } else {
                        views.setViewVisibility(R.id.widget_exam1_container, View.GONE)
                    }

                    // Slot 2
                    if (exams.size > 1) {
                        views.setViewVisibility(R.id.widget_exam2_container, View.VISIBLE)
                        val ex = exams[1]
                        views.setTextViewText(R.id.widget_exam2_name, ex.name)
                        views.setTextViewText(R.id.widget_exam2_days, getDaysLeftString(ex.examDate, todayCalendar.timeInMillis))
                    } else {
                        views.setViewVisibility(R.id.widget_exam2_container, View.GONE)
                    }

                    // Slot 3
                    if (exams.size > 2) {
                        views.setViewVisibility(R.id.widget_exam3_container, View.VISIBLE)
                        val ex = exams[2]
                        views.setTextViewText(R.id.widget_exam3_name, ex.name)
                        views.setTextViewText(R.id.widget_exam3_days, getDaysLeftString(ex.examDate, todayCalendar.timeInMillis))
                    } else {
                        views.setViewVisibility(R.id.widget_exam3_container, View.GONE)
                    }
                }

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    private fun getDaysLeftString(examTime: Long, todayTime: Long): String {
        val examCalendar = Calendar.getInstance().apply {
            timeInMillis = examTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMs = examCalendar.timeInMillis - todayTime
        val daysLeft = (diffMs / (24 * 60 * 60 * 1000L)).toInt()
        return when {
            daysLeft < 0 -> "Passed"
            daysLeft == 0 -> "Today"
            else -> "${daysLeft}d"
        }
    }

    private fun getDaysLeftStringSpecial(examTime: Long, todayTime: Long): String {
        val examCalendar = Calendar.getInstance().apply {
            timeInMillis = examTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMs = examCalendar.timeInMillis - todayTime
        val daysLeft = (diffMs / (24 * 60 * 60 * 1000L)).toInt()
        val adjustedDays = daysLeft + 2
        return when {
            adjustedDays < 0 -> "Passed"
            adjustedDays == 0 -> "Today"
            adjustedDays == 1 -> "1 Day"
            else -> "$adjustedDays Days"
        }
    }

    private fun calculateStreak(completedSessions: List<SessionEntity>): Int {
        if (completedSessions.isEmpty()) return 0

        val calendar = Calendar.getInstance()
        val uniqueDays = completedSessions.map {
            calendar.timeInMillis = it.date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }.distinct().sortedDescending()

        if (uniqueDays.isEmpty()) return 0

        var currentStreak = 0
        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val oneDayMillis = 24 * 60 * 60 * 1000L

        val checkTime = todayCalendar.timeInMillis
        val newestDay = uniqueDays.first()
        if (newestDay != checkTime && newestDay != (checkTime - oneDayMillis)) {
            return 0
        }

        if (newestDay == checkTime || newestDay == (checkTime - oneDayMillis)) {
            currentStreak = 1
            var lastDay = newestDay
            for (i in 1 until uniqueDays.size) {
                if (lastDay - uniqueDays[i] == oneDayMillis) {
                    currentStreak++
                    lastDay = uniqueDays[i]
                } else if (lastDay - uniqueDays[i] > oneDayMillis) {
                    break
                }
            }
        }

        return currentStreak
    }

    private fun getTransparentWidgetBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        return bitmap
    }

    companion object {
        fun triggerWidgetUpdate(context: Context) {
            val intent1 = Intent(context, ExamCountdownWidgetReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent1)

            val intent2 = Intent(context, ExamMatrixWidgetReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent2)

            val intent3 = Intent("com.example.ACTION_WIDGET_AUTO_UPDATE")
            context.sendBroadcast(intent3)
        }

        fun scheduleDayNightAlarm(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
                val intent = Intent(context, ExamCountdownWidgetReceiver::class.java).apply {
                    action = "com.example.ACTION_WIDGET_AUTO_UPDATE"
                }
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    9901,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                val now = System.currentTimeMillis()
                val nextHour = now + (30 * 60 * 1000L)
                alarmManager.setInexactRepeating(
                    android.app.AlarmManager.RTC,
                    nextHour,
                    android.app.AlarmManager.INTERVAL_HALF_HOUR,
                    pendingIntent
                )
            } catch (e: Exception) {
                android.util.Log.e("ExamCountdownWidget", "Error scheduling alarm: ${e.message}")
            }
        }
    }
}
