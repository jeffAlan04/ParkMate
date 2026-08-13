package com.example.parkmate.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.parkmate.data.local.entity.Vehicle
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY name")
    fun getAll(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :vehicleId")
    suspend fun getById(vehicleId: Long): Vehicle?

    @Insert
    suspend fun insert(vehicle: Vehicle): Long

    @Update
    suspend fun update(vehicle: Vehicle)

    @Delete
    suspend fun delete(vehicle: Vehicle)
}