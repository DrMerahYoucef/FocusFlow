package com.example.ui.theme

enum class WallpaperTheme {
    LIGHT,
    DARK
}

data class WallpaperThemeState(
    val followSystemTheme: Boolean = true,
    val manualTheme: WallpaperTheme = WallpaperTheme.LIGHT,
    val activeTheme: WallpaperTheme = WallpaperTheme.LIGHT
)
