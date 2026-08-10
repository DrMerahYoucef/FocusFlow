package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object WidgetRefreshScheduler {

    private const val REQUEST_CODE = 9901
    const val ACTION_AUTO_UPDATE = "com.example.ACTION_WIDGET_AUTO_UPDATE"

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ExamCountdownWidgetReceiver::class.java)
            .setAction(ACTION_AUTO_UPDATE)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** One inexact wake just after local midnight. Battery cost is negligible. */
    fun scheduleNextMidnight(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val next = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val pi = pendingIntent(context)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAndAllowWhileIdle(AlarmManager.RTC, next.timeInMillis, pi)
            } else {
                am.set(AlarmManager.RTC, next.timeInMillis, pi)
            }
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        runCatching { am.cancel(pendingIntent(context)) }
    }

    /** Call from the app whenever exams or sessions change. */
    fun refreshNow(context: Context) {
        listOf(
            ExamCountdownWidgetReceiver::class.java,
            ExamMatrixWidgetReceiver::class.java
        ).forEach {
            context.sendBroadcast(
                Intent(context, it).setAction(
                    android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                )
            )
        }
    }
}
