package com.example.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NotificationSummary(
    val appName: String,
    val packageName: String,
    val sender: String,
    val messageResume: String,
    val postedAt: Long
)

object NotificationSummaryRepository {
    private val _notifications = MutableStateFlow<List<NotificationSummary>>(emptyList())
    val notifications: StateFlow<List<NotificationSummary>> = _notifications.asStateFlow()

    private const val MAX_ENTRIES = 25

    fun addOrUpdate(entry: NotificationSummary) {
        _notifications.update { current ->
            (listOf(entry) + current.filterNot { 
                it.packageName == entry.packageName && 
                it.sender == entry.sender && 
                it.messageResume == entry.messageResume 
            }).take(MAX_ENTRIES)
        }
    }

    fun remove(packageName: String, id: Int) {
        _notifications.update { current ->
            current.filterNot { it.packageName == packageName }
        }
    }

    fun clearAll() {
        _notifications.value = emptyList()
    }
}
