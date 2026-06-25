package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.LocalAppThemeColors
import com.example.ui.theme.LocalIsDarkTheme

@Composable
fun ForestScaffold(
    forestViewModel: ForestViewModel = viewModel(),
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val forestState by forestViewModel.forestState.collectAsState()
    val isDay = forestState.isDayTime

    val surfaceColor by animateColorAsState(
        targetValue = if (isDay) Color(0xFFFFFFFF).copy(alpha = 0.82f) else Color(0xFF1C2128).copy(alpha = 0.82f),
        animationSpec = tween(durationMillis = 700),
        label = "surfaceColor"
    )
    val onSurfaceColor by animateColorAsState(
        targetValue = if (isDay) Color(0xFF1A1A2E) else Color(0xFFE6EDF3),
        animationSpec = tween(durationMillis = 700),
        label = "onSurfaceColor"
    )
    val secondaryTextColor by animateColorAsState(
        targetValue = if (isDay) Color(0xFF57606A) else Color(0xFF8B949E),
        animationSpec = tween(durationMillis = 700),
        label = "secondaryTextColor"
    )
    val accentColor by animateColorAsState(
        targetValue = Color(0xFF7C6AF7),
        animationSpec = tween(durationMillis = 700),
        label = "accentColor"
    )
    val inputBackgroundColor by animateColorAsState(
        targetValue = if (isDay) Color(0xFFF0F2F5).copy(alpha = 0.80f) else Color(0xFF0D1117).copy(alpha = 0.75f),
        animationSpec = tween(durationMillis = 700),
        label = "inputBackgroundColor"
    )
    val dividerColor by animateColorAsState(
        targetValue = if (isDay) Color(0xFFD0D7DE) else Color(0xFF30363D),
        animationSpec = tween(durationMillis = 700),
        label = "dividerColor"
    )
    val iconTintAnimate by animateColorAsState(
        targetValue = if (isDay) Color(0xFF1A1A2E) else Color(0xFFCDD9E5),
        animationSpec = tween(durationMillis = 700),
        label = "iconTintAnimate"
    )

    val themeColors = AppThemeColors(
        surface = surfaceColor,
        onSurface = onSurfaceColor,
        secondaryText = secondaryTextColor,
        accent = accentColor,
        inputBackground = inputBackgroundColor,
        divider = dividerColor,
        iconTint = iconTintAnimate
    )

    CompositionLocalProvider(
        LocalAppThemeColors provides themeColors,
        LocalIsDarkTheme provides !isDay
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Layer 1: forest — always behind everything
            ForestBackground(
                forestState = forestState,
                modifier    = Modifier.fillMaxSize()
            )
            // Layer 2: app content with transparent scaffold
            Scaffold(
                containerColor = Color.Transparent,
                contentColor   = if (isDay) Color(0xFFE0F0E8) else Color.White,
                bottomBar      = bottomBar
            ) { paddingValues ->
                content(paddingValues)
            }
        }
    }
}
