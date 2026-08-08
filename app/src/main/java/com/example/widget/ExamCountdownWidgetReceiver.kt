package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
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

        CoroutineScope(Dispatchers.IO).launch {
            val exams = examDao.getUpcomingExams(todayCalendar.timeInMillis)
            val completedSessions = sessionDao.getAllSessionsList().filter { it.completed }

            // Compute statistics
            val totalSeconds = completedSessions.sumOf { it.durationSeconds }
            val totalFocusHours = totalSeconds / 3600.0
            val totalPoints = completedSessions.sumOf { it.focusScore }
            val sessionsCount = completedSessions.size
            val currentStreak = calculateStreak(completedSessions)

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isDay = hour in 6..17

            // Render sleek professional Neumorphic translucent card bitmap
            val neumorphicBitmap = getNeumorphicWidgetBitmap(context, isDay)

            for (i in 0 until appWidgetIds.size) {
                val widgetId = appWidgetIds[i]
                val views = RemoteViews(context.packageName, R.layout.widget_countdown_layout)

                // Assign neumorphic bitmap to image background
                views.setImageViewBitmap(R.id.widget_background, neumorphicBitmap)

                if (isDay) {
                    views.setInt(R.id.widget_overlay, "setBackgroundColor", android.graphics.Color.TRANSPARENT)
                    
                    // Headers
                    views.setTextColor(R.id.widget_title_label, android.graphics.Color.parseColor("#111827"))
                    views.setTextColor(R.id.widget_stats_title_label, android.graphics.Color.parseColor("#111827"))
                    views.setTextColor(R.id.widget_no_exams_text, android.graphics.Color.parseColor("#64748B"))

                    // Left Column Exam Items
                    views.setTextColor(R.id.widget_exam1_name, android.graphics.Color.parseColor("#111827"))
                    views.setTextColor(R.id.widget_exam1_days, android.graphics.Color.parseColor("#2563EB"))
                    views.setTextColor(R.id.widget_exam2_name, android.graphics.Color.parseColor("#374151"))
                    views.setTextColor(R.id.widget_exam2_days, android.graphics.Color.parseColor("#2563EB"))
                    views.setTextColor(R.id.widget_exam3_name, android.graphics.Color.parseColor("#374151"))
                    views.setTextColor(R.id.widget_exam3_days, android.graphics.Color.parseColor("#2563EB"))

                    // Stats Labels & Values
                    views.setTextColor(R.id.widget_stat_focus_value, android.graphics.Color.parseColor("#111827"))
                    views.setTextColor(R.id.widget_stat_focus_label, android.graphics.Color.parseColor("#64748B"))
                    views.setTextColor(R.id.widget_stat_points_value, android.graphics.Color.parseColor("#111827"))
                    views.setTextColor(R.id.widget_stat_points_label, android.graphics.Color.parseColor("#64748B"))
                    views.setTextColor(R.id.widget_stat_sessions_value, android.graphics.Color.parseColor("#111827"))
                    views.setTextColor(R.id.widget_stat_sessions_label, android.graphics.Color.parseColor("#64748B"))
                    views.setTextColor(R.id.widget_stat_streak_value, android.graphics.Color.parseColor("#111827"))
                    views.setTextColor(R.id.widget_stat_streak_label, android.graphics.Color.parseColor("#64748B"))

                    // Icon Tints (Dark Charcoal Slate)
                    val iconColor = android.graphics.Color.parseColor("#334155")
                    views.setInt(R.id.widget_icon_exams_header, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_stats_header, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_focus, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_points, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_sessions, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_streak, "setColorFilter", iconColor)

                    // Divider
                    views.setInt(R.id.widget_vertical_divider, "setBackgroundColor", android.graphics.Color.parseColor("#20000000"))
                } else {
                    views.setInt(R.id.widget_overlay, "setBackgroundColor", android.graphics.Color.TRANSPARENT)

                    // Headers
                    views.setTextColor(R.id.widget_title_label, android.graphics.Color.parseColor("#F9FAFB"))
                    views.setTextColor(R.id.widget_stats_title_label, android.graphics.Color.parseColor("#F9FAFB"))
                    views.setTextColor(R.id.widget_no_exams_text, android.graphics.Color.parseColor("#9CA3AF"))

                    // Left Column Exam Items
                    views.setTextColor(R.id.widget_exam1_name, android.graphics.Color.parseColor("#F9FAFB"))
                    views.setTextColor(R.id.widget_exam1_days, android.graphics.Color.parseColor("#60A5FA"))
                    views.setTextColor(R.id.widget_exam2_name, android.graphics.Color.parseColor("#E5E7EB"))
                    views.setTextColor(R.id.widget_exam2_days, android.graphics.Color.parseColor("#60A5FA"))
                    views.setTextColor(R.id.widget_exam3_name, android.graphics.Color.parseColor("#E5E7EB"))
                    views.setTextColor(R.id.widget_exam3_days, android.graphics.Color.parseColor("#60A5FA"))

                    // Stats Labels & Values
                    views.setTextColor(R.id.widget_stat_focus_value, android.graphics.Color.parseColor("#F9FAFB"))
                    views.setTextColor(R.id.widget_stat_focus_label, android.graphics.Color.parseColor("#9CA3AF"))
                    views.setTextColor(R.id.widget_stat_points_value, android.graphics.Color.parseColor("#F9FAFB"))
                    views.setTextColor(R.id.widget_stat_points_label, android.graphics.Color.parseColor("#9CA3AF"))
                    views.setTextColor(R.id.widget_stat_sessions_value, android.graphics.Color.parseColor("#F9FAFB"))
                    views.setTextColor(R.id.widget_stat_sessions_label, android.graphics.Color.parseColor("#9CA3AF"))
                    views.setTextColor(R.id.widget_stat_streak_value, android.graphics.Color.parseColor("#F9FAFB"))
                    views.setTextColor(R.id.widget_stat_streak_label, android.graphics.Color.parseColor("#9CA3AF"))

                    // Icon Tints (Off-White)
                    val iconColor = android.graphics.Color.parseColor("#E2E8F0")
                    views.setInt(R.id.widget_icon_exams_header, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_stats_header, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_focus, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_points, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_sessions, "setColorFilter", iconColor)
                    views.setInt(R.id.widget_icon_streak, "setColorFilter", iconColor)

                    // Divider
                    views.setInt(R.id.widget_vertical_divider, "setBackgroundColor", android.graphics.Color.parseColor("#1FFFFFFF"))
                }

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

    private fun getNeumorphicWidgetBitmap(
        context: Context,
        isDay: Boolean
    ): Bitmap {
        val W = 600f
        val H = 400f
        try {
            val imageBitmap = ImageBitmap(W.toInt(), H.toInt())
            val composeCanvas = Canvas(imageBitmap)
            val drawScope = CanvasDrawScope()

            drawScope.draw(
                density = androidx.compose.ui.unit.Density(context),
                layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
                canvas = composeCanvas,
                size = Size(W, H)
            ) {
                val margin = 12f
                val cardRect = Offset(margin, margin)
                val cardSize = Size(W - margin * 2, H - margin * 2)
                val corner = androidx.compose.ui.geometry.CornerRadius(28f, 28f)

                if (isDay) {
                    // White/Light Mode Neumorphic Translucent Card
                    // Soft dark drop shadow at bottom-right
                    drawRoundRect(
                        color = Color(0x288C9BAE),
                        topLeft = Offset(margin + 5f, margin + 5f),
                        size = cardSize,
                        cornerRadius = corner
                    )
                    // Soft white glow highlight at top-left
                    drawRoundRect(
                        color = Color(0xF0FFFFFF),
                        topLeft = Offset(margin - 3f, margin - 3f),
                        size = cardSize,
                        cornerRadius = corner
                    )
                    // Translucent clean white body (90% opacity)
                    drawRoundRect(
                        color = Color(0xEDF3F6F9),
                        topLeft = cardRect,
                        size = cardSize,
                        cornerRadius = corner,
                        style = androidx.compose.ui.graphics.drawscope.Fill
                    )
                    // Glassy rim border
                    drawRoundRect(
                        color = Color(0x90FFFFFF),
                        topLeft = cardRect,
                        size = cardSize,
                        cornerRadius = corner,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f)
                    )
                } else {
                    // Dark/Black Mode Neumorphic Translucent Card
                    // Soft deep dark drop shadow at bottom-right
                    drawRoundRect(
                        color = Color(0x95000000),
                        topLeft = Offset(margin + 5f, margin + 5f),
                        size = cardSize,
                        cornerRadius = corner
                    )
                    // Soft light highlight glow at top-left
                    drawRoundRect(
                        color = Color(0x28FFFFFF),
                        topLeft = Offset(margin - 2f, margin - 2f),
                        size = cardSize,
                        cornerRadius = corner
                    )
                    // Translucent clean black body (90% opacity)
                    drawRoundRect(
                        color = Color(0xED0F131A),
                        topLeft = cardRect,
                        size = cardSize,
                        cornerRadius = corner,
                        style = androidx.compose.ui.graphics.drawscope.Fill
                    )
                    // Glassy rim border
                    drawRoundRect(
                        color = Color(0x22FFFFFF),
                        topLeft = cardRect,
                        size = cardSize,
                        cornerRadius = corner,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8f)
                    )
                }
            }
            return imageBitmap.asAndroidBitmap()
        } catch (e: Exception) {
            val fallback = Bitmap.createBitmap(W.toInt(), H.toInt(), Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(fallback)
            val paint = android.graphics.Paint()
            paint.color = if (isDay) android.graphics.Color.parseColor("#EDF3F6F9") else android.graphics.Color.parseColor("#ED0F131A")
            canvas.drawRoundRect(12f, 12f, W - 12f, H - 12f, 28f, 28f, paint)
            return fallback
        }
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
