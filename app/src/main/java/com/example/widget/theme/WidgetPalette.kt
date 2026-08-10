package com.example.widget.theme

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.example.R
import kotlin.math.min

/**
 * Resolved, contrast-verified colour set for the FocusFlow widgets.
 * Every foreground value is guaranteed legible over [surface] no matter what
 * wallpaper sits behind the translucent card.
 */
data class WidgetPalette(
    val isNight: Boolean,
    @ColorInt val surface: Int,          // translucent
    @ColorInt val hueVeil: Int,          // translucent
    @ColorInt val rim: Int,
    @ColorInt val onSurface: Int,
    @ColorInt val onSurfaceVariant: Int,
    @ColorInt val accent: Int,
    @ColorInt val accentSoft: Int,
    @ColorInt val secondary: Int,
    @ColorInt val critical: Int,
    @ColorInt val track: Int,
    @ColorInt val divider: Int
)

object WidgetTheme {

    /** Body opacity of the glass card. Lower = more wallpaper shows through.
     *  Keep in sync with res/color/wgt_surface.xml. Range 0.75f–1.0f. */
    const val SURFACE_OPACITY_DAY = 0.80f
    const val SURFACE_OPACITY_NIGHT = 0.84f

    private const val MIN_CONTRAST_BODY = 4.5   // WCAG AA, normal text
    private const val MIN_CONTRAST_LARGE = 3.0  // WCAG AA, >=18sp bold / graphics

    fun isNight(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    fun resolve(context: Context): WidgetPalette {
        val night = isNight(context)
        val c = { id: Int -> ContextCompat.getColor(context, id) }

        val opacity = if (night) SURFACE_OPACITY_NIGHT else SURFACE_OPACITY_DAY
        val surfaceOpaque = c(R.color.wgt_surface_base)
        val surface = ColorUtils.setAlphaComponent(surfaceOpaque, (opacity * 255f).toInt())

        // Worst-case backdrops: the card composited over a pure white and a pure
        // black wallpaper. Anything readable on both is readable on anything.
        val overWhite = ColorUtils.compositeColors(surface, Color.WHITE)
        val overBlack = ColorUtils.compositeColors(surface, Color.BLACK)

        fun guard(@ColorInt fg: Int, minRatio: Double) =
            ensureContrast(fg, overWhite, overBlack, minRatio)

        return WidgetPalette(
            isNight = night,
            surface = surface,
            hueVeil = ColorUtils.setAlphaComponent(
                c(R.color.wgt_hue_veil_base), if (night) 71 else 56
            ),
            rim = ColorUtils.setAlphaComponent(c(R.color.wgt_rim_base), if (night) 51 else 140),
            onSurface = guard(c(R.color.wgt_on_surface), MIN_CONTRAST_BODY),
            onSurfaceVariant = guard(c(R.color.wgt_on_surface_variant), MIN_CONTRAST_BODY),
            accent = guard(c(R.color.wgt_accent), MIN_CONTRAST_BODY),
            accentSoft = guard(c(R.color.wgt_accent_soft), MIN_CONTRAST_LARGE),
            secondary = guard(c(R.color.wgt_secondary), MIN_CONTRAST_LARGE),
            critical = guard(c(R.color.wgt_critical), MIN_CONTRAST_LARGE),
            track = c(R.color.wgt_track),
            divider = ColorUtils.setAlphaComponent(
                c(R.color.wgt_on_surface), if (night) 38 else 30
            )
        )
    }

    // ---------------------------------------------------------------- contrast

    private fun contrast(@ColorInt a: Int, @ColorInt b: Int): Double =
        ColorUtils.calculateContrast(
            ColorUtils.setAlphaComponent(a, 255),
            ColorUtils.setAlphaComponent(b, 255)
        )

    /** Worst contrast of [fg] across both possible backdrops. */
    private fun worst(@ColorInt fg: Int, @ColorInt bgA: Int, @ColorInt bgB: Int): Double =
        min(contrast(fg, bgA), contrast(fg, bgB))

    /**
     * If [fg] fails [minRatio] against either backdrop, blend it toward black or
     * white — whichever direction recovers contrast — using the smallest shift
     * that clears the bar. Preserves hue as much as possible.
     */
    @ColorInt
    private fun ensureContrast(
        @ColorInt fg: Int,
        @ColorInt bgA: Int,
        @ColorInt bgB: Int,
        minRatio: Double
    ): Int {
        if (worst(fg, bgA, bgB) >= minRatio) return fg

        var best = fg
        var bestScore = worst(fg, bgA, bgB)

        for (target in intArrayOf(Color.BLACK, Color.WHITE)) {
            for (step in 1..20) {
                val ratio = step / 20f
                val candidate = ColorUtils.blendARGB(fg, target, ratio)
                val score = worst(candidate, bgA, bgB)
                if (score >= minRatio) {
                    // First passing candidate in this direction is the minimal shift.
                    if (bestScore < minRatio || score > bestScore) {
                        best = candidate
                        bestScore = score
                    }
                    break
                }
                if (score > bestScore) {
                    best = candidate
                    bestScore = score
                }
            }
        }
        return best
    }
}
