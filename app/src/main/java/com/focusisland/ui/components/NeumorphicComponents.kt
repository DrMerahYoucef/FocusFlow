package com.focusisland.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusisland.service.Phase
import com.focusisland.ui.theme.LocalIsDarkTheme
import com.focusisland.ui.theme.NeumorphicColors

fun Modifier.neumorphicShadow(
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 6.dp,
    isPressed: Boolean = false
): Modifier = this.composed {
    val isDark = LocalIsDarkTheme.current
    // Cohesive, high-contrast glassy translucent colors with reduced transparency
    val bg = if (isDark) Color(0xCC1E222B) else Color(0xDDE0E5EC)
    val highlightColor = if (isDark) Color(0xFF2E333F) else Color(0xFFFFFFFF)
    val shadowColor = if (isDark) Color(0xFF14161C) else Color(0xFFA3B1C6)

    this.drawBehind {
        val rPx = cornerRadius.toPx()
        val offsetPx = elevation.toPx()

        if (!isPressed) {
            // 1. Draw top-left highlight
            drawRoundRect(
                color = if (isDark) highlightColor.copy(alpha = 0.3f) else highlightColor.copy(alpha = 0.9f),
                topLeft = Offset(-offsetPx, -offsetPx),
                size = size,
                cornerRadius = CornerRadius(rPx, rPx)
            )
            // 2. Draw bottom-right shadow
            drawRoundRect(
                color = if (isDark) shadowColor.copy(alpha = 0.8f) else shadowColor.copy(alpha = 0.8f),
                topLeft = Offset(offsetPx, offsetPx),
                size = size,
                cornerRadius = CornerRadius(rPx, rPx)
            )
            // 3. Cover background to match surface color
            drawRoundRect(
                color = bg,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = CornerRadius(rPx, rPx)
            )
        } else {
            // Pressed (inset shadow effect)
            // Draw glassy background first to preserve text contrast
            drawRoundRect(
                color = bg,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = CornerRadius(rPx, rPx)
            )
            drawRoundRect(
                color = shadowColor.copy(alpha = if (isDark) 0.6f else 0.4f),
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = CornerRadius(rPx, rPx)
            )
            // Draw standard pressed core border
            drawRoundRect(
                color = highlightColor.copy(alpha = if (isDark) 0.15f else 0.2f),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = size,
                cornerRadius = CornerRadius(rPx, rPx)
            )
        }
    }
}

@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 6.dp,
    isPressed: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        content = content
    )
}

@Composable
fun NeumorphicButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = NeumorphicColors.Primary
) {
    GlassButton(
        label = label,
        icon = icon,
        onClick = onClick,
        modifier = modifier,
        accentColor = accentColor
    )
}

@Composable
fun NeumorphicProgressArc(
    progress: Float,           // 0f..1f
    phase: Phase,
    remainingText: String,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val arcColor = when (phase) {
        Phase.FOCUS -> Color(0xFF9482FF) // Lavender violet from screenshot!
        Phase.SHORT_BREAK -> if (isDark) Color(0xFF8BCA9A) else Color(0xFF286542)
        Phase.LONG_BREAK -> if (isDark) Color(0xFFFF829C) else Color(0xFFE71D36)
    }

    val glassColor  = if (isDark) Color(0xCC1E222B) else Color(0xDDE0E5EC)
    val borderColor = if (isDark) Color(0x26FFFFFF) else Color(0x1F000000)
    val shadowColor = Color(0x22000000)

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "arc_progress"
    )

    Box(
        modifier = modifier
            .size(240.dp)
            .shadow(12.dp, CircleShape, clip = false, ambientColor = shadowColor, spotColor = shadowColor)
            .clip(CircleShape)
            .background(glassColor)
            .border(1.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            // Outer ring track (Background, thin & semi-transparent white)
            drawArc(
                color = Color.White.copy(alpha = 0.1f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
            // Active progress arc (Lavender with glowing cap)
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = remainingText,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = if (isDark) Color.White else Color(0xFF1B3D2A), // Soft dark forest green in day, crisp white in night
                fontSize = 42.sp,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = phase.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1B3D2A).copy(alpha = 0.7f),
                letterSpacing = 0.5.sp
            )
        }
    }
}
