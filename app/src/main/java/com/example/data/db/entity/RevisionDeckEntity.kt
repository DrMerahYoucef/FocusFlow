package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "revision_decks")
data class RevisionDeckEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String = "#4CAF50",
    val createdAt: Long = System.currentTimeMillis()
)
