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
    // Recupera tutti i veicoli
    @Query("SELECT * FROM vehicles ORDER BY name")
    fun getAll(): Flow<List<Vehicle>>

    // Recupera un veicolo con un certo id
    @Query("SELECT * FROM vehicles WHERE id = :vehicleId")
    suspend fun getById(vehicleId: Long): Vehicle?

    // Inserisce un nuovo veicolo
    @Insert
    suspend fun insert(vehicle: Vehicle): Long

    // Aggiorna un veicolo esistente
    @Update
    suspend fun update(vehicle: Vehicle)

    // Rimuove un veicolo esistente
    @Delete
    suspend fun delete(vehicle: Vehicle)
}