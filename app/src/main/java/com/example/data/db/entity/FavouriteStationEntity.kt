package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourite_stations")
data class FavouriteStationEntity(
    @PrimaryKey val stationId: String,
    val savedAt: Long = System.currentTimeMillis()
)
