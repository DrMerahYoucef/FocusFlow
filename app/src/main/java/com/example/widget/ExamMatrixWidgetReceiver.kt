package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.db.entity.ExamEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ExamMatrixWidgetReceiver : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        ExamCountdownWidgetReceiver.scheduleDayNightAlarm(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        ExamCountdownWidgetReceiver.scheduleDayNightAlarm(context)
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        ExamCountdownWidgetReceiver.scheduleDayNightAlarm(context)
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, ExamMatrixWidgetReceiver::class.java))
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

        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isDay = hour in 6..17

        CoroutineScope(Dispatchers.IO).launch {
            val upcomingExams = examDao.getUpcomingExams(todayCalendar.timeInMillis)
            val matrixBitmap = generateMatrixBitmap(todayCalendar, upcomingExams, isDay)

            // Intent to open app on widget click
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                101,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_exam_matrix_layout)
                views.setImageViewBitmap(R.id.widget_matrix_image, matrixBitmap)
                views.setOnClickPendingIntent(R.id.widget_matrix_container, pendingIntent)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    private fun generateMatrixBitmap(
        todayCal: Calendar,
        upcomingExams: List<ExamEntity>,
        isDay: Boolean
    ): Bitmap {
        val width = 880f
        val height = 480f
        val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Glassy Container Card for Matrix Grid (Background is transparent so launcher wallpaper shows through)
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

        val cardRect = RectF(12f, 12f, width - 12f, height - 12f)
        val shadowRect = RectF(14f, 14f, width - 14f, height - 14f)
        val innerRadius = 24f
        canvas.drawRoundRect(cardRect, innerRadius, innerRadius, bgPaint)
        canvas.drawRoundRect(cardRect, innerRadius, innerRadius, borderPaint)
        canvas.drawRoundRect(shadowRect, innerRadius - 2f, innerRadius - 2f, shadowBorderPaint)

        // 2. Calculate Matrix Dates & Values
        val monthLabels = arrayOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")

        val currentYear = todayCal.get(Calendar.YEAR)
        val currentMonth = todayCal.get(Calendar.MONTH)
        val todayDayOfYear = todayCal.get(Calendar.DAY_OF_YEAR)
        val totalDaysInYear = todayCal.getActualMaximum(Calendar.DAY_OF_YEAR).toFloat()
        val yearProgressPercent = ((todayDayOfYear / totalDaysInYear) * 100).toInt()

        val nextExam = upcomingExams.firstOrNull()
        val nextExamCal = if (nextExam != null) {
            Calendar.getInstance().apply { timeInMillis = nextExam.examDate }
        } else null

        val daysUntilExam = if (nextExam != null) {
            val examStart = Calendar.getInstance().apply {
                timeInMillis = nextExam.examDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diffMs = examStart.timeInMillis - todayCal.timeInMillis
            (diffMs / (1000 * 60 * 60 * 24)).coerceAtLeast(0).toInt()
        } else -1

        val examDayOfYears = upcomingExams.mapNotNull { exam ->
            val cal = Calendar.getInstance().apply { timeInMillis = exam.examDate }
            if (cal.get(Calendar.YEAR) == currentYear) cal.get(Calendar.DAY_OF_YEAR) else null
        }.toSet()

        val nextExamDayOfYear = nextExamCal?.let {
            if (it.get(Calendar.YEAR) == currentYear) it.get(Calendar.DAY_OF_YEAR) else null
        }

        // 6. Color Schemes for Matrix Elements (Day vs Night)
        val activeMonthColor = if (isDay) android.graphics.Color.parseColor("#1B4324") else android.graphics.Color.parseColor("#FFFFFF")
        val dimMonthColor = if (isDay) android.graphics.Color.parseColor("#4A6B53") else android.graphics.Color.parseColor("#7A808E")

        val labelPaintActive = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = activeMonthColor
            textSize = 21f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val labelPaintDim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dimMonthColor
            textSize = 21f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        val passedColor = if (isDay) android.graphics.Color.parseColor("#286532") else android.graphics.Color.parseColor("#E0E4EC")
        val todayRingColor = if (isDay) android.graphics.Color.parseColor("#00838F") else android.graphics.Color.parseColor("#00E5FF")
        val todayInnerColor = if (isDay) android.graphics.Color.parseColor("#00838F") else android.graphics.Color.parseColor("#00E5FF")
        val runupColor = if (isDay) android.graphics.Color.parseColor("#6750A4") else android.graphics.Color.parseColor("#8B84FF")
        val examOuterColor = if (isDay) android.graphics.Color.parseColor("#D32F2F") else android.graphics.Color.parseColor("#FF5252")
        val examInnerColor = if (isDay) android.graphics.Color.parseColor("#D32F2F") else android.graphics.Color.parseColor("#FF5252")
        val futureColor = if (isDay) android.graphics.Color.parseColor("#A0B0A8") else android.graphics.Color.parseColor("#2C2F38")

        val dotPassedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = passedColor
            style = Paint.Style.FILL
        }
        val dotTodayRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = todayRingColor
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val dotTodayInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = todayInnerColor
            style = Paint.Style.FILL
        }
        val dotExamRunupPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = runupColor
            style = Paint.Style.FILL
        }
        val dotExamDayOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = examOuterColor
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val dotExamDayInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = examInnerColor
            style = Paint.Style.FILL
        }
        val dotFuturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = futureColor
            style = Paint.Style.FILL
        }

        // Layout Geometry
        val gridTop = 40f
        val gridHeight = 356f
        val rowHeight = gridHeight / 12f
        val startX = 142f
        val endX = width - 42f
        val availableWidth = endX - startX
        val dotStepX = availableWidth / 31f

        val tempCal = Calendar.getInstance()

        for (m in 0..11) {
            val rowY = gridTop + (m * rowHeight) + (rowHeight / 2f) + 4f
            val isCurrentMonth = (m == currentMonth)

            // Draw Month Label
            val labelText = monthLabels[m]
            val lPaint = if (isCurrentMonth) labelPaintActive else labelPaintDim
            canvas.drawText(labelText, 42f, rowY + 6f, lPaint)

            // Days in month m
            tempCal.set(currentYear, m, 1)
            val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

            for (d in 1..daysInMonth) {
                tempCal.set(currentYear, m, d)
                val dDayOfYear = tempCal.get(Calendar.DAY_OF_YEAR)
                val dotX = startX + (d - 1) * dotStepX + (dotStepX / 2f)

                val isToday = (dDayOfYear == todayDayOfYear)
                val isExamDay = examDayOfYears.contains(dDayOfYear)
                val isExamRunup = nextExamDayOfYear != null && (dDayOfYear > todayDayOfYear && dDayOfYear < nextExamDayOfYear)
                val isPast = (dDayOfYear < todayDayOfYear)

                when {
                    isExamDay -> {
                        canvas.drawCircle(dotX, rowY, 6.5f, dotExamDayOuterPaint)
                        canvas.drawCircle(dotX, rowY, 3.8f, dotExamDayInnerPaint)
                    }
                    isToday -> {
                        canvas.drawCircle(dotX, rowY, 5.5f, dotTodayRingPaint)
                        canvas.drawCircle(dotX, rowY, 2.2f, dotTodayInnerPaint)
                    }
                    isExamRunup -> {
                        canvas.drawCircle(dotX, rowY, 4.2f, dotExamRunupPaint)
                    }
                    isPast -> {
                        canvas.drawCircle(dotX, rowY, 4.0f, dotPassedPaint)
                    }
                    else -> {
                        canvas.drawCircle(dotX, rowY, 3.6f, dotFuturePaint)
                    }
                }
            }
        }

        // 7. Footer Divider & Action Label Text
        val footerLineY = 414f
        val dividerColor = if (isDay) android.graphics.Color.parseColor("#301B4324") else android.graphics.Color.parseColor("#30FFFFFF")
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dividerColor
            strokeWidth = 1.5f
        }
        canvas.drawLine(42f, footerLineY, endX, footerLineY, dividerPaint)

        val footerY = 450f
        val footerLeftColor = if (isDay) android.graphics.Color.parseColor("#1B4324") else android.graphics.Color.parseColor("#FFFFFF")
        val footerRightColor = if (isDay) android.graphics.Color.parseColor("#6750A4") else android.graphics.Color.parseColor("#8B84FF")

        val textLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = footerLeftColor
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = footerRightColor
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val footerLeftText: String
        val footerRightText: String

        if (nextExam != null) {
            val examTitle = if (nextExam.name.length > 18) nextExam.name.take(16) + "…" else nextExam.name
            footerLeftText = "NEXT EXAM: ${examTitle.uppercase()}"
            footerRightText = if (daysUntilExam == 0) "TODAY!" else "IN $daysUntilExam DAYS"
        } else {
            footerLeftText = "YEAR PROGRESS - $yearProgressPercent%"
            footerRightText = "NO UPCOMING EXAMS"
        }

        canvas.drawText(footerLeftText, 42f, footerY, textLeftPaint)
        val rightWidth = textRightPaint.measureText(footerRightText)
        canvas.drawText(footerRightText, endX - rightWidth, footerY, textRightPaint)

        return bitmap
    }

    companion object {
        fun triggerWidgetUpdate(context: Context) {
            val intent = Intent(context, ExamMatrixWidgetReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }
    }
}
