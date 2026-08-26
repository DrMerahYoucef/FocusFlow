package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

object NeumorphicColors {
    // Light palette
    val BackgroundLight     = Color(0xFFFAFAFD)   // clean white/off-white background
    val SurfaceLightLight   = Color(0xFFFFFFFF)   // highlight shadow
    val SurfaceDarkLight    = Color(0xFFE2E7EE)   // soft subtle shadow
    val PrimaryLightVal     = Color(0xFF6C63FF)   // violet accent
    val PrimaryLightLight   = Color(0xFF9B94FF)
    val AccentLight         = Color(0xFFFF6584)   // coral
    val TextPrimaryLight    = Color(0xFF1B1E2B) // High contrast deep charcoal
    val TextSecondaryLight  = Color(0xFF525D73) // High contrast slate gray

    val SuccessLight        = Color(0xFF4CAF82)
    val WarningLight        = Color(0xFFFFB347)

    // Dark palette
    val BackgroundDark      = Color(0xFF1E222B)   // deep slate charcoal
    val SurfaceLightDark    = Color(0xFF2E333F)   // highlight shadow inside dark
    val SurfaceDarkDark     = Color(0xFF14161C)   // shadow shade inside dark
    val PrimaryDarkVal      = Color(0xFF8B84FF)   // soft glowing violet
    val PrimaryLightDark    = Color(0xFFA59FFF)
    val AccentDark          = Color(0xFFFF829C)   // soft glowing coral
    val TextPrimaryDark     = Color(0xFFFFFFFF)   // Pure white for highest legibility
    val TextSecondaryDark   = Color(0xFFCCD2DE)   // Clean bright silver-gray text
    val SuccessDark         = Color(0xFF5CCB96)
    val WarningDark         = Color(0xFFFFC069)

    // Dynamic properties responding to custom LocalIsDarkTheme composition local
    val Background: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) BackgroundDark else BackgroundLight

    val DialogBackground: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) BackgroundDark else BackgroundLight

    val SurfaceLight: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) SurfaceLightDark else SurfaceLightLight

    val SurfaceDark: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) SurfaceDarkDark else SurfaceDarkLight

    val Primary: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) PrimaryDarkVal else PrimaryLightVal

    val PrimaryLight: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) PrimaryLightDark else PrimaryLightLight

    val Accent: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) AccentDark else AccentLight

    val TextPrimary: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) TextPrimaryDark else TextPrimaryLight

    val TextSecondary: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) TextSecondaryDark else TextSecondaryLight

    val Success: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) SuccessDark else SuccessLight

    val Warning: Color
        @Composable
        get() = if (LocalIsDarkTheme.current) WarningDark else WarningLight
}

object RevisionHeadingColors {
    @Composable
    fun getHeadingColor(level: Int, isDark: Boolean = LocalIsDarkTheme.current): Color {
        return when (level) {
            1 -> if (isDark) Color(0xFF60A5FA) else Color(0xFF1565C0) // Royal Blue / Sky Blue
            2 -> if (isDark) Color(0xFF34D399) else Color(0xFF00796B) // Mint Teal / Deep Teal
            3 -> if (isDark) Color(0xFFA78BFA) else Color(0xFF7B1FA2) // Lavender / Purple
            4 -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706) // Gold / Amber Orange
            5 -> if (isDark) Color(0xFFFB7185) else Color(0xFFC2185B) // Rose / Berry
            else -> if (isDark) Color(0xFF38BDF8) else Color(0xFF00838F) // Cyan / Slate Blue
        }
    }

    @Composable
    fun getHeadingBgColor(level: Int, isDark: Boolean = LocalIsDarkTheme.current): Color {
        val color = getHeadingColor(level, isDark)
        return color.copy(alpha = if (isDark) 0.16f else 0.08f)
    }
}
