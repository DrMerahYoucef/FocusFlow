package com.focusisland.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,              // epoch millis
    val durationSeconds: Int,    // actual focus seconds completed
    val completed: Boolean,      // false if skipped mid-session
    val focusScore: Int          // score = durationSeconds / 60 (1 pt per minute)
)
