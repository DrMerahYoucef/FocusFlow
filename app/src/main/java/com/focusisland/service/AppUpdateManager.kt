package com.focusisland.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.focusisland.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class UpdateAvailable(val latestVersionCode: Int, val downloadUrl: String) : UpdateState
    object UpToDate : UpdateState
    data class Downloading(val progress: Float) : UpdateState // 0.0 to 1.0, or -1f for indeterminate
    data class Error(val message: String) : UpdateState
    object ReadyToInstall : UpdateState
}

class AppUpdateManager(private val context: Context) {

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    init {
        // Configure Remote Config with reasonable developer/fetch intervals
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // Fetch every 1 hour, or override for fresh checking
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
    }

    /**
     * Checks Remote Config for the latest version code and compares it to build configuration.
     * Toggles the updateState accordingly.
     */
    suspend fun checkForUpdates(forceFetch: Boolean = false) {
        _updateState.value = UpdateState.Checking
        try {
            if (forceFetch) {
                // If checking manually in settings, we fetch with minimum interval of 0 to get fresh server configuration instantly
                val tempSettings = FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(0)
                    .build()
                remoteConfig.setConfigSettingsAsync(tempSettings).await()
            }

            remoteConfig.fetchAndActivate().await()

            val latestVersionCode = remoteConfig.getLong("latest_version_code").toInt()
            val downloadUrl = remoteConfig.getString("apk_download_url")

            val currentVersionCode = BuildConfig.VERSION_CODE

            android.util.Log.d("AppUpdateManager", "Latest config: version=$latestVersionCode, url=$downloadUrl. Current=$currentVersionCode")

            if (latestVersionCode > currentVersionCode && downloadUrl.isNotBlank()) {
                _updateState.value = UpdateState.UpdateAvailable(
                    latestVersionCode = latestVersionCode,
                    downloadUrl = downloadUrl
                )
            } else {
                _updateState.value = UpdateState.UpToDate
            }
        } catch (e: Exception) {
            android.util.Log.e("AppUpdateManager", "Failed to check for updates", e)
            _updateState.value = UpdateState.Error(e.localizedMessage ?: "Network query failed")
        }
    }

    /**
     * Downloads the APK file to cache directory and notifies state observers of progress.
     */
    suspend fun downloadUpdate(downloadUrl: String): File? {
        _updateState.value = UpdateState.Downloading(0f)
        return withContext(Dispatchers.IO) {
            try {
                // Determine destination inside standard app cache folder
                val updateDir = File(context.cacheDir, "update_apks")
                if (!updateDir.exists()) {
                    updateDir.mkdirs()
                }

                val destinationFile = File(updateDir, "focusflow_update.apk")
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }

                // Support gs:// URLs directly resolving through Firebase Storage
                val finalUrl = if (downloadUrl.startsWith("gs://")) {
                    try {
                        FirebaseStorage.getInstance().getReferenceFromUrl(downloadUrl).downloadUrl.await().toString()
                    } catch (e: Exception) {
                        android.util.Log.e("AppUpdateManager", "Failed parsing gs:// link with Firebase Storage", e)
                        throw Exception("Firebase Storage link resolution failed: ${e.message}")
                    }
                } else {
                    downloadUrl
                }

                val client = OkHttpClient.Builder().build()
                val request = Request.Builder().url(finalUrl).build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Failed download. Server response code: ${response.code}")
                    }

                    val responseBody = response.body ?: throw Exception("Empty file body response")
                    val totalBytes = responseBody.contentLength()

                    responseBody.byteStream().use { inputStream ->
                        FileOutputStream(destinationFile).use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead = 0L
                            var length: Int

                            while (inputStream.read(buffer).also { length = it } != -1) {
                                outputStream.write(buffer, 0, length)
                                bytesRead += length
                                if (totalBytes > 0) {
                                    val progress = bytesRead.toFloat() / totalBytes
                                    _updateState.value = UpdateState.Downloading(progress)
                                } else {
                                    _updateState.value = UpdateState.Downloading(-1f) // Indeterminate
                                }
                            }
                        }
                    }
                }

                _updateState.value = UpdateState.ReadyToInstall
                destinationFile
            } catch (e: Exception) {
                android.util.Log.e("AppUpdateManager", "Failed download", e)
                _updateState.value = UpdateState.Error(e.localizedMessage ?: "Download failed")
                null
            }
        }
    }

    /**
     * Returns true if system allowed request, or false if it launched settings for permission.
     */
    fun checkAndRequestInstallPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return false
            }
        }
        return true
    }

    /**
     * Launches native apk installer safely using the declared FileProvider.
     */
    fun launchInstaller(apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("AppUpdateManager", "Installer launch failed", e)
            _updateState.value = UpdateState.Error("Failed to open package installer: ${e.localizedMessage}")
        }
    }

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }
}
