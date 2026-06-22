package com.example

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.ui.navigation.AppNavGraph
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Notification permission is required to display the session timers.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        checkAndRequestNotifications()

        // Trigger user statistics background sync with Firestore
        lifecycleScope.launch(Dispatchers.IO) {
            FocusFlowApplication.instance.sessionRepository.syncWithFirestore()
        }

        setContent {
            val settingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.example.ui.screen.settings.SettingsViewModel>()
            val state by settingsViewModel.state.collectAsState()
            val updateState by settingsViewModel.updateState.collectAsState()
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val darkTheme = hour !in 6..17

            androidx.compose.runtime.LaunchedEffect(Unit) {
                // Auto check for updates on startup using standard caching intervals
                settingsViewModel.checkForUpdates(forceFetch = false)
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                val isUpdateRequired = updateState is com.example.service.UpdateState.UpdateAvailable ||
                        updateState is com.example.service.UpdateState.Downloading ||
                        updateState is com.example.service.UpdateState.ReadyToInstall ||
                        updateState is com.example.service.UpdateState.Error

                if (isUpdateRequired) {
                    com.example.ui.components.BlockingUpdateScreen(viewModel = settingsViewModel)
                } else {
                    AppNavGraph(settingsViewModel = settingsViewModel)
                }
            }
        }
    }

    private fun checkAndRequestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
            }
        }
    }
}
