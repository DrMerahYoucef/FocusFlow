package com.focusisland.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.focusisland.data.db.dao.ExamDao
import com.focusisland.data.db.dao.SessionDao
import com.focusisland.data.db.dao.BlockedAppDao
import com.focusisland.data.db.dao.FavouriteStationDao
import com.focusisland.data.db.dao.RadioDao
import com.focusisland.data.db.entity.ExamEntity
import com.focusisland.data.db.entity.SessionEntity
import com.focusisland.data.db.entity.BlockedAppEntity
import com.focusisland.data.db.entity.FavouriteStationEntity
import com.focusisland.data.db.entity.CategoryEntity
import com.focusisland.data.db.entity.StationEntity

@Database(entities = [SessionEntity::class, ExamEntity::class, BlockedAppEntity::class, FavouriteStationEntity::class, CategoryEntity::class, StationEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun examDao(): ExamDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun favouriteStationDao(): FavouriteStationDao
    abstract fun radioDao(): RadioDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS favourite_stations (
                        stationId TEXT PRIMARY KEY NOT NULL,
                        savedAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS radio_categories (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        isCustom INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS radio_stations (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        country TEXT NOT NULL,
                        categoryId TEXT NOT NULL,
                        streamUrl TEXT NOT NULL,
                        fallbackUrl TEXT NOT NULL DEFAULT '',
                        logoUrl TEXT NOT NULL DEFAULT '',
                        description TEXT NOT NULL,
                        isCustom INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(categoryId) REFERENCES radio_categories(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_radio_stations_categoryId ON radio_stations(categoryId)")

                // Seed default categories
                db.execSQL("INSERT OR IGNORE INTO radio_categories (id, name, isCustom) VALUES ('STUDY', 'Study', 0)")
                db.execSQL("INSERT OR IGNORE INTO radio_categories (id, name, isCustom) VALUES ('GLOBAL', 'Global', 0)")

                // Seed default stations
                db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('lofi_hiphop', 'Lo-Fi Hip Hop', '🌍 Global', 'STUDY', 'https://streams.ilovemusic.de/iloveradio17.mp3', '', '', 'Chill beats to study and relax to', 0)")
                db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('study_classical', 'Classical Focus', '🌍 Global', 'STUDY', 'https://live.musopen.org:8085/streamvbr0', '', '', 'Classical music — royalty free', 0)")
                db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('cafe_jazz', 'Jazz Café', '🌍 Global', 'STUDY', 'https://streams.ilovemusic.de/iloveradio29.mp3', '', '', 'Smooth jazz for deep work', 0)")
                db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('ambient_space', 'Ambient Space', '🌍 Global', 'STUDY', 'https://ice1.somafm.com/deepspaceone-128-mp3', '', '', 'Deep space ambient for long sessions', 0)")
                db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('brown_noise', 'Brown Noise', '🌍 Global', 'STUDY', 'https://ice1.somafm.com/darkzone-128-mp3', '', '', 'Noise masking for maximum focus', 0)")
                db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('piano_study', 'Piano Study', '🌍 Global', 'STUDY', 'https://streams.ilovemusic.de/iloveradio2.mp3', '', '', 'Solo piano — no lyrics, pure focus', 0)")
                db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('bbc_radio4', 'BBC Radio 4', '🇬🇧 UK', 'GLOBAL', 'https://stream.live.vc.bbcmedia.co.uk/bbc_radio_fourfm', '', '', 'Talk radio — news and culture', 0)")
                db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('france_inter', 'France Inter', '🇫🇷 France', 'GLOBAL', 'https://icecast.radiofrance.fr/franceinter-hifi.aac', '', '', 'French public radio', 0)")
                db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('monte_carlo', 'Radio Monte Carlo', '🇫🇷 France', 'GLOBAL', 'https://icy.unitedradio.it/RMC.mp3', '', '', 'French pop and hits', 0)")
                db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('soma_groove', 'SomaFM Groove Salad', '🌍 Global', 'GLOBAL', 'https://ice1.somafm.com/groovesalad-128-mp3', '', '', 'A nicely chilled plate of ambient', 0)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focusflow_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default categories
                        db.execSQL("INSERT OR IGNORE INTO radio_categories (id, name, isCustom) VALUES ('STUDY', 'Study', 0)")
                        db.execSQL("INSERT OR IGNORE INTO radio_categories (id, name, isCustom) VALUES ('GLOBAL', 'Global', 0)")

                        // Seed default stations
                        db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('lofi_hiphop', 'Lo-Fi Hip Hop', '🌍 Global', 'STUDY', 'https://streams.ilovemusic.de/iloveradio17.mp3', '', '', 'Chill beats to study and relax to', 0)")
                        db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('study_classical', 'Classical Focus', '🌍 Global', 'STUDY', 'https://live.musopen.org:8085/streamvbr0', '', '', 'Classical music — royalty free', 0)")
                        db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('cafe_jazz', 'Jazz Café', '🌍 Global', 'STUDY', 'https://streams.ilovemusic.de/iloveradio29.mp3', '', '', 'Smooth jazz for deep work', 0)")
                        db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('ambient_space', 'Ambient Space', '🌍 Global', 'STUDY', 'https://ice1.somafm.com/deepspaceone-128-mp3', '', '', 'Deep space ambient for long sessions', 0)")
                        db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('brown_noise', 'Brown Noise', '🌍 Global', 'STUDY', 'https://ice1.somafm.com/darkzone-128-mp3', '', '', 'Noise masking for maximum focus', 0)")
                        db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('piano_study', 'Piano Study', '🌍 Global', 'STUDY', 'https://streams.ilovemusic.de/iloveradio2.mp3', '', '', 'Solo piano — no lyrics, pure focus', 0)")
                        db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('bbc_radio4', 'BBC Radio 4', '🇬🇧 UK', 'GLOBAL', 'https://stream.live.vc.bbcmedia.co.uk/bbc_radio_fourfm', '', '', 'Talk radio — news and culture', 0)")
                        db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('france_inter', 'France Inter', '🇫🇷 France', 'GLOBAL', 'https://icecast.radiofrance.fr/franceinter-hifi.aac', '', '', 'French public radio', 0)")
                        db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('monte_carlo', 'Radio Monte Carlo', '🇫🇷 France', 'GLOBAL', 'https://icy.unitedradio.it/RMC.mp3', '', '', 'French pop and hits', 0)")
                        db.execSQL("INSERT OR IGNORE INTO radio_stations (id, name, country, categoryId, streamUrl, fallbackUrl, logoUrl, description, isCustom) VALUES ('soma_groove', 'SomaFM Groove Salad', '🌍 Global', 'GLOBAL', 'https://ice1.somafm.com/groovesalad-128-mp3', '', '', 'A nicely chilled plate of ambient', 0)")
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
