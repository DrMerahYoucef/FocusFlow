package com.example.data.db.dao

import androidx.room.*
import com.example.data.db.entity.RevisionNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RevisionNoteDao {
    @Query("SELECT * FROM revision_notes WHERE dueDate <= :nowTimestamp ORDER BY dueDate ASC")
    fun getDueNotes(nowTimestamp: Long = System.currentTimeMillis()): Flow<List<RevisionNoteEntity>>

    @Query("SELECT * FROM revision_notes WHERE dueDate <= :nowTimestamp ORDER BY dueDate ASC")
    suspend fun getDueNotesOnce(nowTimestamp: Long = System.currentTimeMillis()): List<RevisionNoteEntity>

    @Query("SELECT * FROM revision_notes WHERE deckId = :deckId AND dueDate <= :nowTimestamp ORDER BY dueDate ASC")
    fun getDueNotesForDeck(deckId: String, nowTimestamp: Long = System.currentTimeMillis()): Flow<List<RevisionNoteEntity>>

    @Query("SELECT * FROM revision_notes WHERE deckId = :deckId ORDER BY createdAt DESC")
    fun getNotesForDeck(deckId: String): Flow<List<RevisionNoteEntity>>

    @Query("SELECT * FROM revision_notes WHERE deckId = :deckId ORDER BY createdAt DESC")
    suspend fun getNotesForDeckOnce(deckId: String): List<RevisionNoteEntity>

    @Query("SELECT * FROM revision_notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<RevisionNoteEntity>>

    @Query("SELECT * FROM revision_notes ORDER BY createdAt DESC")
    suspend fun getAllNotesOnce(): List<RevisionNoteEntity>

    @Query("SELECT * FROM revision_notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RevisionNoteEntity?

    @Query("SELECT COUNT(*) FROM revision_notes WHERE dueDate <= :nowTimestamp")
    suspend fun getDueCount(nowTimestamp: Long = System.currentTimeMillis()): Int

    @Query("SELECT COUNT(*) FROM revision_notes")
    suspend fun getTotalCount(): Int

    @Upsert
    suspend fun upsert(note: RevisionNoteEntity)

    @Upsert
    suspend fun upsertAll(notes: List<RevisionNoteEntity>)

    @Delete
    suspend fun delete(note: RevisionNoteEntity)

    @Query("DELETE FROM revision_notes WHERE deckId = :deckId")
    suspend fun deleteNotesForDeck(deckId: String)

    @Query("DELETE FROM revision_notes")
    suspend fun deleteAll()
}
