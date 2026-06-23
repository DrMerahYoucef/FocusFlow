package com.example.`data`.db.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.example.`data`.db.entity.SessionEntity
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
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
public class SessionDao_Impl(
  __db: RoomDatabase,
) : SessionDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfSessionEntity: EntityInsertAdapter<SessionEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfSessionEntity = object : EntityInsertAdapter<SessionEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `sessions` (`id`,`date`,`durationSeconds`,`completed`,`focusScore`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SessionEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.date)
        statement.bindLong(3, entity.durationSeconds.toLong())
        val _tmp: Int = if (entity.completed) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        statement.bindLong(5, entity.focusScore.toLong())
      }
    }
  }

  public override suspend fun insert(session: SessionEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfSessionEntity.insert(_connection, session)
  }

  public override fun getSessionsInRange(from: Long, to: Long): Flow<List<SessionEntity>> {
    val _sql: String = "SELECT * FROM sessions WHERE date >= ? AND date <= ? ORDER BY date DESC"
    return createFlow(__db, false, arrayOf("sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, from)
        _argIndex = 2
        _stmt.bindLong(_argIndex, to)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfDurationSeconds: Int = getColumnIndexOrThrow(_stmt, "durationSeconds")
        val _columnIndexOfCompleted: Int = getColumnIndexOrThrow(_stmt, "completed")
        val _columnIndexOfFocusScore: Int = getColumnIndexOrThrow(_stmt, "focusScore")
        val _result: MutableList<SessionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SessionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpDate: Long
          _tmpDate = _stmt.getLong(_columnIndexOfDate)
          val _tmpDurationSeconds: Int
          _tmpDurationSeconds = _stmt.getLong(_columnIndexOfDurationSeconds).toInt()
          val _tmpCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfCompleted).toInt()
          _tmpCompleted = _tmp != 0
          val _tmpFocusScore: Int
          _tmpFocusScore = _stmt.getLong(_columnIndexOfFocusScore).toInt()
          _item = SessionEntity(_tmpId,_tmpDate,_tmpDurationSeconds,_tmpCompleted,_tmpFocusScore)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getTotalFocusSeconds(from: Long, to: Long): Flow<Long?> {
    val _sql: String =
        "SELECT SUM(durationSeconds) FROM sessions WHERE date >= ? AND date <= ? AND completed = 1"
    return createFlow(__db, false, arrayOf("sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, from)
        _argIndex = 2
        _stmt.bindLong(_argIndex, to)
        val _result: Long?
        if (_stmt.step()) {
          val _tmp: Long?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(0)
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getSessionCount(from: Long, to: Long): Flow<Int> {
    val _sql: String =
        "SELECT COUNT(*) FROM sessions WHERE date >= ? AND date <= ? AND completed = 1"
    return createFlow(__db, false, arrayOf("sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, from)
        _argIndex = 2
        _stmt.bindLong(_argIndex, to)
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

  public override fun getTotalScore(from: Long, to: Long): Flow<Int?> {
    val _sql: String = "SELECT SUM(focusScore) FROM sessions WHERE date >= ? AND date <= ?"
    return createFlow(__db, false, arrayOf("sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, from)
        _argIndex = 2
        _stmt.bindLong(_argIndex, to)
        val _result: Int?
        if (_stmt.step()) {
          val _tmp: Int?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(0).toInt()
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllSessions(): Flow<List<SessionEntity>> {
    val _sql: String = "SELECT * FROM sessions ORDER BY date DESC"
    return createFlow(__db, false, arrayOf("sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfDurationSeconds: Int = getColumnIndexOrThrow(_stmt, "durationSeconds")
        val _columnIndexOfCompleted: Int = getColumnIndexOrThrow(_stmt, "completed")
        val _columnIndexOfFocusScore: Int = getColumnIndexOrThrow(_stmt, "focusScore")
        val _result: MutableList<SessionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SessionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpDate: Long
          _tmpDate = _stmt.getLong(_columnIndexOfDate)
          val _tmpDurationSeconds: Int
          _tmpDurationSeconds = _stmt.getLong(_columnIndexOfDurationSeconds).toInt()
          val _tmpCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfCompleted).toInt()
          _tmpCompleted = _tmp != 0
          val _tmpFocusScore: Int
          _tmpFocusScore = _stmt.getLong(_columnIndexOfFocusScore).toInt()
          _item = SessionEntity(_tmpId,_tmpDate,_tmpDurationSeconds,_tmpCompleted,_tmpFocusScore)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllSessionsList(): List<SessionEntity> {
    val _sql: String = "SELECT * FROM sessions ORDER BY date DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfDurationSeconds: Int = getColumnIndexOrThrow(_stmt, "durationSeconds")
        val _columnIndexOfCompleted: Int = getColumnIndexOrThrow(_stmt, "completed")
        val _columnIndexOfFocusScore: Int = getColumnIndexOrThrow(_stmt, "focusScore")
        val _result: MutableList<SessionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SessionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpDate: Long
          _tmpDate = _stmt.getLong(_columnIndexOfDate)
          val _tmpDurationSeconds: Int
          _tmpDurationSeconds = _stmt.getLong(_columnIndexOfDurationSeconds).toInt()
          val _tmpCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfCompleted).toInt()
          _tmpCompleted = _tmp != 0
          val _tmpFocusScore: Int
          _tmpFocusScore = _stmt.getLong(_columnIndexOfFocusScore).toInt()
          _item = SessionEntity(_tmpId,_tmpDate,_tmpDurationSeconds,_tmpCompleted,_tmpFocusScore)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getCompletedCountImmediate(): Int {
    val _sql: String = "SELECT COUNT(*) FROM sessions WHERE completed = 1"
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

  public override suspend fun getTotalFocusMinutesImmediate(): Int {
    val _sql: String =
        "SELECT COALESCE(SUM(durationSeconds), 0) / 60 FROM sessions WHERE completed = 1"
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

  public override suspend fun getAllSessionDates(): List<Long> {
    val _sql: String = "SELECT date FROM sessions WHERE completed = 1 ORDER BY date DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: MutableList<Long> = mutableListOf()
        while (_stmt.step()) {
          val _item: Long
          _item = _stmt.getLong(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
