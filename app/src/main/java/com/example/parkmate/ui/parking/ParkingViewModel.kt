package com.example.parkmate.ui.parking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.local.entity.ParkingType
import com.example.parkmate.data.local.entity.Vehicle
import com.example.parkmate.data.repository.LocationRepository
import com.example.parkmate.data.repository.ParkingRepository
import com.example.parkmate.data.repository.VehicleRepository
import com.example.parkmate.data.local.entity.SavedLocation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ParkingViewModel(
    private val parkingRepository: ParkingRepository,
    vehicleRepository: VehicleRepository,
    locationRepository: LocationRepository
) : ViewModel() {

    // Recupera tutti i veicoli dal repository
    val vehicles: StateFlow<List<Vehicle>> = vehicleRepository.getAllVehicles()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recupera tutti i luoghi salvati
    val savedLocations: StateFlow<List<SavedLocation>> = locationRepository.getAllLocations()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Avvia una sessione di parcheggio
    fun startParking(
        vehicleId: Long,
        type: ParkingType,
        latitude: Double,
        longitude: Double,
        hourlyRate: Double?,
        fixedCost: Double?,
        expiryTime: Long?
    ) {
        // Esegue l'operazione nel viewModelScope per non bloccare l'UI
        viewModelScope.launch {

            // Crea una nuova sessione di parcheggio e la salva
            parkingRepository.startSession(
                ParkingSession(
                    vehicleId = vehicleId,
                    type = type,
                    startTime = System.currentTimeMillis(),
                    latitude = latitude,
                    longitude = longitude,
                    hourlyRate = hourlyRate,
                    fixedCost = fixedCost,
                    expiryTime = expiryTime
                )
            )
        }
    }

}