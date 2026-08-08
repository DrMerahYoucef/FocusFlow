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

        // 1. Draw Neumorphic Translucent Card Background
        val margin = 16f
        val cardRect = RectF(margin, margin, width - margin, height - margin)
        val corner = 32f

        if (isDay) {
            // White / Light Mode Neumorphic Translucent Card
            val darkShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#288C9BAE")
                style = Paint.Style.FILL
            }
            val lightGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#F0FFFFFF")
                style = Paint.Style.FILL
            }
            val bodyFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#EDF3F6F9") // Translucent clean white
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#90FFFFFF")
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
            }

            // Bottom-right shadow
            canvas.drawRoundRect(RectF(margin + 6f, margin + 6f, width - margin + 6f, height - margin + 6f), corner, corner, darkShadowPaint)
            // Top-left glow
            canvas.drawRoundRect(RectF(margin - 4f, margin - 4f, width - margin - 4f, height - margin - 4f), corner, corner, lightGlowPaint)
            // Body fill
            canvas.drawRoundRect(cardRect, corner, corner, bodyFillPaint)
            // Glassy rim border
            canvas.drawRoundRect(cardRect, corner, corner, borderPaint)
        } else {
            // Dark / Black Mode Neumorphic Translucent Card
            val darkShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#95000000")
                style = Paint.Style.FILL
            }
            val lightGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#28FFFFFF")
                style = Paint.Style.FILL
            }
            val bodyFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#ED0F131A") // Translucent clean black
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#22FFFFFF")
                style = Paint.Style.STROKE
                strokeWidth = 2.2f
            }

            // Bottom-right shadow
            canvas.drawRoundRect(RectF(margin + 6f, margin + 6f, width - margin + 6f, height - margin + 6f), corner, corner, darkShadowPaint)
            // Top-left glow
            canvas.drawRoundRect(RectF(margin - 3f, margin - 3f, width - margin - 3f, height - margin - 3f), corner, corner, lightGlowPaint)
            // Body fill
            canvas.drawRoundRect(cardRect, corner, corner, bodyFillPaint)
            // Glassy rim border
            canvas.drawRoundRect(cardRect, corner, corner, borderPaint)
        }

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

        // 3. Color Schemes for Matrix Elements
        val activeMonthColor = if (isDay) android.graphics.Color.parseColor("#111827") else android.graphics.Color.parseColor("#F9FAFB")
        val dimMonthColor = if (isDay) android.graphics.Color.parseColor("#64748B") else android.graphics.Color.parseColor("#9CA3AF")

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

        val passedColor = if (isDay) android.graphics.Color.parseColor("#334155") else android.graphics.Color.parseColor("#E5E7EB")
        val todayRingColor = if (isDay) android.graphics.Color.parseColor("#2563EB") else android.graphics.Color.parseColor("#60A5FA")
        val todayInnerColor = if (isDay) android.graphics.Color.parseColor("#2563EB") else android.graphics.Color.parseColor("#60A5FA")
        val runupColor = if (isDay) android.graphics.Color.parseColor("#7C3AED") else android.graphics.Color.parseColor("#A78BFA")
        val examOuterColor = if (isDay) android.graphics.Color.parseColor("#DC2626") else android.graphics.Color.parseColor("#EF4444")
        val examInnerColor = if (isDay) android.graphics.Color.parseColor("#DC2626") else android.graphics.Color.parseColor("#EF4444")
        val futureColor = if (isDay) android.graphics.Color.parseColor("#CBD5E1") else android.graphics.Color.parseColor("#374151")

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
        val gridTop = 38f
        val gridHeight = 358f
        val rowHeight = gridHeight / 12f
        val startX = 142f
        val endX = width - 42f
        val availableWidth = endX - startX
        val dotStepX = availableWidth / 31f

        val tempCal = Calendar.getInstance()

        for (m in 0..11) {
            val rowY = gridTop + (m * rowHeight) + (rowHeight / 2f) + 4f
            val isCurrentMonth = (m == currentMonth)

            val labelText = monthLabels[m]
            val lPaint = if (isCurrentMonth) labelPaintActive else labelPaintDim
            canvas.drawText(labelText, 42f, rowY + 6f, lPaint)

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

        // 4. Footer Divider & Action Label Text
        val footerLineY = 414f
        val dividerColor = if (isDay) android.graphics.Color.parseColor("#20000000") else android.graphics.Color.parseColor("#1FFFFFFF")
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dividerColor
            strokeWidth = 1.5f
        }
        canvas.drawLine(42f, footerLineY, endX, footerLineY, dividerPaint)

        val footerY = 450f
        val footerLeftColor = if (isDay) android.graphics.Color.parseColor("#111827") else android.graphics.Color.parseColor("#F9FAFB")
        val footerRightColor = if (isDay) android.graphics.Color.parseColor("#2563EB") else android.graphics.Color.parseColor("#60A5FA")

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
