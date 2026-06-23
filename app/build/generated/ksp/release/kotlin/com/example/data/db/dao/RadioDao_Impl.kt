package com.example.`data`.db.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performInTransactionSuspending
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.example.`data`.db.entity.CategoryEntity
import com.example.`data`.db.entity.StationEntity
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
public class RadioDao_Impl(
  __db: RoomDatabase,
) : RadioDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCategoryEntity: EntityInsertAdapter<CategoryEntity>

  private val __insertAdapterOfCategoryEntity_1: EntityInsertAdapter<CategoryEntity>

  private val __insertAdapterOfStationEntity: EntityInsertAdapter<StationEntity>

  private val __insertAdapterOfStationEntity_1: EntityInsertAdapter<StationEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfCategoryEntity = object : EntityInsertAdapter<CategoryEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `radio_categories` (`id`,`name`,`isCustom`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CategoryEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        val _tmp: Int = if (entity.isCustom) 1 else 0
        statement.bindLong(3, _tmp.toLong())
      }
    }
    this.__insertAdapterOfCategoryEntity_1 = object : EntityInsertAdapter<CategoryEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR IGNORE INTO `radio_categories` (`id`,`name`,`isCustom`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CategoryEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        val _tmp: Int = if (entity.isCustom) 1 else 0
        statement.bindLong(3, _tmp.toLong())
      }
    }
    this.__insertAdapterOfStationEntity = object : EntityInsertAdapter<StationEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR IGNORE INTO `radio_stations` (`id`,`name`,`country`,`categoryId`,`streamUrl`,`fallbackUrl`,`logoUrl`,`description`,`isCustom`) VALUES (?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: StationEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.country)
        statement.bindText(4, entity.categoryId)
        statement.bindText(5, entity.streamUrl)
        statement.bindText(6, entity.fallbackUrl)
        statement.bindText(7, entity.logoUrl)
        statement.bindText(8, entity.description)
        val _tmp: Int = if (entity.isCustom) 1 else 0
        statement.bindLong(9, _tmp.toLong())
      }
    }
    this.__insertAdapterOfStationEntity_1 = object : EntityInsertAdapter<StationEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `radio_stations` (`id`,`name`,`country`,`categoryId`,`streamUrl`,`fallbackUrl`,`logoUrl`,`description`,`isCustom`) VALUES (?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: StationEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.country)
        statement.bindText(4, entity.categoryId)
        statement.bindText(5, entity.streamUrl)
        statement.bindText(6, entity.fallbackUrl)
        statement.bindText(7, entity.logoUrl)
        statement.bindText(8, entity.description)
        val _tmp: Int = if (entity.isCustom) 1 else 0
        statement.bindLong(9, _tmp.toLong())
      }
    }
  }

  public override suspend fun insertCategory(category: CategoryEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfCategoryEntity.insert(_connection, category)
  }

  public override suspend fun insertCategories(categories: List<CategoryEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfCategoryEntity_1.insert(_connection, categories)
  }

  public override suspend fun insertStations(stations: List<StationEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfStationEntity.insert(_connection, stations)
  }

  public override suspend fun insertStation(station: StationEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfStationEntity_1.insert(_connection, station)
  }

  public override suspend fun deleteCategoryAndStations(id: String): Unit =
      performInTransactionSuspending(__db) {
    super@RadioDao_Impl.deleteCategoryAndStations(id)
  }

  public override fun getAllCategories(): Flow<List<CategoryEntity>> {
    val _sql: String = "SELECT * FROM radio_categories ORDER BY isCustom ASC, name ASC"
    return createFlow(__db, false, arrayOf("radio_categories")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfIsCustom: Int = getColumnIndexOrThrow(_stmt, "isCustom")
        val _result: MutableList<CategoryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CategoryEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpIsCustom: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsCustom).toInt()
          _tmpIsCustom = _tmp != 0
          _item = CategoryEntity(_tmpId,_tmpName,_tmpIsCustom)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getCategoryCount(): Int {
    val _sql: String = "SELECT COUNT(*) FROM radio_categories"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllStations(): Flow<List<StationEntity>> {
    val _sql: String = "SELECT * FROM radio_stations ORDER BY name ASC"
    return createFlow(__db, false, arrayOf("radio_stations")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCountry: Int = getColumnIndexOrThrow(_stmt, "country")
        val _columnIndexOfCategoryId: Int = getColumnIndexOrThrow(_stmt, "categoryId")
        val _columnIndexOfStreamUrl: Int = getColumnIndexOrThrow(_stmt, "streamUrl")
        val _columnIndexOfFallbackUrl: Int = getColumnIndexOrThrow(_stmt, "fallbackUrl")
        val _columnIndexOfLogoUrl: Int = getColumnIndexOrThrow(_stmt, "logoUrl")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfIsCustom: Int = getColumnIndexOrThrow(_stmt, "isCustom")
        val _result: MutableList<StationEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: StationEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCountry: String
          _tmpCountry = _stmt.getText(_columnIndexOfCountry)
          val _tmpCategoryId: String
          _tmpCategoryId = _stmt.getText(_columnIndexOfCategoryId)
          val _tmpStreamUrl: String
          _tmpStreamUrl = _stmt.getText(_columnIndexOfStreamUrl)
          val _tmpFallbackUrl: String
          _tmpFallbackUrl = _stmt.getText(_columnIndexOfFallbackUrl)
          val _tmpLogoUrl: String
          _tmpLogoUrl = _stmt.getText(_columnIndexOfLogoUrl)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpIsCustom: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsCustom).toInt()
          _tmpIsCustom = _tmp != 0
          _item =
              StationEntity(_tmpId,_tmpName,_tmpCountry,_tmpCategoryId,_tmpStreamUrl,_tmpFallbackUrl,_tmpLogoUrl,_tmpDescription,_tmpIsCustom)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteCategoryOnly(id: String) {
    val _sql: String = "DELETE FROM radio_categories WHERE id = ? AND isCustom = 1"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteCategoryForce(id: String) {
    val _sql: String = "DELETE FROM radio_categories WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteStationsByCategoryId(categoryId: String) {
    val _sql: String = "DELETE FROM radio_stations WHERE categoryId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, categoryId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteStation(id: String) {
    val _sql: String = "DELETE FROM radio_stations WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteCustomStation(id: String) {
    val _sql: String = "DELETE FROM radio_stations WHERE id = ? AND isCustom = 1"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
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
