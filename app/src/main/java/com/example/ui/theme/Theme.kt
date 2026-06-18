package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF8B84FF),      // Primary
    secondary = Color(0xFF5CCB96),    // Success
    tertiary = Color(0xFFFF829C),     // Accent
    background = Color(0xFF1E222B),
    surface = Color(0xFF1E222B),
    onPrimary = Color(0xFF2E333F),    // SurfaceLight
    onBackground = Color(0xFFECF0F3), // TextPrimary
    onSurface = Color(0xFFECF0F3)     // TextPrimary
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF6C63FF),
    secondary = Color(0xFF4CAF82),
    tertiary = Color(0xFFFF6584),
    background = Color(0xFFE0E5EC),
    surface = Color(0xFFE0E5EC),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF2D3142),
    onSurface = Color(0xFF2D3142)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve our beautiful custom neumorphic palette branding
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
