package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import org.json.JSONObject
import kotlin.random.Random

private data class CityWindow(
    val id: String,
    val left: Double,
    val top: Double,
    val width: Double,
    val height: Double
)

private const val CITY_ASSET = "window-map.json"

private val cityWindowIds = buildList {
    for (row in 1..14) {
        for (column in 1..5) add("T-r${row}c${column}")
    }
    for (row in 1..11) {
        for (column in 1..3) {
            if (row != 11 || column != 1) add("L-r${row}c${column}")
        }
    }
    add("R-r1c1")
    for (row in 2..12) {
        for (column in 2..4) add("R-r${row}c${column}")
    }
    for (index in 1..49) add("S-${index.toString().padStart(2, '0')}")
}

fun cityLightIds(completedSessions: Int, windowSeed: Long = 0L): Set<String> {
    return cityWindowIds
        .shuffled(Random(windowSeed))
        .take(completedSessions.coerceIn(0, cityWindowIds.size))
        .toSet()
}

private val warmWindowColors = listOf(
    Color(0xFFFFC857),
    Color(0xFFFFD166),
    Color(0xFFFFE08A),
    Color(0xFFFFB84D),
    Color(0xFFFFF0B5)
)

private fun warmWindowColor(id: String): Color = warmWindowColors[
    (id.hashCode() and Int.MAX_VALUE) % warmWindowColors.size
]

@Composable
fun CityBackground(
    lit: Set<String>,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var windows by remember { mutableStateOf(emptyList<CityWindow>()) }

    LaunchedEffect(context) {
        windows = loadCityWindows(context)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(688f / 1536f)
            .background(Color(0xFF0B0714))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
        ) {
            windows.forEach { window ->
                val isLit = window.id in lit
                val shape = androidx.compose.foundation.shape.RoundedCornerShape(6)
                val targetColor = if (isLit) warmWindowColor(window.id) else Color(0xFF0B0714)
                val windowColor by animateColorAsState(
                    targetValue = targetColor,
                    label = "windowColor-${window.id}"
                )
                Box(
                    modifier = Modifier
                        .offset(
                            x = maxWidth * (window.left / 100.0).toFloat(),
                            y = maxHeight * (window.top / 100.0).toFloat()
                        )
                        .size(
                            width = maxWidth * (window.width / 100.0).toFloat(),
                            height = maxHeight * (window.height / 100.0).toFloat()
                        )
                        .shadow(
                            elevation = if (isLit) 7.dp else 0.dp,
                            shape = shape,
                            ambientColor = windowColor.copy(alpha = 0.45f),
                            spotColor = windowColor.copy(alpha = 0.7f)
                        )
                        .clip(shape)
                        .background(windowColor)
                )
            }
        }

        Image(
            painter = painterResource(R.drawable.cityscape_windows_transparent),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f)
        )
    }
}

private fun loadCityWindows(context: Context): List<CityWindow> = runCatching {
    val root = JSONObject(context.assets.open(CITY_ASSET).bufferedReader().use { it.readText() })
    val groups = root.getJSONObject("groups")
    buildList {
        groups.keys().forEach { groupName ->
            val windows = groups.getJSONArray(groupName)
            for (index in 0 until windows.length()) {
                val window = windows.getJSONObject(index)
                add(
                    CityWindow(
                        id = window.getString("id"),
                        left = window.getDouble("left"),
                        top = window.getDouble("top"),
                        width = window.getDouble("width"),
                        height = window.getDouble("height")
                    )
                )
            }
        }
    }
}.getOrDefault(emptyList())