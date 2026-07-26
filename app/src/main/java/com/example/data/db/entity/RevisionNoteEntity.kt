package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "revision_notes")
data class RevisionNoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val deckId: String,
    val title: String,
    val contentMarkdown: String, // structured content from Gemini with Markdown & highlight spans
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // --- SM-2 scheduling fields ---
    val easeFactor: Float = 2.5f,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val dueDate: Long = System.currentTimeMillis(), // next review date timestamp (ms)
    val lastReviewedAt: Long? = null,

    // --- sync metadata ---
    val firestoreId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
)
