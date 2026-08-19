package com.example.parkmate.ui.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkmate.data.local.entity.Vehicle
import com.example.parkmate.data.local.entity.VehicleType
import com.example.parkmate.data.repository.VehicleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VehicleViewModel(private val repository: VehicleRepository) : ViewModel() {

    val vehicles: StateFlow<List<Vehicle>> = repository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getVehicleById(id: Long): Vehicle? = repository.getVehicleById(id)

    fun addVehicle(name: String, type: VehicleType, plateNumber: String?) {
        viewModelScope.launch {
            repository.addVehicle(Vehicle(name = name, type = type, plateNumber = plateNumber))
        }
    }

    fun updateVehicle(id: Long, name: String, type: VehicleType, plateNumber: String?) {
        viewModelScope.launch {
            repository.updateVehicle(Vehicle(id = id, name = name, type = type, plateNumber = plateNumber))
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
        }
    }
}