package com.example.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.FocusFlowApplication
import com.example.data.db.entity.SyncStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class RevisionSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val user = Firebase.auth.currentUser
            if (user == null || user.isAnonymous) {
                return Result.success()
            }
            val uid = user.uid
            val db = Firebase.firestore
            val repository = FocusFlowApplication.instance.revisionRepository

            val unsynced = repository.getUnsyncedNotes()
            for (note in unsynced) {
                when (note.syncStatus) {
                    SyncStatus.PENDING_UPLOAD -> {
                        val notesRef = db.collection("users").document(uid).collection("revision_notes")
                        val docRef = note.firestoreId?.let { notesRef.document(it) } ?: notesRef.document()

                        val noteData = mapOf(
                            "id" to note.id,
                            "deckId" to note.deckId,
                            "title" to note.title,
                            "contentMarkdown" to note.contentMarkdown,
                            "createdAt" to note.createdAt,
                            "updatedAt" to note.updatedAt,
                            "easeFactor" to note.easeFactor,
                            "intervalDays" to note.intervalDays,
                            "repetitions" to note.repetitions,
                            "dueDate" to note.dueDate,
                            "lastReviewedAt" to note.lastReviewedAt
                        )

                        docRef.set(noteData).await()
                        repository.upsertNote(
                            note.copy(
                                firestoreId = docRef.id,
                                syncStatus = SyncStatus.SYNCED
                            )
                        )
                    }
                    SyncStatus.PENDING_DELETE -> {
                        note.firestoreId?.let { fid ->
                            db.collection("users").document(uid).collection("revision_notes")
                                .document(fid).delete().await()
                        }
                        repository.deleteNote(note)
                    }
                    SyncStatus.SYNCED -> Unit
                }
            }
            Result.success()
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            android.util.Log.w("RevisionSyncWorker", "Firestore sync skipped (permission or offline): ${e.message}")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.w("RevisionSyncWorker", "Revision sync exception handled gracefully: ${e.message}")
            Result.success()
        }
    }

    companion object {
        fun scheduleSyncWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<RevisionSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }

        fun schedulePeriodicSyncWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<RevisionSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "revision_periodic_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }
}
