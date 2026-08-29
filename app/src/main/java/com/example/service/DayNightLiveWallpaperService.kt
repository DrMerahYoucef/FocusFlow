package com.example.service

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.example.ui.components.WallpaperHelper
import com.example.ui.theme.WallpaperTheme

class DayNightLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return DayNightEngine()
    }

    inner class DayNightEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var surfaceWidth = 1080
        private var surfaceHeight = 2400

        private val updateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                drawFrame()
            }
        }

        private val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "follow_system_theme" || key == "wallpaper_theme") {
                drawFrame()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            val sharedPrefs = getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE)
            sharedPrefs.registerOnSharedPreferenceChangeListener(prefListener)

            val filter = IntentFilter().apply {
                addAction("com.example.ACTION_WIDGET_AUTO_UPDATE")
                addAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                addAction(Intent.ACTION_CONFIGURATION_CHANGED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(updateReceiver, filter)
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            val sharedPrefs = getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE)
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefListener)
            try {
                unregisterReceiver(updateReceiver)
            } catch (e: Exception) {
                // Ignore if not registered
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                drawFrame()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
        }

        private fun drawFrame() {
            val holder = surfaceHolder ?: return
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    renderWallpaper(canvas, surfaceWidth, surfaceHeight)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        private fun renderWallpaper(canvas: Canvas, width: Int, height: Int) {
            val sharedPrefs = getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE)
            val followSystem = sharedPrefs.getBoolean("follow_system_theme", true)

            val isSystemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            val theme = if (followSystem) {
                if (isSystemDark) WallpaperTheme.DARK else WallpaperTheme.LIGHT
            } else {
                val manualThemeStr = sharedPrefs.getString("wallpaper_theme", "LIGHT") ?: "LIGHT"
                try {
                    WallpaperTheme.valueOf(manualThemeStr)
                } catch (e: Exception) {
                    WallpaperTheme.LIGHT
                }
            }

            val bitmap = WallpaperHelper.renderForestBitmap(
                context = applicationContext,
                width = width,
                height = height,
                theme = theme
            )

            canvas.drawBitmap(bitmap, 0f, 0f, null)
        }
    }
}
