package com.focusisland

import android.app.Application
import com.focusisland.data.db.AppDatabase
import com.focusisland.data.repository.BlockedAppRepository
import com.focusisland.data.repository.ExamRepository
import com.focusisland.data.repository.SessionRepository

class FocusFlowApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val sessionRepository by lazy { SessionRepository(database.sessionDao()) }
    val examRepository by lazy { ExamRepository(database.examDao()) }
    val blockedAppRepository by lazy { BlockedAppRepository(database.blockedAppDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: FocusFlowApplication
            private set
    }
}
