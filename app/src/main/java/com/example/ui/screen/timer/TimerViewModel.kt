package com.example.ui.screen.timer

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import com.example.service.PomodoroTimerService
import com.example.service.TimerState
import kotlinx.coroutines.flow.StateFlow

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    val timerState: StateFlow<TimerState> = PomodoroTimerService.state

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
