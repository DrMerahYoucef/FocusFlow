package com.example.ui.screen.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.NeumorphicColors
import com.example.ui.theme.LocalAppThemeColors

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val themeColors = LocalAppThemeColors.current

    var selectedDayMillis by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(state.dailyStatsList) {
        if (selectedDayMillis == null || state.dailyStatsList.none { it.dayStartMillis == selectedDayMillis }) {
            state.dailyStatsList.lastOrNull()?.let {
                selectedDayMillis = it.dayStartMillis
            }
        }
    }

    val selectedDayStats = state.dailyStatsList.find { it.dayStartMillis == selectedDayMillis }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper Header
        Text(
            text = "ANALYTICS",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 4.sp,
            color = themeColors.onSurface,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        )

        // Stat Grid: 2 columns
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                title = "Total Focus",
                value = formatMinutes(state.totalFocusMinutes),
                icon = Icons.Default.Timer,
                iconColor = themeColors.accent,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Total Points",
                value = "${state.totalScore} pts",
                icon = Icons.Default.Star,
                iconColor = NeumorphicColors.Warning,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                title = "Sessions Done",
                value = "${state.completedSessions}",
                icon = Icons.Default.CheckCircle,
                iconColor = NeumorphicColors.Success,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Current Streak",
                value = "${state.longestStreakDays} days",
                icon = Icons.Default.LocalFireDepartment,
                iconColor = NeumorphicColors.Accent,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // INTERACTIVE DAILY TREES PLANTED SECTION
        NeumorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("daily_trees_planted_card"),
            cornerRadius = 20.dp,
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Daily Trees Planted",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onSurface
                )
                Text(
                    text = "Scroll horizontally to view past days. Tap a day to see statistics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.secondaryText,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (state.dailyStatsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("🌲", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No tree history recorded yet. Complete some sessions!",
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.secondaryText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val maxCompleted = state.dailyStatsList.maxOf { it.completedCount }.coerceAtLeast(1)
                    val listState = rememberLazyListState()

                    // Auto scroll to latest day (today) on launch
                    LaunchedEffect(state.dailyStatsList) {
                        if (state.dailyStatsList.isNotEmpty()) {
                            listState.animateScrollToItem(state.dailyStatsList.lastIndex)
                        }
                    }

                    LazyRow(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        items(state.dailyStatsList) { dayStat ->
                            val isSelected = dayStat.dayStartMillis == selectedDayMillis
                            val percent = dayStat.completedCount.toFloat() / maxCompleted.toFloat()
                            val animatedHeightPercent by animateFloatAsState(
                                targetValue = percent,
                                animationSpec = tween(durationMillis = 600),
                                label = "tree_bar_growth"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .width(44.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedDayMillis = dayStat.dayStartMillis }
                                    .padding(vertical = 4.dp)
                                    .testTag("daily_tree_bar_${dayStat.dayLabel}")
                            ) {
                                // Column labels (number of trees)
                                if (dayStat.completedCount > 0) {
                                    Text(
                                        text = "🌲${dayStat.completedCount}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) themeColors.accent else themeColors.onSurface
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(14.dp))
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Visual bar representing trees planted on this day
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .width(18.dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (isSelected) {
                                                Brush.verticalGradient(
                                                    listOf(
                                                        themeColors.accent,
                                                        themeColors.accent.copy(alpha = 0.5f)
                                                    )
                                                )
                                            } else {
                                                Brush.verticalGradient(
                                                    listOf(
                                                        themeColors.onSurface.copy(alpha = 0.15f),
                                                        themeColors.onSurface.copy(alpha = 0.05f)
                                                    )
                                                )
                                            }
                                        )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Date Label under bar
                                Text(
                                    text = dayStat.dayLabel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) themeColors.accent else themeColors.secondaryText,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Detailed selected day stats panel
                if (selectedDayStats != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(themeColors.divider)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = selectedDayStats.dateFullLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.accent,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailStatPill(
                            icon = "🌲",
                            value = "${selectedDayStats.completedCount}",
                            label = "Trees",
                            modifier = Modifier.weight(1f)
                        )
                        DetailStatPill(
                            icon = "⏱️",
                            value = "${selectedDayStats.totalFocusMinutes}m",
                            label = "Focus Time",
                            modifier = Modifier.weight(1f)
                        )
                        DetailStatPill(
                            icon = "⭐",
                            value = "${selectedDayStats.totalPoints} pts",
                            label = "Points",
                            modifier = Modifier.weight(1f)
                        )
                        DetailStatPill(
                            icon = "📋",
                            value = "${selectedDayStats.totalSessionsCount}",
                            label = "Sessions",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Bar Chart Card representing last 7 days productivity
        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = "Weekly Focus Points",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onSurface
                )
                Text(
                    text = "1 point earned per focused minute completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.secondaryText
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (state.last7DaysScores.all { it.second == 0 }) {
                    // Empty chart state helper description
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = themeColors.secondaryText.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No score data yet. Start focus sessions!",
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.secondaryText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Render Bar Columns
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val maxScore = state.last7DaysScores.maxOf { it.second }.coerceAtLeast(10)

                        state.last7DaysScores.forEach { (dayLabel, score) ->
                            val percent = score.toFloat() / maxScore.toFloat()
                            val animatedHeightPercent by animateFloatAsState(
                                targetValue = percent,
                                animationSpec = tween(durationMillis = 600),
                                label = "bar_growth"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Column point label on top of bar if > 0
                                if (score > 0) {
                                    Text(
                                        text = "$score",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = themeColors.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }

                                // Capsule Shape represent the actual column bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(fraction = animatedHeightPercent.coerceIn(0.05f, 1f))
                                        .width(16.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    themeColors.accent,
                                                    themeColors.accent.copy(alpha = 0.6f)
                                                )
                                            )
                                        )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Days label (Mon, Tue, etc.)
                                Text(
                                    text = dayLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.secondaryText
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Streak details informational banner card
        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            elevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = themeColors.accent,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Build a streak",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onSurface
                    )
                    Text(
                        text = "Complete at least one focused session daily to grow your productivity streak counts.",
                        fontSize = 11.sp,
                        color = themeColors.secondaryText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(56.dp)) // padding for bottom bars
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalAppThemeColors.current
    NeumorphicCard(
        modifier = modifier,
        cornerRadius = 16.dp,
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = themeColors.secondaryText,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = value,
                fontSize = 20.sp,
                color = themeColors.onSurface,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun DetailStatPill(
    icon: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val themeColors = LocalAppThemeColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(themeColors.onSurface.copy(alpha = 0.04f))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = themeColors.onSurface,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            Text(
                text = label,
                color = themeColors.secondaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    if (minutes < 60) return "${minutes}m"
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) "${h}h" else "${h}h ${m}m"
}
