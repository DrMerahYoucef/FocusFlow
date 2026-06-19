package com.example.ui.screen.timer

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.service.PomodoroTimerService
import com.example.service.TimerState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
