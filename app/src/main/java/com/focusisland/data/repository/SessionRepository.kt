package com.focusisland.data.repository

import com.focusisland.data.db.dao.SessionDao
import com.focusisland.data.db.entity.SessionEntity
import com.focusisland.widget.ExamCountdownWidgetReceiver
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
            
            // 1. Fetch the user profile metrics from Firestore to check master stats
            val profileDoc = db.collection("users").document(uid).get().await()
            val remoteTreeCount = profileDoc.getLong("treeCount")?.toInt() ?: 0
            val remoteTotalMinutes = profileDoc.getLong("totalMinutes")?.toInt() ?: 0
            val remotePoints = profileDoc.getLong("points")?.toInt() ?: 0
            val remoteStreak = profileDoc.getLong("currentStreak")?.toInt() ?: 0

            // 2. Fetch all remote sessions from Firestore (sessions subcollection)
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

            // 3. Fetch all local sessions from Room
            val localSessions = sessionDao.getAllSessionsList()
            val localSessionsMap = localSessions.associateBy { it.date }

            var localChanged = false

            // Protect against empty local database on newly reinstalled / fresh device logins!
            // If the local database is completely blank, but the remote user profile has non-zero trees or minutes,
            // we restore/reconstruct the local sessions and update FS to match.
            if (localSessions.isEmpty() && (remoteTreeCount > 0 || remoteTotalMinutes > 0)) {
                // Populate existing remote subcollection sessions to local Room first
                if (remoteSessionsMap.isNotEmpty()) {
                    for (remoteSession in remoteSessionsMap.values) {
                        sessionDao.insert(remoteSession)
                    }
                    localChanged = true
                }

                // If remoteSessions subcollection is empty (old version/migration issue), or there's still a gap
                // between the number of completed sessions in local Room and remoteTreeCount, synthesize sessions retroactively!
                val currentLocalCompleted = sessionDao.getCompletedCountImmediate()
                if (currentLocalCompleted < remoteTreeCount) {
                    val gap = remoteTreeCount - currentLocalCompleted
                    val streakDays = maxOf(remoteStreak, 1)
                    val sessionsPerDay = maxOf(1, gap / streakDays)
                    
                    val now = System.currentTimeMillis()
                    val dayMs = 86_400_000L
                    var sessionsCreated = 0
                    
                    for (dayOffset in 0 until streakDays) {
                        if (sessionsCreated >= gap) break
                        
                        val sessionsToday = if (dayOffset == 0) {
                            gap - (streakDays - 1) * sessionsPerDay
                        } else {
                            sessionsPerDay
                        }.coerceAtLeast(1)
                        
                        for (s in 0 until sessionsToday) {
                            if (sessionsCreated >= gap) break
                            
                            val sessionTime = now - (dayOffset * dayMs) - (s * 3600_000L) - (15 * 60000L)
                            val durationSecs = if (remoteTreeCount > 0 && remoteTotalMinutes > 0) {
                                ((remoteTotalMinutes * 60) / remoteTreeCount).coerceIn(300, 7200)
                            } else {
                                1500 // fallback 25 mins
                            }
                            
                            val synthesizedSession = SessionEntity(
                                date = sessionTime,
                                durationSeconds = durationSecs,
                                completed = true,
                                focusScore = durationSecs / 60
                            )
                            
                            sessionDao.insert(synthesizedSession)
                            
                            // Back-upload the synthesized session so that this subcollection also becomes populated
                            val uploadData = hashMapOf(
                                "date" to synthesizedSession.date,
                                "durationSeconds" to synthesizedSession.durationSeconds,
                                "completed" to synthesizedSession.completed,
                                "focusScore" to synthesizedSession.focusScore
                            )
                            db.collection("users").document(uid).collection("sessions")
                                .document(synthesizedSession.date.toString())
                                .set(uploadData)
                            
                            sessionsCreated++
                        }
                    }
                    localChanged = true
                }
            } else {
                // Regular bidirectional synchronization
                // A. Sync from remote to local
                for (date in remoteSessionsMap.keys) {
                    if (!localSessionsMap.containsKey(date)) {
                        val remoteSession = remoteSessionsMap[date]!!
                        sessionDao.insert(remoteSession)
                        localChanged = true
                    }
                }

                // B. Sync from local to remote
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
            }

            // 5. Recalculate dynamic statistics from the now fully populated local database
            val updatedCompletedCount = sessionDao.getCompletedCountImmediate()
            val totalMinutes = sessionDao.getTotalFocusMinutesImmediate()
            val allCompletedDates = sessionDao.getAllSessionDates()
            val currentStreak = calculateStreak(allCompletedDates)
            val points = totalMinutes / 5 // 1 point per 5 focus minutes

            // If we are about to update, prevent overwriting positive remote stats with 0 (e.g. if room reads as empty inexplicably)
            if (updatedCompletedCount >= remoteTreeCount || totalMinutes >= remoteTotalMinutes) {
                val statsUpdate = mapOf(
                    "treeCount" to updatedCompletedCount,
                    "totalMinutes" to totalMinutes,
                    "points" to points,
                    "currentStreak" to currentStreak
                )
                db.collection("users").document(uid).update(statsUpdate).await()
                db.collection("leaderboard").document(uid).update(statsUpdate).await()
            } else {
                // Keep the database values if local calculation came up short for any transient offline reason
                val statsUpdate = mapOf(
                    "treeCount" to maxOf(updatedCompletedCount, remoteTreeCount),
                    "totalMinutes" to maxOf(totalMinutes, remoteTotalMinutes),
                    "points" to maxOf(points, remotePoints),
                    "currentStreak" to maxOf(currentStreak, remoteStreak)
                )
                db.collection("users").document(uid).update(statsUpdate).await()
                db.collection("leaderboard").document(uid).update(statsUpdate).await()
            }

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

    private fun calculateStreak(dates: List<Long>): Int {
        if (dates.isEmpty()) return 0
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayMs = 86_400_000L
        val uniqueDays = dates.map { it / dayMs }.toSortedSet().toList().reversed()
        var streak = 0
        var expected = today / dayMs
        for (day in uniqueDays) {
            if (day == expected || day == expected - 1) {
                streak++
                expected = day - 1
            } else {
                break
            }
        }
        return streak
    }
}
