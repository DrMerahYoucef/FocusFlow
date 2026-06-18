package com.example.service

enum class Phase(val label: String) {
    FOCUS("Focus Session"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break")
}

data class TimerState(
    val remainingMs: Long = 25 * 60 * 1000L,
    val phase: Phase = Phase.FOCUS,
    val sessionCount: Int = 0,
    val isRunning: Boolean = false,
    val totalFocusSecs: Long = 0L,
    val focusDurationMs: Long = 25 * 60 * 1000L,
    val shortBreakDurationMs: Long = 5 * 60 * 1000L,
    val longBreakDurationMs: Long = 15 * 60 * 1000L,
    val sessionsBeforeLongBreak: Int = 4,
    val isDndActive: Boolean = false
)
