package com.example.parkmate.ui.visualization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkmate.data.local.entity.ParkingType
import com.example.parkmate.data.local.entity.computeCost
import com.example.parkmate.data.repository.ParkingRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Modello dati per il conteggio delle sessioni raggruppate per tipologia.
data class TypeCount(val label: String, val count: Int)

// Modello dati per l'aggregazione dei costi raggruppati per tipologia.
data class CostAggregation(val label: String, val totalCost: Double)

// ViewModel che elabora i dati storici dei parcheggi per la visualizzazione statistica.
class DataViewModel (private val parkingRepository: ParkingRepository) : ViewModel(){

    // Flusso della cronologia completa dei parcheggi recuperato dal database
    private val history = parkingRepository.getHistory()

    // StateFlow che fornisce l'elenco delle coordinate geografiche di tutti i parcheggi passati.
    val heatmapPoints: StateFlow<List<LatLng>> = history
        .map { sessions -> sessions.map { LatLng(it.latitude, it.longitude) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    //StateFlow che calcola la frequenza di utilizzo di ogni tipologia di parcheggio.
    val countByType: StateFlow<List<TypeCount>> = history
        .map { sessions ->
            ParkingType.entries.map { type ->
                TypeCount(label = type.name, count = sessions.count { it.type == type })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // StateFlow che calcola la spesa totale accumulata in tutte le sessioni storiche.
    val totalCost: StateFlow<Double> = history
        .map { sessions -> sessions.sumOf { it.computeCost() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // StateFlow che aggrega i costi totali suddivisi per tipologia di parcheggio.
    val costByType: StateFlow<List<CostAggregation>> = history
        .map { sessions ->
            ParkingType.entries
                .map { type -> CostAggregation(type.name, sessions.filter { it.type == type }.sumOf { it.computeCost() }) }
                .filter { it.totalCost > 0.0 }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}