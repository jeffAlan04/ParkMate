package com.example.parkmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.parkmate.data.local.entity.ParkingSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingSessionDao {
    @Query("SELECT * FROM parkingSession WHERE endTime IS NULL")
    fun getActiveSessions(): Flow<List<ParkingSession>>

    @Query("SELECT * FROM parkingSession WHERE endTime IS NOT NULL ORDER BY startTime DESC")
    fun getHistory(): Flow<List<ParkingSession>>

    @Query("SELECT * FROM parkingSession WHERE vehicleId = :vehicleId ORDER BY startTime DESC")
    fun getByVehicle(vehicleId: Long): Flow<List<ParkingSession>>

    @Insert
    suspend fun insert(session: ParkingSession): Long

    @Update
    suspend fun update(session: ParkingSession)
}