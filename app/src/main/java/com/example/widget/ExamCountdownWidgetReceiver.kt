package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.db.entity.SessionEntity
import com.example.widget.theme.WidgetTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ExamCountdownWidgetReceiver : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefreshScheduler.scheduleNextMidnight(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefreshScheduler.cancel(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        WidgetRefreshScheduler.scheduleNextMidnight(context)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                renderAll(context, appWidgetManager, appWidgetIds)
            } catch (t: Throwable) {
                Log.e(TAG, "Widget render failed", t)
                val fallback = RemoteViews(
                    context.packageName,
                    R.layout.widget_countdown_layout
                )
                appWidgetIds.forEach {
                    appWidgetManager.updateAppWidget(it, fallback)
                }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            WidgetRefreshScheduler.ACTION_AUTO_UPDATE,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_WALLPAPER_CHANGED -> {
                val mgr = AppWidgetManager.getInstance(context)
                val ids = mgr.getAppWidgetIds(
                    ComponentName(context, ExamCountdownWidgetReceiver::class.java)
                )
                onUpdate(context, mgr, ids)
            }
        }
    }

    private suspend fun renderAll(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val database = AppDatabase.getDatabase(context)
        val examDao = database.examDao()
        val sessionDao = database.sessionDao()

        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val exams = examDao.getUpcomingExams(todayCalendar.timeInMillis)
        val completedSessions = sessionDao.getAllSessionsList().filter { it.completed }

        // Compute statistics
        val totalSeconds = completedSessions.sumOf { it.durationSeconds }
        val totalFocusHours = totalSeconds / 3600.0
        val totalPoints = completedSessions.sumOf { it.focusScore }
        val sessionsCount = completedSessions.size
        val currentStreak = calculateStreak(completedSessions)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val launchPendingIntent = PendingIntent.getActivity(
            context, 100, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_countdown_layout)

            applyTheme(context, views)

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

            // Compact mode check based on widget dimensions
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            val compact = minHeightDp < 130

            var nextExamName = ""
            var nextExamDaysLeft = 0

            // Bind list of exams (up to 3 slots)
            if (exams.isEmpty()) {
                views.setViewVisibility(R.id.widget_no_exams_text, View.VISIBLE)
                views.setViewVisibility(R.id.widget_exams_list_container, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_no_exams_text, View.GONE)
                views.setViewVisibility(R.id.widget_exams_list_container, View.VISIBLE)

                // Slot 1
                if (exams.isNotEmpty()) {
                    views.setViewVisibility(R.id.widget_exam1_container, View.VISIBLE)
                    val ex = exams[0]
                    nextExamName = ex.name
                    val examCal = Calendar.getInstance().apply {
                        timeInMillis = ex.examDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    nextExamDaysLeft = ((examCal.timeInMillis - todayCalendar.timeInMillis) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(0)

                    views.setTextViewText(R.id.widget_exam1_name, ex.name)
                    views.setTextViewText(
                        R.id.widget_exam1_days,
                        daysLeftLabel(ex.examDate, todayCalendar.timeInMillis, verbose = true)
                    )
                } else {
                    views.setViewVisibility(R.id.widget_exam1_container, View.GONE)
                }

                // Slot 2
                if (exams.size > 1) {
                    views.setViewVisibility(R.id.widget_exam2_container, View.VISIBLE)
                    val ex = exams[1]
                    views.setTextViewText(R.id.widget_exam2_name, ex.name)
                    views.setTextViewText(
                        R.id.widget_exam2_days,
                        daysLeftLabel(ex.examDate, todayCalendar.timeInMillis, verbose = false)
                    )
                } else {
                    views.setViewVisibility(R.id.widget_exam2_container, View.GONE)
                }

                // Slot 3 (hidden in compact mode or if < 3 exams)
                if (exams.size > 2 && !compact) {
                    views.setViewVisibility(R.id.widget_exam3_container, View.VISIBLE)
                    val ex = exams[2]
                    views.setTextViewText(R.id.widget_exam3_name, ex.name)
                    views.setTextViewText(
                        R.id.widget_exam3_days,
                        daysLeftLabel(ex.examDate, todayCalendar.timeInMillis, verbose = false)
                    )
                } else {
                    views.setViewVisibility(R.id.widget_exam3_container, View.GONE)
                }
            }

            // Click pending intent & accessibility description
            views.setOnClickPendingIntent(R.id.widget_container, launchPendingIntent)
            views.setContentDescription(
                R.id.widget_container,
                if (exams.isNotEmpty()) {
                    context.getString(R.string.wgt_countdown_a11y, nextExamName, nextExamDaysLeft)
                } else {
                    context.getString(R.string.wgt_no_exams)
                }
            )

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun daysLeftLabel(examTime: Long, todayTime: Long, verbose: Boolean): String {
        val exam = Calendar.getInstance().apply {
            timeInMillis = examTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val days = ((exam.timeInMillis - todayTime) / (24 * 60 * 60 * 1000L)).toInt()
        return when {
            days < 0 -> "Passed"
            days == 0 -> "Today"
            days == 1 -> if (verbose) "1 Day" else "1d"
            else -> if (verbose) "$days Days" else "${days}d"
        }
    }

    private fun applyTheme(context: Context, views: RemoteViews) {
        val p = WidgetTheme.resolve(context)

        val textOnSurface = intArrayOf(
            R.id.widget_title_label, R.id.widget_stats_title_label,
            R.id.widget_exam1_name,
            R.id.widget_stat_focus_value, R.id.widget_stat_points_value,
            R.id.widget_stat_sessions_value, R.id.widget_stat_streak_value
        )
        val textVariant = intArrayOf(
            R.id.widget_no_exams_text, R.id.widget_exam2_name, R.id.widget_exam3_name,
            R.id.widget_stat_focus_label, R.id.widget_stat_points_label,
            R.id.widget_stat_sessions_label, R.id.widget_stat_streak_label
        )
        val textAccent = intArrayOf(
            R.id.widget_exam1_days, R.id.widget_exam2_days, R.id.widget_exam3_days
        )
        val icons = intArrayOf(
            R.id.widget_icon_exams_header, R.id.widget_icon_stats_header,
            R.id.widget_icon_focus, R.id.widget_icon_points,
            R.id.widget_icon_sessions, R.id.widget_icon_streak
        )

        textOnSurface.forEach { views.setTextColor(it, p.onSurface) }
        textVariant.forEach { views.setTextColor(it, p.onSurfaceVariant) }
        textAccent.forEach { views.setTextColor(it, p.accent) }
        icons.forEach { views.setInt(it, "setColorFilter", p.onSurfaceVariant) }

        views.setInt(R.id.widget_vertical_divider, "setBackgroundColor", p.divider)
        views.setInt(R.id.widget_overlay, "setBackgroundColor", android.graphics.Color.TRANSPARENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setViewOutlinePreferredRadius(
                R.id.widget_container, 24f, TypedValue.COMPLEX_UNIT_DIP
            )
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

    companion object {
        private const val TAG = "ExamCountdownWidget"

        @JvmStatic
        fun triggerWidgetUpdate(context: Context) {
            WidgetRefreshScheduler.refreshNow(context)
        }

        @JvmStatic
        fun scheduleDayNightAlarm(context: Context) {
            WidgetRefreshScheduler.scheduleNextMidnight(context)
        }
    }
}
