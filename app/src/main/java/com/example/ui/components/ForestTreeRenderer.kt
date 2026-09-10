package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.cos
import kotlin.math.sin

/**
 * Encapsulates the photorealistic tree PNG assets for both light and dark modes.
 */
data class ForestTreeBitmaps(
    val tree1Light: ImageBitmap,
    val tree1Dark: ImageBitmap,
    val tree2Light: ImageBitmap,
    val tree2Dark: ImageBitmap,
    val tree3Light: ImageBitmap,
    val tree3Dark: ImageBitmap,
    val tree4Light: ImageBitmap,
    val tree4Dark: ImageBitmap
) {
    // Aliases for compatibility
    val spruceLight: ImageBitmap get() = tree1Light
    val spruceDark: ImageBitmap get() = tree1Dark
    val pineLight: ImageBitmap get() = tree2Light
    val pineDark: ImageBitmap get() = tree2Dark
    val oakLight: ImageBitmap get() = tree3Light
    val oakDark: ImageBitmap get() = tree3Dark
    val birchLight: ImageBitmap get() = tree4Light
    val birchDark: ImageBitmap get() = tree4Dark
}

/**
 * Coordinate slots defining realistic physical positions for trees across the clearing and hills.
 * Pre-sorted by relY so distant trees are rendered behind nearer ones (strict depth ordering).
 */
data class TreeSlot(
    val relX: Float,      // Relative horizontal position (0.0 .. 1.0)
    val relY: Float,      // Relative vertical ground anchor (0.0 .. 1.0)
    val baseScale: Float, // Perspective scale (small in distance, large in foreground)
    val variant: Int,     // 0 = Spruce, 1 = Scotch Pine, 2 = Oak, 3 = Silver Birch
    val phase: Float      // Gentle wind sway phase
)

object ForestTreeRenderer {

    fun drawStylizedBackground(
        drawScope: DrawScope,
        W: Float,
        H: Float,
        darkProgress: Float
    ) {
        val dayTop = Color(0xFFEAF3F0)
        val dayBottom = Color(0xFFB7D4C7)
        val nightTop = Color(0xFF101A2B)
        val nightBottom = Color(0xFF243E4A)
        val top = lerp(dayTop, nightTop, darkProgress)
        val bottom = lerp(dayBottom, nightBottom, darkProgress)

        drawScope.drawRect(brush = Brush.verticalGradient(listOf(top, bottom), 0f, H))

        val horizon = H * 0.56f
        drawScope.drawCircle(
            color = lerp(Color(0xFFFFD98A), Color(0xFFB8D7DE), darkProgress).copy(alpha = 0.8f),
            radius = W * 0.075f,
            center = Offset(W * 0.78f, H * 0.18f)
        )
        drawScope.drawCircle(
            color = lerp(Color(0xFFFFE8B7), Color(0xFFB8D7DE), darkProgress).copy(alpha = 0.14f),
            radius = W * 0.22f,
            center = Offset(W * 0.78f, H * 0.18f)
        )

        drawScope.drawPath(Path().apply {
            moveTo(0f, horizon + H * 0.03f)
            cubicTo(W * 0.2f, horizon - H * 0.08f, W * 0.34f, horizon + H * 0.03f, W * 0.52f, horizon - H * 0.05f)
            cubicTo(W * 0.7f, horizon - H * 0.13f, W * 0.84f, horizon - H * 0.02f, W, horizon - H * 0.08f)
            lineTo(W, H)
            lineTo(0f, H)
            close()
        }, lerp(Color(0xFF8EAC9D), Color(0xFF1C303B), darkProgress))

        drawScope.drawPath(Path().apply {
            moveTo(0f, H * 0.72f)
            cubicTo(W * 0.2f, H * 0.62f, W * 0.38f, H * 0.76f, W * 0.57f, H * 0.66f)
            cubicTo(W * 0.76f, H * 0.57f, W * 0.9f, H * 0.71f, W, H * 0.63f)
            lineTo(W, H)
            lineTo(0f, H)
            close()
        }, lerp(Color(0xFF547B68), Color(0xFF14272D), darkProgress))

        drawScope.drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, lerp(Color(0xFF304D3F), Color(0xFF08151C), darkProgress).copy(alpha = 0.82f)),
                H * 0.7f,
                H
            )
        )
    }

    private fun lerp(start: Color, end: Color, amount: Float): Color = Color(
        red = start.red + (end.red - start.red) * amount,
        green = start.green + (end.green - start.green) * amount,
        blue = start.blue + (end.blue - start.blue) * amount,
        alpha = start.alpha + (end.alpha - start.alpha) * amount
    )

    private fun DrawScope.drawStylizedTree(
        centerX: Float,
        baseY: Float,
        height: Float,
        darkProgress: Float,
        phase: Float,
        animPhase: Float
    ) {
        val sway = if (animPhase == 0f) 0f else sin(animPhase * 0.04f + phase) * 1.2f
        val foliage = lerp(Color(0xFF315F50), Color(0xFF102A31), darkProgress)
        val foliageLight = lerp(Color(0xFF5F8F76), Color(0xFF24464A), darkProgress)
        val trunk = lerp(Color(0xFF755D43), Color(0xFF372F2C), darkProgress)
        withTransform({ rotate(sway, pivot = Offset(centerX, baseY)) }) {
            drawRoundRect(
                color = trunk,
                topLeft = Offset(centerX - height * 0.025f, baseY - height * 0.2f),
                size = Size(height * 0.05f, height * 0.2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(height * 0.02f)
            )
            listOf(0.08f to 0.22f, 0.25f to 0.34f, 0.45f to 0.46f, 0.68f to 0.58f).forEachIndexed { index, (offset, width) ->
                val y = baseY - height * offset
                val layerHeight = height * 0.34f
                drawOval(
                    color = if (index % 2 == 0) foliageLight else foliage,
                    topLeft = Offset(centerX - height * width * 0.5f, y - layerHeight * 0.5f),
                    size = Size(height * width, layerHeight)
                )
            }
        }
    }

    val TREE_SLOTS: List<TreeSlot> = listOf(
        // Tier 1: Distant island ridges overlooking the sea (relY: 0.50 .. 0.58)
        TreeSlot(relX = 0.20f, relY = 0.510f, baseScale = 0.42f, variant = 0, phase = 0.2f),
        TreeSlot(relX = 0.80f, relY = 0.515f, baseScale = 0.40f, variant = 1, phase = 1.1f),
        TreeSlot(relX = 0.35f, relY = 0.535f, baseScale = 0.46f, variant = 0, phase = 2.4f),
        TreeSlot(relX = 0.65f, relY = 0.530f, baseScale = 0.45f, variant = 1, phase = 0.8f),
        TreeSlot(relX = 0.14f, relY = 0.555f, baseScale = 0.50f, variant = 3, phase = 3.2f),
        TreeSlot(relX = 0.86f, relY = 0.550f, baseScale = 0.50f, variant = 3, phase = 1.9f),
        TreeSlot(relX = 0.48f, relY = 0.570f, baseScale = 0.54f, variant = 2, phase = 2.7f),
        TreeSlot(relX = 0.26f, relY = 0.580f, baseScale = 0.56f, variant = 2, phase = 0.5f),

        // Tier 2: Upper rolling island hills (relY: 0.59 .. 0.68)
        TreeSlot(relX = 0.16f, relY = 0.605f, baseScale = 0.64f, variant = 0, phase = 1.4f),
        TreeSlot(relX = 0.84f, relY = 0.600f, baseScale = 0.62f, variant = 1, phase = 0.3f),
        TreeSlot(relX = 0.38f, relY = 0.625f, baseScale = 0.68f, variant = 3, phase = 2.1f),
        TreeSlot(relX = 0.62f, relY = 0.620f, baseScale = 0.68f, variant = 2, phase = 3.5f),
        TreeSlot(relX = 0.24f, relY = 0.655f, baseScale = 0.76f, variant = 1, phase = 0.9f),
        TreeSlot(relX = 0.76f, relY = 0.650f, baseScale = 0.74f, variant = 0, phase = 1.7f),
        TreeSlot(relX = 0.48f, relY = 0.675f, baseScale = 0.82f, variant = 2, phase = 2.9f),
        TreeSlot(relX = 0.88f, relY = 0.670f, baseScale = 0.80f, variant = 3, phase = 0.4f),

        // Tier 3: Midground central island plateau (relY: 0.69 .. 0.78)
        TreeSlot(relX = 0.12f, relY = 0.705f, baseScale = 0.92f, variant = 0, phase = 3.1f),
        TreeSlot(relX = 0.86f, relY = 0.700f, baseScale = 0.90f, variant = 1, phase = 1.2f),
        TreeSlot(relX = 0.30f, relY = 0.730f, baseScale = 1.00f, variant = 3, phase = 2.6f),
        TreeSlot(relX = 0.70f, relY = 0.725f, baseScale = 0.98f, variant = 2, phase = 0.7f),
        TreeSlot(relX = 0.50f, relY = 0.755f, baseScale = 1.08f, variant = 1, phase = 1.8f),
        TreeSlot(relX = 0.20f, relY = 0.765f, baseScale = 1.10f, variant = 0, phase = 3.4f),
        TreeSlot(relX = 0.80f, relY = 0.770f, baseScale = 1.15f, variant = 2, phase = 0.1f),
        TreeSlot(relX = 0.38f, relY = 0.785f, baseScale = 1.14f, variant = 3, phase = 2.3f),

        // Tier 4: Lower meadow terrace & sprawling green slopes (relY: 0.79 .. 0.88)
        TreeSlot(relX = 0.14f, relY = 0.815f, baseScale = 1.28f, variant = 0, phase = 1.5f),
        TreeSlot(relX = 0.86f, relY = 0.810f, baseScale = 1.25f, variant = 3, phase = 2.8f),
        TreeSlot(relX = 0.44f, relY = 0.835f, baseScale = 1.38f, variant = 1, phase = 0.6f),
        TreeSlot(relX = 0.66f, relY = 0.840f, baseScale = 1.36f, variant = 2, phase = 3.0f),
        TreeSlot(relX = 0.26f, relY = 0.865f, baseScale = 1.48f, variant = 0, phase = 1.6f),
        TreeSlot(relX = 0.74f, relY = 0.870f, baseScale = 1.50f, variant = 3, phase = 2.2f),

        // Tier 5: Foreground framing accents (relY: 0.89 .. 0.95)
        TreeSlot(relX = 0.08f, relY = 0.915f, baseScale = 1.70f, variant = 1, phase = 1.3f),
        TreeSlot(relX = 0.92f, relY = 0.910f, baseScale = 1.68f, variant = 0, phase = 2.0f),
        TreeSlot(relX = 0.30f, relY = 0.940f, baseScale = 1.85f, variant = 2, phase = 0.4f),
        TreeSlot(relX = 0.70f, relY = 0.935f, baseScale = 1.82f, variant = 3, phase = 3.3f)
    )

    /**
     * Renders background image centered with aspect-fill cropping (no distortion, no tiling).
     */
    fun drawCropBitmap(
        drawScope: DrawScope,
        bitmap: ImageBitmap,
        W: Float,
        H: Float,
        alpha: Float = 1f
    ) {
        if (alpha <= 0.001f) return
        val srcW = bitmap.width.toFloat()
        val srcH = bitmap.height.toFloat()
        val scale = maxOf(W / srcW, H / srcH)
        val dstW = srcW * scale
        val dstH = srcH * scale
        val dstX = (W - dstW) / 2f
        val dstY = (H - dstH) / 2f

        drawScope.drawImage(
            image = bitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bitmap.width, bitmap.height),
            dstOffset = IntOffset(dstX.toInt(), dstY.toInt()),
            dstSize = IntSize(dstW.toInt(), dstH.toInt()),
            alpha = alpha
        )
    }

    /**
     * Professional vector tree rendering with a more refined silhouette and controlled depth.
     * It intentionally avoids the crowded photo-sprite look and uses consistent geometric forms.
     */
    fun drawDynamicForestTrees(
        drawScope: DrawScope,
        treeBitmaps: ForestTreeBitmaps,
        W: Float,
        H: Float,
        treeCount: Int,
        darkProgress: Float,
        animPhase: Float = 0f
    ) {
        if (treeCount <= 0) return

        val activeSlots = mutableListOf<Pair<TreeSlot, Float>>()
        val totalSlots = TREE_SLOTS.size

        for (i in 0 until treeCount) {
            val baseSlot = TREE_SLOTS[i % totalSlots]
            val cycle = i / totalSlots
            val jitterX = if (cycle > 0) sin(i * 1.9f) * 0.018f else 0f
            val jitterY = if (cycle > 0) cos(i * 2.5f) * 0.010f else 0f
            val slot = baseSlot.copy(
                relX = (baseSlot.relX + jitterX).coerceIn(0.08f, 0.92f),
                relY = (baseSlot.relY + jitterY).coerceIn(0.55f, 0.95f)
            )
            activeSlots.add(slot to baseSlot.phase)
        }

        val sortedSlots = activeSlots.sortedBy { it.first.relY }

        for ((slot, phase) in sortedSlots) {
            val cx = slot.relX * W
            val baseY = slot.relY * H
            val scaleFactor = slot.baseScale * (W / 1080f).coerceAtLeast(0.85f)
            val treeHeight = 170f * scaleFactor

            val foliageA = if (darkProgress < 0.5f) {
                Color(0xFF34614D)
            } else {
                Color(0xFF16363E)
            }
            val foliageB = if (darkProgress < 0.5f) {
                Color(0xFF5D876E)
            } else {
                Color(0xFF2A5057)
            }
            val trunk = if (darkProgress < 0.5f) {
                Color(0xFF6D503B)
            } else {
                Color(0xFF3A302D)
            }
            val shadowColor = if (darkProgress < 0.5f) {
                Color(0xFF1D2F1C)
            } else {
                Color(0xFF08181E)
            }

            val sway = sin(animPhase * 0.035f + phase) * 6f

            drawScope.drawOval(
                color = shadowColor.copy(alpha = 0.25f),
                topLeft = Offset(cx - treeHeight * 0.58f, baseY - treeHeight * 0.06f),
                size = Size(treeHeight * 1.16f, treeHeight * 0.14f)
            )

            drawScope.withTransform({
                rotate(sway, pivot = Offset(cx, baseY))
            }) {
                drawScope.drawRoundRect(
                    color = trunk,
                    topLeft = Offset(cx - treeHeight * 0.085f, baseY - treeHeight * 0.72f),
                    size = Size(treeHeight * 0.17f, treeHeight * 0.38f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(treeHeight * 0.06f)
                )

                val layerOffsets = listOf(
                    0.16f to treeHeight * 0.38f,
                    0.30f to treeHeight * 0.44f,
                    0.48f to treeHeight * 0.54f,
                    0.62f to treeHeight * 0.58f
                )

                layerOffsets.forEachIndexed { index, pair ->
                    val (yRatio, radius) = pair
                    val y = baseY - treeHeight * yRatio
                    val color = if (index % 2 == 0) foliageA else foliageB
                    drawScope.drawOval(
                        color = color,
                        topLeft = Offset(cx - radius * 0.5f, y - radius * 0.5f),
                        size = Size(radius, radius * 0.86f)
                    )
                }

                if (slot.variant in listOf(1, 2)) {
                    val topY = baseY - treeHeight * 0.75f
                    val half = treeHeight * 0.18f
                    drawScope.drawTriangle(
                        color = foliageB,
                        apex = Offset(cx, topY),
                        left = Offset(cx - half, baseY - treeHeight * 0.45f),
                        right = Offset(cx + half, baseY - treeHeight * 0.45f)
                    )
                }
            }
        }
    }

    private fun DrawScope.drawTriangle(
        color: Color,
        apex: Offset,
        left: Offset,
        right: Offset
    ) {
        drawPath(
            path = Path().apply {
                moveTo(apex.x, apex.y)
                lineTo(left.x, left.y)
                lineTo(right.x, right.y)
                close()
            },
            color = color
        )
    }

    /**
     * Atmospheric particles (Warm golden sun pollen in day, glowing fireflies at night).
     */
    fun drawAtmosphericParticles(
        drawScope: DrawScope,
        W: Float,
        H: Float,
        darkProgress: Float,
        animPhase: Float
    ) {
        if (animPhase == 0f) return

        if (darkProgress < 0.5f) {
            val pollenAlpha = (1f - darkProgress * 2f).coerceIn(0f, 1f)
            if (pollenAlpha > 0.05f) {
                val rng = java.util.Random(999)
                repeat(20) { p ->
                    val seedX = rng.nextFloat()
                    val seedY = rng.nextFloat()
                    val driftX = sin(animPhase * 0.05f + p * 1.8f) * W * 0.03f
                    val px = (seedX * W + driftX).mod(W)
                    val py = (seedY * H * 0.65f + H * 0.25f + animPhase * 0.4f * (p % 3 + 1)).mod(H * 0.65f) + H * 0.20f
                    val alpha = ((0.30f + 0.40f * sin(animPhase * 0.1f + p)) * pollenAlpha).coerceIn(0f, 0.8f)

                    drawScope.drawCircle(
                        color = Color(0xFFFFF6D0).copy(alpha = alpha),
                        radius = (1.6f + rng.nextFloat() * 2.2f) * (W / 1080f),
                        center = Offset(px, py)
                    )
                }
            }
        }

        if (darkProgress > 0.3f) {
            val ffAlpha = ((darkProgress - 0.3f) / 0.7f).coerceIn(0f, 1f)
            val rng = java.util.Random(888)
            repeat(16) { f ->
                val seedX = rng.nextFloat()
                val seedY = rng.nextFloat()
                val floatX = sin(animPhase * 0.06f + f * 2.1f) * W * 0.05f
                val floatY = cos(animPhase * 0.04f + f * 1.7f) * H * 0.03f
                val fx = (seedX * W * 0.82f + W * 0.09f + floatX).coerceIn(0f, W)
                val fy = (seedY * H * 0.55f + H * 0.40f + floatY).coerceIn(H * 0.35f, H * 0.95f)
                val alpha = ((0.45f + 0.45f * sin(animPhase * 0.15f + f * 2f)) * ffAlpha).coerceIn(0f, 0.9f)

                drawScope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x99B8E678).copy(alpha = alpha), Color.Transparent),
                        center = Offset(fx, fy),
                        radius = 20f * (W / 1080f)
                    ),
                    radius = 20f * (W / 1080f),
                    center = Offset(fx, fy)
                )
                drawScope.drawCircle(
                    color = Color(0xFFF4FFDC).copy(alpha = alpha),
                    radius = 3.6f * (W / 1080f),
                    center = Offset(fx, fy)
                )
            }
        }
    }
}
