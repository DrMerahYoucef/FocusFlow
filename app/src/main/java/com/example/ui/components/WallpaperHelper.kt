package com.example.ui.components

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import kotlin.math.sin

object WallpaperHelper {

    fun setForestWallpaper(
        context: Context,
        isDay: Boolean,
        treeCount: Int,
        setHomeScreen: Boolean,
        setLockScreen: Boolean,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val app = context.applicationContext
        val wallpaperManager = WallpaperManager.getInstance(app)

        if (!setHomeScreen && !setLockScreen) {
            onComplete(false, "No flag selected")
            return
        }

        try {
            val metrics = app.resources.displayMetrics
            val W = metrics.widthPixels.coerceAtLeast(1080).toFloat()
            val H = metrics.heightPixels.coerceAtLeast(1920).toFloat()

            val imageBitmap = ImageBitmap(W.toInt(), H.toInt())
            val composeCanvas = Canvas(imageBitmap)
            val drawScope = CanvasDrawScope()

            drawScope.draw(
                density = androidx.compose.ui.unit.Density(app),
                layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
                canvas = composeCanvas,
                size = Size(W, H)
            ) {
                // Background sky
                val skyTop = if (isDay) Color(0xFF90DBE1) else Color(0xFF030A0E)
                val skyBottom = if (isDay) Color(0xFFE2F8F4) else Color(0xFF0D1E24)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(skyTop, skyBottom),
                        startY = 0f, endY = H
                    )
                )

                if (!isDay) {
                    // Stars
                    val r = java.util.Random(101)
                    repeat(35) { index ->
                        val sx = r.nextFloat() * W
                        val sy = r.nextFloat() * H * 0.45f
                        drawCircle(
                            color = Color.White.copy(alpha = 0.4f + r.nextFloat() * 0.5f),
                            radius = 4f + r.nextFloat() * 4f,
                            center = Offset(sx, sy)
                        )
                    }
                    // Moon
                    val moonCenter = Offset(W * 0.8f, H * 0.12f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x22ADC6D1), Color.Transparent),
                            center = moonCenter, radius = W * 0.45f
                        ),
                        radius = W * 0.45f, center = moonCenter
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x3DDCF1F5), Color.Transparent),
                            center = moonCenter, radius = W * 0.18f
                        ),
                        radius = W * 0.18f, center = moonCenter
                    )
                    drawCircle(
                        color = Color(0xFFE9F5F8),
                        radius = W * 0.06f,
                        center = moonCenter
                    )
                } else {
                    // Sun
                    val sunCenter = Offset(W * 0.5f, H * 0.05f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x66FFEFA8), Color.Transparent),
                            center = sunCenter, radius = W * 0.55f
                        ),
                        radius = W * 0.55f, center = sunCenter
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xB3FFF099), Color.Transparent),
                            center = sunCenter, radius = W * 0.16f
                        ),
                        radius = W * 0.16f, center = sunCenter
                    )
                    drawCircle(
                        color = Color(0xFFFFFCEB),
                        radius = W * 0.065f,
                        center = sunCenter
                    )
                }

                // Deterministic Tree Layout
                val count = maxOf(treeCount, 1)
                val layers = List(6) { ArrayList<DeterministicTree>() }
                for (i in 0 until count) {
                    val iLong = i.toLong()
                    val seedX = (iLong * 19349663L) xor 0x5DEECE66DL
                    val seedH = (iLong * 38260237L) xor 0x5DEECE66DL
                    val seedW = (iLong * 85038241L) xor 0x5DEECE66DL

                    val randX = (((seedX xor 12345L) % 10000L).toFloat() / 10000f).coerceIn(0f, 1f)
                    val randH = (((seedH xor 54321L) % 10000L).toFloat() / 10000f).coerceIn(0f, 1f)
                    val randW = (((seedW xor 98765L) % 10000L).toFloat() / 10000f).coerceIn(0f, 1f)

                    val layerIdx = i % 6
                    val xFraction = -0.12f + randX * 1.24f
                    val heightScale = 0.70f + randH * 0.45f
                    val widthScale = 0.75f + randW * 0.35f

                    layers[layerIdx].add(
                        DeterministicTree(
                            xFraction = xFraction,
                            heightScale = heightScale,
                            horizontalSpacing = widthScale,
                            layerIndex = layerIdx,
                            itemIndex = i
                        )
                    )
                }

                val cachedTreePath = Path()
                val cachedShadowPath = Path()

                // Draw layers
                for (layerIdx in 0..5) {
                    val scale = when (layerIdx) {
                        0 -> 0.35f
                        1 -> 0.45f
                        2 -> 0.58f
                        3 -> 0.72f
                        4 -> 0.86f
                        else -> 1.00f
                    }
                    val yFraction = when (layerIdx) {
                        0 -> 0.38f
                        1 -> 0.30f
                        2 -> 0.22f
                        3 -> 0.14f
                        4 -> 0.06f
                        else -> 0.00f
                    }
                    val baseColor = when (layerIdx) {
                        0 -> if (isDay) Color(0xFFCBE3CC) else Color(0xFF1E3A36)
                        1 -> if (isDay) Color(0xFFADCEB1) else Color(0xFF162F2B)
                        2 -> if (isDay) Color(0xFF8BB78F) else Color(0xFF112622)
                        3 -> if (isDay) Color(0xFF639D69) else Color(0xFF0C1E1B)
                        4 -> if (isDay) Color(0xFF42824B) else Color(0xFF071614)
                        else -> if (isDay) Color(0xFF286532) else Color(0xFF030D0C)
                    }

                    val list = layers[layerIdx]
                    val ceiling = when (layerIdx) {
                        0 -> 15
                        1 -> 20
                        2 -> 25
                        3 -> 30
                        4 -> 35
                        else -> 40
                    }
                    val treesInLayer = if (list.size <= ceiling) list else list.take(ceiling)

                    val treeH = H * scale * 0.28f
                    val treeW = treeH * 0.45f
                    val baseY = H * (1f - yFraction)

                    treesInLayer.forEach { tree ->
                        val x = tree.xFraction * W
                        val y = baseY + (tree.itemIndex % 2) * treeH * 0.04f
                        val finalH = treeH * tree.heightScale
                        val finalW = treeW * tree.horizontalSpacing

                        // Draw tree
                        cachedTreePath.reset()
                        cachedTreePath.moveTo(x, y - finalH)
                        cachedTreePath.lineTo(x - finalW * 0.14f, y - finalH + finalH * 0.10f)
                        cachedTreePath.lineTo(x - finalW * 0.07f, y - finalH + finalH * 0.12f)
                        cachedTreePath.lineTo(x - finalW * 0.23f, y - finalH + finalH * 0.22f)
                        cachedTreePath.lineTo(x - finalW * 0.11f, y - finalH + finalH * 0.25f)
                        cachedTreePath.lineTo(x - finalW * 0.33f, y - finalH + finalH * 0.36f)
                        cachedTreePath.lineTo(x - finalW * 0.16f, y - finalH + finalH * 0.39f)
                        cachedTreePath.lineTo(x - finalW * 0.43f, y - finalH + finalH * 0.49f)
                        cachedTreePath.lineTo(x - finalW * 0.21f, y - finalH + finalH * 0.52f)
                        cachedTreePath.lineTo(x - finalW * 0.53f, y - finalH + finalH * 0.64f)
                        cachedTreePath.lineTo(x - finalW * 0.26f, y - finalH + finalH * 0.67f)
                        cachedTreePath.lineTo(x - finalW * 0.63f, y - finalH + finalH * 0.78f)
                        cachedTreePath.lineTo(x - finalW * 0.30f, y - finalH + finalH * 0.82f)
                        cachedTreePath.lineTo(x - finalW * 0.72f, y - finalH + finalH * 0.94f)
                        cachedTreePath.lineTo(x - finalW * 0.33f, y)
                        cachedTreePath.lineTo(x - finalW * 0.08f, y)
                        cachedTreePath.lineTo(x - finalW * 0.08f, y + finalH * 0.12f)
                        cachedTreePath.lineTo(x + finalW * 0.08f, y + finalH * 0.12f)
                        cachedTreePath.lineTo(x + finalW * 0.08f, y)
                        cachedTreePath.lineTo(x + finalW * 0.33f, y)
                        cachedTreePath.lineTo(x + finalW * 0.72f, y - finalH + finalH * 0.94f)
                        cachedTreePath.lineTo(x + finalW * 0.30f, y - finalH + finalH * 0.82f)
                        cachedTreePath.lineTo(x + finalW * 0.63f, y - finalH + finalH * 0.78f)
                        cachedTreePath.lineTo(x + finalW * 0.26f, y - finalH + finalH * 0.67f)
                        cachedTreePath.lineTo(x + finalW * 0.53f, y - finalH + finalH * 0.64f)
                        cachedTreePath.lineTo(x + finalW * 0.21f, y - finalH + finalH * 0.52f)
                        cachedTreePath.lineTo(x + finalW * 0.43f, y - finalH + finalH * 0.49f)
                        cachedTreePath.lineTo(x + finalW * 0.16f, y - finalH + finalH * 0.39f)
                        cachedTreePath.lineTo(x + finalW * 0.33f, y - finalH + finalH * 0.36f)
                        cachedTreePath.lineTo(x + finalW * 0.11f, y - finalH + finalH * 0.25f)
                        cachedTreePath.lineTo(x + finalW * 0.23f, y - finalH + finalH * 0.22f)
                        cachedTreePath.lineTo(x + finalW * 0.07f, y - finalH + finalH * 0.12f)
                        cachedTreePath.lineTo(x + finalW * 0.14f, y - finalH + finalH * 0.10f)
                        cachedTreePath.close()

                        drawPath(cachedTreePath, baseColor)

                        // Add simple shadow overlay
                        cachedShadowPath.reset()
                        cachedShadowPath.moveTo(x, y - finalH)
                        cachedShadowPath.lineTo(x + finalW * 0.14f, y - finalH + finalH * 0.10f)
                        cachedShadowPath.lineTo(x + finalW * 0.07f, y - finalH + finalH * 0.12f)
                        cachedShadowPath.lineTo(x + finalW * 0.23f, y - finalH + finalH * 0.22f)
                        cachedShadowPath.lineTo(x + finalW * 0.11f, y - finalH + finalH * 0.25f)
                        cachedShadowPath.lineTo(x + finalW * 0.33f, y - finalH + finalH * 0.36f)
                        cachedShadowPath.lineTo(x + finalW * 0.16f, y - finalH + finalH * 0.39f)
                        cachedShadowPath.lineTo(x + finalW * 0.43f, y - finalH + finalH * 0.49f)
                        cachedShadowPath.lineTo(x + finalW * 0.21f, y - finalH + finalH * 0.52f)
                        cachedShadowPath.lineTo(x + finalW * 0.53f, y - finalH + finalH * 0.64f)
                        cachedShadowPath.lineTo(x + finalW * 0.26f, y - finalH + finalH * 0.67f)
                        cachedShadowPath.lineTo(x + finalW * 0.63f, y - finalH + finalH * 0.78f)
                        cachedShadowPath.lineTo(x + finalW * 0.30f, y - finalH + finalH * 0.82f)
                        cachedShadowPath.lineTo(x + finalW * 0.72f, y - finalH + finalH * 0.94f)
                        cachedShadowPath.lineTo(x + finalW * 0.33f, y)
                        cachedShadowPath.lineTo(x + finalW * 0.08f, y)
                        cachedShadowPath.lineTo(x + finalW * 0.08f, y + finalH * 0.12f)
                        cachedShadowPath.lineTo(x, y + finalH * 0.12f)
                        cachedShadowPath.lineTo(x, y)
                        cachedShadowPath.lineTo(x, y - finalH)
                        cachedShadowPath.close()

                        val shadowOpacity = if (isDay) 0.22f else 0.28f
                        drawPath(cachedShadowPath, Color.Black.copy(alpha = shadowOpacity))
                    }

                    // Fog
                    val fogColor = if (isDay) Color(0xFFE5FBF6) else Color(0xFF1F353A)
                    val fogY = H * (1f - yFraction) - H * 0.02f
                    val fogAlpha = when (layerIdx) {
                        0 -> 0.16f
                        1 -> 0.13f
                        2 -> 0.11f
                        3 -> 0.09f
                        4 -> 0.07f
                        else -> 0.05f
                    }
                    val fogScale = when (layerIdx) {
                        0 -> 1.2f
                        1 -> 1.0f
                        2 -> 0.85f
                        3 -> 0.70f
                        4 -> 0.55f
                        else -> 0.40f
                    }

                    val fogPath = Path()
                    fogPath.moveTo(0f, H)
                    val steps = 20
                    val stepW = W / steps
                    for (step in 0..steps) {
                        val fx = step * stepW
                        val waveY = fogY + sin(fx * 0.005f) * 12f * fogScale
                        fogPath.lineTo(fx, waveY)
                    }
                    fogPath.lineTo(W, H)
                    fogPath.close()

                    val mistHeight = 160f * fogScale
                    drawPath(
                        path = fogPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                fogColor.copy(alpha = fogAlpha * 0.8f),
                                fogColor.copy(alpha = fogAlpha * 1.5f),
                                Color.Transparent
                            ),
                            startY = fogY - mistHeight * 0.3f,
                            endY = fogY + mistHeight * 0.7f
                        )
                    )
                }

                // Foreground shading
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xBB000000)),
                        startY = H * 0.70f, endY = H
                    )
                )
            }

            val bitmap = imageBitmap.asAndroidBitmap()

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
