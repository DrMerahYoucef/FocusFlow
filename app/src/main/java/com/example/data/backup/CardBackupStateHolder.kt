package com.example.data.backup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class BackupTaskType { EXPORT, IMPORT }

sealed class BackupTaskState {
    object Idle : BackupTaskState()

    data class Running(
        val type: BackupTaskType,
        val progress: Float, // 0.0f to 1.0f
        val statusMessage: String
    ) : BackupTaskState()

    data class ExportCompleted(
        val backupFile: File,
        val result: BackupRestoreResult
    ) : BackupTaskState()

    data class ImportCompleted(
        val result: BackupRestoreResult
    ) : BackupTaskState()

    data class Error(
        val type: BackupTaskType,
        val errorMessage: String
    ) : BackupTaskState()
}

object CardBackupStateHolder {
    private val _state = MutableStateFlow<BackupTaskState>(BackupTaskState.Idle)
    val state: StateFlow<BackupTaskState> = _state.asStateFlow()

    fun updateState(newState: BackupTaskState) {
        _state.value = newState
    }

    fun reset() {
        _state.value = BackupTaskState.Idle
    }
}
