package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.repository.BlockedAppRepository
import com.example.data.repository.ExamRepository
import com.example.data.repository.SessionRepository
import com.google.firebase.FirebaseApp

class FocusFlowApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val sessionRepository by lazy { SessionRepository(database.sessionDao()) }
    val examRepository by lazy { ExamRepository(database.examDao()) }
    val blockedAppRepository by lazy { BlockedAppRepository(database.blockedAppDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId("1:15963877054:android:96d280ae44802a00bbf8ef")
                    .setApiKey("AIzaSyAFKsKjqCROungwqAxXCHgqeUQEu-kg3go")
                    .setProjectId("focuse-island")
                    .setStorageBucket("focuse-island.firebasestorage.app")
                    .setGcmSenderId("15963877054")
                    .build()
                FirebaseApp.initializeApp(this, options)
                android.util.Log.d("FocusFlowApplication", "Firebase initialized successfully with explicit options")
            } else {
                android.util.Log.d("FocusFlowApplication", "Firebase already initialized by system/provider")
            }
        } catch (e: Throwable) {
            android.util.Log.e("FocusFlowApplication", "Failed to initialize Firebase", e)
        }
    }

    companion object {
        lateinit var instance: FocusFlowApplication
            private set
    }
}
