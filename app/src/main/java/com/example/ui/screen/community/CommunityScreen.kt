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
import androidx.compose.ui.geometry.CornerRadius
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
                            "Skyline",
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
                0 -> SkylineTab(
                    friends = friends,
                    pendingReqs = pendingReqs,
                    sentReqs = sentReqs,
                    searchResults = searchResults,
                    searchQuery = searchQuery,
                    lightsActivated = myProfile?.treeCount ?: 0,
                    username = myProfile?.username ?: "You",
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
fun SkylineTab(
    friends: List<UserProfile>,
    pendingReqs: List<FriendRequest>,
    sentReqs: List<FriendRequest>,
    searchResults: List<UserProfile>,
    searchQuery: String,
    lightsActivated: Int,
    username: String,
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

    val isDark = true
    val backgroundColors = listOf(Color(0xFF211A4D), Color(0xFF050916))

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
                            val pos = buildingPosition(i, friends.size, androidx.compose.ui.geometry.Size(size.width.toFloat(), size.height.toFloat()))
                            if ((tap - pos).getDistance() < 90f) {
                                selectedFriend = friend
                            }
                        }
                    }
                }
        ) {
            drawSkylineBackdrop()
            drawBuilding(Offset(size.width / 2f, size.height * .72f), 150f, lightsActivated, username, false)
            friends.forEachIndexed { i, friend ->
                drawBuilding(buildingPosition(i, friends.size, size), 76f + (i % 3) * 14f, friend.treeCount, friend.username, selectedFriend?.uid == friend.uid)
            }
        }

        Column(Modifier.align(Alignment.TopStart).padding(18.dp)) {
            Text("THE NIGHTLY SKYLINE", color = Color.White.copy(alpha = .62f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text("Every focused minute lights a window.", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }

        // Selected Friend Profile Card (Frosted Glass Overlay overlaying content)
        selectedFriend?.let { friend ->
            Box(
                modifier = Modifier.fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 100.dp)
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BuildingPreview(friend.treeCount, Modifier.size(64.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "${friend.username}'s Building",
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
                            LeaderboardStatPill("💡", "${friend.treeCount}", "Lights")
                            LeaderboardStatPill("⏱️", "${friend.totalMinutes}m", "Focus time")
                            LeaderboardStatPill("🎯", "${friend.treeCount}", "Sessions")
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
                                Text("Offline • permanent lights only", color = themeColors.secondaryText, fontSize = 11.sp)
                            }
                        }

                        GlassButton("Remove Friend", Icons.Default.Delete, {
                            onRemove(friend)
                            selectedFriend = null
                        }, modifier = Modifier.fillMaxWidth(), accentColor = Color(0xFFFF6584))
                    }
                }
            }
        }

        // Add Friends Floating Action Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 20.dp)
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

private fun buildingPosition(index: Int, total: Int, size: Size): Offset {
    val columns = maxOf(1, minOf(4, total))
    val row = index / columns
    val column = index % columns
    return Offset(
        x = size.width * ((column + 1f) / (columns + 1f)),
        y = size.height * (.34f + (row % 3) * .16f)
    )
}

@Composable
private fun BuildingPreview(lights: Int, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF111531))
    ) {
        drawBuilding(Offset(size.width / 2f, size.height), size.width * .55f, lights, "", false)
    }
}

private fun DrawScope.drawSkylineBackdrop() {
    repeat(34) { index ->
        val x = (index * 97f) % size.width
        val y = 38f + ((index * 47f) % (size.height * .42f))
        drawCircle(Color.White.copy(alpha = if (index % 4 == 0) .85f else .35f), if (index % 5 == 0) 2.2f else 1.1f, Offset(x, y))
    }
    drawCircle(Color(0xFFE9E5FF), 31f, Offset(size.width - 66f, 74f))
    drawCircle(Color(0xFF211A4D), 28f, Offset(size.width - 53f, 64f))
    repeat(9) { index ->
        val x = index * size.width / 8f
        val height = 70f + (index % 4) * 42f
        drawRect(Color(0xFF090D20), Offset(x, size.height - height), Size(size.width / 9f + 8f, height))
    }
    drawRect(Color(0xFF080B18), Offset(0f, size.height - 28f), Size(size.width, 28f))
}

private fun DrawScope.drawBuilding(
    center: Offset,
    width: Float,
    lights: Int,
    label: String?,
    isSelected: Boolean
) {
    val height = (width * (1.5f + lights.coerceAtMost(24) / 24f)).coerceIn(width * 1.45f, width * 3.2f)
    val left = center.x - width / 2f
    val top = center.y - height
    drawRect(Color.Black.copy(alpha = .35f), Offset(left + 8f, top + 10f), Size(width, height))
    drawRect(Color(0xFF171B38), Offset(left, top), Size(width, height))
    drawRect(Color(0xFFBFA7FF).copy(alpha = .45f), Offset(left, top), Size(width, 5f))
    val rows = maxOf(3, (height / 22f).toInt())
    val columns = maxOf(2, (width / 24f).toInt())
    repeat(rows * columns) { index ->
        val lit = index < lights.coerceAtMost(rows * columns)
        val x = left + 10f + (index % columns) * ((width - 20f) / columns)
        val y = top + 14f + (index / columns) * ((height - 22f) / rows)
        drawRoundRect(if (lit) Color(0xFFFFC96B) else Color(0xFF3B4269), Offset(x, y), Size(8f, 10f), CornerRadius(2f, 2f))
    }
    if (isSelected) drawRect(Color(0xFFBFA7FF), Offset(left - 5f, top - 5f), Size(width + 10f, height + 10f), style = Stroke(3f))
    drawContext.canvas.nativeCanvas.drawText(
        label?.take(14)?.uppercase() ?: "",
        center.x,
        center.y + 18f,
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 21f
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
    var sortMode by remember { mutableStateOf("Lights") }

    val friendUids  = friends.map { it.uid }.toSet()
    val sentToUids  = sentReqs.map { it.toUid }.toSet()
    val pendingFrom = pendingReqs.map { it.fromUid }.toSet()

    val filtered = remember(searchQuery, leaderboard, sortMode) {
        val matching = if (searchQuery.isBlank()) leaderboard else leaderboard.filter { it.username.contains(searchQuery, ignoreCase = true) }
        matching.sortedByDescending {
            when (sortMode) {
                "Time" -> it.totalMinutes
                "Streak" -> it.currentStreak
                "Sessions" -> it.treeCount
                "Weekly" -> it.points
                "Monthly" -> it.points
                else -> it.treeCount
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Text("🏆 Top Focus Flow Builders", color = themeColors.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Lights", "Time", "Streak", "Sessions", "Weekly", "Monthly").forEach { mode ->
                FilterChip(selected = sortMode == mode, onClick = { sortMode = mode }, label = { Text(mode, fontSize = 11.sp) })
            }
        }

        // Search bar
        OutlinedTextField(
            value       = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search username or building...", color = themeColors.secondaryText) },
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
                                LeaderboardStatPill("💡", "${entry.treeCount}", "Lights")
                                LeaderboardStatPill("⏱️", "${entry.totalMinutes}m", "Focus time")
                                LeaderboardStatPill("⭐", "${entry.points}", "Focus score")
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
