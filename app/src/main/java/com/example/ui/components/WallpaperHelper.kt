package com.example.ui.components

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import com.example.R
import com.example.ui.theme.WallpaperTheme

object WallpaperHelper {

    fun renderBuildingBitmap(
        context: Context,
        width: Int,
        height: Int,
        theme: WallpaperTheme,
        completedSessions: Int = -1
    ): Bitmap {
        val app = context.applicationContext
        val W = width.coerceAtLeast(1080).toFloat()
        val H = height.coerceAtLeast(1920).toFloat()

        val count = if (completedSessions >= 0) {
            completedSessions
        } else {
            val sharedPrefs = app.getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE)
            sharedPrefs.getInt(
                "last_synced_completed_sessions",
                sharedPrefs.getInt("last_synced_tree_count", 0)
            )
        }
        val windowSeed = app.getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE)
            .getLong("building_window_seed", 0L)
        val litWindows = cityLightIds(count, windowSeed)
        val cityWindows = loadCityWindows(app)
        val buildingBitmap = BitmapFactory.decodeResource(
            app.resources,
            R.drawable.cityscape_windows_transparent
        ) ?: error("City wallpaper image could not be decoded")
        val wallpaper = Bitmap.createBitmap(W.toInt(), H.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(wallpaper)
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(11, 7, 20)
        }
        canvas.drawRect(0f, 0f, W, H, backgroundPaint)

        cityWindows.forEach { window ->
            val left = W * (window.left / 100f).toFloat()
            val top = H * (window.top / 100f).toFloat()
            val right = left + W * (window.width / 100f).toFloat()
            val bottom = top + H * (window.height / 100f).toFloat()
            val isLit = window.id in litWindows
            val color = warmWindowColor(window.id).toArgb()

            if (isLit) {
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        (left + right) / 2f,
                        (top + bottom) / 2f,
                        maxOf(right - left, bottom - top) * 3f,
                        color,
                        android.graphics.Color.TRANSPARENT,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(left - W * 0.01f, top - H * 0.005f, right + W * 0.01f, bottom + H * 0.005f, glowPaint)
                backgroundPaint.color = color
            } else {
                backgroundPaint.color = android.graphics.Color.rgb(11, 7, 20)
            }
            canvas.drawRect(left, top, right, bottom, backgroundPaint)
        }

        canvas.drawBitmap(
            buildingBitmap,
            null,
            Rect(0, 0, W.toInt(), H.toInt()),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        return wallpaper
    }

    fun setBuildingWallpaper(
        context: Context,
        theme: WallpaperTheme,
        setHomeScreen: Boolean,
        setLockScreen: Boolean,
        completedSessions: Int = -1,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val app = context.applicationContext
        val wallpaperManager = WallpaperManager.getInstance(app)

        if (!setHomeScreen && !setLockScreen) {
            onComplete(false, "No screen selected")
            return
        }

        try {
            val metrics = app.resources.displayMetrics
            val W = metrics.widthPixels.coerceAtLeast(1080)
            val H = metrics.heightPixels.coerceAtLeast(1920)

            val bitmap = renderBuildingBitmap(context, W, H, theme, completedSessions)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (setHomeScreen && setLockScreen) {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                } else if (setHomeScreen) {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                } else if (setLockScreen) {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                }
            } else {
                wallpaperManager.setBitmap(bitmap)
            }

            onComplete(true, null)
        } catch (e: Exception) {
            android.util.Log.e("WallpaperHelper", "Failed to set wallpaper", e)
            onComplete(false, e.localizedMessage ?: "Unknown error")
        }
    }
}
