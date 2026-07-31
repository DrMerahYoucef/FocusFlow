package com.example.service

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.util.Calendar
import java.util.Random

class DayNightLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return DayNightEngine()
    }

    inner class DayNightEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var surfaceWidth = 1080
        private var surfaceHeight = 2400

        private val starPoints = mutableListOf<StarPoint>()
        private val random = Random(42)

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) {
                    // Refresh frame every minute to update sun/moon position & day/night mode seamlessly
                    handler.postDelayed(this, 60_000L)
                }
            }
        }

        init {
            // Generate star positions for night sky
            repeat(60) {
                starPoints.add(
                    StarPoint(
                        xFactor = random.nextFloat(),
                        yFactor = random.nextFloat() * 0.55f,
                        radius = 1.2f + random.nextFloat() * 2.2f,
                        alpha = 140 + random.nextInt(115)
                    )
                )
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
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

        private fun drawFrame() {
            val holder = surfaceHolder ?: return
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    renderWallpaper(canvas, surfaceWidth.toFloat(), surfaceHeight.toFloat())
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

        private fun renderWallpaper(canvas: Canvas, width: Float, height: Float) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isDay = hour in 6..17

            // 1. Sky Gradient Background
            val skyTop = if (isDay) Color.parseColor("#80D3DE") else Color.parseColor("#0A1424")
            val skyBottom = if (isDay) Color.parseColor("#E0F7F3") else Color.parseColor("#1A3048")
            val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, 0f, height, skyTop, skyBottom, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width, height, skyPaint)

            val cornerX = width * 0.82f
            val cornerY = height * 0.14f

            if (!isDay) {
                // 2. Night Sky Stars
                val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                }
                for (star in starPoints) {
                    starPaint.alpha = star.alpha
                    canvas.drawCircle(star.xFactor * width, star.yFactor * height, star.radius, starPaint)
                }

                // Moon Aura
                val moonGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        cornerX, cornerY, width * 0.40f,
                        Color.parseColor("#44ADC6D1"),
                        Color.TRANSPARENT,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawCircle(cornerX, cornerY, width * 0.40f, moonGlowPaint)

                // Moon Disc
                val moonDiscPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#E9F5F8")
                }
                val moonRadius = width * 0.08f
                canvas.drawCircle(cornerX, cornerY, moonRadius, moonDiscPaint)

                // Moon Shadow Cutout (Crescent effect)
                val moonCutoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#0E1B2D")
                }
                canvas.drawCircle(cornerX - width * 0.026f, cornerY - height * 0.012f, moonRadius * 0.88f, moonCutoutPaint)

            } else {
                // Sun Glow & Core
                val sunGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        cornerX, cornerY, width * 0.48f,
                        intArrayOf(
                            Color.parseColor("#77FFEFA8"),
                            Color.parseColor("#22FFD700"),
                            Color.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawCircle(cornerX, cornerY, width * 0.48f, sunGlowPaint)

                // Sun Outer Ring
                val sunOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FFFCEB")
                }
                val sunRadius = width * 0.09f
                canvas.drawCircle(cornerX, cornerY, sunRadius, sunOuterPaint)

                // Sun Core Disc
                val sunCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FFEB3B")
                }
                canvas.drawCircle(cornerX, cornerY, sunRadius * 0.74f, sunCorePaint)
            }

            // 3. Decorative Forest Silhouette at the bottom of the wallpaper
            val forestPath = Path()
            forestPath.moveTo(0f, height)
            val treeColor = if (isDay) Color.parseColor("#38286532") else Color.parseColor("#45071614")
            val forestPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = treeColor
                style = Paint.Style.FILL
            }

            val step = width / 24f
            var tx = 0f
            var idx = 0
            while (tx <= width + step) {
                val wave = Math.sin((idx * 0.6) + (hour * 0.1)).toFloat()
                val treeHeight = (height * 0.06f) + (wave * (height * 0.02f))
                forestPath.lineTo(tx, height - treeHeight)
                tx += step
                idx++
            }
            forestPath.lineTo(width, height)
            forestPath.close()
            canvas.drawPath(forestPath, forestPaint)
        }
    }

    private data class StarPoint(
        val xFactor: Float,
        val yFactor: Float,
        val radius: Float,
        val alpha: Int
    )
}
