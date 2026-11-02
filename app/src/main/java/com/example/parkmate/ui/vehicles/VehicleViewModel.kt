package com.example.parkmate.ui.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkmate.data.local.entity.Vehicle
import com.example.parkmate.data.local.entity.VehicleType
import com.example.parkmate.data.repository.ParkingRepository
import com.example.parkmate.data.repository.VehicleRepository
import com.example.parkmate.data.repository.VehicleSaveResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VehicleListItem(val vehicle: Vehicle, val isParked: Boolean)
class VehicleViewModel(private val repository: VehicleRepository, parkingRepository: ParkingRepository) : ViewModel() {

    val vehicles: StateFlow<List<Vehicle>> = repository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vehicleItems: StateFlow<List<VehicleListItem>?> = combine(
        repository.getAllVehicles(),
        parkingRepository.getActiveSession()
    ) { vehicles, sessions ->
        val parkedIds = sessions.map { it.vehicleId }.toSet()
        vehicles.map { VehicleListItem(it, isParked = it.id in parkedIds) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun getVehicleById(id: Long): Vehicle? = repository.getVehicleById(id)

    suspend fun saveVehicle(
        vehicleId: Long?,
        name: String,
        type: VehicleType,
        plateNumber: String?
    ): VehicleSaveResult {
        val vehicle = Vehicle(
            id = vehicleId ?: 0,
            name = name,
            type = type,
            plateNumber = plateNumber
        )
        return if (vehicleId == null) repository.addVehicle(vehicle)
        else repository.updateVehicle(vehicle)
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
        }
    }
}