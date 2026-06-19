package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.R
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

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
        // Handle trigger update notifications
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

        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        CoroutineScope(Dispatchers.IO).launch {
            val nextExam = examDao.getNextExam(todayCalendar.timeInMillis)

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isDark = hour !in 6..17

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_countdown_layout)

                // Apply correct backgrounds and text colors dynamically
                if (isDark) {
                    views.setInt(R.id.widget_container, "setBackgroundResource", R.drawable.widget_bg_dark)
                    views.setTextColor(R.id.widget_title_label, android.graphics.Color.parseColor("#9EA4B0"))
                    views.setTextColor(R.id.widget_exam_name, android.graphics.Color.parseColor("#ECF0F3"))
                    views.setTextColor(R.id.widget_days_left, android.graphics.Color.parseColor("#8B84FF"))
                    views.setTextColor(R.id.widget_label_days, android.graphics.Color.parseColor("#9EA4B0"))
                } else {
                    views.setInt(R.id.widget_container, "setBackgroundResource", R.drawable.widget_bg)
                    views.setTextColor(R.id.widget_title_label, android.graphics.Color.parseColor("#8A94A6"))
                    views.setTextColor(R.id.widget_exam_name, android.graphics.Color.parseColor("#2D3142"))
                    views.setTextColor(R.id.widget_days_left, android.graphics.Color.parseColor("#6C63FF"))
                    views.setTextColor(R.id.widget_label_days, android.graphics.Color.parseColor("#8A94A6"))
                }

                if (nextExam != null) {
                    val examCalendar = Calendar.getInstance().apply {
                        timeInMillis = nextExam.examDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val diffMs = examCalendar.timeInMillis - todayCalendar.timeInMillis
                    val daysLeft = (diffMs / (24 * 60 * 60 * 1000L)).toInt()

                    views.setTextViewText(R.id.widget_exam_name, nextExam.name)
                    views.setViewVisibility(R.id.widget_days_left, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_label_days, View.VISIBLE)

                    if (daysLeft < 0) {
                        views.setTextViewText(R.id.widget_days_left, "Passed")
                        views.setViewVisibility(R.id.widget_label_days, View.GONE)
                    } else if (daysLeft == 0) {
                        views.setTextViewText(R.id.widget_days_left, "Today")
                        views.setViewVisibility(R.id.widget_label_days, View.GONE)
                    } else {
                        views.setTextViewText(R.id.widget_days_left, "$daysLeft")
                        views.setViewVisibility(R.id.widget_label_days, View.VISIBLE)
                    }
                } else {
                    views.setTextViewText(R.id.widget_exam_name, "No upcoming exams")
                    views.setViewVisibility(R.id.widget_days_left, View.GONE)
                    views.setViewVisibility(R.id.widget_label_days, View.GONE)
                }

                appWidgetManager.updateAppWidget(widgetId, views)
            }
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
