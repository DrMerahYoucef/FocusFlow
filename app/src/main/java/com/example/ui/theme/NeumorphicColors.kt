package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

object NeumorphicColors {
    // Light palette
    val BackgroundLight     = Color(0xFFE0E5EC)   // soft grey-white
    val SurfaceLightLight   = Color(0xFFFFFFFF)   // highlight shadow
    val SurfaceDarkLight    = Color(0xFFA3B1C6)   // dark shadow
    val PrimaryLightVal     = Color(0xFF6C63FF)   // violet accent
    val PrimaryLightLight   = Color(0xFF9B94FF)
    val AccentLight         = Color(0xFFFF6584)   // coral
    val TextPrimaryLight    = Color(0xFF2D3142)
    val TextSecondaryLight  = Color(0xFF8A94A6)
    val SuccessLight        = Color(0xFF4CAF82)
    val WarningLight        = Color(0xFFFFB347)

    // Dark palette
    val BackgroundDark      = Color(0xFF1E222B)   // deep slate charcoal
    val SurfaceLightDark    = Color(0xFF2E333F)   // highlight shadow inside dark
    val SurfaceDarkDark     = Color(0xFF14161C)   // shadow shade inside dark
    val PrimaryDarkVal      = Color(0xFF8B84FF)   // soft glowing violet
    val PrimaryLightDark    = Color(0xFFA59FFF)
    val AccentDark          = Color(0xFFFF829C)   // soft glowing coral
    val TextPrimaryDark     = Color(0xFFECF0F3)   // light gray text
    val TextSecondaryDark   = Color(0xFF9EA4B0)   // medium muted gray text
    val SuccessDark         = Color(0xFF5CCB96)
    val WarningDark         = Color(0xFFFFC069)

    // Dynamic properties responding to custom LocalIsDarkTheme composition local
    val Background: Color
        @Composable
        get() = Color.Transparent

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
