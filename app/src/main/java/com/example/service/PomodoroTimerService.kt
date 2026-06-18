package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.FocusFlowApplication
import com.example.MainActivity
import com.example.R
import com.example.data.db.entity.SessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PomodoroTimerService : Service() {

    private var countdownTimer: CountDownTimer? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    // Cached settings (loaded from SharedPreferences)
    private var focusDurationMs = 25 * 60 * 1000L
    private var shortBreakDurationMs = 5 * 60 * 1000L
    private var longBreakDurationMs = 15 * 60 * 1000L
    private var sessionsBeforeLongBreak = 4
    private var dndEnabled = true
    private var vibrateEnabled = true

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        loadSettings()
        
        // Push initial state
        _state.update {
            it.copy(
                remainingMs = focusDurationMs,
                focusDurationMs = focusDurationMs,
                shortBreakDurationMs = shortBreakDurationMs,
                longBreakDurationMs = longBreakDurationMs,
                sessionsBeforeLongBreak = sessionsBeforeLongBreak
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        loadSettings() // load fresh user preferences just in case they modified settings

        when (intent?.action) {
            ACTION_START -> {
                if (!companionIsRunning) {
                    startTimer()
                }
            }
            ACTION_PAUSE -> {
                pauseTimer()
            }
            ACTION_RESUME -> {
                resumeTimer()
            }
            ACTION_SKIP -> {
                skipPhase()
            }
            ACTION_STOP -> {
                stopTimerEngine()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE)
        val focusMin = prefs.getInt("focus_duration_min", 25)
        val shortMin = prefs.getInt("short_break_min", 5)
        val longMin = prefs.getInt("long_break_min", 15)
        
        focusDurationMs = focusMin * 60 * 1000L
        shortBreakDurationMs = shortMin * 60 * 1000L
        longBreakDurationMs = longMin * 60 * 1000L
        sessionsBeforeLongBreak = prefs.getInt("sessions_before_long", 4)
        dndEnabled = prefs.getBoolean("block_notifications", true)
        vibrateEnabled = prefs.getBoolean("vibrate_on_complete", true)
    }

    private fun startTimer() {
        companionIsRunning = true
        val currentRemaining = _state.value.remainingMs
        launchCountdown(currentRemaining)
        startForeground(NOTIF_ID, buildTimerNotification())
        
        if (_state.value.phase == Phase.FOCUS) {
            if (dndEnabled) {
                blockNotifications(true)
            }
            AppMonitorService.start(this)
            FocusNotificationListenerService.refresh(this)
        }
    }

    private fun pauseTimer() {
        companionIsRunning = false
        countdownTimer?.cancel()
        _state.update { it.copy(isRunning = false) }
        updateNotification()
    }

    private fun resumeTimer() {
        startTimer()
    }

    private fun skipPhase() {
        countdownTimer?.cancel()
        
        // Record as uncompleted session if we were in Focus phase
        if (_state.value.phase == Phase.FOCUS) {
            val focusSecondsCompleted = ((focusDurationMs - _state.value.remainingMs) / 1000).toInt()
            if (focusSecondsCompleted > 30) { // save partial focus attempt if more than 30s
                saveSessionToDb(durationSecs = focusSecondsCompleted, completed = false)
            }
            if (dndEnabled) {
                blockNotifications(false)
            }
            AppMonitorService.stop(this)
        }

        advancePhase()
    }

    private fun stopTimerEngine() {
        companionIsRunning = false
        countdownTimer?.cancel()
        if (dndEnabled) {
            blockNotifications(false)
        }
        AppMonitorService.stop(this)
        _state.update {
            it.copy(
                isRunning = false,
                remainingMs = focusDurationMs,
                phase = Phase.FOCUS
            )
        }
    }

    private fun launchCountdown(durationMs: Long) {
        countdownTimer?.cancel()
        
        _state.update { it.copy(isRunning = true) }
        
        countdownTimer = object : CountDownTimer(durationMs, 1000L) {
            override fun onTick(millisLeft: Long) {
                val totalFocusDiff = if (_state.value.phase == Phase.FOCUS) 1L else 0L

                _state.update {
                    it.copy(
                        remainingMs = millisLeft,
                        isRunning = true,
                        totalFocusSecs = it.totalFocusSecs + totalFocusDiff
                    )
                }
                updateNotification()
            }

            override fun onFinish() {
                onPhaseFinished()
            }
        }.start()
    }

    private fun onPhaseFinished() {
        countdownTimer?.cancel()
        val currentPhase = _state.value.phase

        if (currentPhase == Phase.FOCUS) {
            // Focus session completed! Save with 100% completions
            val completedSeconds = (focusDurationMs / 1000).toInt()
            saveSessionToDb(durationSecs = completedSeconds, completed = true)
            
            val updatedCount = _state.value.sessionCount + 1
            _state.update { it.copy(sessionCount = updatedCount) }
            
            if (dndEnabled) {
                blockNotifications(false)
            }
            AppMonitorService.stop(this)
            
            vibrateDevice()
            playAlertSound()
        } else {
            vibrateDevice()
            playAlertSound()
        }

        advancePhase()
    }

    private fun advancePhase() {
        val nextPhase = when (_state.value.phase) {
            Phase.FOCUS -> {
                val count = _state.value.sessionCount
                if (count > 0 && count % sessionsBeforeLongBreak == 0) {
                    Phase.LONG_BREAK
                } else {
                    Phase.SHORT_BREAK
                }
            }
            Phase.SHORT_BREAK, Phase.LONG_BREAK -> {
                Phase.FOCUS
            }
        }

        val nextDuration = when (nextPhase) {
            Phase.FOCUS -> focusDurationMs
            Phase.SHORT_BREAK -> shortBreakDurationMs
            Phase.LONG_BREAK -> longBreakDurationMs
        }

        _state.update {
            it.copy(
                phase = nextPhase,
                remainingMs = nextDuration,
                isRunning = companionIsRunning
            )
        }

        if (companionIsRunning) {
            startTimer()
        } else {
            updateNotification()
        }
    }

    private fun saveSessionToDb(durationSecs: Int, completed: Boolean) {
        if (durationSecs <= 0) return
        val score = durationSecs / 60 // 1 point per focus minute
        val session = SessionEntity(
            date = System.currentTimeMillis(),
            durationSeconds = durationSecs,
            completed = completed,
            focusScore = score
        )
        serviceScope.launch {
            FocusFlowApplication.instance.sessionRepository.insert(session)
        }
    }

    private fun vibrateDevice() {
        if (!vibrateEnabled) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(200)
            }
        }
    }

    private fun playAlertSound() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notificationUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun blockNotifications(block: Boolean) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.isNotificationPolicyAccessGranted) {
            try {
                nm.setInterruptionFilter(
                    if (block) NotificationManager.INTERRUPTION_FILTER_NONE
                    else NotificationManager.INTERRUPTION_FILTER_ALL
                )
                _state.update { it.copy(isDndActive = block) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Notification handling
    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timerChannel = NotificationChannel(
                CHANNEL_ID,
                "Timer Running",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows countdown while FocusFlow is active"
                setShowBadge(false)
            }
            nm.createNotificationChannel(timerChannel)
        }
    }

    private fun buildTimerNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val minutes = (_state.value.remainingMs / 1000) / 60
        val seconds = (_state.value.remainingMs / 1000) % 60
        val timeStr = String.format("%02d:%02d", minutes, seconds)
        val titleText = when (_state.value.phase) {
            Phase.FOCUS -> "Focused Work"
            Phase.SHORT_BREAK -> "Short Break Time"
            Phase.LONG_BREAK -> "Long Break Time"
        }

        val dndStatus = if (_state.value.isDndActive) " 🔕 DND Enabled" else ""

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$titleText: $timeStr")
            .setContentText("Focus Completed: ${_state.value.sessionCount} session(s)$dndStatus")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (companionIsRunning) {
            nm.notify(NOTIF_ID, buildTimerNotification())
        }
    }

    override fun onDestroy() {
        stopTimerEngine()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_SKIP = "ACTION_SKIP"
        const val ACTION_STOP = "ACTION_STOP"
        
        private const val NOTIF_ID = 1010
        private const val CHANNEL_ID = "focusflow_timer_channel"

        private val _state = MutableStateFlow(TimerState())
        val state: StateFlow<TimerState> = _state.asStateFlow()

        private var companionIsRunning = false
        val isRunning: Boolean get() = companionIsRunning
    }
}
