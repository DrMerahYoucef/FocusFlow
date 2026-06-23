package com.focusisland.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.focusisland.R
import com.focusisland.data.db.AppDatabase
import com.focusisland.data.db.entity.SessionEntity
import com.focusisland.ui.components.DeterministicTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import kotlin.math.sin

class ExamCountdownWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
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

            // Paint deterministic forest background custom bitmap matching current sessions count
            val forestBitmap = getForestWidgetBitmap(context, isDay, sessionsCount)

            for (i in 0 until appWidgetIds.size) {
                val widgetId = appWidgetIds[i]
                val views = RemoteViews(context.packageName, R.layout.widget_countdown_layout)

                // Assign rendered forest bitmap to image background
                views.setImageViewBitmap(R.id.widget_background, forestBitmap)

                // Adjust overlay background color and text themes depending on day/night
                if (isDay) {
                    views.setInt(R.id.widget_overlay, "setBackgroundColor", android.graphics.Color.parseColor("#CFE2F8F4"))
                    views.setTextColor(R.id.widget_title_label, android.graphics.Color.parseColor("#4A6B53"))
                    views.setTextColor(R.id.widget_stats_title_label, android.graphics.Color.parseColor("#4A6B53"))
                    views.setTextColor(R.id.widget_no_exams_text, android.graphics.Color.parseColor("#286532"))
                    
                    // Left column Exam text colors
                    views.setTextColor(R.id.widget_exam1_name, android.graphics.Color.parseColor("#1B4324"))
                    views.setTextColor(R.id.widget_exam1_days, android.graphics.Color.parseColor("#286532"))
                    views.setTextColor(R.id.widget_exam2_name, android.graphics.Color.parseColor("#1B4324"))
                    views.setTextColor(R.id.widget_exam2_days, android.graphics.Color.parseColor("#286532"))
                    views.setTextColor(R.id.widget_exam3_name, android.graphics.Color.parseColor("#1B4324"))
                    views.setTextColor(R.id.widget_exam3_days, android.graphics.Color.parseColor("#286532"))

                    // Stats Labels & Values
                    views.setTextColor(R.id.widget_stat_focus_value, android.graphics.Color.parseColor("#1B4324"))
                    views.setTextColor(R.id.widget_stat_focus_label, android.graphics.Color.parseColor("#4A6B53"))
                    views.setTextColor(R.id.widget_stat_points_value, android.graphics.Color.parseColor("#1B4324"))
                    views.setTextColor(R.id.widget_stat_points_label, android.graphics.Color.parseColor("#4A6B53"))
                    views.setTextColor(R.id.widget_stat_sessions_value, android.graphics.Color.parseColor("#1B4324"))
                    views.setTextColor(R.id.widget_stat_sessions_label, android.graphics.Color.parseColor("#4A6B53"))
                    views.setTextColor(R.id.widget_stat_streak_value, android.graphics.Color.parseColor("#1B4324"))
                    views.setTextColor(R.id.widget_stat_streak_label, android.graphics.Color.parseColor("#4A6B53"))
                    
                    // Divider
                    views.setInt(R.id.widget_vertical_divider, "setBackgroundColor", android.graphics.Color.parseColor("#401B4324"))
                } else {
                    views.setInt(R.id.widget_overlay, "setBackgroundColor", android.graphics.Color.parseColor("#CC030A0E"))
                    views.setTextColor(R.id.widget_title_label, android.graphics.Color.parseColor("#9EA4B0"))
                    views.setTextColor(R.id.widget_stats_title_label, android.graphics.Color.parseColor("#9EA4B0"))
                    views.setTextColor(R.id.widget_no_exams_text, android.graphics.Color.parseColor("#9EA4B0"))

                    // Left column Exam text colors
                    views.setTextColor(R.id.widget_exam1_name, android.graphics.Color.parseColor("#FFFFFF"))
                    views.setTextColor(R.id.widget_exam1_days, android.graphics.Color.parseColor("#8B84FF"))
                    views.setTextColor(R.id.widget_exam2_name, android.graphics.Color.parseColor("#FFFFFF"))
                    views.setTextColor(R.id.widget_exam2_days, android.graphics.Color.parseColor("#8B84FF"))
                    views.setTextColor(R.id.widget_exam3_name, android.graphics.Color.parseColor("#FFFFFF"))
                    views.setTextColor(R.id.widget_exam3_days, android.graphics.Color.parseColor("#8B84FF"))
                    
                    // Stats Labels & Values
                    views.setTextColor(R.id.widget_stat_focus_value, android.graphics.Color.parseColor("#FFFFFF"))
                    views.setTextColor(R.id.widget_stat_focus_label, android.graphics.Color.parseColor("#9EA4B0"))
                    views.setTextColor(R.id.widget_stat_points_value, android.graphics.Color.parseColor("#FFFFFF"))
                    views.setTextColor(R.id.widget_stat_points_label, android.graphics.Color.parseColor("#9EA4B0"))
                    views.setTextColor(R.id.widget_stat_sessions_value, android.graphics.Color.parseColor("#FFFFFF"))
                    views.setTextColor(R.id.widget_stat_sessions_label, android.graphics.Color.parseColor("#9EA4B0"))
                    views.setTextColor(R.id.widget_stat_streak_value, android.graphics.Color.parseColor("#FFFFFF"))
                    views.setTextColor(R.id.widget_stat_streak_label, android.graphics.Color.parseColor("#9EA4B0"))
                    
                    // Divider
                    views.setInt(R.id.widget_vertical_divider, "setBackgroundColor", android.graphics.Color.parseColor("#33FFFFFF"))
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
                        views.setTextViewText(R.id.widget_exam1_days, getDaysLeftString(ex.examDate, todayCalendar.timeInMillis))
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

    private fun getForestWidgetBitmap(
        context: Context,
        isDay: Boolean,
        treeCount: Int
    ): Bitmap {
        val W = 400f
        val H = 300f
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
                // Background sky color
                val skyTop = if (isDay) Color(0xFF90DBE1) else Color(0xFF0E1A29)
                val skyBottom = if (isDay) Color(0xFFE2F8F4) else Color(0xFF1C344A)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(skyTop, skyBottom),
                        startY = 0f, endY = H
                    )
                )

                if (!isDay) {
                    // Draw stars
                    val r = java.util.Random(101)
                    repeat(20) {
                        val sx = r.nextFloat() * W
                        val sy = r.nextFloat() * H * 0.45f
                        drawCircle(
                            color = Color.White.copy(alpha = 0.4f + r.nextFloat() * 0.5f),
                            radius = 1.5f + r.nextFloat() * 2f,
                            center = Offset(sx, sy)
                        )
                    }
                    // Moon
                    val moonCenter = Offset(W * 0.8f, H * 0.15f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x22ADC6D1), Color.Transparent),
                            center = moonCenter, radius = W * 0.35f
                        ),
                        radius = W * 0.35f, center = moonCenter
                    )
                    drawCircle(
                        color = Color(0xFFE9F5F8),
                        radius = W * 0.05f,
                        center = moonCenter
                    )
                } else {
                    // Sun
                    val sunCenter = Offset(W * 0.5f, H * 0.08f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x44FFEFA8), Color.Transparent),
                            center = sunCenter, radius = W * 0.4f
                        ),
                        radius = W * 0.4f, center = sunCenter
                    )
                    drawCircle(
                        color = Color(0xFFFFFCEB),
                        radius = W * 0.05f,
                        center = sunCenter
                    )
                }

                // Deterministic Tree Layout
                val count = maxOf(treeCount, 1)
                val layers = List(6) { ArrayList<DeterministicTree>() }
                for (i in 0 until count) {
                    val iLong = i.toLong()
                    val seedX = (iLong * 19349663L) xor 0x5DEECE66DL
                    val seedH = (iLong * 38260237L) xor 0x5DEECE66DL
                    val seedW = (iLong * 85038241L) xor 0x5DEECE66DL

                    val randX = (((seedX xor 12345L) % 10000L).toFloat() / 10000f).coerceIn(0f, 1f)
                    val randH = (((seedH xor 54321L) % 10000L).toFloat() / 10000f).coerceIn(0f, 1f)
                    val randW = (((seedW xor 98765L) % 10000L).toFloat() / 10000f).coerceIn(0f, 1f)

                    val layerIdx = i % 6
                    val xFraction = -0.12f + randX * 1.24f
                    val heightScale = 0.70f + randH * 0.45f
                    val widthScale = 0.75f + randW * 0.35f

                    layers[layerIdx].add(
                        DeterministicTree(
                            xFraction = xFraction,
                            heightScale = heightScale,
                            horizontalSpacing = widthScale,
                            layerIndex = layerIdx,
                            itemIndex = i
                        )
                    )
                }

                val cachedTreePath = Path()
                val cachedShadowPath = Path()

                // Draw layers
                for (layerIdx in 0..5) {
                    val scale = when (layerIdx) {
                        0 -> 0.35f
                        1 -> 0.45f
                        2 -> 0.58f
                        3 -> 0.72f
                        4 -> 0.86f
                        else -> 1.00f
                    }
                    val yFraction = when (layerIdx) {
                        0 -> 0.38f
                        1 -> 0.30f
                        2 -> 0.22f
                        3 -> 0.14f
                        4 -> 0.06f
                        else -> 0.00f
                    }
                    val baseColor = when (layerIdx) {
                        0 -> if (isDay) Color(0xFFCBE3CC) else Color(0xFF1E3A36)
                        1 -> if (isDay) Color(0xFFADCEB1) else Color(0xFF162F2B)
                        2 -> if (isDay) Color(0xFF8BB78F) else Color(0xFF112622)
                        3 -> if (isDay) Color(0xFF639D69) else Color(0xFF0C1E1B)
                        4 -> if (isDay) Color(0xFF42824B) else Color(0xFF071614)
                        else -> if (isDay) Color(0xFF286532) else Color(0xFF030D0C)
                    }

                    val list = layers[layerIdx]
                    val ceiling = when (layerIdx) {
                        0 -> 10
                        1 -> 12
                        2 -> 15
                        3 -> 18
                        4 -> 20
                        else -> 22
                    }
                    val treesInLayer = if (list.size <= ceiling) list else list.take(ceiling)

                    val treeH = H * scale * 0.28f
                    val treeW = treeH * 0.45f
                    val baseY = H * (1f - yFraction)

                    treesInLayer.forEach { tree ->
                        val x = tree.xFraction * W
                        val y = baseY + (tree.itemIndex % 2) * treeH * 0.04f
                        val finalH = treeH * tree.heightScale
                        val finalW = treeW * tree.horizontalSpacing

                        // Draw tree
                        cachedTreePath.reset()
                        cachedTreePath.moveTo(x, y - finalH)
                        cachedTreePath.lineTo(x - finalW * 0.14f, y - finalH + finalH * 0.10f)
                        cachedTreePath.lineTo(x - finalW * 0.07f, y - finalH + finalH * 0.12f)
                        cachedTreePath.lineTo(x - finalW * 0.23f, y - finalH + finalH * 0.22f)
                        cachedTreePath.lineTo(x - finalW * 0.11f, y - finalH + finalH * 0.25f)
                        cachedTreePath.lineTo(x - finalW * 0.33f, y - finalH + finalH * 0.36f)
                        cachedTreePath.lineTo(x - finalW * 0.16f, y - finalH + finalH * 0.39f)
                        cachedTreePath.lineTo(x - finalW * 0.43f, y - finalH + finalH * 0.49f)
                        cachedTreePath.lineTo(x - finalW * 0.21f, y - finalH + finalH * 0.52f)
                        cachedTreePath.lineTo(x - finalW * 0.53f, y - finalH + finalH * 0.64f)
                        cachedTreePath.lineTo(x - finalW * 0.26f, y - finalH + finalH * 0.67f)
                        cachedTreePath.lineTo(x - finalW * 0.63f, y - finalH + finalH * 0.78f)
                        cachedTreePath.lineTo(x - finalW * 0.30f, y - finalH + finalH * 0.82f)
                        cachedTreePath.lineTo(x - finalW * 0.72f, y - finalH + finalH * 0.94f)
                        cachedTreePath.lineTo(x - finalW * 0.33f, y)
                        cachedTreePath.lineTo(x - finalW * 0.08f, y)
                        cachedTreePath.lineTo(x - finalW * 0.08f, y + finalH * 0.12f)
                        cachedTreePath.lineTo(x + finalW * 0.08f, y + finalH * 0.12f)
                        cachedTreePath.lineTo(x + finalW * 0.08f, y)
                        cachedTreePath.lineTo(x + finalW * 0.33f, y)
                        cachedTreePath.lineTo(x + finalW * 0.72f, y - finalH + finalH * 0.94f)
                        cachedTreePath.lineTo(x + finalW * 0.30f, y - finalH + finalH * 0.82f)
                        cachedTreePath.lineTo(x + finalW * 0.63f, y - finalH + finalH * 0.78f)
                        cachedTreePath.lineTo(x + finalW * 0.26f, y - finalH + finalH * 0.67f)
                        cachedTreePath.lineTo(x + finalW * 0.53f, y - finalH + finalH * 0.64f)
                        cachedTreePath.lineTo(x + finalW * 0.21f, y - finalH + finalH * 0.52f)
                        cachedTreePath.lineTo(x + finalW * 0.43f, y - finalH + finalH * 0.49f)
                        cachedTreePath.lineTo(x + finalW * 0.16f, y - finalH + finalH * 0.39f)
                        cachedTreePath.lineTo(x + finalW * 0.33f, y - finalH + finalH * 0.36f)
                        cachedTreePath.lineTo(x + finalW * 0.11f, y - finalH + finalH * 0.25f)
                        cachedTreePath.lineTo(x + finalW * 0.23f, y - finalH + finalH * 0.22f)
                        cachedTreePath.lineTo(x + finalW * 0.07f, y - finalH + finalH * 0.12f)
                        cachedTreePath.lineTo(x + finalW * 0.14f, y - finalH + finalH * 0.10f)
                        cachedTreePath.close()

                        drawPath(cachedTreePath, baseColor)

                        // Draw simple shadows
                        cachedShadowPath.reset()
                        cachedShadowPath.moveTo(x, y - finalH)
                        cachedShadowPath.lineTo(x + finalW * 0.14f, y - finalH + finalH * 0.10f)
                        cachedShadowPath.lineTo(x + finalW * 0.07f, y - finalH + finalH * 0.12f)
                        cachedShadowPath.lineTo(x + finalW * 0.23f, y - finalH + finalH * 0.22f)
                        cachedShadowPath.lineTo(x + finalW * 0.11f, y - finalH + finalH * 0.25f)
                        cachedShadowPath.lineTo(x + finalW * 0.33f, y - finalH + finalH * 0.36f)
                        cachedShadowPath.lineTo(x + finalW * 0.16f, y - finalH + finalH * 0.39f)
                        cachedShadowPath.lineTo(x + finalW * 0.43f, y - finalH + finalH * 0.49f)
                        cachedShadowPath.lineTo(x + finalW * 0.21f, y - finalH + finalH * 0.52f)
                        cachedShadowPath.lineTo(x + finalW * 0.53f, y - finalH + finalH * 0.64f)
                        cachedShadowPath.lineTo(x + finalW * 0.26f, y - finalH + finalH * 0.67f)
                        cachedShadowPath.lineTo(x + finalW * 0.63f, y - finalH + finalH * 0.78f)
                        cachedShadowPath.lineTo(x + finalW * 0.30f, y - finalH + finalH * 0.82f)
                        cachedShadowPath.lineTo(x + finalW * 0.72f, y - finalH + finalH * 0.94f)
                        cachedShadowPath.lineTo(x + finalW * 0.33f, y)
                        cachedShadowPath.lineTo(x + finalW * 0.08f, y)
                        cachedShadowPath.lineTo(x + finalW * 0.08f, y + finalH * 0.12f)
                        cachedShadowPath.lineTo(x, y + finalH * 0.12f)
                        cachedShadowPath.lineTo(x, y)
                        cachedShadowPath.lineTo(x, y - finalH)
                        cachedShadowPath.close()

                        val shadowOpacity = if (isDay) 0.18f else 0.24f
                        drawPath(cachedShadowPath, Color.Black.copy(alpha = shadowOpacity))
                    }

                    // Fog drawing
                    val fogColor = if (isDay) Color(0xFFE5FBF6) else Color(0xFF1F353A)
                    val fogY = H * (1f - yFraction) - H * 0.02f
                    val fogAlpha = when (layerIdx) {
                        0 -> 0.12f
                        1 -> 0.10f
                        2 -> 0.08f
                        3 -> 0.06f
                        4 -> 0.05f
                        else -> 0.04f
                    }
                    val fogScale = when (layerIdx) {
                        0 -> 1.2f
                        1 -> 1.0f
                        2 -> 0.85f
                        3 -> 0.70f
                        4 -> 0.55f
                        else -> 0.40f
                    }

                    val fogPath = Path()
                    fogPath.moveTo(0f, H)
                    val steps = 15
                    val stepW = W / steps
                    for (step in 0..steps) {
                        val fx = step * stepW
                        val waveY = fogY + sin(fx * 0.005f) * 8f * fogScale
                        fogPath.lineTo(fx, waveY)
                    }
                    fogPath.lineTo(W, H)
                    fogPath.close()

                    val mistHeight = 110f * fogScale
                    drawPath(
                        path = fogPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                fogColor.copy(alpha = fogAlpha * 0.7f),
                                fogColor.copy(alpha = fogAlpha * 1.2f),
                                Color.Transparent
                            ),
                            startY = fogY - mistHeight * 0.3f,
                            endY = fogY + mistHeight * 0.7f
                        )
                    )
                }
            }
            return imageBitmap.asAndroidBitmap()
        } catch (e: Exception) {
            android.util.Log.e("ExamCountdownWidget", "Failed to render forest background: ${e.message}")
            val fallback = Bitmap.createBitmap(W.toInt(), H.toInt(), Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(fallback)
            val paint = android.graphics.Paint()
            paint.color = if (isDay) android.graphics.Color.parseColor("#90DBE1") else android.graphics.Color.parseColor("#030A0E")
            canvas.drawRect(0f, 0f, W, H, paint)
            return fallback
        }
    }

    companion object {
        fun triggerWidgetUpdate(context: Context) {
            val intent = Intent(context, ExamCountdownWidgetReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }
    }
}
