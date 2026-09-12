package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/** Draws the facade and progressively lights its fixed window grid. */
object BuildingBackgroundRenderer {

    private const val SOURCE_WIDTH = 335f
    private const val SOURCE_HEIGHT = 745f

    internal val windowBounds = buildList {
        val columns = listOf(82f, 118f, 154f, 190f, 226f)
        val rows = listOf(155f, 195f, 235f, 275f, 315f, 355f, 395f, 435f, 475f, 515f, 555f)
        rows.forEach { top ->
            columns.forEach { left ->
                add(Rect(left + 3f, top + 3f, left + 24f, top + 26f))
            }
        }
    }

    internal fun shuffledWindowOrder(seed: Long): List<Int> = windowBounds.indices.shuffled(
        Random(seed.toInt())
    )

    private fun windowBrightness(index: Int): Float {
        return 0.7f + ((index * 37) % 31) / 100f
    }

    private fun windowTemperature(index: Int): Color {
        return when (index % 4) {
            0 -> Color(0xFFFFD76A)
            1 -> Color(0xFFFFC857)
            2 -> Color(0xFFFFB74D)
            else -> Color(0xFFFFF3C4)
        }
    }

    fun DrawScope.drawBuilding(
        image: ImageBitmap,
        completedSessions: Int,
        darkProgress: Float,
        windowSeed: Long = 0L,
        litWindows: Set<Int> = shuffledWindowOrder(windowSeed)
            .take(completedSessions.coerceIn(0, windowBounds.size))
            .toSet(),
        animatingWindowIndex: Int = -1,
        animationProgress: Float = 1f
    ) {
        val scale = minOf(size.width / SOURCE_WIDTH, size.height / SOURCE_HEIGHT)
        val drawWidth = (SOURCE_WIDTH * scale).roundToInt()
        val drawHeight = (SOURCE_HEIGHT * scale).roundToInt()
        val left = ((size.width - drawWidth) / 2f).roundToInt()
        val top = ((size.height - drawHeight) / 2f).roundToInt()
        val actualScaleX = drawWidth / SOURCE_WIDTH
        val actualScaleY = drawHeight / SOURCE_HEIGHT

        drawImage(
            image = image,
            dstOffset = androidx.compose.ui.unit.IntOffset(left, top),
            dstSize = androidx.compose.ui.unit.IntSize(drawWidth, drawHeight)
        )

        windowBounds.forEachIndexed { index, window ->
            val scaled = Rect(
                left + window.left * actualScaleX,
                top + window.top * actualScaleY,
                left + window.right * actualScaleX,
                top + window.bottom * actualScaleY
            )
            drawRect(color = Color(0xFF17212A).copy(alpha = 0.92f), topLeft = Offset(scaled.left, scaled.top), size = Size(scaled.width, scaled.height))
            if (index !in litWindows) return@forEachIndexed

            val brightness = windowBrightness(index)
            val progress = if (index == animatingWindowIndex) animationProgress.coerceIn(0f, 1f) else 1f
            val light = windowTemperature(index)
            val glowAlpha = (0.10f + 0.12f * brightness) * progress

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(light.copy(alpha = glowAlpha), Color.Transparent),
                    center = Offset(scaled.center.x, scaled.center.y),
                    radius = maxOf(scaled.width, scaled.height) * 0.72f
                ),
                topLeft = Offset(scaled.left - actualScaleX, scaled.top - actualScaleY),
                size = Size(scaled.width + 2f * actualScaleX, scaled.height + 2f * actualScaleY)
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(light.copy(alpha = brightness * progress), light.copy(alpha = brightness * 0.82f * progress)),
                    startY = scaled.top,
                    endY = scaled.bottom
                ),
                topLeft = Offset(scaled.left, scaled.top),
                size = Size(scaled.width, scaled.height)
            )
            drawInterior(index, scaled, light, progress)
            drawRect(
                color = Color(0xFF202B32).copy(alpha = 0.55f),
                topLeft = Offset(scaled.left, scaled.top),
                size = Size(scaled.width, scaled.height),
                style = Stroke(width = minOf(actualScaleX, actualScaleY).coerceAtLeast(0.75f))
            )
            drawRect(color = Color.White.copy(alpha = 0.10f * progress), topLeft = Offset(scaled.left + scaled.width * 0.12f, scaled.top + scaled.height * 0.08f), size = Size(scaled.width * 0.08f, scaled.height * 0.68f))
        }
    }

    private fun DrawScope.drawInterior(index: Int, bounds: Rect, light: Color, progress: Float) {
        val silhouette = Color(0xFF4B3A2A).copy(alpha = 0.22f * progress)
        when (index % 8) {
            0 -> {
                drawRect(silhouette, Offset(bounds.left, bounds.top), Size(bounds.width * 0.18f, bounds.height))
                drawRect(silhouette, Offset(bounds.right - bounds.width * 0.18f, bounds.top), Size(bounds.width * 0.18f, bounds.height))
            }
            1 -> for (line in 1..4) drawRect(silhouette, Offset(bounds.left, bounds.top + bounds.height * line / 5f), Size(bounds.width, bounds.height * 0.035f))
            2 -> drawRect(Color(0xFFB8D9E8).copy(alpha = 0.14f * progress), Offset(bounds.left + bounds.width * 0.18f, bounds.top + bounds.height * 0.3f), Size(bounds.width * 0.64f, bounds.height * 0.42f))
            3 -> drawRect(light.copy(alpha = 0.16f * progress), Offset(bounds.left, bounds.top + bounds.height * 0.58f), Size(bounds.width, bounds.height * 0.42f))
            4 -> drawRect(silhouette, Offset(bounds.left + bounds.width * 0.12f, bounds.top + bounds.height * 0.64f), Size(bounds.width * 0.76f, bounds.height * 0.1f))
            5 -> drawRect(Color(0xFFE9D9B0).copy(alpha = 0.18f * progress), Offset(bounds.left + bounds.width * 0.42f, bounds.top), Size(bounds.width * 0.16f, bounds.height))
            6 -> Unit
            else -> drawRect(silhouette, Offset(bounds.left, bounds.bottom - bounds.height * 0.16f), Size(bounds.width, bounds.height * 0.16f))
        }
        val noise = (abs(index * 17) % 5) / 100f
        drawRect(color = Color.White.copy(alpha = noise * progress), topLeft = Offset(bounds.left + bounds.width * 0.62f, bounds.top + bounds.height * 0.18f), size = Size(bounds.width * 0.04f, bounds.height * 0.04f))
    }
}