package com.example.data.db.dao

import androidx.room.*
import com.example.data.db.entity.CategoryEntity
import com.example.data.db.entity.StationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RadioDao {
    @Query("SELECT * FROM radio_categories ORDER BY isCustom ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM radio_categories")
    suspend fun getCategoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStations(stations: List<StationEntity>)

    @Query("DELETE FROM radio_categories WHERE id = :id AND isCustom = 1")
    suspend fun deleteCategoryOnly(id: String)

    @Query("DELETE FROM radio_categories WHERE id = :id")
    suspend fun deleteCategoryForce(id: String)

    @Query("DELETE FROM radio_stations WHERE categoryId = :categoryId")
    suspend fun deleteStationsByCategoryId(categoryId: String)

    @Transaction
    suspend fun deleteCategoryAndStations(id: String) {
        deleteStationsByCategoryId(id)
        deleteCategoryOnly(id)
    }

    @Query("SELECT * FROM radio_stations ORDER BY name ASC")
    fun getAllStations(): Flow<List<StationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: StationEntity)

    @Query("DELETE FROM radio_stations WHERE id = :id")
    suspend fun deleteStation(id: String)

    @Query("DELETE FROM radio_stations WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustomStation(id: String)
}
