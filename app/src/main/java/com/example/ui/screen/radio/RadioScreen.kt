package com.example.ui.screen.radio

import android.app.Application
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.RadioStation
import com.example.data.StationCatalogue
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.neumorphicShadow
import com.example.ui.theme.NeumorphicColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RadioScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: RadioViewModel = viewModel()
) {
    val context = LocalContext.current
    val favoriteIds by viewModel.favouriteIds.collectAsState()
    val selectedCat by viewModel.selectedCategory.collectAsState()
    val displayedStations by viewModel.displayedStations.collectAsState()
    val currentStation by viewModel.currentStation.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    var showOnlyFavorites by remember { mutableStateOf(false) }

    // Filtered list depending on the showOnlyFavorites toggle or category selection
    val activeStations = remember(displayedStations, favoriteIds, showOnlyFavorites) {
        if (showOnlyFavorites) {
            displayedStations.filter { it.id in favoriteIds }
        } else {
            displayedStations
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(NeumorphicColors.Background)
            .statusBarsPadding(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        .background(NeumorphicColors.Background)
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
            // Category Selector Tabs
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Favorites Filter Tab
                val favTabSelected = showOnlyFavorites
                Box(
                    modifier = Modifier
                        .neumorphicShadow(
                            cornerRadius = 12.dp,
                            elevation = if (favTabSelected) 1.dp else 4.dp,
                            isPressed = favTabSelected
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (favTabSelected) NeumorphicColors.Background else NeumorphicColors.Background)
                        .clickable {
                            showOnlyFavorites = true
                            viewModel.setCategory(null)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("❤️ Favourites", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (favTabSelected) NeumorphicColors.Primary else NeumorphicColors.TextPrimary)
                    }
                }

                // All Stations Tab
                val allSelected = !showOnlyFavorites && selectedCat == null
                Box(
                    modifier = Modifier
                        .neumorphicShadow(
                            cornerRadius = 12.dp,
                            elevation = if (allSelected) 1.dp else 4.dp,
                            isPressed = allSelected
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeumorphicColors.Background)
                        .clickable {
                            showOnlyFavorites = false
                            viewModel.setCategory(null)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("🌐 All", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (allSelected) NeumorphicColors.Primary else NeumorphicColors.TextPrimary)
                }

                // Category Tabs (ALGERIAN, STUDY, GLOBAL)
                StationCatalogue.Category.values().forEach { cat ->
                    val catSelected = !showOnlyFavorites && selectedCat == cat
                    val label = when (cat) {
                        StationCatalogue.Category.ALGERIAN -> "🇩🇿 Algerian"
                        StationCatalogue.Category.STUDY -> "📚 Study"
                        StationCatalogue.Category.GLOBAL -> "🌍 Global"
                    }
                    Box(
                        modifier = Modifier
                            .neumorphicShadow(
                                cornerRadius = 12.dp,
                                elevation = if (catSelected) 1.dp else 4.dp,
                                isPressed = catSelected
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeumorphicColors.Background)
                            .clickable {
                                showOnlyFavorites = false
                                viewModel.setCategory(cat)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (catSelected) NeumorphicColors.Primary else NeumorphicColors.TextPrimary
                        )
                    }
                }
            }

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
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Station Logo / default fallback
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .neumorphicShadow(cornerRadius = 20.dp, elevation = 4.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(NeumorphicColors.Background),
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
                                                onClick = { viewModel.toggleFavourite(station.id) },
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
                    Text(
                        text = "STATIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicColors.TextSecondary,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                }

                if (activeStations.isEmpty()) {
                    item {
                        Text(
                            text = if (showOnlyFavorites) "No favorite stations configured yet.\nTap ❤️ on any station below to save it!" else "No stations found here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeumorphicColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp)
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
                            onFavourite = { viewModel.toggleFavourite(station.id) }
                        )
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

@Composable
fun StationCard(
    station: RadioStation,
    isFavourite: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onFavourite: () -> Unit
) {
    val borderColor = if (isPlaying) NeumorphicColors.Primary else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .neumorphicShadow(cornerRadius = 16.dp, isPressed = isPlaying)
            .clip(RoundedCornerShape(16.dp))
            .background(NeumorphicColors.Background)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onPlay() }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Station logo or fallback emoji
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeumorphicColors.Background.copy(alpha = 0.5f)),
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
                    style = MaterialTheme.typography.titleSmall,
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
            // Playing wave visualizer animation
            if (isPlaying) {
                PlayingWaveIndicator(color = NeumorphicColors.Primary)
                Spacer(Modifier.width(8.dp))
            }
            // Heart button
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
            .padding(12.dp)
            .neumorphicShadow(cornerRadius = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(NeumorphicColors.Background)
            .clickable { onExpand() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Pulsing live red dot
            LiveDot(color = NeumorphicColors.Accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.labelLarge,
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

            // Play/Pause miniature action button
            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .size(36.dp)
                    .neumorphicShadow(cornerRadius = 18.dp, elevation = 2.dp)
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
            .border(1.dp, color, CircleShape)
    )
}
