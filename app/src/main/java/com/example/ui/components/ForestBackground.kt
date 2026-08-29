package com.example.ui.components

import android.app.Application
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.imageResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewModelScope
import com.example.FocusFlowApplication
import com.example.R
import com.example.ui.theme.WallpaperTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ForestState(
    val treeCount: Int = 0,
    val followSystemTheme: Boolean = true,
    val manualTheme: WallpaperTheme = WallpaperTheme.LIGHT,
    val isDarkTheme: Boolean = false
)

class ForestViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FocusFlowApplication.instance.sessionRepository
    private val sharedPrefs = application.getSharedPreferences("focusflow_prefs", android.content.Context.MODE_PRIVATE)

    private val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "follow_system_theme" || key == "wallpaper_theme" || key == "auto_sync_wallpaper") {
            reloadPreferences()
        }
    }

    private val _forestState = MutableStateFlow(loadInitialState())
    val forestState: StateFlow<ForestState> = _forestState.asStateFlow()

    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefListener)
        viewModelScope.launch {
            repository
                .getSessionCount(0L, Long.MAX_VALUE)
                .collect { count ->
                    _forestState.update { it.copy(treeCount = count) }
                    sharedPrefs.edit().putInt("last_synced_tree_count", count).apply()
                }
        }
    }

    private fun loadInitialState(): ForestState {
        val followSystem = sharedPrefs.getBoolean("follow_system_theme", true)
        val manualThemeStr = sharedPrefs.getString("wallpaper_theme", "LIGHT") ?: "LIGHT"
        val manualTheme = try {
            WallpaperTheme.valueOf(manualThemeStr)
        } catch (e: Exception) {
            WallpaperTheme.LIGHT
        }
        val savedCount = sharedPrefs.getInt("last_synced_tree_count", 0)
        return ForestState(
            treeCount = savedCount,
            followSystemTheme = followSystem,
            manualTheme = manualTheme,
            isDarkTheme = manualTheme == WallpaperTheme.DARK
        )
    }

    private fun reloadPreferences() {
        val followSystem = sharedPrefs.getBoolean("follow_system_theme", true)
        val manualThemeStr = sharedPrefs.getString("wallpaper_theme", "LIGHT") ?: "LIGHT"
        val manualTheme = try {
            WallpaperTheme.valueOf(manualThemeStr)
        } catch (e: Exception) {
            WallpaperTheme.LIGHT
        }
        _forestState.update {
            it.copy(
                followSystemTheme = followSystem,
                manualTheme = manualTheme
            )
        }
    }

    fun updateSystemDarkTheme(isSystemDark: Boolean) {
        val current = _forestState.value
        val effectiveDark = if (current.followSystemTheme) isSystemDark else (current.manualTheme == WallpaperTheme.DARK)
        if (current.isDarkTheme != effectiveDark) {
            _forestState.update { it.copy(isDarkTheme = effectiveDark) }
            checkAndTriggerAutoWallpaper(effectiveDark)
        }
    }

    fun checkAndTriggerAutoWallpaper(isDark: Boolean) {
        val app = getApplication<Application>()
        val autoSync = sharedPrefs.getBoolean("auto_sync_wallpaper", false)
        if (!autoSync) return

        val setHome = sharedPrefs.getBoolean("wallpaper_home_screen", true)
        val setLock = sharedPrefs.getBoolean("wallpaper_lock_screen", false)
        val theme = if (isDark) WallpaperTheme.DARK else WallpaperTheme.LIGHT

        WallpaperHelper.setForestWallpaper(
            context = app,
            theme = theme,
            setHomeScreen = setHome,
            setLockScreen = setLock,
            treeCount = _forestState.value.treeCount
        ) { _, _ -> }
    }

    override fun onCleared() {
        super.onCleared()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
}

@Composable
fun ForestBackground(
    forestState: ForestState,
    modifier: Modifier = Modifier
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = if (forestState.followSystemTheme) systemDark else (forestState.manualTheme == WallpaperTheme.DARK)

    ForestBackgroundContent(
        isDark = isDark,
        treeCount = forestState.treeCount,
        modifier = modifier
    )
}

@Composable
fun ForestBackgroundContent(
    isDark: Boolean,
    treeCount: Int = 0,
    modifier: Modifier = Modifier
) {
    // Smooth Crossfade animation between light and dark backgrounds
    val darkProgress by animateFloatAsState(
        targetValue = if (isDark) 1f else 0f,
        animationSpec = tween(1200, easing = LinearOutSlowInEasing),
        label = "darkProgress"
    )

    // Load High Resolution Open Meadow Backgrounds
    val lightBg = ImageBitmap.imageResource(id = R.drawable.open_meadow_light)
    val darkBg = ImageBitmap.imageResource(id = R.drawable.open_meadow_dark)

    // Load Photorealistic Tree PNG Assets (Light & Dark Variants)
    val tree1Light = ImageBitmap.imageResource(id = R.drawable.tree_1_light)
    val tree1Dark = ImageBitmap.imageResource(id = R.drawable.tree_1_dark)
    val tree2Light = ImageBitmap.imageResource(id = R.drawable.tree_2_light)
    val tree2Dark = ImageBitmap.imageResource(id = R.drawable.tree_2_dark)
    val tree3Light = ImageBitmap.imageResource(id = R.drawable.tree_3_light)
    val tree3Dark = ImageBitmap.imageResource(id = R.drawable.tree_3_dark)
    val tree4Light = ImageBitmap.imageResource(id = R.drawable.tree_4_light)
    val tree4Dark = ImageBitmap.imageResource(id = R.drawable.tree_4_dark)

    val treeBitmaps = remember(tree1Light, tree1Dark, tree2Light, tree2Dark, tree3Light, tree3Dark, tree4Light, tree4Dark) {
        ForestTreeBitmaps(
            tree1Light = tree1Light,
            tree1Dark = tree1Dark,
            tree2Light = tree2Light,
            tree2Dark = tree2Dark,
            tree3Light = tree3Light,
            tree3Dark = tree3Dark,
            tree4Light = tree4Light,
            tree4Dark = tree4Dark
        )
    }

    // Lifecycle-aware Animation Driver (Gentle Sway & Atmospheric Pollen & Fireflies)
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

    val currentAnimPhase = wavePhase.value

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val W = size.width
            val H = size.height

            // 1. Draw Base Pristine Open Meadow Background Artwork (Aspect-fill cropping, zero distortion or tiling)
            if (darkProgress <= 0.001f) {
                ForestTreeRenderer.drawCropBitmap(this, lightBg, W, H, 1f)
            } else if (darkProgress >= 0.999f) {
                ForestTreeRenderer.drawCropBitmap(this, darkBg, W, H, 1f)
            } else {
                ForestTreeRenderer.drawCropBitmap(this, lightBg, W, H, 1f - darkProgress)
                ForestTreeRenderer.drawCropBitmap(this, darkBg, W, H, darkProgress)
            }

            // 2. Dynamic Canvas Tree Sprites Stamped at Fixed Coordinate Slots (0 trees drawn when treeCount == 0)
            ForestTreeRenderer.drawDynamicForestTrees(
                drawScope = this,
                treeBitmaps = treeBitmaps,
                W = W,
                H = H,
                treeCount = treeCount,
                darkProgress = darkProgress,
                animPhase = currentAnimPhase
            )

            // 3. Ambient Atmospheric Particles (Sun pollen / Bioluminescent fireflies)
            ForestTreeRenderer.drawAtmosphericParticles(
                drawScope = this,
                W = W,
                H = H,
                darkProgress = darkProgress,
                animPhase = currentAnimPhase
            )

            // 4. Subtle UI Vignette at the bottom for crystal-clear readability
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0x60000000)),
                    startY = H * 0.78f,
                    endY = H
                ),
                size = Size(W, H)
            )
        }
    }
}
