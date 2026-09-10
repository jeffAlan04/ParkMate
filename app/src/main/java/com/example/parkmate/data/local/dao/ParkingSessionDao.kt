package com.example.parkmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.parkmate.data.local.entity.ParkingSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingSessionDao {

    // Recupera le sessioni attive
    @Query("SELECT * FROM parkingSession WHERE endTime IS NULL")
    fun getActiveSessions(): Flow<List<ParkingSession>>

    // Recupera lo storico dei parcheggi terminati
    @Query("SELECT * FROM parkingSession WHERE endTime IS NOT NULL ORDER BY startTime DESC")
    fun getHistory(): Flow<List<ParkingSession>>

    // Recupera le sessioni appartenenti ad un veicolo
    @Query("SELECT * FROM parkingSession WHERE vehicleId = :vehicleId ORDER BY startTime DESC")
    fun getByVehicle(vehicleId: Long): Flow<List<ParkingSession>>

    // Verifica se un veicolo ha una sessione in corso
    @Query("SELECT * FROM parkingSession WHERE vehicleId = :vehicleId AND endTime IS NULL LIMIT 1")
    suspend fun getActiveSessionForVehicle(vehicleId: Long): ParkingSession?

    // Inserisce una nuova sessione di parcheggio
    @Insert
    suspend fun insert(session: ParkingSession): Long

    // Aggiorna una sessione di parcheggio già presente
    @Update
    suspend fun update(session: ParkingSession)
}