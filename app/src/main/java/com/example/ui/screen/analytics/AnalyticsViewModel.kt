package com.example.ui.screen.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusFlowApplication
import com.example.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.*

data class DailyStats(
    val dayStartMillis: Long,
    val dayLabel: String,
    val dateFullLabel: String,
    val completedCount: Int,
    val totalFocusMinutes: Int,
    val totalPoints: Int,
    val totalSessionsCount: Int
)

data class AnalyticsState(
    val totalFocusMinutes: Int = 0,
    val completedSessions: Int = 0,
    val skippedSessions: Int = 0,
    val totalScore: Int = 0,
    val longestStreakDays: Int = 0,
    val last7DaysScores: List<Pair<String, Int>> = emptyList(),
    val averageSessionLengthMin: Int = 0,
    val dailyStatsList: List<DailyStats> = emptyList()
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
        val dailyStats = calculateDailyStats(sessions)

        return AnalyticsState(
            totalFocusMinutes = totalFocusMin,
            completedSessions = completed.size,
            skippedSessions = skipped.size,
            totalScore = totalScore,
            longestStreakDays = streak,
            last7DaysScores = dailyScores,
            averageSessionLengthMin = avgMin,
            dailyStatsList = dailyStats
        )
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(time1: Long, todayTime: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val calToday = Calendar.getInstance().apply { timeInMillis = todayTime }
        calToday.add(Calendar.DAY_OF_YEAR, -1)
        return cal1.get(Calendar.YEAR) == calToday.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == calToday.get(Calendar.DAY_OF_YEAR)
    }

    private fun calculateStreak(completedSessions: List<SessionEntity>): Int {
        if (completedSessions.isEmpty()) return 0

        // Get unique dates (start of day)
        val uniqueDays = completedSessions.map {
            val cal = Calendar.getInstance().apply {
                timeInMillis = it.date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }.distinct().sortedDescending() // newest days first

        if (uniqueDays.isEmpty()) return 0

        var currentStreak = 0
        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val checkTime = todayCalendar.timeInMillis

        val newestDay = uniqueDays.first()
        val isToday = isSameDay(newestDay, checkTime)
        val isYesterday = isYesterday(newestDay, checkTime)

        if (!isToday && !isYesterday) {
            return 0
        }

        currentStreak = 1
        var lastDay = newestDay
        for (i in 1 until uniqueDays.size) {
            val currentDay = uniqueDays[i]
            if (isYesterday(currentDay, lastDay)) {
                currentStreak++
                lastDay = currentDay
            } else {
                break // sequence broken
            }
        }

        return currentStreak
    }

    private fun calculateLast7DaysScores(completedSessions: List<SessionEntity>): List<Pair<String, Int>> {
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

        return last7Days.map { (dayStart, dayLabel) ->
            val calEnd = Calendar.getInstance().apply {
                timeInMillis = dayStart
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val dayEnd = calEnd.timeInMillis
            val scoreSum = completedSessions.filter {
                it.date >= dayStart && it.date < dayEnd
            }.sumOf { it.focusScore }
            Pair(dayLabel, scoreSum)
        }
    }

    private fun calculateDailyStats(sessions: List<SessionEntity>): List<DailyStats> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = calendar.timeInMillis

        val oldestDate = sessions.minOfOrNull { it.date } ?: todayStart
        val oldestCalendar = Calendar.getInstance().apply {
            timeInMillis = oldestDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val oldestStart = oldestCalendar.timeInMillis

        // Show at least the last 14 days, and up to the last 30 days
        val msInDay = 24 * 60 * 60 * 1000L
        val daysDiff = ((todayStart - oldestStart) / msInDay).toInt().coerceIn(13, 29)

        val list = mutableListOf<DailyStats>()
        val dayLabelFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val fullDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

        for (i in daysDiff downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayStart = cal.timeInMillis

            val calEnd = Calendar.getInstance().apply {
                timeInMillis = dayStart
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val dayEnd = calEnd.timeInMillis

            val daySessions = sessions.filter { it.date >= dayStart && it.date < dayEnd }
            val dayCompleted = daySessions.filter { it.completed }

            val treesCount = dayCompleted.size
            val focusMin = dayCompleted.sumOf { it.durationSeconds } / 60
            val points = dayCompleted.sumOf { it.focusScore }

            list.add(
                DailyStats(
                    dayStartMillis = dayStart,
                    dayLabel = dayLabelFormat.format(cal.time),
                    dateFullLabel = fullDateFormat.format(cal.time),
                    completedCount = treesCount,
                    totalFocusMinutes = focusMin,
                    totalPoints = points,
                    totalSessionsCount = daySessions.size
                )
            )
        }
        return list
    }
}
