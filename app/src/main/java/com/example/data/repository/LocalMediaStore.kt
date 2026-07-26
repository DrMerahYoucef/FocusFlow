package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.util.UUID

class LocalMediaStore(private val context: Context) {
    private val mediaDir: File
        get() = File(context.filesDir, "revision_media").apply { mkdirs() }

    private val maxPhotoDimension = 1600
    private val jpegQuality = 82

    fun savePhoto(bitmap: Bitmap): String {
        val scaled = downscaleToMaxDimension(bitmap, maxPhotoDimension)
        val file = File(mediaDir, "img_${UUID.randomUUID()}.jpg")
        file.outputStream().use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out) }
        if (scaled !== bitmap) scaled.recycle()
        return file.absolutePath
    }

    private fun downscaleToMaxDimension(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longestSide = maxOf(bitmap.width, bitmap.height)
        if (longestSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / longestSide
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun saveAudioFrom(tempRecordingFile: File): String {
        val file = File(mediaDir, "audio_${UUID.randomUUID()}.m4a")
        tempRecordingFile.copyTo(file, overwrite = true)
        tempRecordingFile.delete()
        return file.absolutePath
    }

    fun delete(path: String) {
        if (path.isBlank()) return
        File(path).takeIf { it.exists() }?.delete()
    }
}
