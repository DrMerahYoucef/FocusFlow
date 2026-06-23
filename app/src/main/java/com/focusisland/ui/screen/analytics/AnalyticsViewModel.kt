package com.focusisland.ui.screen.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusisland.FocusFlowApplication
import com.focusisland.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.*

data class AnalyticsState(
    val totalFocusMinutes: Int = 0,
    val completedSessions: Int = 0,
    val skippedSessions: Int = 0,
    val totalScore: Int = 0,
    val longestStreakDays: Int = 0,
    val last7DaysScores: List<Pair<String, Int>> = emptyList(),
    val averageSessionLengthMin: Int = 0
)

class AnalyticsViewModel : ViewModel() {

    private val repository = FocusFlowApplication.instance.sessionRepository

    val state: StateFlow<AnalyticsState> = repository.getAllSessions()
        .map { sessions ->
            calculateAnalytics(sessions)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnalyticsState()
        )

    private fun calculateAnalytics(sessions: List<SessionEntity>): AnalyticsState {
        val completed = sessions.filter { it.completed }
        val skipped = sessions.filter { !it.completed }

        val totalFocusSecs = completed.sumOf { it.durationSeconds }
        val totalFocusMin = totalFocusSecs / 60
        val totalScore = completed.sumOf { it.focusScore }

        val avgSecs = if (completed.isNotEmpty()) completed.map { it.durationSeconds }.average().toInt() else 0
        val avgMin = avgSecs / 60

        val streak = calculateStreak(completed)
        val dailyScores = calculateLast7DaysScores(completed)

        return AnalyticsState(
            totalFocusMinutes = totalFocusMin,
            completedSessions = completed.size,
            skippedSessions = skipped.size,
            totalScore = totalScore,
            longestStreakDays = streak,
            last7DaysScores = dailyScores,
            averageSessionLengthMin = avgMin
        )
    }

    private fun calculateStreak(completedSessions: List<SessionEntity>): Int {
        if (completedSessions.isEmpty()) return 0

        // Get unique dates (start of day)
        val calendar = Calendar.getInstance()
        val uniqueDays = completedSessions.map {
            calendar.timeInMillis = it.date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }.distinct().sortedDescending() // newest days first

        if (uniqueDays.isEmpty()) return 0

        var currentStreak = 0
        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val oneDayMillis = 24 * 60 * 60 * 1000L

        var checkTime = todayCalendar.timeInMillis
        
        // If there are no entries for either today or yesterday, streak is zero
        val newestDay = uniqueDays.first()
        if (newestDay != checkTime && newestDay != (checkTime - oneDayMillis)) {
            return 0
        }

        if (newestDay == checkTime || newestDay == (checkTime - oneDayMillis)) {
            currentStreak = 1
            var lastDay = newestDay
            for (i in 1 until uniqueDays.size) {
                if (lastDay - uniqueDays[i] == oneDayMillis) {
                    currentStreak++
                    lastDay = uniqueDays[i]
                } else if (lastDay - uniqueDays[i] > oneDayMillis) {
                    break // sequence broken
                }
            }
        }

        return currentStreak
    }

    private fun calculateLast7DaysScores(completedSessions: List<SessionEntity>): List<Pair<String, Int>> {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val last7Days = mutableListOf<Pair<Long, String>>()

        // Generate timezone-aware calendar dates for last 7 slots including today
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            last7Days.add(Pair(cal.timeInMillis, dateFormat.format(cal.time)))
        }

        val oneDayMillis = 24 * 60 * 60 * 1000L
        return last7Days.map { (dayStart, dayLabel) ->
            val dayEnd = dayStart + oneDayMillis
            val scoreSum = completedSessions.filter {
                it.date in dayStart..dayEnd
            }.sumOf { it.focusScore }
            Pair(dayLabel, scoreSum)
        }
    }
}
