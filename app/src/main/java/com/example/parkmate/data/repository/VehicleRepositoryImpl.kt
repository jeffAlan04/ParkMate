package com.example.parkmate.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.example.parkmate.data.local.dao.VehicleDao
import com.example.parkmate.data.local.entity.Vehicle
import com.example.parkmate.data.local.entity.VehiclePlate
import com.example.parkmate.data.local.entity.VehicleType
import kotlinx.coroutines.flow.Flow

class VehicleRepositoryImpl (private val dao: VehicleDao) : VehicleRepository{
    // Restituisce tutti i veicoli nel DB
    override fun getAllVehicles(): Flow<List<Vehicle>> = dao.getAll()

    // Restituisce un veicolo con un certo ID
    override suspend fun getVehicleById(id: Long): Vehicle? = dao.getById(id)

    // Aggiunge un veicolo al DB
    override suspend fun addVehicle(vehicle: Vehicle): VehicleSaveResult {
        val validationResult = validate(vehicle)
        if (validationResult != VehicleSaveResult.Success) return validationResult
        return try {
            dao.insert(vehicle)
            VehicleSaveResult.Success
        } catch (constrainExcpetion: SQLiteConstraintException) {
            VehicleSaveResult.DuplicatePlate
        }
    }

    // Aggiorna un veicolo esistente
    override suspend fun updateVehicle(vehicle: Vehicle): VehicleSaveResult {
        val validationResult = validate(vehicle)
        if (validationResult != VehicleSaveResult.Success) return validationResult
        return try {
            dao.update(vehicle)
            VehicleSaveResult.Success
        } catch (constrainExcpetion: SQLiteConstraintException) {
            VehicleSaveResult.DuplicatePlate
        }
    }

    // Rimuove un veicolo esistente
    override suspend fun deleteVehicle(vehicle: Vehicle) {
        dao.delete(vehicle)
    }


    private suspend fun validate(vehicle: Vehicle): VehicleSaveResult {
        if (vehicle.type == VehicleType.BICYCLE) {
            val bicycleWithSameName = dao.countBicyclesByName(vehicle.name, vehicle.id)
            return if (bicycleWithSameName > 0) VehicleSaveResult.DuplicateBicycleName
            else VehicleSaveResult.Success
        }
        val plateNumber = vehicle.plateNumber ?: return VehicleSaveResult.InvalidPlate
        if (!VehiclePlate.isValid(vehicle.type, plateNumber)) return VehicleSaveResult.InvalidPlate
        if (dao.countByPlate(plateNumber, vehicle.id) > 0) return VehicleSaveResult.DuplicatePlate
        return VehicleSaveResult.Success
    }
}