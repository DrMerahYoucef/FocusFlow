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
import androidx.compose.ui.unit.Dp
import kotlin.math.sin
import kotlin.math.cos

data class ForestState(
    val treeCount: Int,
    val isDayTime: Boolean
)

class ForestViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FocusFlowApplication.instance.sessionRepository

    private val _forestState = MutableStateFlow(
        ForestState(treeCount = 0, isDayTime = isDayByClockRule())
    )
    val forestState: StateFlow<ForestState> = _forestState.asStateFlow()

    init {
        // Observe total completed sessions from Room
        viewModelScope.launch {
            repository
                .getSessionCount(0L, Long.MAX_VALUE)
                .collect { count ->
                    _forestState.update { it.copy(treeCount = count) }
                }
        }
        // Re-check day/night every 60 seconds
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                _forestState.update { it.copy(isDayTime = isDayByClockRule()) }
            }
        }
    }

    private fun isDayByClockRule(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour in 6..17  // 06:00–17:59 = Day, 18:00–05:59 = Night
    }
}

@Composable
fun ForestBackground(
    forestState: ForestState,
    modifier: Modifier = Modifier
) {
    val isDay      = forestState.isDayTime
    val treeCount  = forestState.treeCount

    // ── Animated color crossfade (3 seconds on transition) ──────────
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

    // ── Deterministic Forest Layout ──────────────────────────────────
    // Generates static, organic positions and sizes for background layers dynamically.
    // Handles any number of trees seamlessly, preventing limits at 100 or 400 trees.
    val treesToDraw = remember(treeCount) {
        val count = maxOf(treeCount, 1)
        List(count) { i ->
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
        }.sortedWith(compareBy({ it.layerIndex }, { it.itemIndex }))
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
            // Outer halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x22ADC6D1), Color.Transparent),
                    center = moonCenter, radius = W * 0.45f
                ),
                radius = W * 0.45f, center = moonCenter
            )
            // Inner halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3DDCF1F5), Color.Transparent),
                    center = moonCenter, radius = W * 0.18f
                ),
                radius = W * 0.18f, center = moonCenter
            )
            // Moon Core
            drawCircle(
                color = Color(0xFFE9F5F8),
                radius = W * 0.06f,
                center = moonCenter
            )
        } else {
            // Day Sun glow & warm sun beams
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
        // Rendering 6 discrete depth levels for supreme parallax visual rich depth
        for (layerIdx in 0..5) {
            // Base parameters for drawing layer
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

            // Draw trees of current layer
            val treesInLayer = if (treeCount > 0) treesToDraw.filter { it.layerIndex == layerIdx } else emptyList()
            treesInLayer.forEach { tree ->
                val x = tree.xFraction * W
                val y = baseY + (tree.itemIndex % 2) * treeH * 0.04f // Stagger vertically for maximum natural beauty
                val finalH = treeH * tree.heightScale
                val finalW = treeW * tree.horizontalSpacing

                val isActive = tree.itemIndex < treeCount
                val extraScale = if (isActive && tree.itemIndex == treeCount - 1) newestTreeScale.value else 1f

                withTransform({ scale(extraScale, extraScale, pivot = Offset(x, y)) }) {
                    drawDetailedPineTree(
                        drawScope = this,
                        x = x,
                        tipY = y - finalH,
                        baseY = y,
                        width = finalW,
                        color = baseColor,
                        isDay = isDay
                    )
                }

                // If active tree, spawn floating spark bio-particles on a select subset of trees to keep the screen serene
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

            // Draw wavy fog layer behind the next closer tree layer
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
                speedMultiplier = speedMultiplier
            )
        }

        // 4. Bottom Vignette for soft shading transitions
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xBB000000)),
                startY = H * 0.70f, endY = H
            )
        )
    }
}

data class DeterministicTree(
    val xFraction: Float,
    val heightScale: Float,
    val horizontalSpacing: Float,
    val layerIndex: Int,
    val itemIndex: Int
)

private fun DrawScope.drawSunRays(W: Float, H: Float, sunCenter: Offset) {
    val rayColor = Color(0x10FFF4CC)
    val angles = listOf(-25f, -5f, 15f, 35f, 55f)
    angles.forEach { angleDeg ->
        withTransform({
            rotate(angleDeg, pivot = sunCenter)
        }) {
            val rayPath = Path().apply {
                moveTo(sunCenter.x, sunCenter.y)
                lineTo(sunCenter.x - W * 0.15f, H * 1.3f)
                lineTo(sunCenter.x + W * 0.15f, H * 1.3f)
                close()
            }
            drawPath(
                path = rayPath,
                brush = Brush.verticalGradient(
                    colors = listOf(rayColor, Color.Transparent),
                    startY = sunCenter.y,
                    endY = H * 1.3f
                )
            )
        }
    }
}

private fun DrawScope.drawDetailedPineTree(
    drawScope: DrawScope,
    x: Float,
    tipY: Float,
    baseY: Float,
    width: Float,
    color: Color,
    isDay: Boolean
) {
    val h = baseY - tipY
    val w = width

    // 1. Draw the full left-to-right symmetric pine tree shape in base color
    val path = Path().apply {
        moveTo(x, tipY)

        // Left side segments (Top to bottom)
        lineTo(x - w * 0.14f, tipY + h * 0.10f)
        lineTo(x - w * 0.07f, tipY + h * 0.12f)
        lineTo(x - w * 0.23f, tipY + h * 0.22f)
        lineTo(x - w * 0.11f, tipY + h * 0.25f)
        lineTo(x - w * 0.33f, tipY + h * 0.36f)
        lineTo(x - w * 0.16f, tipY + h * 0.39f)
        lineTo(x - w * 0.43f, tipY + h * 0.49f)
        lineTo(x - w * 0.21f, tipY + h * 0.52f)
        lineTo(x - w * 0.53f, tipY + h * 0.64f)
        lineTo(x - w * 0.26f, tipY + h * 0.67f)
        lineTo(x - w * 0.63f, tipY + h * 0.78f)
        lineTo(x - w * 0.30f, tipY + h * 0.82f)
        lineTo(x - w * 0.72f, tipY + h * 0.94f)
        lineTo(x - w * 0.33f, baseY)

        // Under-canopy tuck in
        lineTo(x - w * 0.08f, baseY)

        // Trunk left
        lineTo(x - w * 0.08f, baseY + h * 0.12f)
        // Trunk bottom
        lineTo(x + w * 0.08f, baseY + h * 0.12f)
        // Trunk right
        lineTo(x + w * 0.08f, baseY)

        // Under-canopy tuck in right
        lineTo(x + w * 0.33f, baseY)

        // Right side segments (symmetric going up)
        lineTo(x + w * 0.72f, tipY + h * 0.94f)
        lineTo(x + w * 0.30f, tipY + h * 0.82f)
        lineTo(x + w * 0.63f, tipY + h * 0.78f)
        lineTo(x + w * 0.26f, tipY + h * 0.67f)
        lineTo(x + w * 0.53f, tipY + h * 0.64f)
        lineTo(x + w * 0.21f, tipY + h * 0.52f)
        lineTo(x + w * 0.43f, tipY + h * 0.49f)
        lineTo(x + w * 0.16f, tipY + h * 0.39f)
        lineTo(x + w * 0.33f, tipY + h * 0.36f)
        lineTo(x + w * 0.11f, tipY + h * 0.25f)
        lineTo(x + w * 0.23f, tipY + h * 0.22f)
        lineTo(x + w * 0.07f, tipY + h * 0.12f)
        lineTo(x + w * 0.14f, tipY + h * 0.10f)

        close()
    }
    drawScope.drawPath(path, color)

    // 2. Draw the shadow side (Right side) path with dark overlay for depth shading!
    val rightSidePath = Path().apply {
        moveTo(x, tipY)
        lineTo(x + w * 0.14f, tipY + h * 0.10f)
        lineTo(x + w * 0.07f, tipY + h * 0.12f)
        lineTo(x + w * 0.23f, tipY + h * 0.22f)
        lineTo(x + w * 0.11f, tipY + h * 0.25f)
        lineTo(x + w * 0.33f, tipY + h * 0.36f)
        lineTo(x + w * 0.16f, tipY + h * 0.39f)
        lineTo(x + w * 0.43f, tipY + h * 0.49f)
        lineTo(x + w * 0.21f, tipY + h * 0.52f)
        lineTo(x + w * 0.53f, tipY + h * 0.64f)
        lineTo(x + w * 0.26f, tipY + h * 0.67f)
        lineTo(x + w * 0.63f, tipY + h * 0.78f)
        lineTo(x + w * 0.30f, tipY + h * 0.82f)
        lineTo(x + w * 0.72f, tipY + h * 0.94f)
        lineTo(x + w * 0.33f, baseY)
        // Under trunk right
        lineTo(x + w * 0.08f, baseY)
        lineTo(x + w * 0.08f, baseY + h * 0.12f)
        lineTo(x, baseY + h * 0.12f)
        lineTo(x, baseY)
        lineTo(x, tipY)
        close()
    }
    val shadowOpacity = if (isDay) 0.22f else 0.28f
    drawScope.drawPath(rightSidePath, Color.Black.copy(alpha = shadowOpacity))

    // 3. Draw a gorgeous left rim highlight to make the edges pop in 3D
    val highlightPath = Path().apply {
        moveTo(x, tipY)
        lineTo(x - w * 0.14f, tipY + h * 0.10f)
        lineTo(x - w * 0.07f, tipY + h * 0.12f)
        lineTo(x - w * 0.23f, tipY + h * 0.22f)
        lineTo(x - w * 0.11f, tipY + h * 0.25f)
        lineTo(x - w * 0.33f, tipY + h * 0.36f)
        lineTo(x - w * 0.16f, tipY + h * 0.39f)
        lineTo(x - w * 0.43f, tipY + h * 0.49f)
        lineTo(x - w * 0.21f, tipY + h * 0.52f)
        lineTo(x - w * 0.53f, tipY + h * 0.64f)
        lineTo(x - w * 0.26f, tipY + h * 0.67f)
        lineTo(x - w * 0.63f, tipY + h * 0.78f)
        lineTo(x - w * 0.30f, tipY + h * 0.82f)
        lineTo(x - w * 0.72f, tipY + h * 0.94f)
        lineTo(x - w * 0.33f, baseY)
        lineTo(x, baseY)
        close()
    }
    val highlightOpacity = if (isDay) 0.12f else 0.05f
    drawScope.drawPath(highlightPath, Color.White.copy(alpha = highlightOpacity))
}

private fun DrawScope.drawWavyFog(
    baseY: Float,
    fogColor: Color,
    wavePhase: Float,
    alphaMultiplier: Float,
    waveScale: Float,
    speedMultiplier: Float
) {
    val W = size.width
    val H = size.height

    val time = wavePhase * speedMultiplier
    val fogPath = Path().apply {
        moveTo(0f, H)

        val steps = 25
        val stepW = W / steps
        for (i in 0..steps) {
            val x = i * stepW
            val wave1 = sin(x * 0.005f + time * 1.5f) * 20f * waveScale
            val wave2 = cos(x * 0.012f - time * 0.7f) * 12f * waveScale
            val wave3 = sin(x * 0.025f + time * 2.2f) * 6f * waveScale
            val waveY = baseY + wave1 + wave2 + wave3
            lineTo(x, waveY)
        }
        lineTo(W, H)
        close()
    }

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

private fun DrawScope.drawFairyFireflies(
    x: Float,
    y: Float,
    treeW: Float,
    treeH: Float,
    wavePhase: Float,
    isDay: Boolean
) {
    for (p in 0 until 4) {
        val particlePhase = wavePhase * 0.08f + p * 1.5f + (x * 0.005f)
        val dx = sin(particlePhase) * treeW * 0.6f + cos(particlePhase * 0.5f) * treeW * 0.2f
        val dy = -((particlePhase * 15f + p * 30f) % treeH)
        val pX = x + dx
        val pY = y + dy

        val pColor = if (isDay) {
            Color(0x99FFE885) // Gold
        } else {
            Color(0xCCB9A8FF) // Magical lavender/indigo bioluminescent spark
        }
        val pGlowRadius = 8.dp.toPx()
        val pCoreRadius = 2.dp.toPx()

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(pColor, Color.Transparent),
                center = Offset(pX, pY),
                radius = pGlowRadius
            ),
            radius = pGlowRadius,
            center = Offset(pX, pY)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = pCoreRadius,
            center = Offset(pX, pY)
        )
    }
}
