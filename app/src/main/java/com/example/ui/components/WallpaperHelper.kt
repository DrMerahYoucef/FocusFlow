package com.example.ui.components

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import com.example.R
import com.example.ui.theme.WallpaperTheme

object WallpaperHelper {

    fun renderBuildingBitmap(
        context: Context,
        width: Int,
        height: Int,
        theme: WallpaperTheme,
        completedSessions: Int = -1
    ): Bitmap {
        val app = context.applicationContext
        val W = width.coerceAtLeast(1080).toFloat()
        val H = height.coerceAtLeast(1920).toFloat()

        val count = if (completedSessions >= 0) {
            completedSessions
        } else {
            val sharedPrefs = app.getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE)
            sharedPrefs.getInt(
                "last_synced_completed_sessions",
                sharedPrefs.getInt("last_synced_tree_count", 0)
            )
        }
        val windowSeed = app.getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE)
            .getLong("building_window_seed", 0L)
        val windowOrder = BuildingBackgroundRenderer.shuffledWindowOrder(windowSeed)
        val litWindows = windowOrder
            .take(count.coerceIn(0, windowOrder.size))
            .toSet()

        val buildingBitmap = BitmapFactory.decodeResource(app.resources, R.drawable.building_background_skycrap)

        val imageBitmap = ImageBitmap(W.toInt(), H.toInt())
        val composeCanvas = Canvas(imageBitmap)
        val drawScope = CanvasDrawScope()

        drawScope.draw(
            density = androidx.compose.ui.unit.Density(app),
            layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
            canvas = composeCanvas,
            size = Size(W, H)
        ) {
            if (buildingBitmap != null) {
                BuildingBackgroundRenderer.run {
                    drawBuilding(
                        image = buildingBitmap.asImageBitmap(),
                        completedSessions = count,
                        darkProgress = 0f,
                        windowSeed = windowSeed,
                        litWindows = litWindows
                    )
                }
            }
        }

        return imageBitmap.asAndroidBitmap()
    }

    fun setBuildingWallpaper(
        context: Context,
        theme: WallpaperTheme,
        setHomeScreen: Boolean,
        setLockScreen: Boolean,
        completedSessions: Int = -1,
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

            val bitmap = renderBuildingBitmap(context, W, H, theme, completedSessions)

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
