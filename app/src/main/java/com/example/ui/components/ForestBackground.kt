package com.example.ui.components

import android.app.Application
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
                    checkAndTriggerAutoWallpaper()
                }
        }
        // Re-check day/night every 60 seconds
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                val isDay = isDayByClockRule()
                val oldState = _forestState.value.isDayTime
                _forestState.update { it.copy(isDayTime = isDay) }
                if (oldState != isDay) {
                    checkAndTriggerAutoWallpaper()
                }
            }
        }
    }

    private fun isDayByClockRule(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour in 6..17  // 06:00–17:59 = Day, 18:00–05:59 = Night
    }

    fun checkAndTriggerAutoWallpaper() {
        val app = getApplication<Application>()
        val sharedPrefs = app.getSharedPreferences("focusflow_prefs", android.content.Context.MODE_PRIVATE)
        val autoSync = sharedPrefs.getBoolean("auto_sync_wallpaper", false)
        if (!autoSync) return

        val setHome = sharedPrefs.getBoolean("wallpaper_home_screen", true)
        val setLock = sharedPrefs.getBoolean("wallpaper_lock_screen", false)
        val isDay = isDayByClockRule()
        val count = _forestState.value.treeCount

        val lastDay = if (sharedPrefs.contains("last_synced_daytime")) {
            sharedPrefs.getBoolean("last_synced_daytime", false)
        } else {
            !isDay // Force initial sync
        }
        val lastCount = sharedPrefs.getInt("last_synced_tree_count", -1)

        if (isDay != lastDay || count != lastCount) {
            // Re-apply!
            com.example.ui.components.WallpaperHelper.setForestWallpaper(
                context = app,
                isDay = isDay,
                treeCount = count,
                setHomeScreen = setHome,
                setLockScreen = setLock
            ) { success, _ ->
                if (success) {
                    sharedPrefs.edit()
                        .putBoolean("last_synced_daytime", isDay)
                        .putInt("last_synced_tree_count", count)
                        .apply()
                }
            }
        }
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

    // ── Lifecycle-aware Animation Driver (Pause when off-screen) ─────
    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppResumed by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isAppResumed = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val wavePhase = remember { Animatable(0f) }
    LaunchedEffect(isAppResumed) {
        if (isAppResumed) {
            while (true) {
                wavePhase.animateTo(
                    targetValue = wavePhase.value + 1000f,
                    animationSpec = tween(120000, easing = LinearEasing)
                )
            }
        } else {
            wavePhase.stop()
        }
    }

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

    // ── Deterministic Forest Layout (Optimal O(N) grouping) ──────────
    val treesByLayer = remember(treeCount) {
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

            // Scalable layout distributed across 6 layers
            val layerIdx = i % 6

            // Horizontally spread the trees cross-screen with margin bleed
            val xFraction = -0.12f + randX * 1.24f

            // Scale height and width beautifully
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
        layers
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

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Static Sky, Celestial Moon/Sun Layer (graphicsLayer caches drawn output on GPU)
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer()) {
            val W = size.width
            val H = size.height

            // Sky Gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(skyTop, skyBottom),
                    startY = 0f, endY = H
                )
            )

            if (!isDay) {
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
                // Day Sun glow & warm sun beams (Make the sun highly visible)
                val sunCenter = Offset(W * 0.5f, H * 0.05f)
                // 1. Broad soft ambient sun glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x66FFEFA8), Color.Transparent),
                        center = sunCenter, radius = W * 0.55f
                    ),
                    radius = W * 0.55f, center = sunCenter
                )
                // 2. Strong inner sun glow / halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xB3FFF099), Color.Transparent),
                        center = sunCenter, radius = W * 0.16f
                    ),
                    radius = W * 0.16f, center = sunCenter
                )
                // 3. Dense glowing visual sun center core (Brilliant white-yellow)
                drawCircle(
                    color = Color(0xFFFFFCEB),
                    radius = W * 0.065f,
                    center = sunCenter
                )
                drawSunRays(W, H, sunCenter)
            }
        }

        // 2. Animated Starfield Layer
        if (!isDay) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val W = size.width
                val H = size.height
                val currentPhase = wavePhase.value
                // Stars field
                starPositions.forEachIndexed { idx, pos ->
                    val xVal = pos.x * W
                    val yVal = pos.y * H
                    val starA = 0.3f + 0.5f * sin(currentPhase * 0.1f + idx * 1.2f)
                    drawCircle(
                        color = Color.White.copy(alpha = starA.coerceIn(0f, 1f)),
                        radius = 2.dp.toPx(),
                        center = Offset(xVal, yVal)
                    )
                }
            }
        }

        // 3. Draw dynamic layered forest with wavy mist/fog in between
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

            // --- Smart capping/culling logic to maintain constant render-time ---
            val list = if (treeCount > 0) treesByLayer[layerIdx] else emptyList()
            val ceiling = when (layerIdx) {
                0 -> 15
                1 -> 20
                2 -> 25
                3 -> 30
                4 -> 35
                else -> 40
            }
            val treesInLayer = remember(list, ceiling, treeCount) {
                if (list.size <= ceiling) {
                    list
                } else {
                    val newest = list.lastOrNull()
                    if (newest != null && newest.itemIndex == treeCount - 1) {
                        list.take(ceiling - 1) + newest
                    } else {
                        list.take(ceiling)
                    }
                }
            }

            // Static tree drawing Canvas for local layer (GPU RenderNode Cached)
            val treeScaleVal = newestTreeScale.value
            Canvas(modifier = Modifier.fillMaxSize().graphicsLayer()) {
                val W = size.width
                val H = size.height

                val treeH = H * scale * 0.28f
                val treeW = treeH * 0.45f
                val baseY = H * (1f - yFraction)

                // Dynamic Level of Detail (LOD) optimization for buttery smooth 120 FPS
                val useSimplified = when (layerIdx) {
                    0, 1, 2 -> true
                    3, 4 -> treeCount > 120
                    else -> false
                }

                treesInLayer.forEach { tree ->
                    val x = tree.xFraction * W
                    val y = baseY + (tree.itemIndex % 2) * treeH * 0.04f
                    val finalH = treeH * tree.heightScale
                    val finalW = treeW * tree.horizontalSpacing

                    val isActive = tree.itemIndex < treeCount
                    val isNewest = isActive && tree.itemIndex == treeCount - 1
                    val extraScale = if (isNewest) treeScaleVal else 1f

                    // matrix transform only for the animating newest tree!
                    if (isNewest && extraScale != 1f) {
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
                    } else {
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
                }
            }

            // Animated Overlay layer draw Canvas (Wavy fog and bioluminescent spark fireflies)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val W = size.width
                val H = size.height
                val currentPhase = wavePhase.value

                val treeH = H * scale * 0.28f
                val treeW = treeH * 0.45f
                val baseY = H * (1f - yFraction)

                // Particles on active trees
                treesInLayer.forEach { tree ->
                    val isActive = tree.itemIndex < treeCount
                    if (isActive && tree.itemIndex % 15 == 0) {
                        val x = tree.xFraction * W
                        val y = baseY + (tree.itemIndex % 2) * treeH * 0.04f
                        val finalH = treeH * tree.heightScale
                        val finalW = treeW * tree.horizontalSpacing

                        drawFairyFireflies(
                            x = x,
                            y = y,
                            treeW = finalW,
                            treeH = finalH,
                            wavePhase = currentPhase,
                            isDay = isDay
                        )
                    }
                }

                // Draw wavy fog layer
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
                    wavePhase = currentPhase,
                    alphaMultiplier = fogAlpha,
                    waveScale = fogScale,
                    speedMultiplier = speedMultiplier,
                    fogPath = cachedFogPath
                )
            }
        }

        // 4. Front Vignette Layer for shading transitions (Static, cached)
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer()) {
            val H = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xBB000000)),
                    startY = H * 0.70f, endY = H
                )
            )
        }
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
    isDay: Boolean,
    path: Path,
    rightSidePath: Path,
    highlightPath: Path
) {
    val h = baseY - tipY
    val w = width

    // 1. Draw the full left-to-right symmetric pine tree shape in base color
    path.reset()
    path.moveTo(x, tipY)

    // Left side segments (Top to bottom)
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

    // Under-canopy tuck in
    path.lineTo(x - w * 0.08f, baseY)

    // Trunk left
    path.lineTo(x - w * 0.08f, baseY + h * 0.12f)
    // Trunk bottom
    path.lineTo(x + w * 0.08f, baseY + h * 0.12f)
    // Trunk right
    path.lineTo(x + w * 0.08f, baseY)

    // Under-canopy tuck in right
    path.lineTo(x + w * 0.33f, baseY)

    // Right side segments (symmetric going up)
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

    // 2. Draw the shadow side (Right side) path with dark overlay for depth shading!
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
    // Under trunk right
    rightSidePath.lineTo(x + w * 0.08f, baseY)
    rightSidePath.lineTo(x + w * 0.08f, baseY + h * 0.12f)
    rightSidePath.lineTo(x, baseY + h * 0.12f)
    rightSidePath.lineTo(x, baseY)
    rightSidePath.lineTo(x, tipY)
    rightSidePath.close()
    val shadowOpacity = if (isDay) 0.22f else 0.28f
    drawScope.drawPath(rightSidePath, Color.Black.copy(alpha = shadowOpacity))

    // 3. Draw a gorgeous left rim highlight to make the edges pop in 3D
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

    // Trunk left
    path.lineTo(x - w * 0.08f, baseY + h * 0.12f)
    // Trunk right
    path.lineTo(x + w * 0.08f, baseY + h * 0.12f)
    path.lineTo(x + w * 0.08f, baseY)

    // Tier 3 right
    path.lineTo(x + w * 0.65f, tipY + h * 0.94f)
    path.lineTo(x + w * 0.24f, tipY + h * 0.65f)

    // Tier 2 right
    path.lineTo(x + w * 0.48f, tipY + h * 0.63f)
    path.lineTo(x + w * 0.12f, tipY + h * 0.32f)

    // Tier 1 right
    path.lineTo(x + w * 0.25f, tipY + h * 0.3f)

    path.close()
    drawScope.drawPath(path, color)
}

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
