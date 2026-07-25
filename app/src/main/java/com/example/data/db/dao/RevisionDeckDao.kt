package com.example.data.db.dao

import androidx.room.*
import com.example.data.db.entity.RevisionDeckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RevisionDeckDao {
    @Query("SELECT * FROM revision_decks ORDER BY createdAt ASC")
    fun getAllDecks(): Flow<List<RevisionDeckEntity>>

    @Query("SELECT * FROM revision_decks ORDER BY createdAt ASC")
    suspend fun getAllDecksOnce(): List<RevisionDeckEntity>

    @Query("SELECT * FROM revision_decks WHERE id = :id LIMIT 1")
    suspend fun getDeckById(id: String): RevisionDeckEntity?

    @Upsert
    suspend fun upsert(deck: RevisionDeckEntity)

    @Delete
    suspend fun delete(deck: RevisionDeckEntity)

    @Query("DELETE FROM revision_decks")
    suspend fun deleteAll()
}
