package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.repository.ExamRepository
import com.example.data.repository.SessionRepository

class FocusFlowApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val sessionRepository by lazy { SessionRepository(database.sessionDao()) }
    val examRepository by lazy { ExamRepository(database.examDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: FocusFlowApplication
            private set
    }
}
