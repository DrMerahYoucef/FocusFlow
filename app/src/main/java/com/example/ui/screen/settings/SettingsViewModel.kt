package com.example.ui.screen.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusFlowApplication
import com.example.data.db.entity.SessionEntity
import com.example.service.AppUpdateManager
import com.example.service.UpdateState
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
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

data class SettingsState(
    val focusMin: Int = 25,
    val shortBreakMin: Int = 5,
    val longBreakMin: Int = 15,
    val sessionsBeforeLong: Int = 4,
    val blockNotifications: Boolean = true,
    val vibrateOnComplete: Boolean = true,
    val themeMode: String = "system",
    val autoSyncWallpaper: Boolean = false,
    val wallpaperHomeScreen: Boolean = true,
    val wallpaperLockScreen: Boolean = false,
    val ambientRotationMin: Int = 5,
    val useLocationForDayNight: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val swipeToNavigate: Boolean = true
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE)
    private val sessionRepo = FocusFlowApplication.instance.sessionRepository
    private val database = FocusFlowApplication.instance.database

    val updateManager = AppUpdateManager(application)
    val updateState: StateFlow<UpdateState> = updateManager.updateState
    private var downloadedApkFile: java.io.File? = null

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _geminiApiKey = MutableStateFlow<String?>(null)
    val geminiApiKey: StateFlow<String?> = _geminiApiKey.asStateFlow()

    init {
        loadSettings()
        loadGeminiApiKey()
    }

    fun loadGeminiApiKey() {
        _geminiApiKey.value = com.example.util.SecureStorage.getGeminiApiKey(getApplication())
    }

    fun saveGeminiApiKey(key: String) {
        com.example.util.SecureStorage.saveGeminiApiKey(getApplication(), key)
        _geminiApiKey.value = key.trim().ifEmpty { null }
    }

    fun clearGeminiApiKey() {
        com.example.util.SecureStorage.clearGeminiApiKey(getApplication())
        _geminiApiKey.value = null
    }

    /**
     * Trigger check for updates from Remote Config
     */
    fun checkForUpdates(forceFetch: Boolean = true) {
        viewModelScope.launch {
            updateManager.checkForUpdates(forceFetch = forceFetch)
        }
    }

    /**
     * Start downloading update from designated CDN / Storage URL
     */
    fun downloadUpdate(downloadUrl: String) {
        viewModelScope.launch {
            downloadedApkFile = updateManager.downloadUpdate(downloadUrl)
        }
    }

    /**
     * Run native installation intent
     */
    fun installUpdate() {
        val apkFile = downloadedApkFile
        if (apkFile != null && apkFile.exists()) {
            if (updateManager.checkAndRequestInstallPermission()) {
                updateManager.launchInstaller(apkFile)
            }
        }
    }

    /**
     * Reset checking wizard state context
     */
    fun resetUpdateState() {
        updateManager.resetState()
        downloadedApkFile = null
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
                themeMode = sharedPrefs.getString("theme_mode", "system") ?: "system",
                autoSyncWallpaper = sharedPrefs.getBoolean("auto_sync_wallpaper", false),
                wallpaperHomeScreen = sharedPrefs.getBoolean("wallpaper_home_screen", true),
                wallpaperLockScreen = sharedPrefs.getBoolean("wallpaper_lock_screen", false),
                ambientRotationMin = sharedPrefs.getInt("ambient_rotation_min", 5),
                useLocationForDayNight = sharedPrefs.getBoolean("use_location_for_daynight", false),
                latitude = sharedPrefs.getFloat("last_known_latitude", 0.0f).toDouble(),
                longitude = sharedPrefs.getFloat("last_known_longitude", 0.0f).toDouble(),
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

    fun updateUseLocationForDayNight(value: Boolean) {
        sharedPrefs.edit().putBoolean("use_location_for_daynight", value).apply()
        _state.update { it.copy(useLocationForDayNight = value) }
    }

    fun updateSwipeToNavigate(value: Boolean) {
        sharedPrefs.edit().putBoolean("swipe_to_navigate", value).apply()
        _state.update { it.copy(swipeToNavigate = value) }
    }

    fun fetchAndSaveLocation(onComplete: (Boolean) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplication<Application>())
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    sharedPrefs.edit()
                        .putFloat("last_known_latitude", location.latitude.toFloat())
                        .putFloat("last_known_longitude", location.longitude.toFloat())
                        .apply()
                    _state.update { it.copy(
                        latitude = location.latitude,
                        longitude = location.longitude
                    ) }
                    onComplete(true)
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            sharedPrefs.edit()
                                .putFloat("last_known_latitude", lastLoc.latitude.toFloat())
                                .putFloat("last_known_longitude", lastLoc.longitude.toFloat())
                                .apply()
                            _state.update { it.copy(
                                latitude = lastLoc.latitude,
                                longitude = lastLoc.longitude
                            ) }
                            onComplete(true)
                        } else {
                            onComplete(false)
                        }
                    }.addOnFailureListener {
                        onComplete(false)
                    }
                }
            }.addOnFailureListener {
                onComplete(false)
            }
        } else {
            onComplete(false)
        }
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
