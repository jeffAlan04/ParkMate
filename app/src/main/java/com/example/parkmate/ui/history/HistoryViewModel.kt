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

// Range temporali per filtrare la cronologia
enum class TimeRange { TODAY, WEEK, MONTH }

// Criteri di filtraggio per lo storico
sealed class HistoryFilter {
    object None : HistoryFilter()
    data class ByVehicle(val vehicleId: Long) : HistoryFilter()
    data class ByPeriod(val range: TimeRange) : HistoryFilter()
    data class ByType(val type: ParkingType) : HistoryFilter()
}

data class HistoryEntry(
    val session: ParkingSession,
    val vehicleName: String,
    val cost: Double
)

// ViewModel che gestisce la logica di visualizzazione e filtraggio della cronologia
class HistoryViewModel(
    private val parkingRepository: ParkingRepository, 
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    // Stato interno del filtro corrente (inizialmente nessuno)
    private val _filter = MutableStateFlow<HistoryFilter>(HistoryFilter.None)
    val filter: StateFlow<HistoryFilter> = _filter

    // StateFlow per l'elenco completo dei veicoli
    val vehicles: StateFlow<List<Vehicle>> = vehicleRepository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // StateFlow che mostra la cronologia filtrata
    val filteredHistory: StateFlow<List<HistoryEntry>> = combine(
        parkingRepository.getHistory(),
        vehicles,
        _filter
    ) {
        sessions, vehicleList, currentFilter ->
        sessions
            // pplica il filtro selezionato dall'utente
            .filter { session -> matchesFilter(session, currentFilter) }
            //  Trasforma ogni sessione in un oggetto HistoryEntry con dati calcolati
            .map { session ->
                HistoryEntry(
                    session = session,
                    vehicleName = vehicleList.find { it.id == session.vehicleId }?.name ?: "Veicolo",
                    cost = session.computeCost()
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Aggiorna il filtro in base alla scelta dell'utente.
    fun setFilter(newFilter: HistoryFilter) {
        _filter.value = newFilter
    }

    // Verifica se una sessione soddisfa il criterio del filtro attivo
    private fun matchesFilter(session: ParkingSession, currentFilter: HistoryFilter): Boolean =
        when (currentFilter) {
            is HistoryFilter.None -> true
            is HistoryFilter.ByVehicle -> session.vehicleId == currentFilter.vehicleId
            is HistoryFilter.ByPeriod -> isWithinRange(session.startTime, currentFilter.range)
            is HistoryFilter.ByType -> session.type == currentFilter.type
        }

    // Calcola se una data rientra nel range temporale specificato
    private fun isWithinRange(startTime: Long, range: TimeRange): Boolean {
        val millisBack = when (range) {
            TimeRange.TODAY -> 24 * 3600000L
            TimeRange.WEEK -> 7 * 24 * 3600000L
            TimeRange.MONTH -> 30 * 24 * 3600000L
        }
        return startTime >= System.currentTimeMillis() - millisBack

    }
}