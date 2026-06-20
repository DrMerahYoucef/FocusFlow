package com.example.data.repository

import com.example.data.db.dao.SessionDao
import com.example.data.db.entity.SessionEntity
import com.example.widget.ExamCountdownWidgetReceiver
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SessionRepository(private val sessionDao: SessionDao) {
    suspend fun insert(session: SessionEntity) {
        sessionDao.insert(session)
        // Whenever a session is added, trigger background sync and refresh app widgets!
        CoroutineScope(Dispatchers.IO).launch {
            syncWithFirestore()
            // Also notify widgets to refresh statistics
            com.example.FocusFlowApplication.instance.let { app ->
                ExamCountdownWidgetReceiver.triggerWidgetUpdate(app)
            }
        }
    }

    fun getSessionsInRange(from: Long, to: Long): Flow<List<SessionEntity>> {
        return sessionDao.getSessionsInRange(from, to)
    }

    fun getTotalFocusSeconds(from: Long, to: Long): Flow<Long?> {
        return sessionDao.getTotalFocusSeconds(from, to)
    }

    fun getSessionCount(from: Long, to: Long): Flow<Int> {
        return sessionDao.getSessionCount(from, to)
    }

    fun getTotalScore(from: Long, to: Long): Flow<Int?> {
        return sessionDao.getTotalScore(from, to)
    }

    fun getAllSessions(): Flow<List<SessionEntity>> {
        return sessionDao.getAllSessions()
    }

    suspend fun syncWithFirestore() {
        val uid = com.google.firebase.Firebase.auth.currentUser?.uid ?: return
        try {
            val db = com.google.firebase.Firebase.firestore
            
            // 1. Fetch all remote sessions from Firestore
            val remoteDocs = db.collection("users").document(uid).collection("sessions")
                .get()
                .await()
            
            val remoteSessionsMap = remoteDocs.documents.mapNotNull { doc ->
                val date = doc.getLong("date") ?: return@mapNotNull null
                val durationSeconds = doc.getLong("durationSeconds")?.toInt() ?: 0
                val completed = doc.getBoolean("completed") ?: false
                val focusScore = doc.getLong("focusScore")?.toInt() ?: 0
                SessionEntity(
                    date = date,
                    durationSeconds = durationSeconds,
                    completed = completed,
                    focusScore = focusScore
                )
            }.associateBy { it.date }

            // 2. Fetch all local sessions from Room
            val localSessions = sessionDao.getAllSessionsList()
            val localSessionsMap = localSessions.associateBy { it.date }

            // 3. Sync missing remote sessions to local Room database
            var localChanged = false
            for (date in remoteSessionsMap.keys) {
                if (!localSessionsMap.containsKey(date)) {
                    val remoteSession = remoteSessionsMap[date]!!
                    sessionDao.insert(remoteSession)
                    localChanged = true
                }
            }

            // 4. Sync missing local sessions to remote Firestore
            for (date in localSessionsMap.keys) {
                if (!remoteSessionsMap.containsKey(date)) {
                    val localSession = localSessionsMap[date]!!
                    val data = hashMapOf(
                        "date" to localSession.date,
                        "durationSeconds" to localSession.durationSeconds,
                        "completed" to localSession.completed,
                        "focusScore" to localSession.focusScore
                    )
                    db.collection("users").document(uid).collection("sessions")
                        .document(localSession.date.toString())
                        .set(data)
                        .await()
                }
            }

            // 5. Update user dynamic statistics in user profile document
            val updatedCompletedCount = sessionDao.getCompletedCountImmediate()
            db.collection("users").document(uid).update("treeCount", updatedCompletedCount)
            db.collection("leaderboard").document(uid).update("treeCount", updatedCompletedCount)

            // If local data changed, trigger widget update to update progress screen
            if (localChanged) {
                com.example.FocusFlowApplication.instance.let { app ->
                    ExamCountdownWidgetReceiver.triggerWidgetUpdate(app)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SessionRepository", "Firestore bidirectional statistics sync bypass/fail (offline): ${e.localizedMessage}")
        }
    }
}
