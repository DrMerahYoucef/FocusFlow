package com.focusisland.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "radio_categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isCustom: Boolean = false
)
