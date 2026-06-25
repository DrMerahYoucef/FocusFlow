package com.example.ui.screen.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
            modifier = Modifier.fillMaxWidth().padding(4.dp),
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

private fun formatMinutes(minutes: Int): String {
    if (minutes < 60) return "${minutes}m"
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) "${h}h" else "${h}h ${m}m"
}
