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

/** Draws the facade and progressively lights every mapped transparent window. */
object BuildingBackgroundRenderer {

    private const val SOURCE_WIDTH = 688f
    private const val SOURCE_HEIGHT = 1536f

    internal val windowBounds = buildList {
        addFacadeWindows(
            columns = listOf(226f, 277f, 327f, 378f, 429f),
            rows = listOf(468f, 524f, 580f, 636f, 693f, 751f, 809f, 865f, 924f, 980f, 1038f, 1094f, 1151f, 1208f),
            width = 35f,
            height = 38f
        )
        addFacadeWindows(
            columns = listOf(7f, 38f, 53f, 84f, 99f),
            rows = listOf(647f, 703f, 761f, 819f, 876f, 934f, 991f, 1048f, 1103f, 1160f, 1218f),
            width = 14f,
            height = 21f
        )
        addFacadeWindows(
            columns = listOf(579f, 594f, 625f, 640f, 671f),
            rows = listOf(648f, 705f, 762f, 820f, 877f, 935f, 993f, 1049f, 1105f, 1163f, 1221f),
            width = 14f,
            height = 21f
        )

        // Rear towers use their own facade rhythm; these windows remain transparent in the source art.
        addFacadeWindows(listOf(170f), listOf(285f, 301f, 325f, 360f), 27f, 25f)
        addFacadeWindows(listOf(369f), listOf(266f, 289f, 307f, 346f), 8f, 25f)
        addFacadeWindows(listOf(401f, 434f), listOf(261f, 300f, 320f, 368f), 25f, 22f)
        addFacadeWindows(listOf(254f, 276f, 318f), listOf(347f, 368f, 394f), 35f, 28f)
        addFacadeWindows(listOf(488f, 506f, 563f, 570f), listOf(419f, 439f, 485f, 519f), 28f, 20f)
        addFacadeWindows(listOf(28f, 146f), listOf(387f, 427f, 456f), 30f, 24f)
    }

    private fun MutableList<Rect>.addFacadeWindows(
        columns: List<Float>,
        rows: List<Float>,
        width: Float,
        height: Float
    ) {
        rows.forEach { top ->
            columns.forEach { left ->
                add(Rect(left, top, left + width, top + height))
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