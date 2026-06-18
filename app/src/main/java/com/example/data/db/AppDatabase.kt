package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.db.dao.ExamDao
import com.example.data.db.dao.SessionDao
import com.example.data.db.dao.BlockedAppDao
import com.example.data.db.entity.ExamEntity
import com.example.data.db.entity.SessionEntity
import com.example.data.db.entity.BlockedAppEntity

@Database(entities = [SessionEntity::class, ExamEntity::class, BlockedAppEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun examDao(): ExamDao
    abstract fun blockedAppDao(): BlockedAppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS blocked_apps (
                        packageName TEXT PRIMARY KEY NOT NULL,
                        appName TEXT NOT NULL,
                        appIconBase64 TEXT NOT NULL,
                        blockNotifications INTEGER NOT NULL DEFAULT 1,
                        blockLaunch INTEGER NOT NULL DEFAULT 1
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focusflow_database"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
