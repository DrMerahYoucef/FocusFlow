package com.example.data.db.dao

import androidx.room.*
import com.example.data.db.entity.FavouriteStationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteStationDao {
    @Query("SELECT stationId FROM favourite_stations ORDER BY savedAt DESC")
    fun getAllFavourites(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(fav: FavouriteStationEntity)

    @Query("DELETE FROM favourite_stations WHERE stationId = :id")
    suspend fun remove(id: String)

    @Query("SELECT COUNT(*) FROM favourite_stations WHERE stationId = :id")
    suspend fun isFavourite(id: String): Int
}
