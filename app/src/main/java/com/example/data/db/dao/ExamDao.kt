package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.db.entity.ExamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exam: ExamEntity)

    @Delete
    suspend fun delete(exam: ExamEntity)

    @Query("SELECT * FROM exams ORDER BY examDate ASC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE examDate >= :todayStart ORDER BY examDate ASC LIMIT 1")
    suspend fun getNextExam(todayStart: Long): ExamEntity?

    @Query("SELECT * FROM exams WHERE examDate >= :todayStart ORDER BY examDate ASC")
    suspend fun getUpcomingExams(todayStart: Long): List<ExamEntity>
}
