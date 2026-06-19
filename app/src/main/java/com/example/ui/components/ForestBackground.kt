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

    // ── Animated colour crossfade (3 seconds on transition) ──────────
    val skyTop by animateColorAsState(
        targetValue = if (isDay) Color(0xFF1B3D2A) else Color(0xFF040D07),
        animationSpec = tween(3000), label = "skyTop"
    )
    val skyBottom by animateColorAsState(
        targetValue = if (isDay) Color(0xFF2D6B45) else Color(0xFF0A1A0F),
        animationSpec = tween(3000), label = "skyBottom"
    )
    val fogColor by animateColorAsState(
        targetValue = if (isDay) Color(0x557EC8A0) else Color(0x3320403A),
        animationSpec = tween(3000), label = "fog"
    )

    // ── Tree layer definitions ────────────────────────────────────────
    data class TreeLayer(
        val scale: Float,
        val tint: Color,
        val yFraction: Float,
        val countFraction: Float
    )
    val layers = listOf(
        TreeLayer(1.00f, if (isDay) Color(0xFF0D2010) else Color(0xFF060E08), 0.00f, 0.35f),
        TreeLayer(0.75f, if (isDay) Color(0xFF1A4D2E) else Color(0xFF0A1F10), 0.12f, 0.30f),
        TreeLayer(0.55f, if (isDay) Color(0xFF2D6B45) else Color(0xFF122B1A), 0.22f, 0.20f),
        TreeLayer(0.38f, if (isDay) Color(0xFF4A9B6F) else Color(0xFF1A3D28), 0.30f, 0.15f),
    )

    // ── Fog drift animations ─────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "fog")
    val fogOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(40000, easing = LinearEasing), RepeatMode.Restart),
        label = "fog1"
    )
    val fogOffset2 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing), RepeatMode.Restart),
        label = "fog2"
    )
    val fogOffset3 by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(80000, easing = LinearEasing), RepeatMode.Restart),
        label = "fog3"
    )

    // ── Tree scale-in animation when new tree is planted ─────────────
    // Animate only the newest tree globally
    val newestTreeScale = remember { Animatable(0f) }
    LaunchedEffect(treeCount) {
        if (treeCount > 0) {
            newestTreeScale.snapTo(0f)
            newestTreeScale.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val W = size.width
        val H = size.height

        // Sky gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(skyTop, skyBottom),
                startY = 0f, endY = H
            )
        )

        // Moon glow (night) or sun haze (day)
        if (!isDay) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x44E0F0EA), Color.Transparent),
                    center = Offset(W * 0.8f, H * 0.08f), radius = W * 0.25f
                ),
                radius = W * 0.25f, center = Offset(W * 0.8f, H * 0.08f)
            )
        } else {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x337EC8A0), Color.Transparent),
                    center = Offset(W * 0.5f, H * 0.05f), radius = W * 0.4f
                ),
                radius = W * 0.4f, center = Offset(W * 0.5f, H * 0.05f)
            )
        }

        // Draw tree layers
        // Calculate exact tree counts per layer to prevent rounding loss
        val l0Count = (treeCount * 0.35f).toInt()
        val l1Count = (treeCount * 0.30f).toInt()
        val l2Count = (treeCount * 0.20f).toInt()
        val l3Count = treeCount - l0Count - l1Count - l2Count
        val layerTreeCounts = listOf(l0Count, l1Count, l2Count, l3Count)
        
        var treeIndex = 0
        
        layers.forEachIndexed { layerIdx, layer ->
            val layerTreeCount = layerTreeCounts[layerIdx]
            val treeH    = H * layer.scale * 0.28f
            val treeW    = treeH * 0.45f
            val baseY    = H * (1f - layer.yFraction)
            val spacing  = treeW * 1.1f
            val treesPerRow = (W / spacing).toInt().coerceAtLeast(1)

            for (i in 0 until layerTreeCount) {
                val col   = i % treesPerRow
                val row   = i / treesPerRow
                val x     = col * spacing + treeW * 0.55f
                val y     = baseY - row * treeH * 0.18f
                
                // Only the very last tree planted gets the scale animation
                val scale = if (treeIndex == treeCount - 1) newestTreeScale.value else 1f
                treeIndex++

                withTransform({ scale(scale, scale, pivot = Offset(x, y)) }) {
                    drawPineTree(x = x, tipY = y - treeH, baseY = y, width = treeW, color = layer.tint)
                }
            }
        }

        // Drifting fog layers
        listOf(
            Triple(fogOffset1, H * 0.35f, 0.18f),
            Triple(fogOffset2, H * 0.50f, 0.12f),
            Triple(fogOffset3, H * 0.22f, 0.10f)
        ).forEach { (offset, fogY, alpha) ->
            // Shift the rectangle itself, not the gradient inside it
            val shiftX = (offset % 1f) * W * 1.5f - W * 0.5f
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        fogColor.copy(alpha = alpha),
                        fogColor.copy(alpha = alpha * 1.6f),
                        fogColor.copy(alpha = alpha),
                        Color.Transparent
                    ),
                    startX = 0f, endX = W * 1.5f // Gradient is relative to the drawn rect
                ),
                topLeft = Offset(shiftX, fogY - H * 0.08f), // Move the rect horizontally
                size    = Size(W * 1.5f, H * 0.16f)
            )
        }

        // Bottom vignette
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0x88000000)),
                startY = H * 0.75f, endY = H
            )
        )
    }
}

private fun DrawScope.drawPineTree(
    x: Float, tipY: Float, baseY: Float, width: Float, color: Color
) {
    val treeH = baseY - tipY
    listOf(
        Triple(0.00f, 0.55f, 1.00f),
        Triple(0.28f, 0.72f, 0.85f),
        Triple(0.52f, 0.90f, 0.70f),
    ).forEach { (startFrac, widthFrac, yFrac) ->
        val tierTipY  = tipY + treeH * startFrac
        val tierBaseY = tipY + treeH * yFrac
        val tierW     = width * widthFrac
        val path = Path().apply {
            moveTo(x, tierTipY)
            lineTo(x - tierW / 2f, tierBaseY)
            lineTo(x + tierW / 2f, tierBaseY)
            close()
        }
        drawPath(path, color)
        
        // Highlight (left rim light)
        val highlight = Path().apply {
            moveTo(x, tierTipY)
            lineTo(x - tierW / 2f, tierBaseY)
            lineTo(x - tierW * 0.1f, tierBaseY)
            close()
        }
        // Use white with low alpha to actually highlight, not transparent tree color
        drawPath(highlight, Color.White.copy(alpha = 0.15f))
    }
    // Trunk
    drawRect(
        color   = color.copy(alpha = 0.6f),
        topLeft = Offset(x - width * 0.06f, baseY - treeH * 0.12f),
        size    = Size(width * 0.12f, treeH * 0.12f)
    )
}
