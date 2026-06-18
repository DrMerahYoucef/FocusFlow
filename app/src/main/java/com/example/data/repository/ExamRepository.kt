package com.example.data.repository

import com.example.data.db.dao.ExamDao
import com.example.data.db.entity.ExamEntity
import kotlinx.coroutines.flow.Flow

class ExamRepository(private val examDao: ExamDao) {
    suspend fun insert(exam: ExamEntity) {
        examDao.insert(exam)
    }

    suspend fun delete(exam: ExamEntity) {
        examDao.delete(exam)
    }

    fun getAllExams(): Flow<List<ExamEntity>> {
        return examDao.getAllExams()
    }

    suspend fun getNextExam(todayStart: Long): ExamEntity? {
        return examDao.getNextExam(todayStart)
    }
}
