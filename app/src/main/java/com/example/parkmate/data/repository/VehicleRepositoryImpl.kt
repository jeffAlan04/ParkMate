package com.example.parkmate.data.repository

import com.example.parkmate.data.local.dao.VehicleDao
import com.example.parkmate.data.local.entity.Vehicle
import kotlinx.coroutines.flow.Flow

class VehicleRepositoryImpl (private val dao: VehicleDao) : VehicleRepository{
    override fun getAllVehicles(): Flow<List<Vehicle>> = dao.getAll()

    override suspend fun getVehicleById(id: Long): Vehicle? = dao.getById(id)

    override suspend fun addVehicle(vehicle: Vehicle) {
        dao.insert(vehicle)
    }

    override suspend fun updateVehicle(vehicle: Vehicle) {
        dao.update(vehicle)
    }

    override suspend fun deleteVehicle(vehicle: Vehicle) {
        dao.delete(vehicle)
    }
}