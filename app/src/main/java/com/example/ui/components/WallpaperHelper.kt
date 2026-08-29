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

        val resId = if (isDark) R.drawable.open_meadow_dark else R.drawable.open_meadow_light
        val bgBitmap = BitmapFactory.decodeResource(app.resources, resId)

        val tree1Light = BitmapFactory.decodeResource(app.resources, R.drawable.tree_1_light).asImageBitmap()
        val tree1Dark = BitmapFactory.decodeResource(app.resources, R.drawable.tree_1_dark).asImageBitmap()
        val tree2Light = BitmapFactory.decodeResource(app.resources, R.drawable.tree_2_light).asImageBitmap()
        val tree2Dark = BitmapFactory.decodeResource(app.resources, R.drawable.tree_2_dark).asImageBitmap()
        val tree3Light = BitmapFactory.decodeResource(app.resources, R.drawable.tree_3_light).asImageBitmap()
        val tree3Dark = BitmapFactory.decodeResource(app.resources, R.drawable.tree_3_dark).asImageBitmap()
        val tree4Light = BitmapFactory.decodeResource(app.resources, R.drawable.tree_4_light).asImageBitmap()
        val tree4Dark = BitmapFactory.decodeResource(app.resources, R.drawable.tree_4_dark).asImageBitmap()

        val treeBitmaps = ForestTreeBitmaps(
            tree1Light = tree1Light,
            tree1Dark = tree1Dark,
            tree2Light = tree2Light,
            tree2Dark = tree2Dark,
            tree3Light = tree3Light,
            tree3Dark = tree3Dark,
            tree4Light = tree4Light,
            tree4Dark = tree4Dark
        )

        val imageBitmap = ImageBitmap(W.toInt(), H.toInt())
        val composeCanvas = Canvas(imageBitmap)
        val drawScope = CanvasDrawScope()

        drawScope.draw(
            density = androidx.compose.ui.unit.Density(app),
            layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
            canvas = composeCanvas,
            size = Size(W, H)
        ) {
            // 1. Draw Base Clean Open Meadow Artwork (Aspect-fill cropping, zero distortion or tiling)
            if (bgBitmap != null) {
                ForestTreeRenderer.drawCropBitmap(this, bgBitmap.asImageBitmap(), W, H, 1f)
            }

            // 2. Dynamic Canvas Tree Sprites Stamped at Fixed Coordinate Slots (0 trees drawn when count == 0)
            ForestTreeRenderer.drawDynamicForestTrees(
                drawScope = this,
                treeBitmaps = treeBitmaps,
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
