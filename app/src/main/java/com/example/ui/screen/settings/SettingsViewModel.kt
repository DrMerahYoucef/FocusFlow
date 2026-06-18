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
    val vibrateOnComplete: Boolean = true
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
                vibrateOnComplete = sharedPrefs.getBoolean("vibrate_on_complete", true)
            )
        }
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
            database.clearAllTables()
            onCompleted()
        }
    }
}
