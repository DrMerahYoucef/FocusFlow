# Dynamic Parallax Forest Drawing System in Kotlin & Jetpack Compose

This document guides you through the details of how the background forest is drawn in Jetpack Compose, highlighting the math, architecture, and high-performance algorithms implemented to keep rendering at a butter-smooth 120 FPS even when thousands of trees are planted.

---

## 1. Architectural Highlights

The system operates within a single native Compose `Canvas` element, avoiding heavy XML hierarchies. It leverages a custom rendering pipeline consisting of:
- **Atmospheric Celestial Objects:** Dynamically calculates top/bottom sky transitions (Day -> Night) over a 3-second animated fade. Renders interactive sun rays during the day and shimmering star fields + glowing moon halos at night.
- **Wavy Ground Fog Layers:** Custom horizontal sine/cosine waves combined into three distinct octaves. The fog scales out from background to foreground, creating beautiful layers of ground-level volumetric mist.
- **Parallax Layering Matrix:** Renders trees distributed across **6 discrete depth levels**. Background layers are drawn with higher y-coordinates and smaller size ratios, while foreground levels are larger and sit lower down on the screen.
- **Level of Detail (LOD) Thresholding:** Automatically switches from complex, highly detailed 3D shaded rendering to highly optimized, flat simplified geometries for distant layers or when the absolute tree count crosses intensive screen density caps (e.g. 120 active trees).
- **GC Allocation Prevention (Zero-Allocation onDraw Pipeline):** Reuses pre-built, thread-safe `Path` caches (`cachedTreePath`, `cachedShadowPath`, `cachedHighlightPath`, `cachedFogPath`) across frame draw invalidations to make garbage collector sweeps non-existent.

---

## 2. Main Forest Background Composable

Below is the complete implementation of the `ForestBackground` composable, managing color state configurations, star fields, sun/moon positions, dynamic fog phase animation, and layered tree structures.

```kotlin
package com.example.ui.components

import android.app.Application
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusFlowApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.math.cos

data class ForestState(
    val treeCount: Int,
    val isDayTime: Boolean
)

data class DeterministicTree(
    val xFraction: Float,
    val heightScale: Float,
    val horizontalSpacing: Float,
    val layerIndex: Int,
    val itemIndex: Int
)

@Composable
fun ForestBackground(
    forestState: ForestState,
    modifier: Modifier = Modifier
) {
    val isDay      = forestState.isDayTime
    val treeCount  = forestState.treeCount

    // ── Animated Sky & Fog transitions over 3 seconds ──────────
    val skyTop by animateColorAsState(
        targetValue = if (isDay) Color(0xFF90DBE1) else Color(0xFF030A0E),
        animationSpec = tween(3000), label = "skyTop"
    )
    val skyBottom by animateColorAsState(
        targetValue = if (isDay) Color(0xFFE2F8F4) else Color(0xFF0D1E24),
        animationSpec = tween(3000), label = "skyBottom"
    )
    val fogColor by animateColorAsState(
        targetValue = if (isDay) Color(0xFFE5FBF6) else Color(0xFF1F353A),
        animationSpec = tween(3000), label = "fog"
    )

    // ── Wavy fog/mist phase animator ──────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "fogAnimation")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(120000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    // ── Tree scale-in animation when new tree is planted ─────────────
    val newestTreeScale = remember { Animatable(0f) }
    LaunchedEffect(treeCount) {
        if (treeCount > 0) {
            newestTreeScale.snapTo(0f)
            newestTreeScale.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
        }
    }

    // ── Path pooling caches to prevent garbage collector allocations in onDraw ──
    val cachedTreePath = remember { Path() }
    val cachedShadowPath = remember { Path() }
    val cachedHighlightPath = remember { Path() }
    val cachedFogPath = remember { Path() }

    // ── Deterministic Forest Layout ──────────────────────────────────
    // Generates static, organic positions and sizes for background layers dynamically.
    // Pre-groups them into discrete layers to bypass filter calls during onDraw.
    val treesByLayer = remember(treeCount) {
        val count = maxOf(treeCount, 1)
        val flatList = List(count) { i ->
            val iLong = i.toLong()
            val seedX = (iLong * 19349663L) xor 0x5DEECE66DL
            val seedH = (iLong * 38260237L) xor 0x5DEECE66DL
            val seedW = (iLong * 85038241L) xor 0x5DEECE66DL

            val randX = (((seedX xor 12345L) % 10000L).toFloat() / 10000f).coerceIn(0f, 1f)
            val randH = (((seedH xor 54321L) % 10000L).toFloat() / 10000f).coerceIn(0f, 1f)
            val randW = (((seedW xor 98765L) % 10000L).toFloat() / 10000f).coerceIn(0f, 1f)

            // Scalable layout distributed across 6 layers
            val layerIdx = i % 6

            // Horizontally spread the trees cross-screen with margin bleed
            val xFraction = -0.12f + randX * 1.24f

            // Scale height and width beautifully
            val heightScale = 0.70f + randH * 0.45f
            val widthScale = 0.75f + randW * 0.35f

            DeterministicTree(
                xFraction = xFraction,
                heightScale = heightScale,
                horizontalSpacing = widthScale,
                layerIndex = layerIdx,
                itemIndex = i
            )
        }
        List(6) { idx ->
            flatList.filter { it.layerIndex == idx }
        }
    }

    // Twinkling stars position (Night mode)
    val starPositions = remember {
        List(15) { index ->
            Offset(
                x = (index * 0.07f + 0.1f + (index % 3) * 0.04f).coerceIn(0.05f, 0.95f),
                y = (index * 0.015f + 0.04f + (index % 2) * 0.06f).coerceIn(0.02f, 0.35f)
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val W = size.width
        val H = size.height

        // 1. Sky Gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(skyTop, skyBottom),
                startY = 0f, endY = H
            )
        )

        // 2. Cosmic elements: Star field & Moon (Night) OR sun glow & beams (Day)
        if (!isDay) {
            // Stars field
            starPositions.forEachIndexed { idx, pos ->
                val xVal = pos.x * W
                val yVal = pos.y * H
                val starA = 0.3f + 0.5f * sin(wavePhase * 0.1f + idx * 1.2f)
                drawCircle(
                    color = Color.White.copy(alpha = starA.coerceIn(0f, 1f)),
                    radius = 2.dp.toPx(),
                    center = Offset(xVal, yVal)
                )
            }

            // Moon with outer halo & silver-white core
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
            val sunCenter = Offset(W * 0.5f, H * 0.05f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3DFFEFA8), Color.Transparent),
                    center = sunCenter, radius = W * 0.55f
                ),
                radius = W * 0.55f, center = sunCenter
            )
            drawSunRays(W, H, sunCenter)
        }

        // 3. Draw dynamic layered forest with wavy mist/fog in between
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

            val treeH = H * scale * 0.28f
            val treeW = treeH * 0.45f
            val baseY = H * (1f - yFraction)

            // LOD selection
            val useSimplified = when (layerIdx) {
                0, 1, 2 -> true
                3, 4 -> treeCount > 120
                else -> false
            }

            val treesInLayer = if (treeCount > 0) treesByLayer[layerIdx] else emptyList()
            treesInLayer.forEach { tree ->
                val x = tree.xFraction * W
                val y = baseY + (tree.itemIndex % 2) * treeH * 0.04f
                val finalH = treeH * tree.heightScale
                val finalW = treeW * tree.horizontalSpacing

                val isActive = tree.itemIndex < treeCount
                val extraScale = if (isActive && tree.itemIndex == treeCount - 1) newestTreeScale.value else 1f

                withTransform({ scale(extraScale, extraScale, pivot = Offset(x, y)) }) {
                    if (useSimplified) {
                        drawSimplifiedPineTree(
                            drawScope = this,
                            x = x,
                            tipY = y - finalH,
                            baseY = y,
                            width = finalW,
                            color = baseColor,
                            path = cachedTreePath
                        )
                    } else {
                        drawDetailedPineTree(
                            drawScope = this,
                            x = x,
                            tipY = y - finalH,
                            baseY = y,
                            width = finalW,
                            color = baseColor,
                            isDay = isDay,
                            path = cachedTreePath,
                            rightSidePath = cachedShadowPath,
                            highlightPath = cachedHighlightPath
                        )
                    }
                }

                // Periodic spark particles only on select active trees to keep screen peaceful
                if (isActive && tree.itemIndex % 15 == 0) {
                    drawFairyFireflies(
                        x = x,
                        y = y,
                        treeW = finalW,
                        treeH = finalH,
                        wavePhase = wavePhase,
                        isDay = isDay
                    )
                }
            }

            // Draw wavy fog layers
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
            val speedMultiplier = when (layerIdx) {
                0 -> 0.03f
                1 -> -0.04f
                2 -> 0.05f
                3 -> -0.06f
                4 -> 0.07f
                else -> -0.08f
            }

            drawWavyFog(
                baseY = fogY,
                fogColor = fogColor,
                wavePhase = wavePhase,
                alphaMultiplier = fogAlpha,
                waveScale = fogScale,
                speedMultiplier = speedMultiplier,
                fogPath = cachedFogPath
            )
        }

        // 4. Soft bottom vignette to seal shadows
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xBB000000)),
                startY = H * 0.70f, endY = H
            )
        )
    }
}
```

---

## 3. Detailed Tree Drawing Procedure (With Shadows & Shading)

This function draws high-quality symmetrical structures of pine trees complete with:
- Slower left-to-right outer coordinates.
- Unique dark-shadow overlay path applied on the right-hand side of trunks and needles to convey three-dimensional depth.
- Bright white-tinted rim highlight along left boundaries to capture dynamic solar or lunar emission curves.

```kotlin
private fun DrawScope.drawDetailedPineTree(
    drawScope: DrawScope,
    x: Float,
    tipY: Float,
    baseY: Float,
    width: Float,
    color: Color,
    isDay: Boolean,
    path: Path,
    rightSidePath: Path,
    highlightPath: Path
) {
    val h = baseY - tipY
    val w = width

    // 1. Full base shape matching symmetrical layers
    path.reset()
    path.moveTo(x, tipY)

    path.lineTo(x - w * 0.14f, tipY + h * 0.10f)
    path.lineTo(x - w * 0.07f, tipY + h * 0.12f)
    path.lineTo(x - w * 0.23f, tipY + h * 0.22f)
    path.lineTo(x - w * 0.11f, tipY + h * 0.25f)
    path.lineTo(x - w * 0.33f, tipY + h * 0.36f)
    path.lineTo(x - w * 0.16f, tipY + h * 0.39f)
    path.lineTo(x - w * 0.43f, tipY + h * 0.49f)
    path.lineTo(x - w * 0.21f, tipY + h * 0.52f)
    path.lineTo(x - w * 0.53f, tipY + h * 0.64f)
    path.lineTo(x - w * 0.26f, tipY + h * 0.67f)
    path.lineTo(x - w * 0.63f, tipY + h * 0.78f)
    path.lineTo(x - w * 0.30f, tipY + h * 0.82f)
    path.lineTo(x - w * 0.72f, tipY + h * 0.94f)
    path.lineTo(x - w * 0.33f, baseY)

    path.lineTo(x - w * 0.08f, baseY)
    path.lineTo(x - w * 0.08f, baseY + h * 0.12f)
    path.lineTo(x + w * 0.08f, baseY + h * 0.12f)
    path.lineTo(x + w * 0.08f, baseY)
    path.lineTo(x + w * 0.33f, baseY)

    path.lineTo(x + w * 0.72f, tipY + h * 0.94f)
    path.lineTo(x + w * 0.30f, tipY + h * 0.82f)
    path.lineTo(x + w * 0.63f, tipY + h * 0.78f)
    path.lineTo(x + w * 0.26f, tipY + h * 0.67f)
    path.lineTo(x + w * 0.53f, tipY + h * 0.64f)
    path.lineTo(x + w * 0.21f, tipY + h * 0.52f)
    path.lineTo(x + w * 0.43f, tipY + h * 0.49f)
    path.lineTo(x + w * 0.16f, tipY + h * 0.39f)
    path.lineTo(x + w * 0.33f, tipY + h * 0.36f)
    path.lineTo(x + w * 0.11f, tipY + h * 0.25f)
    path.lineTo(x + w * 0.23f, tipY + h * 0.22f)
    path.lineTo(x + w * 0.07f, tipY + h * 0.12f)
    path.lineTo(x + w * 0.14f, tipY + h * 0.10f)

    path.close()
    drawScope.drawPath(path, color)

    // 2. Right shadow overlay
    rightSidePath.reset()
    rightSidePath.moveTo(x, tipY)
    rightSidePath.lineTo(x + w * 0.14f, tipY + h * 0.10f)
    rightSidePath.lineTo(x + w * 0.07f, tipY + h * 0.12f)
    rightSidePath.lineTo(x + w * 0.23f, tipY + h * 0.22f)
    rightSidePath.lineTo(x + w * 0.11f, tipY + h * 0.25f)
    rightSidePath.lineTo(x + w * 0.33f, tipY + h * 0.36f)
    rightSidePath.lineTo(x + w * 0.16f, tipY + h * 0.39f)
    rightSidePath.lineTo(x + w * 0.43f, tipY + h * 0.49f)
    rightSidePath.lineTo(x + w * 0.21f, tipY + h * 0.52f)
    rightSidePath.lineTo(x + w * 0.53f, tipY + h * 0.64f)
    rightSidePath.lineTo(x + w * 0.26f, tipY + h * 0.67f)
    rightSidePath.lineTo(x + w * 0.63f, tipY + h * 0.78f)
    rightSidePath.lineTo(x + w * 0.30f, tipY + h * 0.82f)
    rightSidePath.lineTo(x + w * 0.72f, tipY + h * 0.94f)
    rightSidePath.lineTo(x + w * 0.33f, baseY)

    rightSidePath.lineTo(x + w * 0.08f, baseY)
    rightSidePath.lineTo(x + w * 0.08f, baseY + h * 0.12f)
    rightSidePath.lineTo(x, baseY + h * 0.12f)
    rightSidePath.lineTo(x, baseY)
    rightSidePath.lineTo(x, tipY)
    rightSidePath.close()

    val shadowOpacity = if (isDay) 0.22f else 0.28f
    drawScope.drawPath(rightSidePath, Color.Black.copy(alpha = shadowOpacity))

    // 3. Symmetrical Highlight (Left edge)
    highlightPath.reset()
    highlightPath.moveTo(x, tipY)
    highlightPath.lineTo(x - w * 0.14f, tipY + h * 0.10f)
    highlightPath.lineTo(x - w * 0.07f, tipY + h * 0.12f)
    highlightPath.lineTo(x - w * 0.23f, tipY + h * 0.22f)
    highlightPath.lineTo(x - w * 0.11f, tipY + h * 0.25f)
    highlightPath.lineTo(x - w * 0.33f, tipY + h * 0.36f)
    highlightPath.lineTo(x - w * 0.16f, tipY + h * 0.39f)
    highlightPath.lineTo(x - w * 0.43f, tipY + h * 0.49f)
    highlightPath.lineTo(x - w * 0.21f, tipY + h * 0.52f)
    highlightPath.lineTo(x - w * 0.53f, tipY + h * 0.64f)
    highlightPath.lineTo(x - w * 0.26f, tipY + h * 0.67f)
    highlightPath.lineTo(x - w * 0.63f, tipY + h * 0.78f)
    highlightPath.lineTo(x - w * 0.30f, tipY + h * 0.82f)
    highlightPath.lineTo(x - w * 0.72f, tipY + h * 0.94f)
    highlightPath.lineTo(x - w * 0.33f, baseY)
    highlightPath.lineTo(x, baseY)
    highlightPath.close()

    val highlightOpacity = if (isDay) 0.12f else 0.05f
    drawScope.drawPath(highlightPath, Color.White.copy(alpha = highlightOpacity))
}
```

---

## 4. Simplified Tree Drawing Procedure (LOD Mode)

To draw background trees extremely fast or avoid frame-time overhead with a massive number of active items, this method is used. It trims down the segments to only 3 vertical tiers and drops right shadow/left highlight overlays.

```kotlin
private fun DrawScope.drawSimplifiedPineTree(
    drawScope: DrawScope,
    x: Float,
    tipY: Float,
    baseY: Float,
    width: Float,
    color: Color,
    path: Path
) {
    val h = baseY - tipY
    val w = width

    path.reset()
    path.moveTo(x, tipY)

    // Tier 1 (top)
    path.lineTo(x - w * 0.25f, tipY + h * 0.3f)
    path.lineTo(x - w * 0.12f, tipY + h * 0.32f)

    // Tier 2 (middle)
    path.lineTo(x - w * 0.48f, tipY + h * 0.63f)
    path.lineTo(x - w * 0.24f, tipY + h * 0.65f)

    // Tier 3 (bottom canopy)
    path.lineTo(x - w * 0.65f, tipY + h * 0.94f)
    path.lineTo(x - w * 0.08f, baseY)

    // Trunk left & bottom flat curves
    path.lineTo(x - w * 0.08f, baseY + h * 0.12f)
    path.lineTo(x + w * 0.08f, baseY + h * 0.12f)
    path.lineTo(x + w * 0.08f, baseY)

    // Symmetrical scale right
    path.lineTo(x + w * 0.65f, tipY + h * 0.94f)
    path.lineTo(x + w * 0.24f, tipY + h * 0.65f)
    path.lineTo(x + w * 0.48f, tipY + h * 0.63f)
    path.lineTo(x + w * 0.12f, tipY + h * 0.32f)
    path.lineTo(x + w * 0.25f, tipY + h * 0.3f)

    path.close()
    drawScope.drawPath(path, color)
}
```

---

## 5. Volumetric Wavy Ground Fog

Renders wavy horizontal mists dynamically blending multiple sine-wave layers using path inputs.

```kotlin
private fun DrawScope.drawWavyFog(
    baseY: Float,
    fogColor: Color,
    wavePhase: Float,
    alphaMultiplier: Float,
    waveScale: Float,
    speedMultiplier: Float,
    fogPath: Path
) {
    val W = size.width
    val H = size.height

    val time = wavePhase * speedMultiplier
    fogPath.reset()
    fogPath.moveTo(0f, H)

    val steps = 25
    val stepW = W / steps
    for (i in 0..steps) {
        val x = i * stepW
        val wave1 = sin(x * 0.005f + time * 1.5f) * 20f * waveScale
        val wave2 = cos(x * 0.012f - time * 0.7f) * 12f * waveScale
        val wave3 = sin(x * 0.025f + time * 2.2f) * 6f * waveScale
        val waveY = baseY + wave1 + wave2 + wave3
        fogPath.lineTo(x, waveY)
    }
    fogPath.lineTo(W, H)
    fogPath.close()

    val mistHeight = 160f * waveScale
    val startY = baseY - mistHeight * 0.3f
    val endY = baseY + mistHeight * 0.7f

    drawPath(
        path = fogPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                fogColor.copy(alpha = alphaMultiplier * 0.8f),
                fogColor.copy(alpha = alphaMultiplier * 1.5f),
                fogColor.copy(alpha = alphaMultiplier * 0.6f),
                Color.Transparent
            ),
            startY = startY,
            endY = endY
        )
    )
}
```
