package com.example.parkmate.data.repository

import com.example.parkmate.data.local.entity.ParkingSession
import kotlinx.coroutines.flow.Flow

interface ParkingRepository {
    fun getActiveSession(): Flow<List<ParkingSession>>
    fun getHistory(): Flow<List<ParkingSession>>
    fun getVehicle(vehicleId: Long): Flow<List<ParkingSession>>

    suspend fun startSession(session: ParkingSession)
    suspend fun endSession(session: ParkingSession)
}