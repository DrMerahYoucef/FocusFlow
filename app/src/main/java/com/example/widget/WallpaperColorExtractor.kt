package com.example.widget

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.graphics.ColorUtils

object WallpaperColorExtractor {

    data class WidgetPalette(
        val primary: Int,       // dominant wallpaper color, used for accents/icons
        val secondary: Int,     // secondary wallpaper color, used for less prominent accents
        val textColor: Int,     // black or white — picked for contrast
        val iconTint: Int,      // same as textColor by default
        val dividerColor: Int   // low-alpha version of textColor
    )

    /**
     * Extracts current home-screen wallpaper colors and derives a readable palette.
     * Falls back to a neutral gray palette if wallpaper colors are unavailable.
     */
    fun extract(context: Context): WidgetPalette {
        val wallpaperManager = WallpaperManager.getInstance(context)

        val (primary, secondary) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                val wallpaperColors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                val p = wallpaperColors?.primaryColor?.toArgb() ?: Color.parseColor("#808080")
                val s = wallpaperColors?.secondaryColor?.toArgb()
                    ?: wallpaperColors?.primaryColor?.toArgb()
                    ?: Color.parseColor("#A0A0A0")
                Pair(p, s)
            } catch (e: Exception) {
                Pair(Color.parseColor("#808080"), Color.parseColor("#A0A0A0"))
            }
        } else {
            Pair(Color.parseColor("#808080"), Color.parseColor("#A0A0A0"))
        }

        val textColor = contrastingTextColor(primary)
        val dividerAlpha = if (textColor == Color.WHITE) 0x33 else 0x22

        return WidgetPalette(
            primary = primary,
            secondary = secondary,
            textColor = textColor,
            iconTint = textColor,
            dividerColor = ColorUtils.setAlphaComponent(textColor, dividerAlpha)
        )
    }

    /**
     * Returns Color.WHITE or Color.BLACK depending on which gives better
     * contrast against the given background color, using relative luminance.
     */
    private fun contrastingTextColor(backgroundColor: Int): Int {
        val luminance = ColorUtils.calculateLuminance(backgroundColor)
        return if (luminance > 0.45) Color.BLACK else Color.WHITE
    }
}
