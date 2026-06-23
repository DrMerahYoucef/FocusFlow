package com.example.`data`.db.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.example.`data`.db.entity.ExamEntity
import javax.`annotation`.processing.Generated
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
public class ExamDao_Impl(
  __db: RoomDatabase,
) : ExamDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfExamEntity: EntityInsertAdapter<ExamEntity>

  private val __deleteAdapterOfExamEntity: EntityDeleteOrUpdateAdapter<ExamEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfExamEntity = object : EntityInsertAdapter<ExamEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `exams` (`id`,`name`,`examDate`,`subject`,`color`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ExamEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindLong(3, entity.examDate)
        statement.bindText(4, entity.subject)
        statement.bindLong(5, entity.color.toLong())
      }
    }
    this.__deleteAdapterOfExamEntity = object : EntityDeleteOrUpdateAdapter<ExamEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `exams` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ExamEntity) {
        statement.bindLong(1, entity.id)
      }
    }
  }

  public override suspend fun insert(exam: ExamEntity): Unit = performSuspending(__db, false, true)
      { _connection ->
    __insertAdapterOfExamEntity.insert(_connection, exam)
  }

  public override suspend fun delete(exam: ExamEntity): Unit = performSuspending(__db, false, true)
      { _connection ->
    __deleteAdapterOfExamEntity.handle(_connection, exam)
  }

  public override fun getAllExams(): Flow<List<ExamEntity>> {
    val _sql: String = "SELECT * FROM exams ORDER BY examDate ASC"
    return createFlow(__db, false, arrayOf("exams")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfExamDate: Int = getColumnIndexOrThrow(_stmt, "examDate")
        val _columnIndexOfSubject: Int = getColumnIndexOrThrow(_stmt, "subject")
        val _columnIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _result: MutableList<ExamEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ExamEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpExamDate: Long
          _tmpExamDate = _stmt.getLong(_columnIndexOfExamDate)
          val _tmpSubject: String
          _tmpSubject = _stmt.getText(_columnIndexOfSubject)
          val _tmpColor: Int
          _tmpColor = _stmt.getLong(_columnIndexOfColor).toInt()
          _item = ExamEntity(_tmpId,_tmpName,_tmpExamDate,_tmpSubject,_tmpColor)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getNextExam(todayStart: Long): ExamEntity? {
    val _sql: String = "SELECT * FROM exams WHERE examDate >= ? ORDER BY examDate ASC LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, todayStart)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfExamDate: Int = getColumnIndexOrThrow(_stmt, "examDate")
        val _columnIndexOfSubject: Int = getColumnIndexOrThrow(_stmt, "subject")
        val _columnIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _result: ExamEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpExamDate: Long
          _tmpExamDate = _stmt.getLong(_columnIndexOfExamDate)
          val _tmpSubject: String
          _tmpSubject = _stmt.getText(_columnIndexOfSubject)
          val _tmpColor: Int
          _tmpColor = _stmt.getLong(_columnIndexOfColor).toInt()
          _result = ExamEntity(_tmpId,_tmpName,_tmpExamDate,_tmpSubject,_tmpColor)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getUpcomingExams(todayStart: Long): List<ExamEntity> {
    val _sql: String = "SELECT * FROM exams WHERE examDate >= ? ORDER BY examDate ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, todayStart)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfExamDate: Int = getColumnIndexOrThrow(_stmt, "examDate")
        val _columnIndexOfSubject: Int = getColumnIndexOrThrow(_stmt, "subject")
        val _columnIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _result: MutableList<ExamEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ExamEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpExamDate: Long
          _tmpExamDate = _stmt.getLong(_columnIndexOfExamDate)
          val _tmpSubject: String
          _tmpSubject = _stmt.getText(_columnIndexOfSubject)
          val _tmpColor: Int
          _tmpColor = _stmt.getLong(_columnIndexOfColor).toInt()
          _item = ExamEntity(_tmpId,_tmpName,_tmpExamDate,_tmpSubject,_tmpColor)
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
