package com.example.ui.components

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusFlowApplication
import com.example.R
import com.example.ui.theme.WallpaperTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ForestState(
    val completedSessions: Int = 0,
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
                    _forestState.update { it.copy(completedSessions = count) }
                    sharedPrefs.edit().putInt("last_synced_completed_sessions", count).apply()
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
        val savedCount = sharedPrefs.getInt(
            "last_synced_completed_sessions",
            sharedPrefs.getInt("last_synced_tree_count", 0)
        )
        return ForestState(
            completedSessions = savedCount,
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

        WallpaperHelper.setBuildingWallpaper(
            context = app,
            theme = theme,
            setHomeScreen = setHome,
            setLockScreen = setLock,
            completedSessions = _forestState.value.completedSessions
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
        completedSessions = forestState.completedSessions,
        modifier = modifier
    )
}

@Composable
fun ForestBackgroundContent(
    isDark: Boolean,
    completedSessions: Int = 0,
    modifier: Modifier = Modifier
) {
    val building = ImageBitmap.imageResource(R.drawable.building_background)

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            BuildingBackgroundRenderer.run {
                drawBuilding(building, completedSessions, if (isDark) 1f else 0f)
            }
        }
    }
}
