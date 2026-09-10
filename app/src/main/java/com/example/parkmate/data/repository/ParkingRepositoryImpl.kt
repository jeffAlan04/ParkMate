package com.example.parkmate.data.repository

import com.example.parkmate.data.local.dao.ParkingSessionDao
import com.example.parkmate.data.local.entity.ParkingSession
import kotlinx.coroutines.flow.Flow

class ParkingRepositoryImpl(private val dao: ParkingSessionDao) : ParkingRepository {

    // Restituisce le sessioni attive di parcheggio
    override fun getActiveSession(): Flow<List<ParkingSession>> = dao.getActiveSessions()

    // Restituisce lo storico delle sessioni terminate
    override fun getHistory(): Flow<List<ParkingSession>> = dao.getHistory()

    // Restituisce le sessioni di un veicolo
    override fun getVehicle(vehicleId: Long): Flow<List<ParkingSession>> = dao.getByVehicle(vehicleId)

    // Avvia una sessione di parcheggio
    override suspend fun startSession(session: ParkingSession) {

        // Controlla se il veicolo ha una sessione attiva
        val existingActive = dao.getActiveSessionForVehicle(session.vehicleId)

        // Se esiste, la termina
        if (existingActive != null) {
            dao.update(existingActive.copy(endTime = System.currentTimeMillis()))
        }

        // Inserisce la nuova sessione nel DB
        dao.insert(session)
    }

    // Termina una sessione
    override suspend fun endSession(session: ParkingSession) {
        // Crea una copia della sessione impostando endTime all'orario corrente e aggiorna nel DB
        dao.update(session.copy(endTime = System.currentTimeMillis()))
    }
}