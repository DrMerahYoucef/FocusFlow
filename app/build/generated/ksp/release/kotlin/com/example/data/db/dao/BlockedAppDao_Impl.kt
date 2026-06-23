package com.example.`data`.db.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.example.`data`.db.entity.BlockedAppEntity
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class BlockedAppDao_Impl(
  __db: RoomDatabase,
) : BlockedAppDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfBlockedAppEntity: EntityInsertAdapter<BlockedAppEntity>

  private val __deleteAdapterOfBlockedAppEntity: EntityDeleteOrUpdateAdapter<BlockedAppEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfBlockedAppEntity = object : EntityInsertAdapter<BlockedAppEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `blocked_apps` (`packageName`,`appName`,`appIconBase64`,`blockNotifications`,`blockLaunch`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: BlockedAppEntity) {
        statement.bindText(1, entity.packageName)
        statement.bindText(2, entity.appName)
        statement.bindText(3, entity.appIconBase64)
        val _tmp: Int = if (entity.blockNotifications) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        val _tmp_1: Int = if (entity.blockLaunch) 1 else 0
        statement.bindLong(5, _tmp_1.toLong())
      }
    }
    this.__deleteAdapterOfBlockedAppEntity = object :
        EntityDeleteOrUpdateAdapter<BlockedAppEntity>() {
      protected override fun createQuery(): String =
          "DELETE FROM `blocked_apps` WHERE `packageName` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: BlockedAppEntity) {
        statement.bindText(1, entity.packageName)
      }
    }
  }

  public override suspend fun upsert(app: BlockedAppEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfBlockedAppEntity.insert(_connection, app)
  }

  public override suspend fun delete(app: BlockedAppEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __deleteAdapterOfBlockedAppEntity.handle(_connection, app)
  }

  public override fun getAllBlocked(): Flow<List<BlockedAppEntity>> {
    val _sql: String = "SELECT * FROM blocked_apps"
    return createFlow(__db, false, arrayOf("blocked_apps")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfPackageName: Int = getColumnIndexOrThrow(_stmt, "packageName")
        val _columnIndexOfAppName: Int = getColumnIndexOrThrow(_stmt, "appName")
        val _columnIndexOfAppIconBase64: Int = getColumnIndexOrThrow(_stmt, "appIconBase64")
        val _columnIndexOfBlockNotifications: Int = getColumnIndexOrThrow(_stmt,
            "blockNotifications")
        val _columnIndexOfBlockLaunch: Int = getColumnIndexOrThrow(_stmt, "blockLaunch")
        val _result: MutableList<BlockedAppEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BlockedAppEntity
          val _tmpPackageName: String
          _tmpPackageName = _stmt.getText(_columnIndexOfPackageName)
          val _tmpAppName: String
          _tmpAppName = _stmt.getText(_columnIndexOfAppName)
          val _tmpAppIconBase64: String
          _tmpAppIconBase64 = _stmt.getText(_columnIndexOfAppIconBase64)
          val _tmpBlockNotifications: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfBlockNotifications).toInt()
          _tmpBlockNotifications = _tmp != 0
          val _tmpBlockLaunch: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfBlockLaunch).toInt()
          _tmpBlockLaunch = _tmp_1 != 0
          _item =
              BlockedAppEntity(_tmpPackageName,_tmpAppName,_tmpAppIconBase64,_tmpBlockNotifications,_tmpBlockLaunch)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getNotifBlockedPackages(): List<String> {
    val _sql: String = "SELECT packageName FROM blocked_apps WHERE blockNotifications = 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: MutableList<String> = mutableListOf()
        while (_stmt.step()) {
          val _item: String
          _item = _stmt.getText(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getLaunchBlockedPackages(): List<String> {
    val _sql: String = "SELECT packageName FROM blocked_apps WHERE blockLaunch = 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: MutableList<String> = mutableListOf()
        while (_stmt.step()) {
          val _item: String
          _item = _stmt.getText(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteByPackage(pkg: String) {
    val _sql: String = "DELETE FROM blocked_apps WHERE packageName = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, pkg)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
