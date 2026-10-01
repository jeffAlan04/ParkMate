package com.example.parkmate.ui.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkmate.data.local.entity.ParkingType
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.data.repository.LocationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

//ViewModel che gestisce la logica dei luoghi salvati dall'utente.
class LocationViewModel(private val repository: LocationRepository) : ViewModel() {

    // Espone la lista di tutti i luoghi salvati come StateFlow.
    val locations: StateFlow<List<SavedLocation>> = repository.getAllLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recupera un singolo luogo dal database in base al suo ID univoco.

    suspend fun getLocationById(id: Long): SavedLocation? = repository.getLocationById(id)

    // Aggiunge un nuovo luogo salvandolo nel database
    fun addLocation(name: String, latitude: Double, longitude: Double, parkingType: ParkingType, hourlyRate: Double? = null, fixedCost: Double? = null) {
        viewModelScope.launch {
            repository.addLocation(SavedLocation(name = name, latitude = latitude, longitude = longitude, parkingType = parkingType, hourlyRate = hourlyRate, fixedCost = fixedCost))
        }
    }

    // Aggiorna le informazioni di un luogo già esistente
    fun updateLocation(id: Long, name: String, latitude: Double, longitude: Double, parkingType: ParkingType, hourlyRate: Double? = null, fixedCost: Double? = null) {
        viewModelScope.launch {
            repository.updateLocation(SavedLocation(id = id, name = name, latitude = latitude, longitude = longitude, parkingType = parkingType, hourlyRate = hourlyRate, fixedCost = fixedCost))
        }
    }

    // Rimuove definitivamente un luogo salvato dal database
    fun deleteLocation(location: SavedLocation) {
        viewModelScope.launch {
            repository.deleteLocation(location)
        }
    }
}