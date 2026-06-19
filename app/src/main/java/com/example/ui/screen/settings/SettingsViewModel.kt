package com.example.ui.screen.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusFlowApplication
import com.example.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SettingsState(
    val focusMin: Int = 25,
    val shortBreakMin: Int = 5,
    val longBreakMin: Int = 15,
    val sessionsBeforeLong: Int = 4,
    val blockNotifications: Boolean = true,
    val vibrateOnComplete: Boolean = true,
    val themeMode: String = "system"
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE)
    private val sessionRepo = FocusFlowApplication.instance.sessionRepository
    private val database = FocusFlowApplication.instance.database

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _state.update {
            SettingsState(
                focusMin = sharedPrefs.getInt("focus_duration_min", 25),
                shortBreakMin = sharedPrefs.getInt("short_break_min", 5),
                longBreakMin = sharedPrefs.getInt("long_break_min", 15),
                sessionsBeforeLong = sharedPrefs.getInt("sessions_before_long", 4),
                blockNotifications = sharedPrefs.getBoolean("block_notifications", true),
                vibrateOnComplete = sharedPrefs.getBoolean("vibrate_on_complete", true),
                themeMode = sharedPrefs.getString("theme_mode", "system") ?: "system"
            )
        }
    }

    fun updateThemeMode(value: String) {
        sharedPrefs.edit().putString("theme_mode", value).apply()
        _state.update { it.copy(themeMode = value) }
        // Let's also trigger an app widget update so the widget theme matches the app selection immediately
        com.example.widget.ExamCountdownWidgetReceiver.triggerWidgetUpdate(getApplication())
    }

    fun updateFocusMin(value: Int) {
        sharedPrefs.edit().putInt("focus_duration_min", value).apply()
        _state.update { it.copy(focusMin = value) }
    }

    fun updateShortBreakMin(value: Int) {
        sharedPrefs.edit().putInt("short_break_min", value).apply()
        _state.update { it.copy(shortBreakMin = value) }
    }

    fun updateLongBreakMin(value: Int) {
        sharedPrefs.edit().putInt("long_break_min", value).apply()
        _state.update { it.copy(longBreakMin = value) }
    }

    fun updateSessionsBeforeLong(value: Int) {
        sharedPrefs.edit().putInt("sessions_before_long", value).apply()
        _state.update { it.copy(sessionsBeforeLong = value) }
    }

    fun updateBlockNotifications(value: Boolean) {
        sharedPrefs.edit().putBoolean("block_notifications", value).apply()
        _state.update { it.copy(blockNotifications = value) }
    }

    fun updateVibrateOnComplete(value: Boolean) {
        sharedPrefs.edit().putBoolean("vibrate_on_complete", value).apply()
        _state.update { it.copy(vibrateOnComplete = value) }
    }

    fun exportSessionsAsCsv(onCompleted: (String) -> Unit) {
        viewModelScope.launch {
            val sessions = sessionRepo.getAllSessions().first()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val sb = StringBuilder()
            sb.append("ID,Timestamp,Date_Readable,Duration_Seconds,Completed,Points_Earned\n")
            
            sessions.forEach { s ->
                val dateStr = sdf.format(Date(s.date))
                sb.append("${s.id},${s.date},$dateStr,${s.durationSeconds},${s.completed},${s.focusScore}\n")
            }
            onCompleted(sb.toString())
        }
    }

    fun resetAllData(onCompleted: () -> Unit) {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                database.clearAllTables()
            }
            onCompleted()
        }
    }

    fun seed100Sessions(onCompleted: () -> Unit) {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val now = System.currentTimeMillis()
                for (i in 1..100) {
                    // Spread over past 10 days, 10 sessions per day
                    val dayOffset = (i - 1) / 10
                    val timeOffset = ((i - 1) % 10) * 60 * 60 * 1000L // 1 hour apart
                    val date = now - (dayOffset * 24 * 60 * 60 * 1000L) - timeOffset - (15 * 60 * 1000L)
                    val duration = 1500 // 25 minutes = 1500 seconds
                    database.sessionDao().insert(
                        SessionEntity(
                            date = date,
                            durationSeconds = duration,
                            completed = true,
                            focusScore = duration / 60
                        )
                    )
                }
            }
            onCompleted()
        }
    }
}
