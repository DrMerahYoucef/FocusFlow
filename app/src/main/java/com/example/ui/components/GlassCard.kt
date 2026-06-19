package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme(),
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val glassColor  = if (isDark) Color(0x26FFFFFF) else Color(0x40FFFFFF)
    val borderColor = if (isDark) Color(0x33FFFFFF) else Color(0x66FFFFFF)
    val shadowColor = if (isDark) Color(0x40000000) else Color(0x22000000)

    Box(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(cornerRadius), clip = false,
                ambientColor = shadowColor, spotColor = shadowColor)
            .clip(RoundedCornerShape(cornerRadius))
            .background(glassColor)
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
            .padding(16.dp),
        content = content
    )
}
