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
    private var ambientRotationMin = 5
    private var ambientSecondsElapsed = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        loadSettings()
        
        // Push initial state
        val prefs = getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE)
        val savedAmbient = prefs.getString("selected_ambient_id", "none") ?: "none"
        _state.update {
            it.copy(
                remainingMs = focusDurationMs,
                focusDurationMs = focusDurationMs,
                shortBreakDurationMs = shortBreakDurationMs,
                longBreakDurationMs = longBreakDurationMs,
                sessionsBeforeLongBreak = sessionsBeforeLongBreak,
                currentAmbientId = savedAmbient
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
            ACTION_SET_AMBIENT -> {
                val ambientId = intent.getStringExtra(EXTRA_AMBIENT_ID) ?: "none"
                setAmbientSoundInternal(ambientId)
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
        ambientRotationMin = prefs.getInt("ambient_rotation_min", 5)
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
        syncAmbientPlayback()
    }

    private fun pauseTimer() {
        companionIsRunning = false
        countdownTimer?.cancel()
        _state.update { it.copy(isRunning = false) }
        updateNotification()
        syncAmbientPlayback()
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
        syncAmbientPlayback()
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
                
                if (_state.value.currentAmbientId != "none") {
                    ambientSecondsElapsed++
                    val rotationPeriodSecs = ambientRotationMin * 60L
                    if (ambientSecondsElapsed >= rotationPeriodSecs) {
                        ambientSecondsElapsed = 0L
                        val sounds = listOf("rain", "white_noise", "campfire", "stream", "space")
                        val currentId = _state.value.currentAmbientId
                        val nextSounds = sounds.filter { it != currentId }
                        val nextId = if (nextSounds.isNotEmpty()) nextSounds.random() else currentId
                        setAmbientSoundInternal(nextId)
                    }
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

    private var exoPlayer: androidx.media3.exoplayer.ExoPlayer? = null

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps    = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun getOrCreatePlayer(): androidx.media3.exoplayer.ExoPlayer {
        val currentExo = exoPlayer
        if (currentExo == null) {
            val newExo = androidx.media3.exoplayer.ExoPlayer.Builder(this)
                .setAudioAttributes(
                    androidx.media3.common.AudioAttributes.Builder()
                        .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                        .build(),
                    /* handleAudioFocus = */ true
                )
                .setHandleAudioBecomingNoisy(true)
                .build().apply {
                    repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
                    addListener(object : androidx.media3.common.Player.Listener {
                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            android.util.Log.e("Ambient", "Primary URL failed: ${error.message}, trying fallback")
                            val currentId = _state.value.currentAmbientId
                            val fallbackUrl = getAmbientFallbackUrl(currentId)
                            if (fallbackUrl != null) {
                                try {
                                    val fallbackMediaItem = androidx.media3.common.MediaItem.Builder()
                                        .setMediaId(currentId)
                                        .setUri(fallbackUrl)
                                        .setMediaMetadata(
                                            androidx.media3.common.MediaMetadata.Builder()
                                                .setTitle(getAmbientName(currentId))
                                                .setArtist("Ambient Flow (Fallback)")
                                                .build()
                                        )
                                        .build()
                                    setMediaItem(fallbackMediaItem)
                                    prepare()
                                    play()
                                } catch (e: Exception) {
                                    android.util.Log.e("Ambient", "Fallback also failed: ${e.message}")
                                }
                            }
                        }
                    })
                }
            exoPlayer = newExo
            return newExo
        }
        return currentExo
    }

    private fun setAmbientSoundInternal(ambientId: String) {
        val currentId = _state.value.currentAmbientId
        val player = exoPlayer
        
        ambientSecondsElapsed = 0L // Reset rotation progress on any manual or auto sound change
        
        if (ambientId == "none") {
            if (player != null && player.isPlaying) {
                serviceScope.launch(Dispatchers.Main) {
                    val steps = 15
                    val fadeDurationMs = 1000L
                    val stepDelay = fadeDurationMs / steps
                    for (i in 0..steps) {
                        val volume = 1f - (i.toFloat() / steps)
                        player.volume = volume
                        kotlinx.coroutines.delay(stepDelay)
                    }
                    player.pause()
                    _state.update { it.copy(currentAmbientId = "none") }
                    val prefs = getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("selected_ambient_id", "none").apply()
                }
            } else {
                _state.update { it.copy(currentAmbientId = "none") }
                val prefs = getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("selected_ambient_id", "none").apply()
                syncAmbientPlayback()
            }
        } else if (currentId != "none" && currentId != ambientId && player != null && player.isPlaying) {
            // Fade transition between two active sounds
            fadeToSound(ambientId)
        } else {
            // Standard start (no active player, or same sound)
            _state.update { it.copy(currentAmbientId = ambientId) }
            val prefs = getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("selected_ambient_id", ambientId).apply()
            syncAmbientPlayback()
            exoPlayer?.volume = 1f // ensure volume is reset to 1f
        }
    }

    private fun fadeToSound(nextId: String) {
        serviceScope.launch(Dispatchers.Main) {
            val player = getOrCreatePlayer()
            val steps = 20
            val fadeDurationMs = 1500L
            val stepDelay = fadeDurationMs / steps
            
            // 1. Fade Out
            for (i in 0..steps) {
                val volume = 1f - (i.toFloat() / steps)
                player.volume = volume
                kotlinx.coroutines.delay(stepDelay)
            }
            
            player.pause()
            
            _state.update { it.copy(currentAmbientId = nextId) }
            val prefs = getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("selected_ambient_id", nextId).apply()
            
            val url = getAmbientUrl(nextId)
            if (url != null) {
                val mediaItem = androidx.media3.common.MediaItem.Builder()
                    .setMediaId(nextId)
                    .setUri(url)
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(getAmbientName(nextId))
                            .setArtist("Ambient Flow")
                            .build()
                    )
                    .build()
                player.setMediaItem(mediaItem)
                player.prepare()
                if (_state.value.isRunning) {
                    player.play()
                }
            }
            
            // 2. Fade In
            for (i in 0..steps) {
                val volume = i.toFloat() / steps
                player.volume = volume
                kotlinx.coroutines.delay(stepDelay)
            }
            player.volume = 1f
        }
    }

    private fun syncAmbientPlayback() {
        val ambientId = _state.value.currentAmbientId
        val timerIsRunning = _state.value.isRunning

        if (ambientId == "none" || !timerIsRunning) {
            exoPlayer?.pause()
        } else {
            if (!isNetworkAvailable()) {
                android.util.Log.w("Ambient", "No network — skipping ambient sound")
                exoPlayer?.pause()
                return
            }
            val player = getOrCreatePlayer()
            val url = getAmbientUrl(ambientId)
            if (url != null) {
                val currentMediaItem = player.currentMediaItem
                if (currentMediaItem?.mediaId == ambientId) {
                    if (!player.isPlaying) {
                        player.play()
                    }
                } else {
                    val mediaItem = androidx.media3.common.MediaItem.Builder()
                        .setMediaId(ambientId)
                        .setUri(url)
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(getAmbientName(ambientId))
                                .setArtist("Ambient Flow")
                                .build()
                        )
                        .build()
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                }
            } else {
                exoPlayer?.pause()
            }
        }
    }

    private fun getAmbientUrl(id: String): String? {
        return when (id) {
            // 🌧️ Rain — Zapsplat CDN public stream (reliable loop)
            "rain" -> "https://www.soundjay.com/nature/rain-01.mp3"

            // 🤍 White Noise — SomaFM noise channel (same server as space, proven working)
            "white_noise" -> "https://ice1.somafm.com/darkzone-128-mp3"

            // 🔥 Campfire — Internet Archive direct MP3 (no hotlink block)
            "campfire" -> "https://ia800301.us.archive.org/5/items/CampfireSounds/campfire.mp3"

            // 🌊 Forest Stream — Internet Archive direct MP3
            "stream" -> "https://ia800204.us.archive.org/11/items/foreststream/forest_stream.mp3"

            // 🌌 Deep Space — SomaFM (already working, keep as-is)
            "space" -> "https://ice1.somafm.com/deepspaceone-128-mp3"

            else -> null
        }
    }

    private fun getAmbientFallbackUrl(id: String): String? {
        return when (id) {
            // Rain fallback — SomaFM ambient channel
            "rain" -> "https://ice1.somafm.com/thistle-128-mp3"

            // White noise fallback — SomaFM drone zone
            "white_noise" -> "https://ice1.somafm.com/dronezone-128-mp3"

            // Campfire fallback — SomaFM boot liquor (warm country feel)
            "campfire" -> "https://ice1.somafm.com/bootliquor-128-mp3"

            // Stream fallback — SomaFM suburbs of goa (nature ambient)
            "stream" -> "https://ice1.somafm.com/suburbsofgoa-128-mp3"

            // Space fallback — SomaFM mission control
            "space" -> "https://ice1.somafm.com/missioncontrol-128-mp3"

            else -> null
        }
    }

    private fun getAmbientName(id: String): String {
        return when (id) {
            "rain" -> "Rain on Roof"
            "white_noise" -> "White Noise"
            "campfire" -> "Cozy Campfire"
            "stream" -> "Forest Stream"
            "space" -> "Space Drone"
            else -> "None"
        }
    }

    override fun onDestroy() {
        stopTimerEngine()
        exoPlayer?.let {
            it.stop()
            it.release()
            exoPlayer = null
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_SKIP = "ACTION_SKIP"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_SET_AMBIENT = "ACTION_SET_AMBIENT"
        const val EXTRA_AMBIENT_ID = "EXTRA_AMBIENT_ID"
        
        private const val NOTIF_ID = 1010
        private const val CHANNEL_ID = "focusflow_timer_channel"

        private val _state = MutableStateFlow(TimerState())
        val state: StateFlow<TimerState> = _state.asStateFlow()

        private var companionIsRunning = false
        val isRunning: Boolean get() = companionIsRunning
    }
}
