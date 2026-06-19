package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.neumorphicShadow
import com.example.ui.screen.analytics.AnalyticsScreen
import com.example.ui.screen.analytics.AnalyticsViewModel
import com.example.ui.screen.exams.ExamsScreen
import com.example.ui.screen.exams.ExamsViewModel
import com.example.ui.screen.settings.SettingsScreen
import com.example.ui.screen.settings.SettingsViewModel
import com.example.ui.screen.timer.TimerScreen
import com.example.ui.screen.timer.TimerViewModel
import com.example.ui.theme.NeumorphicColors

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Timer : Screen("timer", "Timer", Icons.Default.Timer)
    object Analytics : Screen("analytics", "Stats", Icons.Default.Analytics)
    object Radio : Screen("radio", "Radio", Icons.Default.Radio)
    object Exams : Screen("exams", "Exams", Icons.Default.CalendarMonth)
    object Settings : Screen("settings", "Config", Icons.Default.Settings)
}

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val items = listOf(
        Screen.Timer,
        Screen.Analytics,
        Screen.Radio,
        Screen.Exams,
        Screen.Settings
    )

    val timerViewModel: TimerViewModel = viewModel()
    val analyticsViewModel: AnalyticsViewModel = viewModel()
    val examsViewModel: ExamsViewModel = viewModel()

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Timer.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Timer.route) {
                com.example.ui.components.ForestScaffold(
                    bottomBar = { NeumorphicBottomNavigation(navController = navController, items = items) }
                ) { padding ->
                    TimerScreen(viewModel = timerViewModel, modifier = Modifier.padding(padding))
                }
            }
            composable(Screen.Analytics.route) {
                com.example.ui.components.ForestScaffold(
                    bottomBar = { NeumorphicBottomNavigation(navController = navController, items = items) }
                ) { padding ->
                    AnalyticsScreen(viewModel = analyticsViewModel, modifier = Modifier.padding(padding))
                }
            }
            composable(Screen.Radio.route) {
                com.example.ui.components.ForestScaffold(
                    bottomBar = { NeumorphicBottomNavigation(navController = navController, items = items) }
                ) { padding ->
                    com.example.ui.screen.radio.RadioScreen(navController = navController, modifier = Modifier.padding(padding))
                }
            }
            composable(Screen.Exams.route) {
                com.example.ui.components.ForestScaffold(
                    bottomBar = { NeumorphicBottomNavigation(navController = navController, items = items) }
                ) { padding ->
                    ExamsScreen(viewModel = examsViewModel, modifier = Modifier.padding(padding))
                }
            }
            composable(Screen.Settings.route) {
                com.example.ui.components.ForestScaffold(
                    bottomBar = { NeumorphicBottomNavigation(navController = navController, items = items) }
                ) { padding ->
                    SettingsScreen(viewModel = settingsViewModel, navController = navController, modifier = Modifier.padding(padding))
                }
            }
            composable("app_blocker") {
                com.example.ui.components.ForestScaffold(
                    bottomBar = { NeumorphicBottomNavigation(navController = navController, items = items) }
                ) { padding ->
                    com.example.ui.screen.appblocker.AppBlockerScreen(navController = navController, modifier = Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
fun NeumorphicBottomNavigation(
    navController: NavHostController,
    items: List<Screen>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isDark = com.example.ui.theme.LocalIsDarkTheme.current

    val glassColor  = if (isDark) Color(0xCC1E222B) else Color(0xDDE0E5EC)
    val borderColor = if (isDark) Color(0x26FFFFFF) else Color(0x1F000000)
    val shadowColor = if (isDark) Color(0x40000000) else Color(0x22000000)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars) // Safeguard notch / bottom nav gesture bar overlap!
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(RoundedCornerShape(24.dp))
            .background(glassColor)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { screen ->
            val isSelected = currentRoute == screen.route
            val itemBackground = if (isSelected) {
                if (isDark) Color(0x33FFFFFF) else Color(0x4DFFFFFF)
            } else {
                Color.Transparent
            }
            val itemBorder = if (isSelected) {
                if (isDark) Color(0x4DFFFFFF) else Color(0x80FFFFFF)
            } else {
                Color.Transparent
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(itemBackground)
                    .border(1.dp, itemBorder, RoundedCornerShape(16.dp))
                    .clickable {
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title,
                        tint = if (isSelected) NeumorphicColors.Primary else NeumorphicColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = screen.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                        color = if (isSelected) NeumorphicColors.TextPrimary else NeumorphicColors.TextSecondary
                    )
                }
            }
        }
    }
}
