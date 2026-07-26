package com.example.service

import android.content.Context
import android.media.MediaRecorder
import java.io.File
import java.util.UUID

class AudioCaptureManager(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var tempFile: File? = null

    fun startRecording(): File {
        val file = File(context.cacheDir, "rec_${UUID.randomUUID()}.m4a")
        tempFile = file
        recorder = @Suppress("DEPRECATION") (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioChannels(1)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(64_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        return file
    }

    fun stopRecording(): File {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioCaptureManager", "Error stopping recording", e)
        } finally {
            recorder = null
        }
        return tempFile ?: error("No active recording")
    }
}
