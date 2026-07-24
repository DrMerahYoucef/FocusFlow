package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap
)

object TrackedAppsStorage {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_TRACKED_APPS = "tracked_notification_apps"

    fun saveTrackedApps(context: Context, packages: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_TRACKED_APPS, packages).apply()
    }

    fun getTrackedApps(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_TRACKED_APPS, emptySet()) ?: emptySet()
    }

    fun getInstalledApps(context: Context): List<InstalledAppInfo> {
        return try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            resolveInfos
                .map { it.activityInfo.applicationInfo }
                .filter { it.packageName != context.packageName }
                .distinctBy { it.packageName }
                .map { appInfo ->
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val drawable = pm.getApplicationIcon(appInfo.packageName)
                    val bitmap = drawableToBitmap(drawable)
                    InstalledAppInfo(
                        packageName = appInfo.packageName,
                        label = label,
                        icon = bitmap.asImageBitmap()
                    )
                }
                .sortedBy { it.label.lowercase() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
