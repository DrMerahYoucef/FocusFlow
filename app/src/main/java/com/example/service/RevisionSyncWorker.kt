package com.example.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class RevisionSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Cloud sync is disabled for revision media/notes (local-only paradigm)
        return Result.success()
    }

    companion object {
        fun scheduleSyncWork(context: Context) {
            // No-op
        }

        fun schedulePeriodicSyncWork(context: Context) {
            // No-op
        }
    }
}
