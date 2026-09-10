package com.example.parkmate.data.repository

import com.example.parkmate.data.local.dao.VehicleDao
import com.example.parkmate.data.local.entity.Vehicle
import kotlinx.coroutines.flow.Flow

class VehicleRepositoryImpl (private val dao: VehicleDao) : VehicleRepository{
    // Restituisce tutti i veicoli nel DB
    override fun getAllVehicles(): Flow<List<Vehicle>> = dao.getAll()

    // Restituisce un veicolo con un certo ID
    override suspend fun getVehicleById(id: Long): Vehicle? = dao.getById(id)

    // Aggiunge un veicolo al DB
    override suspend fun addVehicle(vehicle: Vehicle) {
        dao.insert(vehicle)
    }

    // Aggiorna un veicolo esistente
    override suspend fun updateVehicle(vehicle: Vehicle) {
        dao.update(vehicle)
    }

    // Rimuove un veicolo esistente
    override suspend fun deleteVehicle(vehicle: Vehicle) {
        dao.delete(vehicle)
    }
}