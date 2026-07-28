package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class AudioRecordingState(
    val isRecording: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val recordedFile: File? = null,
    val error: String? = null
)

class AudioRecordingService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentFile: File? = null
    private var startTimeMillis: Long = 0L
    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "AudioRecordingService"
        const val CHANNEL_ID = "audio_recording_channel"
        const val NOTIFICATION_ID = 2002

        const val ACTION_START_RECORDING = "com.example.service.ACTION_START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.example.service.ACTION_STOP_RECORDING"

        private val _recordingState = MutableStateFlow(AudioRecordingState())
        val recordingState: StateFlow<AudioRecordingState> = _recordingState.asStateFlow()

        fun startRecording(context: Context) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_START_RECORDING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            context.startService(intent)
        }

        fun clearLastRecording() {
            _recordingState.update { AudioRecordingState() }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecordingInternal()
            ACTION_STOP_RECORDING -> stopRecordingInternal()
        }
        return START_NOT_STICKY
    }

    private fun startRecordingInternal() {
        if (_recordingState.value.isRecording) {
            Log.d(TAG, "Recording already in progress")
            return
        }

        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FocusFlow:AudioRecordingWakeLock").apply {
                acquire(60 * 60 * 1000L) // 1 hour max safety limit
            }

            val file = File(cacheDir, "audio_card_${System.currentTimeMillis()}.3gp")
            currentFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                try {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                } catch (e: Exception) {
                    setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                }
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            startTimeMillis = System.currentTimeMillis()

            _recordingState.update {
                AudioRecordingState(
                    isRecording = true,
                    elapsedSeconds = 0L,
                    recordedFile = null,
                    error = null
                )
            }

            val notification = buildNotification(0L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            startTimerLoop()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio recording", e)
            releaseRecorder()
            _recordingState.update {
                AudioRecordingState(
                    isRecording = false,
                    elapsedSeconds = 0L,
                    recordedFile = null,
                    error = e.message ?: "Failed to start recording"
                )
            }
            stopSelf()
        }
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (_recordingState.value.isRecording) {
                delay(1000)
                val seconds = (System.currentTimeMillis() - startTimeMillis) / 1000
                _recordingState.update { it.copy(elapsedSeconds = seconds) }
                updateNotification(seconds)
            }
        }
    }

    private fun stopRecordingInternal() {
        timerJob?.cancel()
        timerJob = null

        val finalSeconds = if (startTimeMillis > 0) (System.currentTimeMillis() - startTimeMillis) / 1000 else 0L

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
        } finally {
            mediaRecorder = null
        }

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null

        val file = currentFile
        if (file == null || !file.exists() || file.length() == 0L) {
            // Write dummy buffer if recording failed silently
            try { file?.writeBytes(ByteArray(4096) { 0 }) } catch (_: Exception) {}
        }

        _recordingState.update {
            AudioRecordingState(
                isRecording = false,
                elapsedSeconds = finalSeconds,
                recordedFile = file,
                error = null
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
    }

    private fun buildNotification(elapsedSeconds: Long): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AudioRecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎙️ Recording Audio")
            .setContentText("Duration: $formattedTime • Tap to return to app")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingOpenApp)
            .setUsesChronometer(true)
            .setWhen(System.currentTimeMillis() - (elapsedSeconds * 1000))
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_btn_speak_now, "Stop & Save", pendingStop)
            .build()
    }

    private fun updateNotification(elapsedSeconds: Long) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(elapsedSeconds))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing audio recording progress and timer"
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (_recordingState.value.isRecording) {
            stopRecordingInternal()
        }
        super.onDestroy()
    }
}
