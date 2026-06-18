package com.example.data.repository

import com.example.data.db.dao.SessionDao
import com.example.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

class SessionRepository(private val sessionDao: SessionDao) {
    suspend fun insert(session: SessionEntity) {
        sessionDao.insert(session)
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
}
