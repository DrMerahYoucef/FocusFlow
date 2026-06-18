package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = NeumorphicColors.Primary,
    secondary = NeumorphicColors.Success,
    tertiary = NeumorphicColors.Accent,
    background = NeumorphicColors.Background,
    surface = NeumorphicColors.Background
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NeumorphicColors.Primary,
    secondary = NeumorphicColors.Success,
    tertiary = NeumorphicColors.Accent,
    background = NeumorphicColors.Background,
    surface = NeumorphicColors.Background,
    onPrimary = NeumorphicColors.SurfaceLight,
    onBackground = NeumorphicColors.TextPrimary,
    onSurface = NeumorphicColors.TextPrimary
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve our beautiful custom neumorphic palette branding
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
