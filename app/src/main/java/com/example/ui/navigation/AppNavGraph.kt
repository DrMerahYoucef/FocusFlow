package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
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
        Screen.Exams,
        Screen.Settings
    )

    val timerViewModel: TimerViewModel = viewModel()
    val analyticsViewModel: AnalyticsViewModel = viewModel()
    val examsViewModel: ExamsViewModel = viewModel()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NeumorphicColors.Background,
        bottomBar = {
            NeumorphicBottomNavigation(
                navController = navController,
                items = items
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Timer.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Timer.route) {
                TimerScreen(viewModel = timerViewModel)
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen(viewModel = analyticsViewModel)
            }
            composable(Screen.Exams.route) {
                ExamsScreen(viewModel = examsViewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = settingsViewModel, navController = navController)
            }
            composable("app_blocker") {
                com.example.ui.screen.appblocker.AppBlockerScreen(navController = navController)
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars) // Safeguard notch / bottom nav gesture bar overlap!
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .neumorphicShadow(cornerRadius = 24.dp, elevation = 4.dp, isPressed = false)
            .clip(RoundedCornerShape(24.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { screen ->
            val isSelected = currentRoute == screen.route

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .neumorphicShadow(
                        cornerRadius = 16.dp,
                        elevation = if (isSelected) 2.dp else 4.dp,
                        isPressed = isSelected
                    )
                    .clip(RoundedCornerShape(16.dp))
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
