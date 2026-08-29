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
     * Stamped realistic PNG tree sprites placed at fixed coordinate slots based on treeCount.
     * When treeCount is 0, zero trees are drawn.
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
            val jitterX = if (cycle > 0) sin(i * 1.7f) * 0.025f else 0f
            val jitterY = if (cycle > 0) cos(i * 2.3f) * 0.012f else 0f
            val slot = baseSlot.copy(
                relX = (baseSlot.relX + jitterX).coerceIn(0.05f, 0.95f),
                relY = (baseSlot.relY + jitterY).coerceIn(0.52f, 0.95f)
            )
            activeSlots.add(slot to baseSlot.phase)
        }

        // Sort by relY ascending so farther trees are strictly drawn behind nearer ones
        val sortedSlots = activeSlots.sortedBy { it.first.relY }

        for ((slot, phase) in sortedSlots) {
            val cx = slot.relX * W
            val baseY = slot.relY * H

            // Depth perspective scaling
            val scaleFactor = slot.baseScale * (W / 1080f).coerceAtLeast(0.85f)

            // Select light and dark bitmap variants based on slot.variant
            val (lightBmp, darkBmp) = when (slot.variant) {
                0 -> treeBitmaps.spruceLight to treeBitmaps.spruceDark
                1 -> treeBitmaps.pineLight to treeBitmaps.pineDark
                2 -> treeBitmaps.oakLight to treeBitmaps.oakDark
                else -> treeBitmaps.birchLight to treeBitmaps.birchDark
            }

            // Calculate tree sprite dimensions maintaining original aspect ratio
            val srcW = lightBmp.width.toFloat()
            val srcH = lightBmp.height.toFloat()
            val aspect = srcW / srcH
            val dstH = (650f * scaleFactor)
            val dstW = dstH * aspect

            val dstLeft = cx - dstW / 2f
            val dstTop = baseY - dstH

            val swayAngle = if (animPhase != 0f) {
                sin(animPhase * 0.04f + phase) * (0.8f * (slot.baseScale * 0.6f).coerceAtMost(1f))
            } else 0f

            // Realistic ground contact shadow with natural turf color and sun direction
            val shadowAlpha = if (darkProgress < 0.5f) {
                (0.24f * (1f - darkProgress * 0.3f) * (slot.baseScale * 0.6f).coerceIn(0.4f, 1f)).coerceIn(0.10f, 0.28f)
            } else {
                (0.32f * darkProgress * (slot.baseScale * 0.6f).coerceIn(0.4f, 1f)).coerceIn(0.12f, 0.35f)
            }
            val shadowColor = if (darkProgress < 0.5f) Color(0xFF1E2808) else Color(0xFF020E12)
            val shadowW = dstW * 0.72f
            val shadowH = dstH * 0.085f
            val shadowOffsetX = if (darkProgress < 0.5f) dstW * 0.06f else 0f
            val shadowOffsetY = dstH * 0.02f

            drawScope.drawOval(
                color = shadowColor.copy(alpha = shadowAlpha),
                topLeft = Offset(cx - shadowW / 2f + shadowOffsetX, baseY - shadowH * 0.55f + shadowOffsetY),
                size = Size(shadowW, shadowH)
            )

            // Draw tree sprite with gentle wind sway anchored at base
            drawScope.withTransform({
                if (swayAngle != 0f) {
                    rotate(swayAngle, pivot = Offset(cx, baseY))
                }
            }) {
                val dstOffset = IntOffset(dstLeft.toInt(), dstTop.toInt())
                val dstSize = IntSize(dstW.toInt(), dstH.toInt())

                if (darkProgress <= 0.001f) {
                    drawImage(
                        image = lightBmp,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(lightBmp.width, lightBmp.height),
                        dstOffset = dstOffset,
                        dstSize = dstSize,
                        alpha = 1f
                    )
                } else if (darkProgress >= 0.999f) {
                    drawImage(
                        image = darkBmp,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(darkBmp.width, darkBmp.height),
                        dstOffset = dstOffset,
                        dstSize = dstSize,
                        alpha = 1f
                    )
                } else {
                    drawImage(
                        image = lightBmp,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(lightBmp.width, lightBmp.height),
                        dstOffset = dstOffset,
                        dstSize = dstSize,
                        alpha = 1f - darkProgress
                    )
                    drawImage(
                        image = darkBmp,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(darkBmp.width, darkBmp.height),
                        dstOffset = dstOffset,
                        dstSize = dstSize,
                        alpha = darkProgress
                    )
                }
            }

            // Root base atmospheric grass blending (soft turf integration so trees are embedded into soil)
            val turfAlpha = if (darkProgress < 0.5f) 0.16f * (1f - darkProgress * 0.3f) else 0.12f * darkProgress
            val turfColor = if (darkProgress < 0.5f) Color(0xFF425618) else Color(0xFF0F3235)
            val turfW = dstW * 0.38f
            val turfH = dstH * 0.045f
            drawScope.drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(turfColor.copy(alpha = turfAlpha), Color.Transparent),
                    center = Offset(cx, baseY - turfH * 0.2f),
                    radius = turfW / 2f
                ),
                topLeft = Offset(cx - turfW / 2f, baseY - turfH),
                size = Size(turfW, turfH)
            )
        }
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
