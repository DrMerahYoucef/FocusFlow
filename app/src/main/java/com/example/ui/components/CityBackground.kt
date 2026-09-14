package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.zIndex
import com.example.R
import org.json.JSONObject

private data class CityWindow(
    val id: String,
    val left: Double,
    val top: Double,
    val width: Double,
    val height: Double
)

private const val CITY_ASSET = "window-map.json"

fun towerLightIds(completedSessions: Int): Set<String> = buildSet {
    val count = completedSessions.coerceIn(0, 70)
    for (lightOrder in 1..count) {
        val row = 14 - ((lightOrder - 1) / 5)
        val column = ((lightOrder - 1) % 5) + 1
        add("T-r${row}c${column}")
    }
}

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
                .matchParentSize()
                .zIndex(1f)
        ) {
            windows.forEach { window ->
                val isLit = window.id in lit
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
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(6))
                        .background(if (isLit) Color(0xFFFFF3C4) else Color(0xFF0B0714))
                )
            }
        }

        Image(
            painter = painterResource(R.drawable.cityscape_windows_transparent),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .matchParentSize()
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