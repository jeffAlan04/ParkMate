package com.example.parkmate.ui.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.data.local.entity.Vehicle
import com.example.parkmate.data.local.entity.VehicleType
import com.example.parkmate.data.repository.LocationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LocationViewModel(private val repository: LocationRepository) : ViewModel() {

    val locations: StateFlow<List<SavedLocation>> = repository.getAllLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getLocationById(id: Long): SavedLocation? = repository.getLocationById(id)

    fun addLocation(name: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            repository.addLocation(SavedLocation(name = name, latitude = latitude, longitude = longitude))
        }
    }

    fun updateLocation(id: Long, name: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            repository.updateLocation(SavedLocation(id = id, name = name, latitude = latitude, longitude = longitude))
        }
    }

    fun deleteLocation(location: SavedLocation) {
        viewModelScope.launch {
            repository.deleteLocation(location)
        }
    }
}