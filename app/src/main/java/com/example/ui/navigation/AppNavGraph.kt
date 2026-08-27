package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
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

import androidx.compose.material.icons.filled.AutoAwesome
import com.example.ui.screen.revisions.CaptureScreen
import com.example.ui.screen.revisions.CropEditorScreen
import com.example.ui.screen.revisions.RevisionDeckDetailScreen
import com.example.ui.screen.revisions.RevisionNoteDetailScreen
import com.example.ui.screen.revisions.RevisionSessionScreen
import com.example.ui.screen.revisions.RevisionsHomeScreen
import com.example.ui.screen.revisions.RevisionsViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Timer : Screen("timer", "Timer", Icons.Default.Timer)
    object Revisions : Screen("revisions", "Revisions", Icons.Default.AutoAwesome)
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
    revisionsViewModel: RevisionsViewModel = viewModel(),
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
                        pagerState.scrollToPage(index)
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
                        onNavigateToBatterySaver = { navController.navigate("battery_saver") },
                        onNavigateToRadio = { navController.navigate("radio") },
                        modifier = screenModifier
                    )
                }
                Screen.Revisions -> {
                    RevisionsHomeScreen(
                        viewModel = revisionsViewModel,
                        onAddClick = { navController.navigate("revisions/capture") },
                        onImageCaptured = { imagePath ->
                            val encoded = java.net.URLEncoder.encode(imagePath, "UTF-8")
                            navController.navigate("revisions/crop?path=$encoded")
                        },
                        onDeckClick = { deckId -> navController.navigate("revisions/deck/$deckId") },
                        onNoteClick = { noteId -> navController.navigate("revisions/note/$noteId") },
                        onStartSessionClick = { deckId -> navController.navigate("revisions/session?deckId=$deckId") },
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

class UpwardBumpBarShape(
    private val cornerRadius: Dp = 28.dp,
    private val bumpWidth: Dp = 96.dp,
    private val bumpHeight: Dp = 22.dp,
    private val topMargin: Dp = 2.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cr = with(density) { cornerRadius.toPx() }
        val bw = with(density) { bumpWidth.toPx() }
        val bh = with(density) { bumpHeight.toPx() }
        val yTop = with(density) { topMargin.toPx() }
        val w = size.width
        val h = size.height
        val cx = w / 2f

        val flatY = bh + yTop
        val wHalf = bw / 2f
        val c1x = cx - wHalf * 0.52f
        val c2x = cx - wHalf * 0.48f
        val c3x = cx + wHalf * 0.48f
        val c4x = cx + wHalf * 0.52f

        val path = Path().apply {
            moveTo(cr, flatY)
            lineTo(cx - wHalf, flatY)
            cubicTo(
                c1x, flatY,
                c2x, yTop,
                cx, yTop
            )
            cubicTo(
                c3x, yTop,
                c4x, flatY,
                cx + wHalf, flatY
            )
            lineTo(w - cr, flatY)
            arcTo(
                rect = Rect(w - 2 * cr, flatY, w, flatY + 2 * cr),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(w, h - cr)
            arcTo(
                rect = Rect(w - 2 * cr, h - 2 * cr, w, h),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(cr, h)
            arcTo(
                rect = Rect(0f, h - 2 * cr, 2 * cr, h),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(0f, flatY + cr)
            arcTo(
                rect = Rect(0f, flatY, 2 * cr, flatY + 2 * cr),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            close()
        }
        return Outline.Generic(path)
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

    val activeColor = themeColors.accent
    val inactiveColor = if (isDark) Color(0x99ECECF0) else themeColors.secondaryText
    val barBg = if (isDark) Color(0xFA1A1D28) else themeColors.surface
    val centerCircleBg = if (isDark) Color(0xFF1E222F) else themeColors.surface

    val bumpHeight = 24.dp
    val barShape = remember { UpwardBumpBarShape(cornerRadius = 28.dp, bumpWidth = 96.dp, bumpHeight = bumpHeight) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. SINGLE CONTINUOUS BENT BAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = barShape,
                    ambientColor = if (isDark) Color(0x40000000) else Color(0x18000000),
                    spotColor = if (isDark) Color(0x60000000) else Color(0x20000000)
                )
                .clip(barShape)
                .background(barBg)
                .border(
                    width = 1.dp,
                    color = themeColors.divider.copy(alpha = if (isDark) 0.5f else 0.8f),
                    shape = barShape
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item 0: Revisions
                val isRev = currentPage == 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(0) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .background(if (isRev) activeColor.copy(alpha = 0.14f) else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Screen.Revisions.icon,
                            contentDescription = Screen.Revisions.title,
                            tint = if (isRev) activeColor else inactiveColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Screen.Revisions.title,
                            fontSize = 11.sp,
                            fontWeight = if (isRev) FontWeight.Bold else FontWeight.Medium,
                            color = if (isRev) activeColor else inactiveColor
                        )
                    }
                }

                // Item 1: Stats
                val isStats = currentPage == 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(1) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .background(if (isStats) activeColor.copy(alpha = 0.14f) else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Screen.Analytics.icon,
                            contentDescription = Screen.Analytics.title,
                            tint = if (isStats) activeColor else inactiveColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Screen.Analytics.title,
                            fontSize = 11.sp,
                            fontWeight = if (isStats) FontWeight.Bold else FontWeight.Medium,
                            color = if (isStats) activeColor else inactiveColor
                        )
                    }
                }

                // CENTER SPACER FOR FLOATING TIMER CIRCLE
                Spacer(modifier = Modifier.width(76.dp))

                // Item 3: Islands
                val isIslands = currentPage == 3
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(3) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .background(if (isIslands) activeColor.copy(alpha = 0.14f) else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Screen.Community.icon,
                            contentDescription = Screen.Community.title,
                            tint = if (isIslands) activeColor else inactiveColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Screen.Community.title,
                            fontSize = 11.sp,
                            fontWeight = if (isIslands) FontWeight.Bold else FontWeight.Medium,
                            color = if (isIslands) activeColor else inactiveColor
                        )
                    }
                }

                // Item 4: Config
                val isConfig = currentPage == 4
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(4) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .background(if (isConfig) activeColor.copy(alpha = 0.14f) else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Screen.Settings.icon,
                            contentDescription = Screen.Settings.title,
                            tint = if (isConfig) activeColor else inactiveColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Screen.Settings.title,
                            fontSize = 11.sp,
                            fontWeight = if (isConfig) FontWeight.Bold else FontWeight.Medium,
                            color = if (isConfig) activeColor else inactiveColor
                        )
                    }
                }
            }
        }

        // 2. HERO FLOATING CENTER CIRCLE (Timer - Index 2)
        val isTimer = currentPage == 2
        val circleShape = CircleShape

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 6.dp)
                .size(68.dp)
                .then(
                    if (isTimer) {
                        Modifier
                            .shadow(10.dp, circleShape, ambientColor = activeColor.copy(alpha = 0.25f), spotColor = activeColor.copy(alpha = 0.25f))
                            .border(2.dp, activeColor, circleShape)
                    } else {
                        Modifier
                            .shadow(6.dp, circleShape)
                            .border(1.dp, themeColors.divider, circleShape)
                    }
                )
                .clip(circleShape)
                .background(if (isTimer) activeColor.copy(alpha = 0.12f) else centerCircleBg)
                .clickable { onTabSelected(2) },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Screen.Timer.icon,
                    contentDescription = Screen.Timer.title,
                    tint = if (isTimer) activeColor else inactiveColor,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = Screen.Timer.title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isTimer) activeColor else inactiveColor
                )
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
        Screen.Revisions,
        Screen.Analytics,
        Screen.Timer,
        Screen.Community,
        Screen.Settings
    )

    val timerViewModel: TimerViewModel = viewModel()
    val analyticsViewModel: AnalyticsViewModel = viewModel()
    val examsViewModel: ExamsViewModel = viewModel()
    val revisionsViewModel: RevisionsViewModel = viewModel()

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
            composable(Screen.Revisions.route) {
                MainPagerScreen(
                    navController = navController,
                    settingsViewModel = settingsViewModel,
                    timerViewModel = timerViewModel,
                    analyticsViewModel = analyticsViewModel,
                    examsViewModel = examsViewModel,
                    revisionsViewModel = revisionsViewModel,
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
                    revisionsViewModel = revisionsViewModel,
                    items = items,
                    initialPage = 1
                )
            }
            composable(Screen.Timer.route) {
                MainPagerScreen(
                    navController = navController,
                    settingsViewModel = settingsViewModel,
                    timerViewModel = timerViewModel,
                    analyticsViewModel = analyticsViewModel,
                    examsViewModel = examsViewModel,
                    revisionsViewModel = revisionsViewModel,
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
                    revisionsViewModel = revisionsViewModel,
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
                    revisionsViewModel = revisionsViewModel,
                    items = items,
                    initialPage = 4
                )
            }
            composable("radio") {
                com.example.ui.components.ForestScaffold {
                    com.example.ui.screen.radio.RadioScreen(
                        navController = navController
                    )
                }
            }
            composable("revisions/capture") {
                CaptureScreen(
                    viewModel = revisionsViewModel,
                    onImageCaptured = { path ->
                        val encoded = java.net.URLEncoder.encode(path, "UTF-8")
                        navController.navigate("revisions/crop?path=$encoded") {
                            popUpTo("revisions/capture") { inclusive = true }
                        }
                    },
                    onCardCreated = {
                        navController.navigate(Screen.Revisions.route) {
                            popUpTo(Screen.Revisions.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "revisions/crop?path={path}",
                arguments = listOf(
                    androidx.navigation.navArgument("path") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val pathArg = backStackEntry.arguments?.getString("path") ?: ""
                val decodedPath = java.net.URLDecoder.decode(pathArg, "UTF-8")
                CropEditorScreen(
                    imagePath = decodedPath,
                    viewModel = revisionsViewModel,
                    onCropConfirmed = {
                        navController.navigate(Screen.Revisions.route) {
                            popUpTo(Screen.Revisions.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "revisions/session?deckId={deckId}",
                arguments = listOf(
                    androidx.navigation.navArgument("deckId") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "default_deck"
                    }
                )
            ) { backStackEntry ->
                val deckIdArg = backStackEntry.arguments?.getString("deckId") ?: "default_deck"
                com.example.ui.components.ForestScaffold {
                    RevisionSessionScreen(
                        deckId = deckIdArg,
                        viewModel = revisionsViewModel,
                        onFinishSession = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(
                route = "revisions/deck/{deckId}",
                arguments = listOf(
                    androidx.navigation.navArgument("deckId") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val deckIdArg = backStackEntry.arguments?.getString("deckId") ?: "default_deck"
                com.example.ui.components.ForestScaffold {
                    RevisionDeckDetailScreen(
                        deckId = deckIdArg,
                        viewModel = revisionsViewModel,
                        onNoteClick = { noteId -> navController.navigate("revisions/note/$noteId") },
                        onStartReview = { deckId -> navController.navigate("revisions/session?deckId=$deckId") },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(
                route = "revisions/note/{noteId}",
                arguments = listOf(
                    androidx.navigation.navArgument("noteId") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val noteIdArg = backStackEntry.arguments?.getString("noteId") ?: ""
                com.example.ui.components.ForestScaffold {
                    RevisionNoteDetailScreen(
                        noteId = noteIdArg,
                        viewModel = revisionsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
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
            composable("battery_saver") {
                com.example.ui.screen.timer.BatterySaverScreen(
                    viewModel = timerViewModel,
                    settingsViewModel = settingsViewModel,
                    onDismiss = {
                        navController.popBackStack()
                    }
                )
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

    val currentIndex = when (currentRoute) {
        Screen.Revisions.route -> 0
        Screen.Analytics.route -> 1
        Screen.Timer.route -> 2
        Screen.Community.route -> 3
        Screen.Settings.route -> 4
        else -> 2
    }

    NeumorphicBottomNavigationForPager(
        items = items,
        currentPage = currentIndex,
        onTabSelected = { index ->
            val targetScreen = when (index) {
                0 -> Screen.Revisions
                1 -> Screen.Analytics
                2 -> Screen.Timer
                3 -> Screen.Community
                4 -> Screen.Settings
                else -> Screen.Timer
            }
            if (currentRoute != targetScreen.route) {
                navController.navigate(targetScreen.route) {
                    popUpTo(navController.graph.id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    )
}
