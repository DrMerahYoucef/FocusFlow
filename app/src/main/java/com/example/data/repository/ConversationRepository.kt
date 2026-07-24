package com.example.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ConversationThread(
    val packageName: String,
    val appName: String,
    val sender: String,
    val messages: List<String>,   // raw recent lines, oldest to newest
    val lastUpdated: Long,
    val aiSummary: String? = null // filled in after Gemini call
)

object ConversationRepository {
    private val _threadsMap = MutableStateFlow<Map<String, ConversationThread>>(emptyMap())
    private val _threadsList = MutableStateFlow<List<ConversationThread>>(emptyList())
    val threads: StateFlow<List<ConversationThread>> = _threadsList.asStateFlow()

    private const val MAX_MESSAGES_PER_THREAD = 15

    fun addMessages(packageName: String, appName: String, sender: String, newMessages: List<String>) {
        val key = "$packageName|$sender"
        _threadsMap.update { current ->
            val existing = current[key]
            val merged = ((existing?.messages ?: emptyList()) + newMessages)
                .filter { it.isNotBlank() }
                .distinct()
                .takeLast(MAX_MESSAGES_PER_THREAD)

            val updatedAppName = if (appName.isNotBlank() && appName != packageName) appName else (existing?.appName ?: packageName)
            val updated = current + (key to ConversationThread(
                packageName = packageName,
                appName = updatedAppName,
                sender = sender,
                messages = merged,
                lastUpdated = System.currentTimeMillis(),
                aiSummary = null // invalidate old summary when new messages arrive
            ))
            _threadsList.value = updated.values.sortedByDescending { it.lastUpdated }
            updated
        }
    }

    fun updateSummary(packageName: String, sender: String, summary: String) {
        val key = "$packageName|$sender"
        _threadsMap.update { current ->
            val existing = current[key]
            if (existing != null) {
                val updated = current + (key to existing.copy(aiSummary = summary))
                _threadsList.value = updated.values.sortedByDescending { it.lastUpdated }
                updated
            } else {
                current
            }
        }
    }

    fun remove(packageName: String) {
        _threadsMap.update { current ->
            val updated = current.filterKeys { !it.startsWith("$packageName|") }
            _threadsList.value = updated.values.sortedByDescending { it.lastUpdated }
            updated
        }
    }

    fun clearAll() {
        _threadsMap.value = emptyMap()
        _threadsList.value = emptyList()
    }
}
