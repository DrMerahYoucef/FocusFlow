package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import com.example.service.Phase
import com.example.ui.theme.NeumorphicColors

fun Modifier.neumorphicShadow(
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 6.dp,
    isPressed: Boolean = false
): Modifier = this.drawBehind {
    val rPx = cornerRadius.toPx()
    val offsetPx = elevation.toPx()

    if (!isPressed) {
        // 1. Draw top-left highlight (White shadow)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.9f),
            topLeft = Offset(-offsetPx, -offsetPx),
            size = size,
            cornerRadius = CornerRadius(rPx, rPx)
        )
        // 2. Draw bottom-right shadow (Slate gray-blue)
        drawRoundRect(
            color = Color(0xFFA3B1C6).copy(alpha = 0.8f),
            topLeft = Offset(offsetPx, offsetPx),
            size = size,
            cornerRadius = CornerRadius(rPx, rPx)
        )
        // 3. Cover background to match surface color
        drawRoundRect(
            color = Color(0xFFE0E5EC),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(rPx, rPx)
        )
    } else {
        // Pressed (inset shadow effect)
        drawRoundRect(
            color = Color(0xFFA3B1C6).copy(alpha = 0.4f),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(rPx, rPx)
        )
        // Draw standard pressed core border
        drawRoundRect(
            color = Color.White.copy(alpha = 0.2f),
            topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
            size = size,
            cornerRadius = CornerRadius(rPx, rPx)
        )
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
    Box(
        modifier = modifier
            .neumorphicShadow(cornerRadius, elevation, isPressed)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun NeumorphicButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = NeumorphicColors.Primary
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .neumorphicShadow(cornerRadius = 20.dp, elevation = 5.dp, isPressed = isPressed)
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                onClick()
            }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = NeumorphicColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun NeumorphicProgressArc(
    progress: Float,           // 0f..1f
    phase: Phase,
    remainingText: String,
    modifier: Modifier = Modifier
) {
    val arcColor = when (phase) {
        Phase.FOCUS -> NeumorphicColors.Primary
        Phase.SHORT_BREAK -> NeumorphicColors.Success
        Phase.LONG_BREAK -> NeumorphicColors.Accent
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "arc_progress"
    )

    Box(
        modifier = modifier
            .size(240.dp)
            .neumorphicShadow(cornerRadius = 120.dp, elevation = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            // Outer ring track (Background)
            drawArc(
                color = NeumorphicColors.SurfaceDark.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
            // Active progress arc
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        arcColor,
                        arcColor.copy(alpha = 0.6f),
                        arcColor
                    )
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
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
                color = NeumorphicColors.TextPrimary,
                fontSize = 42.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = phase.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = NeumorphicColors.TextSecondary
            )
        }
    }
}
