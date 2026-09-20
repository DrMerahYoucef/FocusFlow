package com.example.ui.components

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import com.example.ui.theme.WallpaperTheme

object WallpaperHelper {

    fun renderForestBitmap(
        context: Context,
        width: Int,
        height: Int,
        theme: WallpaperTheme,
        treeCount: Int = -1
    ): Bitmap {
        val isDark = theme == WallpaperTheme.DARK
        val app = context.applicationContext
        val W = width.coerceAtLeast(1080).toFloat()
        val H = height.coerceAtLeast(1920).toFloat()

        val count = if (treeCount >= 0) {
            treeCount
        } else {
            val sharedPrefs = app.getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE)
            sharedPrefs.getInt("last_synced_tree_count", 0)
        }

        val imageBitmap = ImageBitmap(W.toInt(), H.toInt())
        val composeCanvas = Canvas(imageBitmap)
        val drawScope = CanvasDrawScope()

        drawScope.draw(
            density = androidx.compose.ui.unit.Density(app),
            layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
            canvas = composeCanvas,
            size = Size(W, H)
        ) {
            ForestTreeRenderer.drawModernLandscape(
                drawScope = this,
                W = W,
                H = H,
                treeCount = count,
                darkProgress = if (isDark) 1f else 0f,
                animPhase = 0f
            )
        }

        return imageBitmap.asAndroidBitmap()
    }

    fun setForestWallpaper(
        context: Context,
        theme: WallpaperTheme,
        setHomeScreen: Boolean,
        setLockScreen: Boolean,
        treeCount: Int = -1,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val app = context.applicationContext
        val wallpaperManager = WallpaperManager.getInstance(app)

        if (!setHomeScreen && !setLockScreen) {
            onComplete(false, "No screen selected")
            return
        }

        try {
            val metrics = app.resources.displayMetrics
            val W = metrics.widthPixels.coerceAtLeast(1080)
            val H = metrics.heightPixels.coerceAtLeast(1920)

            val bitmap = renderForestBitmap(context, W, H, theme, treeCount)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (setHomeScreen && setLockScreen) {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                } else if (setHomeScreen) {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                } else if (setLockScreen) {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                }
            } else {
                wallpaperManager.setBitmap(bitmap)
            }

            onComplete(true, null)
        } catch (e: Exception) {
            android.util.Log.e("WallpaperHelper", "Failed to set wallpaper", e)
            onComplete(false, e.localizedMessage ?: "Unknown error")
        }
    }
}
