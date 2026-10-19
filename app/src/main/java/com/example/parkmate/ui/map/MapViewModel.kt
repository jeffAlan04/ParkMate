package com.example.parkmate.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkmate.data.local.entity.ParkingType
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.data.repository.SavedLocationRepository
import com.example.parkmate.geofence.GeofenceManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

//ViewModel che gestisce la logica dei luoghi salvati dall'utente.
class MapViewModel(private val repository: SavedLocationRepository, private val geofenceManager: GeofenceManager) : ViewModel() {

    // Espone la lista di tutti i luoghi salvati come StateFlow.
    val locations: StateFlow<List<SavedLocation>> = repository.getAllLocations()
        .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), emptyList())

    // Recupera un singolo luogo dal database in base al suo ID univoco.

    suspend fun getLocationById(id: Long): SavedLocation? = repository.getLocationById(id)

    // Aggiunge un nuovo luogo salvandolo nel database
    fun addLocation(name: String, latitude: Double, longitude: Double, parkingType: ParkingType, hourlyRate: Double? = null, fixedCost: Double? = null, geofenceEnabled: Boolean = false, geofenceRadius: Float = 100f) {
        viewModelScope.launch {
            val location = SavedLocation(
                name = name,
                latitude = latitude,
                longitude = longitude,
                parkingType = parkingType,
                hourlyRate = hourlyRate,
                fixedCost = fixedCost,
                geofenceEnabled = geofenceEnabled,
                geofenceRadius = geofenceRadius
            )
            val newId = repository.addLocation(location)

            if (geofenceEnabled)
                geofenceManager.register(location.copy(id = newId))

        }
    }

    // Aggiorna le informazioni di un luogo già esistente
    fun updateLocation(id: Long, name: String, latitude: Double, longitude: Double, parkingType: ParkingType, hourlyRate: Double? = null, fixedCost: Double? = null, geofenceEnabled: Boolean = false, geofenceRadius: Float = 100f) {
        viewModelScope.launch {
            val location = SavedLocation(
                id = id,
                name = name,
                latitude = latitude,
                longitude = longitude,
                parkingType = parkingType,
                hourlyRate = hourlyRate,
                fixedCost = fixedCost,
                geofenceEnabled = geofenceEnabled,
                geofenceRadius = geofenceRadius
            )
            repository.updateLocation(location)

            if (geofenceEnabled)
                geofenceManager.register(location)
            else
                geofenceManager.unregister(id)
        }
    }

    // Rimuove definitivamente un luogo salvato dal database
    fun deleteLocation(location: SavedLocation) {
        viewModelScope.launch {
            repository.deleteLocation(location)
            geofenceManager.unregister(location.id)
        }
    }
}