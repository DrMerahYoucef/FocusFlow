package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "radio_stations",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class StationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val country: String,
    val categoryId: String,
    val streamUrl: String,
    val fallbackUrl: String = "",
    val logoUrl: String = "",
    val description: String,
    val isCustom: Boolean = false
)
