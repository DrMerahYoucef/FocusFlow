package com.example.`data`.db

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.example.`data`.db.dao.BlockedAppDao
import com.example.`data`.db.dao.BlockedAppDao_Impl
import com.example.`data`.db.dao.ExamDao
import com.example.`data`.db.dao.ExamDao_Impl
import com.example.`data`.db.dao.FavouriteStationDao
import com.example.`data`.db.dao.FavouriteStationDao_Impl
import com.example.`data`.db.dao.RadioDao
import com.example.`data`.db.dao.RadioDao_Impl
import com.example.`data`.db.dao.SessionDao
import com.example.`data`.db.dao.SessionDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _sessionDao: Lazy<SessionDao> = lazy {
    SessionDao_Impl(this)
  }

  private val _examDao: Lazy<ExamDao> = lazy {
    ExamDao_Impl(this)
  }

  private val _blockedAppDao: Lazy<BlockedAppDao> = lazy {
    BlockedAppDao_Impl(this)
  }

  private val _favouriteStationDao: Lazy<FavouriteStationDao> = lazy {
    FavouriteStationDao_Impl(this)
  }

  private val _radioDao: Lazy<RadioDao> = lazy {
    RadioDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(4,
        "5ef8ff35e47f86c3c9a8c046b9fde107", "0dccd085bc402e7f56132dfacb36816e") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, `durationSeconds` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `focusScore` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `exams` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `examDate` INTEGER NOT NULL, `subject` TEXT NOT NULL, `color` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `blocked_apps` (`packageName` TEXT NOT NULL, `appName` TEXT NOT NULL, `appIconBase64` TEXT NOT NULL, `blockNotifications` INTEGER NOT NULL, `blockLaunch` INTEGER NOT NULL, PRIMARY KEY(`packageName`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `favourite_stations` (`stationId` TEXT NOT NULL, `savedAt` INTEGER NOT NULL, PRIMARY KEY(`stationId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `radio_categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `isCustom` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `radio_stations` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `country` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `streamUrl` TEXT NOT NULL, `fallbackUrl` TEXT NOT NULL, `logoUrl` TEXT NOT NULL, `description` TEXT NOT NULL, `isCustom` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`categoryId`) REFERENCES `radio_categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_radio_stations_categoryId` ON `radio_stations` (`categoryId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '5ef8ff35e47f86c3c9a8c046b9fde107')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `sessions`")
        connection.execSQL("DROP TABLE IF EXISTS `exams`")
        connection.execSQL("DROP TABLE IF EXISTS `blocked_apps`")
        connection.execSQL("DROP TABLE IF EXISTS `favourite_stations`")
        connection.execSQL("DROP TABLE IF EXISTS `radio_categories`")
        connection.execSQL("DROP TABLE IF EXISTS `radio_stations`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsSessions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSessions.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("date", TableInfo.Column("date", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("durationSeconds", TableInfo.Column("durationSeconds", "INTEGER", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("completed", TableInfo.Column("completed", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSessions.put("focusScore", TableInfo.Column("focusScore", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSessions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSessions: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSessions: TableInfo = TableInfo("sessions", _columnsSessions, _foreignKeysSessions,
            _indicesSessions)
        val _existingSessions: TableInfo = read(connection, "sessions")
        if (!_infoSessions.equals(_existingSessions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |sessions(com.example.data.db.entity.SessionEntity).
              | Expected:
              |""".trimMargin() + _infoSessions + """
              |
              | Found:
              |""".trimMargin() + _existingSessions)
        }
        val _columnsExams: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsExams.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsExams.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsExams.put("examDate", TableInfo.Column("examDate", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsExams.put("subject", TableInfo.Column("subject", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsExams.put("color", TableInfo.Column("color", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysExams: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesExams: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoExams: TableInfo = TableInfo("exams", _columnsExams, _foreignKeysExams,
            _indicesExams)
        val _existingExams: TableInfo = read(connection, "exams")
        if (!_infoExams.equals(_existingExams)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |exams(com.example.data.db.entity.ExamEntity).
              | Expected:
              |""".trimMargin() + _infoExams + """
              |
              | Found:
              |""".trimMargin() + _existingExams)
        }
        val _columnsBlockedApps: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBlockedApps.put("packageName", TableInfo.Column("packageName", "TEXT", true, 1,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBlockedApps.put("appName", TableInfo.Column("appName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBlockedApps.put("appIconBase64", TableInfo.Column("appIconBase64", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBlockedApps.put("blockNotifications", TableInfo.Column("blockNotifications",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBlockedApps.put("blockLaunch", TableInfo.Column("blockLaunch", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBlockedApps: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBlockedApps: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoBlockedApps: TableInfo = TableInfo("blocked_apps", _columnsBlockedApps,
            _foreignKeysBlockedApps, _indicesBlockedApps)
        val _existingBlockedApps: TableInfo = read(connection, "blocked_apps")
        if (!_infoBlockedApps.equals(_existingBlockedApps)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |blocked_apps(com.example.data.db.entity.BlockedAppEntity).
              | Expected:
              |""".trimMargin() + _infoBlockedApps + """
              |
              | Found:
              |""".trimMargin() + _existingBlockedApps)
        }
        val _columnsFavouriteStations: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsFavouriteStations.put("stationId", TableInfo.Column("stationId", "TEXT", true, 1,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFavouriteStations.put("savedAt", TableInfo.Column("savedAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysFavouriteStations: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesFavouriteStations: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoFavouriteStations: TableInfo = TableInfo("favourite_stations",
            _columnsFavouriteStations, _foreignKeysFavouriteStations, _indicesFavouriteStations)
        val _existingFavouriteStations: TableInfo = read(connection, "favourite_stations")
        if (!_infoFavouriteStations.equals(_existingFavouriteStations)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |favourite_stations(com.example.data.db.entity.FavouriteStationEntity).
              | Expected:
              |""".trimMargin() + _infoFavouriteStations + """
              |
              | Found:
              |""".trimMargin() + _existingFavouriteStations)
        }
        val _columnsRadioCategories: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRadioCategories.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRadioCategories.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRadioCategories.put("isCustom", TableInfo.Column("isCustom", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRadioCategories: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRadioCategories: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRadioCategories: TableInfo = TableInfo("radio_categories", _columnsRadioCategories,
            _foreignKeysRadioCategories, _indicesRadioCategories)
        val _existingRadioCategories: TableInfo = read(connection, "radio_categories")
        if (!_infoRadioCategories.equals(_existingRadioCategories)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |radio_categories(com.example.data.db.entity.CategoryEntity).
              | Expected:
              |""".trimMargin() + _infoRadioCategories + """
              |
              | Found:
              |""".trimMargin() + _existingRadioCategories)
        }
        val _columnsRadioStations: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRadioStations.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRadioStations.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRadioStations.put("country", TableInfo.Column("country", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRadioStations.put("categoryId", TableInfo.Column("categoryId", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRadioStations.put("streamUrl", TableInfo.Column("streamUrl", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRadioStations.put("fallbackUrl", TableInfo.Column("fallbackUrl", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRadioStations.put("logoUrl", TableInfo.Column("logoUrl", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRadioStations.put("description", TableInfo.Column("description", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRadioStations.put("isCustom", TableInfo.Column("isCustom", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRadioStations: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysRadioStations.add(TableInfo.ForeignKey("radio_categories", "CASCADE",
            "NO ACTION", listOf("categoryId"), listOf("id")))
        val _indicesRadioStations: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesRadioStations.add(TableInfo.Index("index_radio_stations_categoryId", false,
            listOf("categoryId"), listOf("ASC")))
        val _infoRadioStations: TableInfo = TableInfo("radio_stations", _columnsRadioStations,
            _foreignKeysRadioStations, _indicesRadioStations)
        val _existingRadioStations: TableInfo = read(connection, "radio_stations")
        if (!_infoRadioStations.equals(_existingRadioStations)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |radio_stations(com.example.data.db.entity.StationEntity).
              | Expected:
              |""".trimMargin() + _infoRadioStations + """
              |
              | Found:
              |""".trimMargin() + _existingRadioStations)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "sessions", "exams",
        "blocked_apps", "favourite_stations", "radio_categories", "radio_stations")
  }

  public override fun clearAllTables() {
    super.performClear(true, "sessions", "exams", "blocked_apps", "favourite_stations",
        "radio_categories", "radio_stations")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(SessionDao::class, SessionDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ExamDao::class, ExamDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(BlockedAppDao::class, BlockedAppDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(FavouriteStationDao::class,
        FavouriteStationDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(RadioDao::class, RadioDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun sessionDao(): SessionDao = _sessionDao.value

  public override fun examDao(): ExamDao = _examDao.value

  public override fun blockedAppDao(): BlockedAppDao = _blockedAppDao.value

  public override fun favouriteStationDao(): FavouriteStationDao = _favouriteStationDao.value

  public override fun radioDao(): RadioDao = _radioDao.value
}
