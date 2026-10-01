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

data class TypeCount(val label: String, val count: Int)
data class CostAggregation(val label: String, val totalCost: Double)

class DataViewModel (private val parkingRepository: ParkingRepository) : ViewModel(){

    private val history = parkingRepository.getHistory()

    val heatmapPoints: StateFlow<List<LatLng>> = history
        .map { sessions -> sessions.map { LatLng(it.latitude, it.longitude) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val countByType: StateFlow<List<TypeCount>> = history
        .map { sessions ->
            ParkingType.entries.map { type ->
                TypeCount(label = type.name, count = sessions.count { it.type == type })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCost: StateFlow<Double> = history
        .map { sessions -> sessions.sumOf { it.computeCost() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val costByType: StateFlow<List<CostAggregation>> = history
        .map { sessions ->
            ParkingType.entries
                .map { type -> CostAggregation(type.name, sessions.filter { it.type == type }.sumOf { it.computeCost() }) }
                .filter { it.totalCost > 0.0 }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}