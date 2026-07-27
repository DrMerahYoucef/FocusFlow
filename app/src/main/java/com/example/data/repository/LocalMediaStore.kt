package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.util.UUID

enum class LocalCardMediaType { IMAGE, AUDIO }

class LocalMediaStore(private val context: Context) {
    private val mediaDir: File
        get() = File(context.filesDir, "revision_media").apply { mkdirs() }

    private val maxPhotoDimension = 1600
    private val jpegQuality = 82

    fun savePhoto(bitmap: Bitmap): String {
        val longestSide = maxOf(bitmap.width, bitmap.height)
        val scaled = if (longestSide <= maxPhotoDimension) bitmap else {
            val scale = maxPhotoDimension.toFloat() / longestSide
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        }
        val file = File(mediaDir, "img_${UUID.randomUUID()}.jpg")
        file.outputStream().use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out) }
        if (scaled !== bitmap) scaled.recycle()
        return file.absolutePath
    }

    fun saveAudioFrom(tempRecordingFile: File): String {
        val file = File(mediaDir, "audio_${UUID.randomUUID()}.m4a")
        tempRecordingFile.copyTo(file, overwrite = true)
        tempRecordingFile.delete()
        return file.absolutePath
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        File(path).takeIf { it.exists() }?.delete()
    }
}
