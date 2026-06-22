package com.example.ui.screen.timer

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusFlowApplication
import com.example.service.PomodoroTimerService
import com.example.service.TimerState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    val timerState: StateFlow<TimerState> = PomodoroTimerService.state

    private val _treePlanted = MutableSharedFlow<Int>()
    val treePlanted = _treePlanted.asSharedFlow()

    init {
        var lastCount = -1
        timerState
            .map { it.sessionCount }
            .distinctUntilChanged()
            .onEach { count ->
                if (lastCount != -1 && count > lastCount) {
                    _treePlanted.emit(count)
                }
                lastCount = count
            }
            .launchIn(viewModelScope)

        // Observe local completed sessions and keep Firebase Firestore up to date
        FocusFlowApplication.instance.sessionRepository.getAllSessions()
            .map { sessions -> sessions.count { it.completed } }
            .distinctUntilChanged()
            .onEach { completedCount ->
                val uid = Firebase.auth.currentUser?.uid
                if (uid != null) {
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val db = Firebase.firestore
                            db.collection("users").document(uid).update("treeCount", completedCount).await()
                            db.collection("leaderboard").document(uid).update("treeCount", completedCount).await()
                            android.util.Log.d("TimerViewModel", "Synced treeCount ($completedCount) to Firestore")
                        } catch (e: Exception) {
                            android.util.Log.e("TimerViewModel", "Failed to sync treeCount with Firestore", e)
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun startTimer() {
        sendCommand(PomodoroTimerService.ACTION_START)
    }

    fun pauseTimer() {
        sendCommand(PomodoroTimerService.ACTION_PAUSE)
    }

    fun resumeTimer() {
        sendCommand(PomodoroTimerService.ACTION_RESUME)
    }

    fun skipPhase() {
        sendCommand(PomodoroTimerService.ACTION_SKIP)
    }

    fun stopTimer() {
        sendCommand(PomodoroTimerService.ACTION_STOP)
    }

    fun setAmbientSound(ambientId: String) {
        val context = getApplication<Application>()
        val intent = Intent(context, PomodoroTimerService::class.java).apply {
            action = PomodoroTimerService.ACTION_SET_AMBIENT
            putExtra(PomodoroTimerService.EXTRA_AMBIENT_ID, ambientId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun sendCommand(action: String) {
        val context = getApplication<Application>()
        val intent = Intent(context, PomodoroTimerService::class.java).apply {
            this.action = action
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
