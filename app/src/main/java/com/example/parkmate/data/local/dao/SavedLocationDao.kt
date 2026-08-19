package com.example.parkmate.data.local.dao

import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.parkmate.data.local.entity.SavedLocation
import kotlinx.coroutines.flow.Flow

interface SavedLocationDao {

    @Query("SELECT * FROM savedLocations ORDER BY name")
    fun getAll(): Flow<List<SavedLocation>>

    @Insert
    suspend fun insert(location: SavedLocation): Long

    @Update
    suspend fun update(location: SavedLocation): Long

    @Update
    suspend fun delete(location: SavedLocation): Long
}