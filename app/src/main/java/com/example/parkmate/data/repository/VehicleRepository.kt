package com.example.parkmate.data.repository

import com.example.parkmate.data.local.entity.Vehicle
import kotlinx.coroutines.flow.Flow

// Esisto di un salvataggio
sealed interface VehicleSaveResult {
    data object Success : VehicleSaveResult
    data object InvalidPlate : VehicleSaveResult
    data object DuplicatePlate : VehicleSaveResult
    data object DuplicateBicycleName : VehicleSaveResult
}
interface VehicleRepository {
    fun getAllVehicles(): Flow<List<Vehicle>>
    suspend fun getVehicleById(id: Long): Vehicle?
    suspend fun addVehicle(vehicle: Vehicle): VehicleSaveResult
    suspend fun updateVehicle(vehicle: Vehicle): VehicleSaveResult
    suspend fun deleteVehicle(vehicle: Vehicle)
}
