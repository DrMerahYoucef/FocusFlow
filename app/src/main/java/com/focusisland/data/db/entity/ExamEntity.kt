package com.focusisland.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val examDate: Long,          // epoch millis
    val subject: String,
    val color: Int               // ARGB for card accent
)
