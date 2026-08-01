package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.db.entity.ExamEntity
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
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, ExamCountdownWidgetReceiver::class.java))
        updateAllWidgets(context, manager, ids)
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

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            100,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val exams = examDao.getUpcomingExams(todayCalendar.timeInMillis)
                val completedSessions = sessionDao.getAllSessionsList().filter { it.completed }

                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val isDay = hour in 6..17

                val widgetBitmap = renderStatsAndCountdownBitmap(
                    isDay = isDay,
                    upcomingExams = exams,
                    completedSessions = completedSessions,
                    todayMs = todayCalendar.timeInMillis
                )

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_countdown_layout)

                    views.setImageViewBitmap(R.id.widget_background, widgetBitmap)
                    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                android.util.Log.e("ExamCountdownWidgetReceiver", "Failed to update widgets", e)
            }
        }
    }

    private fun renderStatsAndCountdownBitmap(
        isDay: Boolean,
        upcomingExams: List<ExamEntity>,
        completedSessions: List<SessionEntity>,
        todayMs: Long
    ): Bitmap {
        val width = 680f
        val height = 360f
        val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val totalSeconds = completedSessions.sumOf { it.durationSeconds }
        val totalFocusHours = totalSeconds / 3600.0
        val totalPoints = completedSessions.sumOf { it.focusScore }
        val sessionsCount = completedSessions.size
        val currentStreak = calculateStreak(completedSessions)

        val cardBgColor = if (isDay) android.graphics.Color.parseColor("#40FFFFFF") else android.graphics.Color.parseColor("#48101A28")
        val cardBorderColor = if (isDay) android.graphics.Color.parseColor("#80FFFFFF") else android.graphics.Color.parseColor("#608B84FF")
        val cardShadowColor = if (isDay) android.graphics.Color.parseColor("#354A6B53") else android.graphics.Color.parseColor("#40FFFFFF")

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cardBgColor
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cardBorderColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val shadowBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cardShadowColor
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }

        val cardRect = RectF(10f, 10f, width - 10f, height - 10f)
        val shadowRect = RectF(12f, 12f, width - 12f, height - 12f)
        val innerRadius = 24f
        canvas.drawRoundRect(cardRect, innerRadius, innerRadius, bgPaint)
        canvas.drawRoundRect(cardRect, innerRadius, innerRadius, borderPaint)
        canvas.drawRoundRect(shadowRect, innerRadius - 2f, innerRadius - 2f, shadowBorderPaint)

        val subTitleColor = if (isDay) android.graphics.Color.parseColor("#4A6B53") else android.graphics.Color.parseColor("#9EA4B0")
        val textColor = if (isDay) android.graphics.Color.parseColor("#1B4324") else android.graphics.Color.parseColor("#FFFFFF")
        val accentColor = if (isDay) android.graphics.Color.parseColor("#6750A4") else android.graphics.Color.parseColor("#8B84FF")

        val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = subTitleColor
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // LEFT COLUMN: Upcoming Exams
        canvas.drawText("🎯 UPCOMING EXAMS", 32f, 50f, subTitlePaint)

        if (upcomingExams.isEmpty()) {
            val noExamsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = subTitleColor
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }
            canvas.drawText("No upcoming exams listed.", 32f, 120f, noExamsPaint)
            canvas.drawText("Great time to study!", 32f, 150f, noExamsPaint)
        } else {
            var startY = 100f
            for (i in 0 until minOf(upcomingExams.size, 3)) {
                val exam = upcomingExams[i]
                val name = if (exam.name.length > 15) exam.name.take(13) + "…" else exam.name
                val daysStr = getDaysLeftStringSpecial(exam.examDate, todayMs)

                canvas.drawText(name, 32f, startY, textPaint)
                val daysWidth = accentPaint.measureText(daysStr)
                canvas.drawText(daysStr, 310f - daysWidth, startY, accentPaint)
                startY += 75f
            }
        }

        // VERTICAL DIVIDER LINE
        val dividerColor = if (isDay) android.graphics.Color.parseColor("#401B4324") else android.graphics.Color.parseColor("#33FFFFFF")
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dividerColor
            strokeWidth = 2f
        }
        canvas.drawLine(330f, 30f, 330f, height - 30f, dividerPaint)

        // RIGHT COLUMN: Focus Stats
        canvas.drawText("📊 ISLAND STATS", 360f, 50f, subTitlePaint)

        val focusStr = if (totalFocusHours >= 10.0) {
            "${totalFocusHours.toInt()}h"
        } else {
            "${totalSeconds / 60}m"
        }

        val stats = listOf(
            Pair("⏱️ Focus", focusStr),
            Pair("⭐ Points", "$totalPoints"),
            Pair("✅ Sessions", "$sessionsCount"),
            Pair("🔥 Streak", "${currentStreak}d")
        )

        var statY = 105f
        for ((label, valStr) in stats) {
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = subTitleColor
                textSize = 16f
            }
            val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(label, 360f, statY, labelPaint)
            val valWidth = valPaint.measureText(valStr)
            canvas.drawText(valStr, width - 32f - valWidth, statY, valPaint)
            statY += 60f
        }

        return bitmap
    }

    companion object {
        fun getDaysLeftStringSpecial(examTime: Long, todayTime: Long): String {
            val examCalendar = Calendar.getInstance().apply {
                timeInMillis = examTime
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diffMs = examCalendar.timeInMillis - todayTime
            val days = (diffMs / (1000 * 60 * 60 * 24)).toInt()

            return when {
                days == 0 -> "TODAY"
                days == 1 -> "1 Day"
                days > 1 -> "$days Days"
                else -> "Passed"
            }
        }

        fun calculateStreak(sessions: List<SessionEntity>): Int {
            if (sessions.isEmpty()) return 0
            val dates = sessions.map { session ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = session.date
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            }.distinct().sortedDescending()

            if (dates.isEmpty()) return 0

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val yesterday = today - (24 * 60 * 60 * 1000L)

            if (!dates.contains(today) && !dates.contains(yesterday)) {
                return 0
            }

            var streak = 0
            var checkDate = if (dates.contains(today)) today else yesterday

            while (dates.contains(checkDate)) {
                streak++
                checkDate -= (24 * 60 * 60 * 1000L)
            }

            return streak
        }

        fun triggerWidgetUpdate(context: Context) {
            val intent = Intent(context, ExamCountdownWidgetReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }

        fun scheduleDayNightAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                ?: return

            val intent = Intent(context, ExamCountdownWidgetReceiver::class.java).apply {
                action = "com.example.ACTION_WIDGET_AUTO_UPDATE"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                9901,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val now = System.currentTimeMillis()
            val nextHour = now + (30 * 60 * 1000L)
            alarmManager.setInexactRepeating(
                android.app.AlarmManager.RTC,
                nextHour,
                android.app.AlarmManager.INTERVAL_HALF_HOUR,
                pendingIntent
            )
        }
    }
}
