package com.example.ui.screen.community

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.screen.radio.PlayingWaveIndicator
import com.example.ui.theme.LocalAppThemeColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CommunityScreen(
    navController: NavController,
    viewModel: CommunityViewModel = viewModel(),
    bottomBar: @Composable () -> Unit
) {
    val friends by viewModel.friends.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()
    val pendingReqs by viewModel.pendingRequests.collectAsState()
    val sentReqs by viewModel.sentRequests.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val myProfile by viewModel.myProfile.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    val themeColors = LocalAppThemeColors.current

    com.example.ui.components.ForestScaffold(
        bottomBar = bottomBar
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = themeColors.accent,
                divider = { HorizontalDivider(color = themeColors.divider) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "🏝️ Island Map",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) themeColors.accent else themeColors.secondaryText
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "🏆 Leaderboard",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) themeColors.accent else themeColors.secondaryText
                        )
                    }
                )
            }

            when (selectedTab) {
                0 -> IslandMapTab(
                    friends = friends,
                    pendingReqs = pendingReqs,
                    sentReqs = sentReqs,
                    searchResults = searchResults,
                    searchQuery = searchQuery,
                    myTreeCount = myProfile?.treeCount ?: 0,
                    myUsername = myProfile?.username ?: "YOU",
                    onSearch = {
                        searchQuery = it
                        viewModel.searchUsers(it)
                    },
                    onSendRequest = { viewModel.sendFriendRequest(it) },
                    onAccept = { viewModel.acceptRequest(it) },
                    onDecline = { viewModel.declineRequest(it) },
                    onRemove = { viewModel.removeFriend(it) }
                )
                1 -> LeaderboardTab(
                    leaderboard = leaderboard,
                    currentUid = viewModel.currentUid,
                    friends = friends,
                    pendingReqs = pendingReqs,
                    sentReqs = sentReqs,
                    onSendRequest = { entry ->
                        viewModel.sendFriendRequestById(entry.uid, entry.username)
                    },
                    onAccept = viewModel::acceptRequest,
                    onDecline = viewModel::declineRequest,
                    onCancel = viewModel::cancelRequest
                )
            }
        }
    }
}

@Composable
fun IslandMapTab(
    friends: List<UserProfile>,
    pendingReqs: List<FriendRequest>,
    sentReqs: List<FriendRequest>,
    searchResults: List<UserProfile>,
    searchQuery: String,
    myTreeCount: Int,
    myUsername: String,
    onSearch: (String) -> Unit,
    onSendRequest: (UserProfile) -> Unit,
    onAccept: (FriendRequest) -> Unit,
    onDecline: (FriendRequest) -> Unit,
    onRemove: (UserProfile) -> Unit
) {
    var selectedFriend by remember { mutableStateOf<UserProfile?>(null) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showSearchPanel by remember { mutableStateOf(false) }
    val themeColors = LocalAppThemeColors.current

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3.0f)
        offset += panChange
    }

    val isDark = com.example.ui.theme.LocalIsDarkTheme.current
    val backgroundColors = if (isDark) {
        listOf(Color(0xFF111E36), Color(0xFF040B18))
    } else {
        listOf(Color(0xFF4DB8C8), Color(0xFF1A7A8F))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = backgroundColors,
                    center = Offset.Unspecified,
                    radius = 2200f
                )
            )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .transformable(transformState)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .pointerInput(friends) {
                    detectTapGestures { tap ->
                        friends.forEachIndexed { i, friend ->
                            val pos = islandPosition(i, friends.size, androidx.compose.ui.geometry.Size(size.width.toFloat(), size.height.toFloat()))
                            if ((tap - pos).getDistance() < 90f) {
                                selectedFriend = friend
                            }
                        }
                    }
                }
        ) {
            // Celestial body (Sun in the day, Moon at night) in the top-right corner
            val celestialCenter = Offset(size.width - 150f, 150f)
            if (isDark) {
                // Moon glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x558AB4F8), Color.Transparent),
                        center = celestialCenter,
                        radius = 120f
                    ),
                    radius = 120f,
                    center = celestialCenter
                )
                // Moon body
                drawCircle(
                    color = Color(0xFFF1F5F9),
                    radius = 35f,
                    center = celestialCenter
                )
                // Moon crescent shadow overlay
                drawCircle(
                    color = Color(0xFF111E36),
                    radius = 30f,
                    center = celestialCenter - Offset(12f, 12f)
                )
            } else {
                // Sun glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x66FFFBE2), Color.Transparent),
                        center = celestialCenter,
                        radius = 160f
                    ),
                    radius = 160f,
                    center = celestialCenter
                )
                // Sun body
                drawCircle(
                    color = Color(0xFFFBBF24),
                    radius = 45f,
                    center = celestialCenter
                )
            }

            // Wave ripples
            repeat(5) { i ->
                drawCircle(
                    color = if (isDark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.15f),
                    radius = 120f + i * 80f,
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(3f)
                )
            }

            // Central personal island
            drawIsland(
                center = Offset(size.width / 2f, size.height / 2f),
                radius = 110f,
                treeCount = myTreeCount,
                label = ((myUsername ?: "YOU").uppercase()) + "'S COVE",
                isSelected = false,
                isDark = isDark
            )

            // Outer Orbit Friends' Islands
            friends.forEachIndexed { i, friend ->
                val fName = friend.username ?: "FRIEND"
                drawIsland(
                    center = islandPosition(i, friends.size, size),
                    radius = 75f,
                    treeCount = friend.treeCount,
                    label = fName.uppercase(),
                    isSelected = selectedFriend?.uid == friend.uid,
                    isDark = isDark
                )
            }
        }

        // Selected Friend Profile Card (Frosted Glass Overlay overlaying content)
        selectedFriend?.let { friend ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🏝️ ${friend.username}'s Isle",
                                style = MaterialTheme.typography.titleMedium,
                                color = themeColors.onSurface,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { selectedFriend = null }) {
                                Icon(Icons.Default.Close, null, tint = themeColors.onSurface)
                            }
                        }

                        // Stats Grid (Row of Pillars)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LeaderboardStatPill("🌲", "${friend.treeCount}", "Trees")
                            LeaderboardStatPill("⏱️", "${friend.totalMinutes}", "Time")
                            LeaderboardStatPill("⭐", "${friend.points}", "Points")
                            LeaderboardStatPill("🔥", "${friend.currentStreak}", "Streak")
                        }

                        // Audio live indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            if (friend.currentRadio.isNotEmpty()) {
                                PlayingWaveIndicator(color = Color(0xFF6C63FF))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Currently Radio Streaming", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                    Text(friend.currentRadio, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            } else {
                                Text("💤 Player is currently focus-studying or offline", color = themeColors.secondaryText, fontSize = 11.sp)
                            }
                        }

                        GlassButton(
                            label = "Remove Friend",
                            icon = Icons.Default.Delete,
                            onClick = {
                                onRemove(friend)
                                selectedFriend = null
                            },
                            accentColor = Color(0xFFFF6584),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Add Friends Floating Action Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 24.dp)
        ) {
            FloatingActionButton(
                onClick = { showSearchPanel = true },
                containerColor = themeColors.accent,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                BadgedBox(
                    badge = {
                        if (pendingReqs.isNotEmpty()) {
                            Badge(containerColor = Color.Red, contentColor = Color.White) {
                                Text(pendingReqs.size.toString())
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Friends")
                }
            }
        }

        // Glass Search overlay panel
        if (showSearchPanel) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showSearchPanel = false }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Search & Friends",
                                style = MaterialTheme.typography.titleLarge,
                                color = themeColors.onSurface,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { showSearchPanel = false }) {
                                Icon(Icons.Default.Close, null, tint = themeColors.onSurface)
                            }
                        }

                        // Search Input
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearch,
                            placeholder = { Text("Search by email or username...", color = themeColors.secondaryText) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = themeColors.onSurface) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeColors.accent,
                                unfocusedBorderColor = themeColors.divider,
                                focusedTextColor = themeColors.onSurface,
                                unfocusedTextColor = themeColors.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Search Results List
                        if (searchResults.isNotEmpty()) {
                            Text("USERS FOUND", color = themeColors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 140.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(searchResults) { _, user ->
                                    val isAdded = friends.any { it.uid == user.uid } || sentReqs.any { it.toUid == user.uid }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            "👤 ${user.username}",
                                            color = themeColors.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isAdded) {
                                            Text(
                                                "Sent",
                                                color = themeColors.secondaryText,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                        } else {
                                            IconButton(
                                                onClick = { onSendRequest(user) }
                                            ) {
                                                Icon(Icons.Default.PersonAdd, "Invite", tint = themeColors.accent)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Active Pending Requests List
                        if (pendingReqs.isNotEmpty()) {
                            Text("PENDING INVITATIONS", color = themeColors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 140.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(pendingReqs) { _, req ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            req.fromName,
                                            color = themeColors.onSurface,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { onAccept(req) }) {
                                            Icon(Icons.Default.Check, "Accept", tint = Color(0xFF4CAF82))
                                        }
                                        IconButton(onClick = { onDecline(req) }) {
                                            Icon(Icons.Default.Close, "Decline", tint = Color(0xFFFF6584))
                                        }
                                    }
                                }
                            }
                        }

                        // Sent pending requests tracker
                        if (sentReqs.isNotEmpty()) {
                            Text("SENT REQUESTS", color = themeColors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 80.dp)
                            ) {
                                itemsIndexed(sentReqs) { _, req ->
                                    Text(
                                        text = "→ Outgoing request sent (pending)",
                                        color = themeColors.secondaryText,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun islandPosition(index: Int, total: Int, size: Size): Offset {
    val totalCount = if (total == 0) 1 else total
    val angle = (2 * Math.PI / totalCount * index) - Math.PI / 2
    val radius = minOf(size.width, size.height) * 0.35f
    return Offset(
        x = size.width / 2f + (cos(angle) * radius).toFloat(),
        y = size.height / 2f + (sin(angle) * radius).toFloat()
    )
}

private fun DrawScope.drawIsland(
    center: Offset,
    radius: Float,
    treeCount: Int,
    label: String?,
    isSelected: Boolean,
    isDark: Boolean = false
) {
    val sandColor = if (isDark) Color(0xFF6B5D39) else Color(0xFFD4A853)
    val grassColor = if (isDark) Color(0xFF1F4D2B) else Color(0xFF3D7A4A)
    val trunkColor = if (isDark) Color(0xFF132A0D) else Color(0xFF2D5A1B)
    val leafColor = if (isDark) Color(0xFF235C2B) else Color(0xFF4CAF50)

    // Drop shadow
    drawCircle(
        color = Color(0x33000000),
        radius = radius + 10f,
        center = center + Offset(6f, 6f)
    )
    // Sand coast
    drawCircle(
        color = sandColor,
        radius = radius,
        center = center
    )
    // Grassy core of island
    drawCircle(
        color = grassColor,
        radius = radius * 0.82f,
        center = center - Offset(0f, radius * 0.08f)
    )

    // Layout little decorative trees represent count
    val treesToShow = treeCount.coerceAtMost(16)
    repeat(treesToShow) { i ->
        val angle = 2 * Math.PI / treesToShow * i
        val dist = radius * 0.45f * (0.35f + (i % 3) * 0.22f)
        val treePos = center + Offset(
            x = (cos(angle) * dist).toFloat(),
            y = (sin(angle) * dist).toFloat() - radius * 0.08f
        )
        // Draw miniature layered trees
        drawCircle(trunkColor, 7f, treePos)
        drawCircle(leafColor, 10f, treePos - Offset(0f, 9f))
    }

    // Border highlight if selected
    if (isSelected) {
        drawCircle(
            color = Color(0xFF7C6AF7),
            radius = radius + 6f,
            center = center,
            style = Stroke(4f)
        )
    }

    // Canvas Label Text drawing
    drawContext.canvas.nativeCanvas.drawText(
        label ?: "",
        center.x,
        center.y + radius + 22f,
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setShadowLayer(4f, 1f, 1f, android.graphics.Color.BLACK)
        }
    )
}

@Composable
fun RankBadge(rank: Int, modifier: Modifier = Modifier) {
    val themeColors = LocalAppThemeColors.current
    
    val backgroundColor = when (rank) {
        1 -> Brush.linearGradient(colors = listOf(Color(0xFFFBBF24), Color(0xFFD97706))) // Gold
        2 -> Brush.linearGradient(colors = listOf(Color(0xFF94A3B8), Color(0xFF475569))) // Silver
        3 -> Brush.linearGradient(colors = listOf(Color(0xFFF97316), Color(0xFFC2410C))) // Bronze
        else -> Brush.linearGradient(colors = listOf(themeColors.onSurface.copy(alpha = 0.08f), themeColors.onSurface.copy(alpha = 0.04f))) // Neutral
    }
    
    val textColor = when (rank) {
        1 -> Color(0xFF5C3E00)
        2 -> Color(0xFFFFFFFF)
        3 -> Color(0xFFFFFFFF)
        else -> themeColors.onSurface
    }
    
    val borderBrush = when (rank) {
        1 -> Brush.linearGradient(colors = listOf(Color(0xFFFFFBE2), Color(0xFFFBBF24)))
        2 -> Brush.linearGradient(colors = listOf(Color(0xFFF1F5F9), Color(0xFF94A3B8)))
        3 -> Brush.linearGradient(colors = listOf(Color(0xFFFFEDD5), Color(0xFFF97316)))
        else -> Brush.linearGradient(colors = listOf(themeColors.divider.copy(alpha = 0.5f), themeColors.divider.copy(alpha = 0.3f)))
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderBrush, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$rank",
            color = textColor,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun LeaderboardStatPill(
    emoji: String,
    value: String,
    label: String,
    color: Color = Color.Unspecified
) {
    val themeColors = LocalAppThemeColors.current
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = emoji, fontSize = 14.sp)
            Text(
                text = value,
                color = themeColors.onSurface,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            )
        }
        Text(
            text = label,
            color = themeColors.secondaryText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp
            )
        )
    }
}

@Composable
fun LeaderboardTab(
    leaderboard:   List<LeaderboardEntry>,
    currentUid:    String,
    friends:       List<UserProfile>,
    pendingReqs:   List<FriendRequest>,
    sentReqs:      List<FriendRequest>,
    onSendRequest: (LeaderboardEntry) -> Unit,
    onAccept:      (FriendRequest) -> Unit,
    onDecline:     (FriendRequest) -> Unit,
    onCancel:      (FriendRequest) -> Unit   // cancel a sent request
) {
    val themeColors = LocalAppThemeColors.current
    var searchQuery by remember { mutableStateOf("") }

    val friendUids  = friends.map { it.uid }.toSet()
    val sentToUids  = sentReqs.map { it.toUid }.toSet()
    val pendingFrom = pendingReqs.map { it.fromUid }.toSet()

    val filtered = remember(searchQuery, leaderboard) {
        if (searchQuery.isBlank()) leaderboard
        else leaderboard.filter {
            it.username.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // Search bar
        OutlinedTextField(
            value       = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search players...", color = themeColors.secondaryText) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = themeColors.onSurface) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, null, tint = themeColors.onSurface)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = themeColors.inputBackground,
                unfocusedContainerColor = themeColors.inputBackground,
                focusedBorderColor   = themeColors.accent,
                unfocusedBorderColor = themeColors.divider,
                focusedTextColor     = themeColors.onSurface,
                unfocusedTextColor   = themeColors.onSurface
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            shape    = RoundedCornerShape(16.dp)
        )

        // Pending invitations received — pinned at top
        if (pendingReqs.isNotEmpty()) {
            GlassCard(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📩 Pending Invitations",
                        color = themeColors.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    pendingReqs.forEach { req ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("From: ${req.fromName}",
                                color = themeColors.onSurface, modifier = Modifier.weight(1f), fontSize = 13.sp)
                            GlassButton("✓", Icons.Default.Check,
                                onClick = { onAccept(req) }, accentColor = Color(0xFF4CAF82),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp))
                            Spacer(Modifier.width(4.dp))
                            GlassButton("✕", Icons.Default.Close,
                                onClick = { onDecline(req) }, accentColor = Color(0xFFFF6584),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }
            }
        }

        // Full leaderboard list
        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(filtered) { index, entry ->
                val isMe       = entry.uid == currentUid
                val isFriend   = entry.uid in friendUids
                val sentTo     = entry.uid in sentToUids
                val pendingFr  = entry.uid in pendingFrom
                val sentReq    = sentReqs.find { it.toUid == entry.uid }

                val cardShape = RoundedCornerShape(24.dp)
                val cardBackground = if (isMe) {
                    Brush.linearGradient(
                        colors = listOf(
                            themeColors.accent.copy(alpha = 0.15f),
                            themeColors.surface.copy(alpha = 0.90f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            themeColors.surface,
                            themeColors.surface
                        )
                    )
                }

                val cardBorderModifier = when (index + 1) {
                    1 -> Modifier.border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFBBF24), // Gold
                                Color(0xFFFBBF24).copy(alpha = 0.4f)
                            )
                        ),
                        shape = cardShape
                    )
                    2 -> Modifier.border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF94A3B8), // Silver
                                Color(0xFF94A3B8).copy(alpha = 0.4f)
                            )
                        ),
                        shape = cardShape
                    )
                    3 -> Modifier.border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFF97316), // Bronze
                                Color(0xFFF97316).copy(alpha = 0.4f)
                            )
                        ),
                        shape = cardShape
                    )
                    else -> Modifier.border(1.dp, themeColors.divider, cardShape)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = if (isMe) 8.dp else 4.dp,
                            shape = cardShape,
                            clip = false,
                            ambientColor = if (isMe) themeColors.accent.copy(alpha = 0.2f) else Color(0x0A000000),
                            spotColor = if (isMe) themeColors.accent.copy(alpha = 0.2f) else Color(0x0A000000)
                        )
                        .clip(cardShape)
                        .background(cardBackground)
                        .then(cardBorderModifier)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Top Row: Medal + Rank + Name on the left, Status Badge on the right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Medal + Rank + Name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val medal = when (index + 1) {
                                    1 -> "🏅"
                                    2 -> "🥈"
                                    3 -> "🥉"
                                    else -> null
                                }
                                if (medal != null) {
                                    Text(medal, fontSize = 16.sp)
                                }
                                Text(
                                    text = "#${index + 1}",
                                    color = themeColors.onSurface,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = entry.username,
                                    color = if (isMe) themeColors.accent else themeColors.onSurface,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        letterSpacing = 0.2.sp
                                    )
                                )
                            }

                            // Right Status Badge
                            when {
                                isMe -> {
                                    Text(
                                        text = "You",
                                        color = themeColors.secondaryText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                isFriend -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF82),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Friends",
                                            color = Color(0xFF4CAF82),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                sentTo -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("⏳", fontSize = 12.sp)
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = null,
                                            tint = themeColors.secondaryText,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Row: Stats on left, Action Button on the right (if any)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Left Stats Column
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                LeaderboardStatPill("🌲", "${entry.treeCount}", "Trees")
                                LeaderboardStatPill("⏱️", "${entry.totalMinutes}", "Time")
                                LeaderboardStatPill("⭐", "${entry.points}", "Points")
                                LeaderboardStatPill("🔥", "${entry.currentStreak}", "Streak")
                            }

                            // Right Action Button (if applicable)
                            Box(contentAlignment = Alignment.Center) {
                                when {
                                    isMe || isFriend -> { /* no button needed */ }
                                    sentTo -> {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(themeColors.onSurface.copy(alpha = 0.08f))
                                                .border(1.dp, themeColors.divider, RoundedCornerShape(16.dp))
                                                .clickable { sentReq?.let { onCancel(it) } }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.History,
                                                    contentDescription = null,
                                                    tint = themeColors.secondaryText,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "Invitation Sent",
                                                    color = themeColors.secondaryText,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    pendingFr -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFF4CAF82))
                                                    .clickable {
                                                        val req = pendingReqs.find { it.fromUid == entry.uid }
                                                        req?.let { onAccept(it) }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("Accept", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFFFF6584).copy(alpha = 0.15f))
                                                    .border(1.dp, Color(0xFFFF6584).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        val req = pendingReqs.find { it.fromUid == entry.uid }
                                                        req?.let { onDecline(it) }
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Decline", tint = Color(0xFFFF6584), modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                    else -> {
                                        // Stranger -> "Add Friend" Button
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(themeColors.accent)
                                                .clickable { onSendRequest(entry) }
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PersonAdd,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "Add Friend",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
