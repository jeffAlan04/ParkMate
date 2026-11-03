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

// Rappresenta un elemento della lista veicoli e il relativo stato di sosta
data class VehicleListItem(val vehicle: Vehicle, val isParked: Boolean)

// ViewModel che gestisce la logica dei veicoli dell'utente
class VehicleViewModel(
    private val repository: VehicleRepository, 
    parkingRepository: ParkingRepository
) : ViewModel() {

    // Flow State che mostra la lista di tutti i veicoli salvati
    val vehicles: StateFlow<List<Vehicle>> = repository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow State che combina i veicoli con le sessioni attive per determinare quali sono parcheggiati
    val vehicleItems: StateFlow<List<VehicleListItem>?> = combine(
        repository.getAllVehicles(),
        parkingRepository.getActiveSession()
    ) { vehicles, sessions ->
        // Crea un set di ID dei veicoli che hanno una sessione di parcheggio attiva
        val parkedIds = sessions.map { it.vehicleId }.toSet()
        // Aggiunge un flag isParked a ciascun veicolo per indicare se è parcheggiato o meno
        vehicles.map { VehicleListItem(it, isParked = it.id in parkedIds) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Recupera un singolo veicolo in base al suo ID univoco.
    suspend fun getVehicleById(id: Long): Vehicle? = repository.getVehicleById(id)

    // Salva i dati di un veicolo (sia inserimento che aggiornamento)
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

    // Elimina un veicolo dal database
    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
        }
    }
}