package com.example.ui.screen.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusFlowApplication
import com.example.data.db.entity.SessionEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.theme.WallpaperTheme

data class SettingsState(
    val focusMin: Int = 25,
    val shortBreakMin: Int = 5,
    val longBreakMin: Int = 15,
    val sessionsBeforeLong: Int = 4,
    val blockNotifications: Boolean = false,
    val vibrateOnComplete: Boolean = true,
    val themeMode: String = "system",
    val followSystemTheme: Boolean = true,
    val wallpaperTheme: WallpaperTheme = WallpaperTheme.LIGHT,
    val autoSyncWallpaper: Boolean = false,
    val wallpaperHomeScreen: Boolean = true,
    val wallpaperLockScreen: Boolean = false,
    val ambientRotationMin: Int = 5,
    val swipeToNavigate: Boolean = true
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
        val savedThemeStr = sharedPrefs.getString("wallpaper_theme", "LIGHT") ?: "LIGHT"
        val savedTheme = try {
            WallpaperTheme.valueOf(savedThemeStr)
        } catch (e: Exception) {
            WallpaperTheme.LIGHT
        }

        _state.update {
            SettingsState(
                focusMin = sharedPrefs.getInt("focus_duration_min", 25),
                shortBreakMin = sharedPrefs.getInt("short_break_min", 5),
                longBreakMin = sharedPrefs.getInt("long_break_min", 15),
                sessionsBeforeLong = sharedPrefs.getInt("sessions_before_long", 4),
                blockNotifications = sharedPrefs.getBoolean("block_notifications", false),
                vibrateOnComplete = sharedPrefs.getBoolean("vibrate_on_complete", true),
                themeMode = sharedPrefs.getString("theme_mode", "system") ?: "system",
                followSystemTheme = sharedPrefs.getBoolean("follow_system_theme", true),
                wallpaperTheme = savedTheme,
                autoSyncWallpaper = sharedPrefs.getBoolean("auto_sync_wallpaper", false),
                wallpaperHomeScreen = sharedPrefs.getBoolean("wallpaper_home_screen", true),
                wallpaperLockScreen = sharedPrefs.getBoolean("wallpaper_lock_screen", false),
                ambientRotationMin = sharedPrefs.getInt("ambient_rotation_min", 5),
                swipeToNavigate = sharedPrefs.getBoolean("swipe_to_navigate", true)
            )
        }
    }

    fun updateAmbientRotationMin(value: Int) {
        sharedPrefs.edit().putInt("ambient_rotation_min", value).apply()
        _state.update { it.copy(ambientRotationMin = value) }
    }

    fun updateThemeMode(value: String) {
        sharedPrefs.edit().putString("theme_mode", value).apply()
        _state.update { it.copy(themeMode = value) }
        // Let's also trigger an app widget update so the widget theme matches the app selection immediately
        com.example.widget.ExamCountdownWidgetReceiver.triggerWidgetUpdate(getApplication())
    }

    fun updateFollowSystemTheme(value: Boolean) {
        sharedPrefs.edit().putBoolean("follow_system_theme", value).apply()
        _state.update { it.copy(followSystemTheme = value) }
    }

    fun updateWallpaperTheme(value: WallpaperTheme) {
        sharedPrefs.edit().putString("wallpaper_theme", value.name).apply()
        _state.update { it.copy(wallpaperTheme = value) }
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

    fun updateAutoSyncWallpaper(value: Boolean) {
        sharedPrefs.edit().putBoolean("auto_sync_wallpaper", value).apply()
        _state.update { it.copy(autoSyncWallpaper = value) }
    }

    fun updateWallpaperHomeScreen(value: Boolean) {
        sharedPrefs.edit().putBoolean("wallpaper_home_screen", value).apply()
        _state.update { it.copy(wallpaperHomeScreen = value) }
    }

    fun updateWallpaperLockScreen(value: Boolean) {
        sharedPrefs.edit().putBoolean("wallpaper_lock_screen", value).apply()
        _state.update { it.copy(wallpaperLockScreen = value) }
    }

    fun updateSwipeToNavigate(value: Boolean) {
        sharedPrefs.edit().putBoolean("swipe_to_navigate", value).apply()
        _state.update { it.copy(swipeToNavigate = value) }
    }


    fun resetAllData(onCompleted: () -> Unit) {
        viewModelScope.launch {
            try {
                val uid = com.google.firebase.Firebase.auth.currentUser?.uid
                if (uid != null) {
                    val firestore = com.google.firebase.Firebase.firestore
                    
                    // 1. Reset user profile stats to zero on Firestore (both users collection and leaderboard)
                    val statsReset = mapOf(
                        "treeCount" to 0,
                        "totalMinutes" to 0,
                        "points" to 0,
                        "currentStreak" to 0
                    )
                    firestore.collection("users").document(uid).update(statsReset).await()
                    firestore.collection("leaderboard").document(uid).update(statsReset).await()

                    // 2. Delete all remote sessions in Firestore sessions subcollection
                    val sessionsRef = firestore.collection("users").document(uid).collection("sessions")
                    val querySnapshot = sessionsRef.get().await()
                    for (doc in querySnapshot.documents) {
                        sessionsRef.document(doc.id).delete().await()
                    }
                }
            } catch (e: Exception) {
                // Log and continue to local purge so user is not blocked if offline
                android.util.Log.e("SettingsViewModel", "Failed to clear Firestore stats, proceeding with local clear", e)
            }

            // 3. Clear local SQLite Database
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                database.clearAllTables()
            }
            
            // Trigger app widget update to show 0 trees on home screen as well
            com.example.widget.ExamCountdownWidgetReceiver.triggerWidgetUpdate(getApplication())
            
            onCompleted()
        }
    }

    fun deleteAccount(onCompleted: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val user = com.google.firebase.Firebase.auth.currentUser
                val uid = user?.uid
                if (uid != null) {
                    val firestore = com.google.firebase.Firebase.firestore
                    
                    // 1. Delete all remote sessions in Firestore
                    val sessionsRef = firestore.collection("users").document(uid).collection("sessions")
                    val querySnapshot = sessionsRef.get().await()
                    for (doc in querySnapshot.documents) {
                        sessionsRef.document(doc.id).delete().await()
                    }
                    
                    // 2. Delete user profile and leaderboard documents
                    firestore.collection("users").document(uid).delete().await()
                    firestore.collection("leaderboard").document(uid).delete().await()
                    
                    // 3. Delete Firebase Auth User Account
                    user.delete().await()
                }
                
                // 4. Wipe local databases
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    database.clearAllTables()
                }
                
                // 5. Reset local preference configurations
                sharedPrefs.edit().clear().apply()
                
                // Update widgets after account wipe
                com.example.widget.ExamCountdownWidgetReceiver.triggerWidgetUpdate(getApplication())
                
                onCompleted(true, null)
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to delete account completely", e)
                onCompleted(false, e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }
}
