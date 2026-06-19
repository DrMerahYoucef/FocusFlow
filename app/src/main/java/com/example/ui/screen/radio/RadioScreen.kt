package com.example.ui.screen.radio

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.RadioStation
import com.example.data.db.entity.CategoryEntity
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.neumorphicShadow
import com.example.ui.theme.NeumorphicColors

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun RadioScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: RadioViewModel = viewModel()
) {
    val context = LocalContext.current
    val favoriteIds by viewModel.favouriteIds.collectAsState()
    val displayedStations by viewModel.displayedStations.collectAsState()
    val currentStation by viewModel.currentStation.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    var showDiscover by remember { mutableStateOf(false) }

    // Filtered list: show only favorited stations under the static Favorites tab
    val activeStations = remember(displayedStations, favoriteIds) {
        displayedStations.filter { it.id in favoriteIds }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .neumorphicShadow(cornerRadius = 12.dp, elevation = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
                            tint = NeumorphicColors.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Study Radio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicColors.TextPrimary
                    )
                }

                // Top right favorites mini indicator badge
                Box(
                    modifier = Modifier
                        .neumorphicShadow(cornerRadius = 12.dp, elevation = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = NeumorphicColors.Accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${favoriteIds.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Twin Sleek Neumorphic Tabs: Favorites vs Discover
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val favSelected = !showDiscover
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .neumorphicShadow(
                            cornerRadius = 16.dp,
                            elevation = if (favSelected) 1.dp else 4.dp,
                            isPressed = favSelected
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showDiscover = false }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = if (favSelected) NeumorphicColors.Primary else NeumorphicColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Favorites",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (favSelected) NeumorphicColors.Primary else NeumorphicColors.TextPrimary
                        )
                    }
                }

                val discoverSelected = showDiscover
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .neumorphicShadow(
                            cornerRadius = 16.dp,
                            elevation = if (discoverSelected) 1.dp else 4.dp,
                            isPressed = discoverSelected
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showDiscover = true }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = if (discoverSelected) NeumorphicColors.Primary else NeumorphicColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Discover",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (discoverSelected) NeumorphicColors.Primary else NeumorphicColors.TextPrimary
                        )
                    }
                }
            }

            if (showDiscover) {
                DiscoverPanel(
                    viewModel = viewModel,
                    favoriteIds = favoriteIds,
                    currentStation = currentStation,
                    isPlaying = isPlaying,
                    onToggleFavourite = { station ->
                        viewModel.toggleFavourite(station)
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Now Playing Hero layout
                    if (currentStation != null) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "NOW PLAYING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeumorphicColors.TextSecondary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                val station = currentStation!!
                                val isFav = station.id in favoriteIds

                                NeumorphicCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 24.dp,
                                    elevation = 6.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(20.dp)
                                    ) {
                                        // Station Logo / default fallback
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .neumorphicShadow(cornerRadius = 20.dp, elevation = 4.dp)
                                                .clip(RoundedCornerShape(20.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            SubcomposeAsyncImage(
                                                model = station.logoUrl.ifEmpty { null },
                                                contentDescription = station.name,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(6.dp)
                                                    .clip(RoundedCornerShape(14.dp)),
                                                error = {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_radio_default),
                                                        contentDescription = station.name,
                                                        tint = NeumorphicColors.TextPrimary,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(horizontal = 24.dp)
                                            ) {
                                                Text(
                                                    text = station.name,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NeumorphicColors.TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                IconButton(
                                                    onClick = { viewModel.toggleFavourite(station) },
                                                    modifier = Modifier.size(24.dp)
                                                 ) {
                                                    Icon(
                                                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                                        contentDescription = "Favorite",
                                                        tint = if (isFav) NeumorphicColors.Accent else NeumorphicColors.TextSecondary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = station.country,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = NeumorphicColors.TextSecondary,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = station.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = NeumorphicColors.TextSecondary.copy(alpha = 0.8f),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                            )
                                        }

                                        // Dynamic Pulsing Stream State Indicator
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (isPlaying) {
                                                PlayingWaveIndicator(color = NeumorphicColors.Primary)
                                                Spacer(Modifier.width(12.dp))
                                                Text(
                                                    text = "Streaming live...",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = NeumorphicColors.Primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(NeumorphicColors.TextSecondary, CircleShape)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = "Paused",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = NeumorphicColors.TextSecondary
                                                )
                                            }
                                        }

                                        // Controls: Next / Previous / Pause
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                                        ) {
                                            // Previous Station
                                            IconButton(
                                                onClick = {
                                                    val currIndex = activeStations.indexOfFirst { it.id == station.id }
                                                    if (currIndex > 0) {
                                                        viewModel.selectStation(activeStations[currIndex - 1], context)
                                                    } else if (activeStations.isNotEmpty()) {
                                                        viewModel.selectStation(activeStations.last(), context)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .neumorphicShadow(cornerRadius = 16.dp, elevation = 3.dp)
                                            ) {
                                                Text("⏮", fontSize = 20.sp, color = NeumorphicColors.TextPrimary)
                                            }

                                            // Play / Pause Toggle
                                            IconButton(
                                                onClick = { viewModel.togglePlayback(context) },
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .neumorphicShadow(cornerRadius = 28.dp, elevation = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                                    tint = NeumorphicColors.Primary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }

                                            // Next Station
                                            IconButton(
                                                onClick = {
                                                    val currIndex = activeStations.indexOfFirst { it.id == station.id }
                                                    if (currIndex != -1 && currIndex < activeStations.size - 1) {
                                                        viewModel.selectStation(activeStations[currIndex + 1], context)
                                                    } else if (activeStations.isNotEmpty()) {
                                                        viewModel.selectStation(activeStations.first(), context)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .neumorphicShadow(cornerRadius = 16.dp, elevation = 3.dp)
                                            ) {
                                                Text("⏭", fontSize = 20.sp, color = NeumorphicColors.TextPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Station List header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FAVORITE STATIONS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicColors.TextSecondary
                            )
                        }
                    }

                    if (activeStations.isEmpty()) {
                        item {
                            Text(
                                text = "No favorite stations yet.\nTap the heart ❤️ on any station under \"Discover\" tab to add them here!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NeumorphicColors.TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp, horizontal = 24.dp)
                            )
                        }
                    } else {
                        items(activeStations, key = { it.id }) { station ->
                            val isFav = station.id in favoriteIds
                            val isCurrent = currentStation?.id == station.id
                            val isCurrentlyPlaying = isCurrent && isPlaying

                            StationCard(
                                station = station,
                                isFavourite = isFav,
                                isPlaying = isCurrentlyPlaying,
                                onPlay = { viewModel.selectStation(station, context) },
                                onFavourite = { viewModel.toggleFavourite(station) }
                            )
                        }
                    }
                }
            }

            // Now Playing Bar (persistent bottom panel - shows if currentStation is non-null and we scrolled past)
            if (currentStation != null) {
                val station = currentStation!!
                NowPlayingBar(
                    station = station,
                    isPlaying = isPlaying,
                    onToggle = { viewModel.togglePlayback(context) },
                    onExpand = { /* already expanded list above, maybe scroll to top */ }
                )
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StationCard(
    station: RadioStation,
    isFavourite: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onFavourite: () -> Unit
) {
    val borderColor = if (isPlaying) NeumorphicColors.Primary else Color.Transparent
    val isDark = com.example.ui.theme.LocalIsDarkTheme.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .neumorphicShadow(cornerRadius = 16.dp, isPressed = isPlaying)
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onPlay() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0x26FFFFFF) else Color(0x11000000)),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = station.logoUrl.ifEmpty { null },
                    contentDescription = station.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    error = {
                        Icon(
                            painter = painterResource(R.drawable.ic_radio_default),
                            contentDescription = station.name,
                            tint = NeumorphicColors.TextPrimary,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                        )
                    }
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isPlaying) NeumorphicColors.Primary else NeumorphicColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${station.country} · ${station.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeumorphicColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isPlaying) {
                PlayingWaveIndicator(color = NeumorphicColors.Primary)
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = onFavourite) {
                Icon(
                    imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favourite",
                    tint = if (isFavourite) NeumorphicColors.Accent else NeumorphicColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun NowPlayingBar(
    station: RadioStation?,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onExpand: () -> Unit
) {
    if (station == null) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .neumorphicShadow(cornerRadius = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onExpand() }
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            LiveDot(color = NeumorphicColors.Accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = NeumorphicColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "LIVE STREAMING",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeumorphicColors.Accent,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .size(40.dp)
                    .neumorphicShadow(cornerRadius = 20.dp, elevation = 2.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = NeumorphicColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PlayingWaveIndicator(color: Color) {
    val bars = 4
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(18.dp)
    ) {
        repeat(bars) { i ->
            val height by infiniteTransition.animateFloat(
                initialValue = 4f,
                targetValue = 18f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400 + i * 50, delayMillis = i * 80, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$i"
            )
            Box(
                Modifier
                    .width(3.dp)
                    .height(height.dp)
                    .background(color, RoundedCornerShape(1.5.dp))
            )
        }
    }
}

@Composable
fun LiveDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color.copy(alpha = alpha), CircleShape)
            .border(1.5.dp, color, CircleShape)
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun DiscoverPanel(
    viewModel: RadioViewModel,
    favoriteIds: Set<String>,
    currentStation: RadioStation?,
    isPlaying: Boolean,
    onToggleFavourite: (RadioStation) -> Unit
) {
    val context = LocalContext.current
    val searchQuery by viewModel.globalSearchQuery.collectAsState()
    val searchResults by viewModel.globalSearchResults.collectAsState()
    val trendingStations by viewModel.globalTrendingStations.collectAsState()
    val isSearching by viewModel.isDiscoverSearching.collectAsState()
    val isTrendingLoading by viewModel.isDiscoverTrendingLoading.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val trendingError by viewModel.trendingError.collectAsState()

    // Automatically load trending streams on enter
    LaunchedEffect(Unit) {
        viewModel.loadTrendingStations()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "SEARCH GLOBAL RADIO-BROWSER",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = NeumorphicColors.TextSecondary,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .neumorphicShadow(cornerRadius = 16.dp, elevation = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = NeumorphicColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchGlobalStations(it) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = NeumorphicColors.TextPrimary),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search 40,000+ stations (e.g. Jazz, BBC...)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = NeumorphicColors.TextSecondary.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.searchGlobalStations("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = NeumorphicColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (searchQuery.trim().length >= 2) {
            Text(
                text = "SEARCH RESULTS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = NeumorphicColors.TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = NeumorphicColors.Primary, modifier = Modifier.size(24.dp))
                        Text("Searching world-wide...", style = MaterialTheme.typography.bodySmall, color = NeumorphicColors.TextSecondary)
                    }
                }
            } else if (searchError != null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    com.example.ui.components.GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = searchError ?: "An issue occurred.",
                                color = NeumorphicColors.TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            NeumorphicButton(
                                label = "Retry Discovery",
                                icon = Icons.Default.Refresh,
                                onClick = { viewModel.searchGlobalStations(searchQuery) }
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(searchResults, key = { it.id }) { station ->
                            val isPlayingCurrent = currentStation?.id == station.id && isPlaying
                            val isFav = station.id in favoriteIds
                            DiscoverStationItem(
                                station = station,
                                isFavourite = isFav,
                                isPlaying = isPlayingCurrent,
                                onPlay = { viewModel.selectStation(station, context) },
                                onFavourite = { onToggleFavourite(station) }
                            )
                        }
                    }
                }
            } else if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No stations found.\nTry a simpler keyword or check your network.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeumorphicColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(searchResults, key = { it.id }) { station ->
                        val isPlayingCurrent = currentStation?.id == station.id && isPlaying
                        val isFav = station.id in favoriteIds
                        DiscoverStationItem(
                            station = station,
                            isFavourite = isFav,
                            isPlaying = isPlayingCurrent,
                            onPlay = { viewModel.selectStation(station, context) },
                            onFavourite = { onToggleFavourite(station) }
                        )
                    }
                }
            }
        } else {
            Text(
                text = "🔥 POPULAR INTERNATIONAL STREAMS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = NeumorphicColors.TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (isTrendingLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = NeumorphicColors.Primary, modifier = Modifier.size(24.dp))
                        Text("Loading trending streams...", style = MaterialTheme.typography.bodySmall, color = NeumorphicColors.TextSecondary)
                    }
                }
            } else if (trendingError != null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    com.example.ui.components.GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = trendingError ?: "An issue occurred.",
                                color = NeumorphicColors.TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            NeumorphicButton(
                                label = "Retry Trending",
                                icon = Icons.Default.Refresh,
                                onClick = { viewModel.loadTrendingStations() }
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(trendingStations, key = { it.id }) { station ->
                            val isPlayingCurrent = currentStation?.id == station.id && isPlaying
                            val isFav = station.id in favoriteIds
                            DiscoverStationItem(
                                station = station,
                                isFavourite = isFav,
                                isPlaying = isPlayingCurrent,
                                onPlay = { viewModel.selectStation(station, context) },
                                onFavourite = { onToggleFavourite(station) }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(trendingStations, key = { it.id }) { station ->
                        val isPlayingCurrent = currentStation?.id == station.id && isPlaying
                        val isFav = station.id in favoriteIds
                        DiscoverStationItem(
                            station = station,
                            isFavourite = isFav,
                            isPlaying = isPlayingCurrent,
                            onPlay = { viewModel.selectStation(station, context) },
                            onFavourite = { onToggleFavourite(station) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoverStationItem(
    station: RadioStation,
    isFavourite: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onFavourite: () -> Unit
) {
    val borderColor = if (isPlaying) NeumorphicColors.Primary else Color.Transparent
    val isDark = com.example.ui.theme.LocalIsDarkTheme.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .neumorphicShadow(cornerRadius = 16.dp, isPressed = isPlaying)
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onPlay() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0x26FFFFFF) else Color(0x11000000)),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = station.logoUrl.ifEmpty { null },
                    contentDescription = station.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    error = {
                        Icon(
                            painter = painterResource(R.drawable.ic_radio_default),
                            contentDescription = station.name,
                            tint = NeumorphicColors.TextPrimary,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                        )
                    }
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isPlaying) NeumorphicColors.Primary else NeumorphicColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (station.country.isNotBlank()) "${station.country} · ${station.description}" else station.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = NeumorphicColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isPlaying) {
                PlayingWaveIndicator(color = NeumorphicColors.Primary)
                Spacer(Modifier.width(12.dp))
            }

            // Favorite Button
            IconButton(
                onClick = onFavourite,
                modifier = Modifier
                    .size(40.dp)
                    .neumorphicShadow(cornerRadius = 20.dp, elevation = 2.dp)
            ) {
                Icon(
                    imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavourite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavourite) NeumorphicColors.Primary else NeumorphicColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
