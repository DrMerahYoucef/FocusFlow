package com.example.`data`.db.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.example.`data`.db.entity.FavouriteStationEntity
import javax.`annotation`.processing.Generated
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
public class FavouriteStationDao_Impl(
  __db: RoomDatabase,
) : FavouriteStationDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfFavouriteStationEntity: EntityInsertAdapter<FavouriteStationEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfFavouriteStationEntity = object :
        EntityInsertAdapter<FavouriteStationEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `favourite_stations` (`stationId`,`savedAt`) VALUES (?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: FavouriteStationEntity) {
        statement.bindText(1, entity.stationId)
        statement.bindLong(2, entity.savedAt)
      }
    }
  }

  public override suspend fun add(fav: FavouriteStationEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfFavouriteStationEntity.insert(_connection, fav)
  }

  public override fun getAllFavourites(): Flow<List<String>> {
    val _sql: String = "SELECT stationId FROM favourite_stations ORDER BY savedAt DESC"
    return createFlow(__db, false, arrayOf("favourite_stations")) { _connection ->
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

  public override suspend fun isFavourite(id: String): Int {
    val _sql: String = "SELECT COUNT(*) FROM favourite_stations WHERE stationId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
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

  public override suspend fun remove(id: String) {
    val _sql: String = "DELETE FROM favourite_stations WHERE stationId = ?"
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
