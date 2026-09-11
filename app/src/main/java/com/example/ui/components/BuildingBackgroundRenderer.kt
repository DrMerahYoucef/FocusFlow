package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill

/** Draws the facade and progressively lights its fixed window grid. */
object BuildingBackgroundRenderer {

    private const val SOURCE_WIDTH = 335f
    private const val SOURCE_HEIGHT = 745f

    private val windows = buildList {
        val columns = listOf(82f, 118f, 154f, 190f, 226f)
        val rows = listOf(188f, 228f, 268f, 308f, 348f, 389f, 429f, 469f, 510f, 550f)
        rows.forEach { top ->
            columns.forEach { left ->
                add(Rect(left, top, left + 27f, top + 28f))
            }
        }
    }

    private val randomizedWindowOrder = windows.indices.sortedBy { index ->
        (index * 1103515245 + 12345) and Int.MAX_VALUE
    }

    fun DrawScope.drawBuilding(
        image: ImageBitmap,
        completedSessions: Int,
        darkProgress: Float
    ) {
        val scale = minOf(size.width / SOURCE_WIDTH, size.height / SOURCE_HEIGHT)
        val drawWidth = SOURCE_WIDTH * scale
        val drawHeight = SOURCE_HEIGHT * scale
        val left = (size.width - drawWidth) / 2f
        val top = (size.height - drawHeight) / 2f

        drawImage(
            image = image,
            dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
            dstSize = androidx.compose.ui.unit.IntSize(drawWidth.toInt(), drawHeight.toInt())
        )

        val litCount = completedSessions.coerceIn(0, windows.size)
        val litWindows = randomizedWindowOrder.take(litCount).toSet()
        windows.forEachIndexed { index, window ->
            val scaled = Rect(
                left + window.left * scale,
                top + window.top * scale,
                left + window.right * scale,
                top + window.bottom * scale
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.88f * (1f - darkProgress * 0.12f)),
                topLeft = Offset(scaled.left, scaled.top),
                size = Size(scaled.width, scaled.height),
                style = Fill
            )
            if (index in litWindows) {
                drawRect(
                    color = Color(0xFFFFD66B).copy(alpha = 0.92f),
                    topLeft = Offset(scaled.left, scaled.top),
                    size = Size(scaled.width, scaled.height),
                    style = Fill
                )
            }
        }
    }
}