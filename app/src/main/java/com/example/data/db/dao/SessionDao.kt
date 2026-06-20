package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE date >= :from AND date <= :to ORDER BY date DESC")
    fun getSessionsInRange(from: Long, to: Long): Flow<List<SessionEntity>>

    @Query("SELECT SUM(durationSeconds) FROM sessions WHERE date >= :from AND date <= :to AND completed = 1")
    fun getTotalFocusSeconds(from: Long, to: Long): Flow<Long?>

    @Query("SELECT COUNT(*) FROM sessions WHERE date >= :from AND date <= :to AND completed = 1")
    fun getSessionCount(from: Long, to: Long): Flow<Int>

    @Query("SELECT SUM(focusScore) FROM sessions WHERE date >= :from AND date <= :to")
    fun getTotalScore(from: Long, to: Long): Flow<Int?>
    
    @Query("SELECT * FROM sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY date DESC")
    suspend fun getAllSessionsList(): List<SessionEntity>

    @Query("SELECT COUNT(*) FROM sessions WHERE completed = 1")
    suspend fun getCompletedCountImmediate(): Int
}
