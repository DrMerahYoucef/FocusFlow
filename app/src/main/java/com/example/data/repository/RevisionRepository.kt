package com.example.data.repository

import com.example.data.db.dao.RevisionDeckDao
import com.example.data.db.dao.RevisionNoteDao
import com.example.data.db.entity.RevisionDeckEntity
import com.example.data.db.entity.RevisionNoteEntity
import com.example.data.db.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

enum class ImportMode { MERGE, REPLACE_ALL }

class RevisionRepository(
    private val deckDao: RevisionDeckDao,
    private val noteDao: RevisionNoteDao
) {
    val allDecks: Flow<List<RevisionDeckEntity>> = deckDao.getAllDecks()
    val dueNotes: Flow<List<RevisionNoteEntity>> = noteDao.getDueNotes()
    val allNotes: Flow<List<RevisionNoteEntity>> = noteDao.getAllNotes()

    fun getNotesForDeck(deckId: String): Flow<List<RevisionNoteEntity>> = noteDao.getNotesForDeck(deckId)
    fun getDueNotesForDeck(deckId: String): Flow<List<RevisionNoteEntity>> = noteDao.getDueNotesForDeck(deckId)

    suspend fun getDueCount(): Int = noteDao.getDueCount()
    suspend fun getTotalCount(): Int = noteDao.getTotalCount()
    suspend fun getAllDecksOnce(): List<RevisionDeckEntity> = deckDao.getAllDecksOnce()
    suspend fun getAllNotesOnce(): List<RevisionNoteEntity> = noteDao.getAllNotesOnce()

    suspend fun upsertDeck(deck: RevisionDeckEntity) = deckDao.upsert(deck)
    suspend fun deleteDeck(deck: RevisionDeckEntity) = deckDao.delete(deck)
    suspend fun deleteDeckAndNotes(deckId: String) {
        deckDao.getDeckById(deckId)?.let { deck ->
            noteDao.deleteNotesForDeck(deckId)
            deckDao.delete(deck)
        }
    }

    suspend fun getNoteById(noteId: String): RevisionNoteEntity? = noteDao.getById(noteId)
    suspend fun getDeckById(deckId: String): RevisionDeckEntity? = deckDao.getDeckById(deckId)

    suspend fun upsertNote(note: RevisionNoteEntity) = noteDao.upsert(note)
    suspend fun upsertNotes(notes: List<RevisionNoteEntity>) = noteDao.upsertAll(notes)
    suspend fun deleteNote(note: RevisionNoteEntity) = noteDao.delete(note)

    suspend fun getUnsyncedNotes() = noteDao.getUnsyncedNotes()
}
