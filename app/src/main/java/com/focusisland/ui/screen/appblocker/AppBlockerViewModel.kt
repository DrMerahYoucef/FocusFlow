package com.focusisland.ui.screen.appblocker

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focusisland.FocusFlowApplication
import com.focusisland.data.db.entity.BlockedAppEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?
)

class AppBlockerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FocusFlowApplication.instance.blockedAppRepository
    private val packageManager = application.packageManager

    val blockedApps: StateFlow<List<BlockedAppEntity>> =
        repository.getAllBlocked()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            try {
                val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                    .map { info ->
                        InstalledAppInfo(
                            packageName = info.packageName,
                            appName = packageManager.getApplicationLabel(info).toString(),
                            icon = try { packageManager.getApplicationIcon(info) } catch (e: Exception) { null }
                        )
                    }
                    .sortedBy { it.appName }
                _installedApps.value = apps
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleNotifBlock(pkg: String, appName: String, block: Boolean) {
        viewModelScope.launch {
            val existing = blockedApps.value.find { it.packageName == pkg }
            if (existing != null) {
                if (!block && !existing.blockLaunch) {
                    repository.deleteByPackage(pkg)
                } else {
                    repository.upsert(existing.copy(blockNotifications = block))
                }
            } else if (block) {
                repository.upsert(
                    BlockedAppEntity(
                        packageName = pkg,
                        appName = appName,
                        appIconBase64 = "",
                        blockNotifications = true,
                        blockLaunch = false
                    )
                )
            }
            com.example.service.FocusNotificationListenerService.refresh(getApplication())
        }
    }

    fun toggleLaunchBlock(pkg: String, appName: String, block: Boolean) {
        viewModelScope.launch {
            val existing = blockedApps.value.find { it.packageName == pkg }
            if (existing != null) {
                if (!block && !existing.blockNotifications) {
                    repository.deleteByPackage(pkg)
                } else {
                    repository.upsert(existing.copy(blockLaunch = block))
                }
            } else if (block) {
                repository.upsert(
                    BlockedAppEntity(
                        packageName = pkg,
                        appName = appName,
                        appIconBase64 = "",
                        blockNotifications = false,
                        blockLaunch = true
                    )
                )
            }
        }
    }
}
