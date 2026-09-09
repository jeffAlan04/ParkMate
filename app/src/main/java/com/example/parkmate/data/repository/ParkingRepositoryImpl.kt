package com.example.parkmate.data.repository

import com.example.parkmate.data.local.dao.ParkingSessionDao
import com.example.parkmate.data.local.entity.ParkingSession
import kotlinx.coroutines.flow.Flow

class ParkingRepositoryImpl(private val dao: ParkingSessionDao) : ParkingRepository {

    override fun getActiveSession(): Flow<List<ParkingSession>> = dao.getActiveSessions()
    override fun getHistory(): Flow<List<ParkingSession>> = dao.getHistory()
    override fun getVehicle(vehicleId: Long): Flow<List<ParkingSession>> = dao.getByVehicle(vehicleId)

    override suspend fun startSession(session: ParkingSession) {
        val existingActive = dao.getActiveSessionForVehicle(session.vehicleId)

        if (existingActive != null) {
            dao.update(existingActive.copy(endTime = System.currentTimeMillis()))
        }

        dao.insert(session)
    }

    override suspend fun endSession(session: ParkingSession) {
        dao.update(session.copy(endTime = System.currentTimeMillis()))
    }
}