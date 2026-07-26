package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class RevisionMediaType { IMAGE, AUDIO }

@Entity(tableName = "revision_notes")
data class RevisionNoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val deckId: String,
    val question: String,            // Gemini-generated study question — this is the card FRONT
    val mediaType: RevisionMediaType, // IMAGE or AUDIO
    val mediaFilePath: String,       // local path under app-private storage — this is the card BACK
    val title: String,               // short label for list screens, derived from the question
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // --- SM-2 scheduling fields ---
    val easeFactor: Float = 2.5f,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val dueDate: Long = System.currentTimeMillis(), // next review date timestamp (ms)
    val lastReviewedAt: Long? = null
)
