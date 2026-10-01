package com.example.parkmate.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.local.entity.ParkingType
import com.example.parkmate.data.local.entity.Vehicle
import com.example.parkmate.data.local.entity.computeCost
import com.example.parkmate.data.repository.ParkingRepository
import com.example.parkmate.data.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class TimeRange { TODAY, WEEK, MONTH }

sealed class HistoryFilter {
    object None : HistoryFilter()
    data class ByVehicle(val vehicleId: Long) : HistoryFilter()
    data class ByPeriod(val range: TimeRange) : HistoryFilter()
    data class ByType(val type: ParkingType) : HistoryFilter()
}

data class HistoryEnter(
    val session: ParkingSession,
    val vehicleName: String,
    val cost: Double
)

class HistoryViewModel(private val parkingRepository: ParkingRepository, private val vehicleRepository: VehicleRepository) : ViewModel() {

    private val _filter = MutableStateFlow<HistoryFilter>(HistoryFilter.None)
    val filter: StateFlow<HistoryFilter> = _filter

    val vehicles: StateFlow<List<Vehicle>> = vehicleRepository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredHistory: StateFlow<List<HistoryEnter>> = combine(
        parkingRepository.getHistory(),
        vehicles,
        _filter
    ) {
        sessions, vehicleList, currentFilter ->
        sessions
            .filter { it.endTime != null }
            .filter { session -> matchesFilter(session, currentFilter) }
            .sortedByDescending { it.startTime }
            .map { session ->
                HistoryEnter(
                    session = session,
                    vehicleName = vehicleList.find { it.id == session.vehicleId }?.name
                        ?: "Veicolo",
                    cost = session.computeCost()
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(newFilter: HistoryFilter) {
        _filter.value = newFilter
    }

    private fun matchesFilter(session: ParkingSession, currentFilter: HistoryFilter): Boolean =
        when (currentFilter) {
            is HistoryFilter.None -> true
            is HistoryFilter.ByVehicle -> session.vehicleId == currentFilter.vehicleId
            is HistoryFilter.ByPeriod -> isWithinRange(session.startTime, currentFilter.range)
            is HistoryFilter.ByType -> session.type == currentFilter.type
        }

    private fun isWithinRange(startTime: Long, range: TimeRange): Boolean {
        val millisBack = when (range) {
            TimeRange.TODAY -> 24 * 3600000
            TimeRange.WEEK -> 7 * 24 * 3600000
            TimeRange.MONTH -> 30 * 24 * 3600000
        }
        return startTime >= System.currentTimeMillis() - millisBack

    }
}