package com.example.service

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.example.data.db.AppDatabase
import com.example.ui.components.WallpaperHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class DayNightLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return DayNightEngine()
    }

    inner class DayNightEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var surfaceWidth = 1080
        private var surfaceHeight = 2400
        private var grownTreeCount = 0

        private val updateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                fetchTreeCountAndDraw()
            }
        }

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) {
                    // Refresh frame every minute to update sun/moon position & day/night mode seamlessly
                    handler.postDelayed(this, 60_000L)
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            val filter = IntentFilter().apply {
                addAction("com.example.ACTION_WIDGET_AUTO_UPDATE")
                addAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(updateReceiver, filter)
            }
            fetchTreeCountAndDraw()
        }

        override fun onDestroy() {
            super.onDestroy()
            try {
                unregisterReceiver(updateReceiver)
            } catch (e: Exception) {
                // Ignore if not registered
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                fetchTreeCountAndDraw()
                handler.removeCallbacks(drawRunnable)
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
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
            handler.removeCallbacks(drawRunnable)
        }

        private fun fetchTreeCountAndDraw() {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val completed = db.sessionDao().getAllSessionsList().filter { it.completed }
                    grownTreeCount = completed.size
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                handler.post { drawFrame() }
            }
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
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isDay = hour in 6..17

            // Draw exact shared forest bitmap rendered via WallpaperHelper
            val bitmap = WallpaperHelper.renderForestBitmap(
                context = applicationContext,
                width = width,
                height = height,
                isDay = isDay,
                treeCount = grownTreeCount
            )

            canvas.drawBitmap(bitmap, 0f, 0f, null)
        }
    }
}
