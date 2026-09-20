package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.cos
import kotlin.math.sin

data class TreeSlot(val relX: Float, val relY: Float, val scale: Float, val phase: Float)

object ForestTreeRenderer {
    private val treeSlots = listOf(
        TreeSlot(0.10f, 0.70f, 0.46f, 0.2f), TreeSlot(0.90f, 0.69f, 0.48f, 1.3f),
        TreeSlot(0.25f, 0.77f, 0.66f, 2.2f), TreeSlot(0.76f, 0.76f, 0.62f, 0.9f),
        TreeSlot(0.05f, 0.88f, 0.88f, 2.8f), TreeSlot(0.95f, 0.86f, 0.92f, 1.7f),
        TreeSlot(0.36f, 0.91f, 1.02f, 0.4f), TreeSlot(0.66f, 0.90f, 0.98f, 2.6f)
    )

    fun drawModernLandscape(drawScope: DrawScope, W: Float, H: Float, treeCount: Int, darkProgress: Float, animPhase: Float) {
        val top = mix(Color(0xFFF8D9C4), Color(0xFF171A3A), darkProgress)
        val bottom = mix(Color(0xFFB6E3D1), Color(0xFF244A68), darkProgress)
        drawScope.drawRect(Brush.verticalGradient(listOf(top, bottom), 0f, H))

        val sun = Offset(W * 0.78f, H * 0.20f)
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                listOf(mix(Color(0xFFFFF1C2), Color(0xFFB9D9FF), darkProgress).copy(alpha = 0.55f), Color.Transparent),
                sun,
                W * 0.32f
            ),
            radius = W * 0.32f,
            center = sun
        )
        drawScope.drawCircle(mix(Color(0xFFFFC978), Color(0xFFE5EEFF), darkProgress), W * 0.055f, sun)

        if (darkProgress > 0.35f) {
            val starColor = Color(0xFFE5F3FF).copy(alpha = (darkProgress - 0.35f) * 0.8f)
            for (index in 0 until 14) {
                val x = (0.08f + (index * 0.071f) % 0.84f) * W
                val y = (0.07f + (index % 4) * 0.055f) * H
                drawScope.drawCircle(starColor, 1.4f + (index % 3), Offset(x, y))
            }
        }

        drawHill(drawScope, W, H, 0.55f, 0.08f, mix(Color(0xFFE6C9C1), Color(0xFF34466B), darkProgress))
        drawHill(drawScope, W, H, 0.67f, 0.10f, mix(Color(0xFFB8D7C9), Color(0xFF28506B), darkProgress))
        drawHill(drawScope, W, H, 0.80f, 0.12f, mix(Color(0xFF6EAF91), Color(0xFF183C4E), darkProgress))

        for (index in 0 until treeCount) {
            val slot = treeSlots[index % treeSlots.size]
            val cycle = index / treeSlots.size
            val x = ((slot.relX + if (cycle == 0) 0f else sin(index * 1.7f) * 0.035f) * W).coerceIn(-W * 0.05f, W * 1.05f)
            val baseY = (slot.relY + if (cycle == 0) 0f else cos(index * 2.3f) * 0.012f) * H
            val scale = slot.scale * (W / 900f).coerceAtLeast(0.8f)
            val sway = sin(animPhase * 0.04f + slot.phase) * 1.2f
            drawModernTree(drawScope, x, baseY, 180f * scale, sway, darkProgress, index)
        }
    }

    private fun drawHill(drawScope: DrawScope, W: Float, H: Float, base: Float, amplitude: Float, color: Color) {
        val path = Path().apply {
            moveTo(0f, H * base)
            cubicTo(W * 0.22f, H * (base - amplitude), W * 0.38f, H * (base + amplitude * 0.35f), W * 0.56f, H * (base - amplitude * 0.75f))
            cubicTo(W * 0.74f, H * (base - amplitude * 1.2f), W * 0.88f, H * (base + amplitude * 0.25f), W, H * (base - amplitude * 0.35f))
            lineTo(W, H)
            lineTo(0f, H)
            close()
        }
        drawScope.drawPath(path, color)
    }

    private fun drawModernTree(drawScope: DrawScope, x: Float, baseY: Float, height: Float, sway: Float, darkProgress: Float, index: Int) {
        val trunk = mix(Color(0xFF7D5A4C), Color(0xFF172B43), darkProgress)
        val canopy = mix(Color(0xFF2D806E), Color(0xFF173C57), darkProgress)
        val highlight = mix(Color(0xFF8CC6A7), Color(0xFF3D718B), darkProgress)
        val width = height * 0.46f
        drawScope.drawOval(Color.Black.copy(alpha = 0.12f + darkProgress * 0.08f), Offset(x - width * 0.45f, baseY - 4f), Size(width * 0.9f, 12f))
        drawScope.withTransform({ rotate(sway, Offset(x, baseY)) }) {
            drawRect(trunk, Offset(x - width * 0.055f, baseY - height * 0.30f), Size(width * 0.11f, height * 0.30f))
            val center = Offset(x, baseY - height * 0.63f)
            drawCircle(canopy, width * 0.31f, center.copy(x = center.x - width * 0.18f, y = center.y + height * 0.06f))
            drawCircle(canopy, width * 0.36f, center.copy(x = center.x + width * 0.16f))
            drawCircle(canopy, width * 0.29f, center.copy(y = center.y - height * 0.16f))
            drawCircle(highlight.copy(alpha = 0.52f), width * 0.12f, center.copy(x = center.x - width * 0.16f, y = center.y - height * 0.14f))
            if (index % 3 == 0) {
                drawLine(highlight.copy(alpha = 0.45f), Offset(x - width * 0.24f, center.y + height * 0.03f), Offset(x + width * 0.24f, center.y + height * 0.03f), width * 0.025f)
            }
        }
    }

    fun drawAtmosphericParticles(drawScope: DrawScope, W: Float, H: Float, darkProgress: Float, animPhase: Float) {
        if (animPhase == 0f) return
        val color = mix(Color(0xFFFFF1C7), Color(0xFFBCE6FF), darkProgress)
        for (index in 0 until 18) {
            val x = ((index * 83f + sin(animPhase * 0.05f + index) * 22f) % W + W) % W
            val y = H * (0.20f + (index % 7) * 0.085f)
            val alpha = (0.18f + 0.18f * sin(animPhase * 0.12f + index)).coerceIn(0.04f, 0.38f)
            drawScope.drawCircle(color.copy(alpha = alpha), 2.2f + index % 2, Offset(x, y))
        }
    }

    private fun mix(start: Color, end: Color, amount: Float): Color {
        val t = amount.coerceIn(0f, 1f)
        return Color(
            start.red + (end.red - start.red) * t,
            start.green + (end.green - start.green) * t,
            start.blue + (end.blue - start.blue) * t,
            start.alpha + (end.alpha - start.alpha) * t
        )
    }
}
