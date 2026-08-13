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
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.db.entity.ExamEntity
import com.example.widget.theme.WidgetTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ExamMatrixWidgetReceiver : AppWidgetProvider() {

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
                updateAllWidgets(context, appWidgetManager, appWidgetIds)
            } catch (t: Throwable) {
                Log.e(TAG, "Widget render failed", t)
                val fallback = RemoteViews(
                    context.packageName,
                    R.layout.widget_exam_matrix_layout
                )
                appWidgetIds.forEach {
                    appWidgetManager.updateAppWidget(it, fallback)
                }
            } finally {
                pending.finish()
            }
        }
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
                    ComponentName(context, ExamMatrixWidgetReceiver::class.java)
                )
                onUpdate(context, mgr, ids)
            }
        }
    }

    private suspend fun updateAllWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val database = AppDatabase.getDatabase(context)
        val examDao = database.examDao()

        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val upcomingExams = examDao.getUpcomingExams(todayCalendar.timeInMillis)
        val matrixBitmap = generateMatrixBitmap(context, todayCalendar, upcomingExams)

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            101,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextExam = upcomingExams.firstOrNull()
        val a11yText = if (nextExam != null) {
            val examStart = Calendar.getInstance().apply {
                timeInMillis = nextExam.examDate
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val daysLeft = ((examStart.timeInMillis - todayCalendar.timeInMillis) / (1000 * 60 * 60 * 24)).coerceAtLeast(0).toInt()
            context.getString(R.string.wgt_countdown_a11y, nextExam.name, daysLeft)
        } else {
            context.getString(R.string.matrix_widget_label)
        }

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_exam_matrix_layout)
            views.setImageViewBitmap(R.id.widget_matrix_image, matrixBitmap)
            views.setOnClickPendingIntent(R.id.widget_matrix_container, pendingIntent)
            views.setContentDescription(R.id.widget_matrix_container, a11yText)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views.setViewOutlinePreferredRadius(
                    R.id.widget_matrix_container, 24f, TypedValue.COMPLEX_UNIT_DIP
                )
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun generateMatrixBitmap(
        context: Context,
        todayCal: Calendar,
        upcomingExams: List<ExamEntity>
    ): Bitmap {
        val p = WidgetTheme.resolve(context)

        val w = 1000f
        val h = 560f
        val bitmap = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val pad = w * 0.030f
        val labelColW = w * 0.105f
        val gridLeft = pad + labelColW
        val gridRight = w - pad
        val footerH = h * 0.135f
        val gridTop = pad
        val gridBottom = h - footerH
        val rowH = (gridBottom - gridTop) / 12f
        val dotStepX = (gridRight - gridLeft) / 31f
        val unit = rowH * 0.34f

        val monthLabels = arrayOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")

        val currentYear = todayCal.get(Calendar.YEAR)
        val currentMonth = todayCal.get(Calendar.MONTH)
        val todayDayOfYear = todayCal.get(Calendar.DAY_OF_YEAR)
        val totalDaysInYear = todayCal.getActualMaximum(Calendar.DAY_OF_YEAR).toFloat()
        val yearProgressPercent = ((todayDayOfYear / totalDaysInYear) * 100).toInt()

        val nextExam = upcomingExams.firstOrNull()
        val nextExamCal = nextExam?.let {
            Calendar.getInstance().apply { timeInMillis = it.examDate }
        }

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

        // Paints
        val monthActive = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.onSurface
            textSize = rowH * 0.62f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val monthDim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.onSurfaceVariant
            textSize = rowH * 0.58f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        val dotPast = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.onSurfaceVariant
            style = Paint.Style.FILL
            alpha = 140
        }
        val dotFuture = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.track
            style = Paint.Style.FILL
        }
        val dotRunup = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.secondary
            style = Paint.Style.FILL
        }
        val todayRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.accent
            style = Paint.Style.STROKE
            strokeWidth = unit * 0.28f
        }
        val todayCore = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.accent
            style = Paint.Style.FILL
        }
        val examRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.critical
            style = Paint.Style.STROKE
            strokeWidth = unit * 0.28f
        }
        val examCore = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.critical
            style = Paint.Style.FILL
        }

        val tempCal = Calendar.getInstance()

        for (m in 0..11) {
            val rowY = gridTop + (m * rowH) + (rowH / 2f)
            val isCurrentMonth = (m == currentMonth)

            val labelText = monthLabels[m]
            val lPaint = if (isCurrentMonth) monthActive else monthDim
            canvas.drawText(labelText, pad, rowY + (rowH * 0.22f), lPaint)

            tempCal.set(currentYear, m, 1)
            val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

            for (d in 1..daysInMonth) {
                tempCal.set(currentYear, m, d)
                val dDayOfYear = tempCal.get(Calendar.DAY_OF_YEAR)
                val dotX = gridLeft + (d - 1) * dotStepX + (dotStepX / 2f)

                val isToday = (dDayOfYear == todayDayOfYear)
                val isExamDay = examDayOfYears.contains(dDayOfYear)
                val isExamRunup = nextExamDayOfYear != null && (dDayOfYear > todayDayOfYear && dDayOfYear < nextExamDayOfYear)
                val isPast = (dDayOfYear < todayDayOfYear)

                when {
                    isExamDay -> {
                        canvas.drawCircle(dotX, rowY, unit * 1.30f, examRing)
                        canvas.drawCircle(dotX, rowY, unit * 0.75f, examCore)
                    }
                    isToday -> {
                        canvas.drawCircle(dotX, rowY, unit * 1.10f, todayRing)
                        canvas.drawCircle(dotX, rowY, unit * 0.44f, todayCore)
                    }
                    isExamRunup -> {
                        canvas.drawCircle(dotX, rowY, unit * 0.84f, dotRunup)
                    }
                    isPast -> {
                        canvas.drawCircle(dotX, rowY, unit * 0.80f, dotPast)
                    }
                    else -> {
                        canvas.drawCircle(dotX, rowY, unit * 0.72f, dotFuture)
                    }
                }
            }
        }

        // Year Progress Track in Footer
        val trackY = gridBottom + footerH * 0.22f
        val trackH = h * 0.011f
        val trackRect = RectF(pad, trackY, w - pad, trackY + trackH)
        canvas.drawRoundRect(
            trackRect, trackH / 2f, trackH / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.track; alpha = 120 }
        )

        val filled = pad + (w - pad * 2f) * (yearProgressPercent / 100f)
        canvas.drawRoundRect(
            RectF(pad, trackY, filled, trackY + trackH), trackH / 2f, trackH / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.accent }
        )

        // Footer Text
        val footerY = gridBottom + footerH * 0.78f
        val textLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.onSurface
            textSize = footerH * 0.34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.accent
            textSize = footerH * 0.34f
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

        canvas.drawText(footerLeftText, pad, footerY, textLeftPaint)
        val rightWidth = textRightPaint.measureText(footerRightText)
        canvas.drawText(footerRightText, (w - pad) - rightWidth, footerY, textRightPaint)

        return bitmap
    }

    companion object {
        private const val TAG = "ExamMatrixWidget"

        @JvmStatic
        fun triggerWidgetUpdate(context: Context) {
            WidgetRefreshScheduler.refreshNow(context)
        }
    }
}
