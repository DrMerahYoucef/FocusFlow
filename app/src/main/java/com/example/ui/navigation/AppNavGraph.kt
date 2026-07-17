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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import kotlinx.coroutines.launch
import com.example.ui.components.neumorphicShadow
import com.example.ui.screen.analytics.AnalyticsScreen
import com.example.ui.screen.analytics.AnalyticsViewModel
import com.example.ui.screen.exams.ExamsScreen
import com.example.ui.screen.exams.ExamsViewModel
import com.example.ui.screen.settings.SettingsScreen
import com.example.ui.screen.settings.SettingsViewModel
import androidx.compose.material.icons.filled.Public
import com.example.ui.screen.timer.TimerScreen
import com.example.ui.screen.timer.TimerViewModel
import com.example.ui.theme.NeumorphicColors
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Timer : Screen("timer", "Timer", Icons.Default.Timer)
    object Analytics : Screen("analytics", "Stats", Icons.Default.Analytics)
    object Radio : Screen("radio", "Radio", Icons.Default.Radio)
    object Community : Screen("community", "Islands", Icons.Default.Public)
    object Settings : Screen("settings", "Config", Icons.Default.Settings)
}

@Composable
fun MainPagerScreen(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    timerViewModel: TimerViewModel,
    analyticsViewModel: AnalyticsViewModel,
    examsViewModel: ExamsViewModel,
    items: List<Screen>,
    initialPage: Int = 0
) {
    val settingsState by settingsViewModel.state.collectAsState()
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { items.size })
    val coroutineScope = rememberCoroutineScope()

    com.example.ui.components.ForestScaffold(
        bottomBar = {
            NeumorphicBottomNavigationForPager(
                items = items,
                currentPage = pagerState.currentPage,
                onTabSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = settingsState.swipeToNavigate
        ) { page ->
            val screenModifier = Modifier.padding(padding)
            when (items[page]) {
                Screen.Timer -> {
                    TimerScreen(
                        viewModel = timerViewModel,
                        settingsViewModel = settingsViewModel,
                        modifier = screenModifier
                    )
                }
                Screen.Analytics -> {
                    AnalyticsScreen(
                        viewModel = analyticsViewModel,
                        modifier = screenModifier
                    )
                }
                Screen.Radio -> {
                    com.example.ui.screen.radio.RadioScreen(
                        navController = navController,
                        modifier = screenModifier
                    )
                }
                Screen.Community -> {
                    val themeColors = com.example.ui.theme.LocalAppThemeColors.current
                    var hasError by remember { mutableStateOf(false) }

                    val owner = androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current
                    val resolvedViewModel = remember(owner) {
                        try {
                            if (owner != null) {
                                androidx.lifecycle.ViewModelProvider(owner)[com.example.ui.screen.community.CommunityViewModel::class.java]
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("AppNavGraph", "CommunityViewModel initialization failed", e)
                            hasError = true
                            null
                        }
                    }

                    if (resolvedViewModel != null && !hasError) {
                        com.example.ui.screen.community.CommunityScreen(
                            navController = navController,
                            viewModel = resolvedViewModel,
                            bottomBar = {}
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Screen.Community.icon,
                                    contentDescription = "Islands Offline",
                                    tint = themeColors.secondaryText,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Islands Offline",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = themeColors.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Online Focus Islands and Leaderboards are currently unavailable. Please check your internet connection or try again later.",
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontSize = 14.sp,
                                    color = themeColors.secondaryText
                                )
                            }
                        }
                    }
                }
                Screen.Settings -> {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        navController = navController,
                        modifier = screenModifier
                    )
                }
            }
        }
    }
}

@Composable
fun NeumorphicBottomNavigationForPager(
    items: List<Screen>,
    currentPage: Int,
    onTabSelected: (Int) -> Unit
) {
    val themeColors = com.example.ui.theme.LocalAppThemeColors.current
    val isDark = com.example.ui.theme.LocalIsDarkTheme.current
    val shadowColor = if (isDark) Color(0x40000000) else Color(0x1F000000)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(RoundedCornerShape(24.dp))
            .background(themeColors.surface)
            .border(1.dp, themeColors.divider, RoundedCornerShape(24.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, screen ->
            val isSelected = currentPage == index
            val itemBackground = if (isSelected) {
                themeColors.accent.copy(alpha = 0.15f)
            } else {
                Color.Transparent
            }
            val itemBorder = if (isSelected) {
                themeColors.accent.copy(alpha = 0.3f)
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
                        onTabSelected(index)
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
                        tint = if (isSelected) themeColors.accent else themeColors.secondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = screen.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                        color = if (isSelected) themeColors.accent else themeColors.secondaryText
                    )
                }
            }
        }
    }
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
        Screen.Community,
        Screen.Settings
    )

    val timerViewModel: TimerViewModel = viewModel()
    val analyticsViewModel: AnalyticsViewModel = viewModel()
    val examsViewModel: ExamsViewModel = viewModel()

    val currentUser = try {
        Firebase.auth.currentUser
    } catch (e: Exception) {
        android.util.Log.e("AppNavGraph", "Firebase auth not initialized or available", e)
        null
    }
    val startDestination = if (currentUser != null) Screen.Timer.route else "auth"

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
            composable("auth") {
                com.example.ui.screen.auth.AuthScreen(
                    onAuthenticated = {
                        navController.navigate(Screen.Timer.route) {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Timer.route) {
                MainPagerScreen(
                    navController = navController,
                    settingsViewModel = settingsViewModel,
                    timerViewModel = timerViewModel,
                    analyticsViewModel = analyticsViewModel,
                    examsViewModel = examsViewModel,
                    items = items,
                    initialPage = 0
                )
            }
            composable(Screen.Analytics.route) {
                MainPagerScreen(
                    navController = navController,
                    settingsViewModel = settingsViewModel,
                    timerViewModel = timerViewModel,
                    analyticsViewModel = analyticsViewModel,
                    examsViewModel = examsViewModel,
                    items = items,
                    initialPage = 1
                )
            }
            composable(Screen.Radio.route) {
                MainPagerScreen(
                    navController = navController,
                    settingsViewModel = settingsViewModel,
                    timerViewModel = timerViewModel,
                    analyticsViewModel = analyticsViewModel,
                    examsViewModel = examsViewModel,
                    items = items,
                    initialPage = 2
                )
            }
            composable(Screen.Community.route) {
                MainPagerScreen(
                    navController = navController,
                    settingsViewModel = settingsViewModel,
                    timerViewModel = timerViewModel,
                    analyticsViewModel = analyticsViewModel,
                    examsViewModel = examsViewModel,
                    items = items,
                    initialPage = 3
                )
            }
            composable(Screen.Settings.route) {
                MainPagerScreen(
                    navController = navController,
                    settingsViewModel = settingsViewModel,
                    timerViewModel = timerViewModel,
                    analyticsViewModel = analyticsViewModel,
                    examsViewModel = examsViewModel,
                    items = items,
                    initialPage = 4
                )
            }
            composable("exams") {
                com.example.ui.components.ForestScaffold(
                    bottomBar = { NeumorphicBottomNavigation(navController = navController, items = items) }
                ) { padding ->
                    ExamsScreen(viewModel = examsViewModel, modifier = Modifier.padding(padding))
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
    val themeColors = com.example.ui.theme.LocalAppThemeColors.current
    val isDark = com.example.ui.theme.LocalIsDarkTheme.current

    val shadowColor = if (isDark) Color(0x40000000) else Color(0x1F000000)

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
            .background(themeColors.surface)
            .border(1.dp, themeColors.divider, RoundedCornerShape(24.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { screen ->
            val isSelected = currentRoute == screen.route
            val itemBackground = if (isSelected) {
                themeColors.accent.copy(alpha = 0.15f)
            } else {
                Color.Transparent
            }
            val itemBorder = if (isSelected) {
                themeColors.accent.copy(alpha = 0.3f)
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
                        tint = if (isSelected) themeColors.accent else themeColors.secondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = screen.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                        color = if (isSelected) themeColors.accent else themeColors.secondaryText
                    )
                }
            }
        }
    }
}
