package com.example.parkmate.ui.parking

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.local.entity.ParkingType
import com.example.parkmate.data.local.entity.Vehicle
import com.example.parkmate.data.repository.LocationRepository
import com.example.parkmate.data.repository.ParkingRepository
import com.example.parkmate.data.repository.VehicleRepository
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.notification.ParkingExpiryWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class ActiveSessionDisplay(
    val session: ParkingSession,
    val vehicleName: String,
    val elapsedMillis: Long,
    val currentCost: Double?,
    val remainingMillis: Long? = null
)

private fun tickerFlow(intervalMillis: Long = 1000L): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(intervalMillis)
    }
}
class ParkingViewModel(
    private val parkingRepository: ParkingRepository,
    vehicleRepository: VehicleRepository,
    locationRepository: LocationRepository,
    private val appContext: Context
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
        expiryTime: Long?,
        warningMinutesBefore: Int?,
        note: String?
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
                    expiryTime = expiryTime,
                    note = note
                )
            )

            if (type == ParkingType.TICKET && expiryTime != null) {
                val vehicleName = vehicles.value.find { it.id == vehicleId }?.name ?: "Veicolo"
                scheduleExpiryNotifications(vehicleId, vehicleName, expiryTime, warningMinutesBefore ?: 15)
            }
        }
    }

    private fun scheduleExpiryNotifications(vehicleId: Long, vehicleName: String, expiryTime: Long, warningMinutesBefore: Int) {
        val now = System.currentTimeMillis()
        val workManager = WorkManager.getInstance(appContext)

        val finalDelay = expiryTime - now
        if (finalDelay > 0) {
            val finalRequest = OneTimeWorkRequestBuilder<ParkingExpiryWorker>()
                .setInitialDelay(finalDelay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("vehicleName" to vehicleName, "isWarning" to false))
                .build()
            workManager.enqueueUniqueWork("parking_expiry_final_$vehicleId", ExistingWorkPolicy.REPLACE, finalRequest)
        }

        val warningDelay = expiryTime - now - warningMinutesBefore * 60000
        if (warningDelay > 0) {
            val warningRequest = OneTimeWorkRequestBuilder<ParkingExpiryWorker>()
                .setInitialDelay(warningDelay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("vehicleName" to vehicleName, "isWarning" to true))
                .build()
            workManager.enqueueUniqueWork("parking_expiry_warning_$vehicleId", ExistingWorkPolicy.REPLACE, warningRequest)
        }
    }

    // StateFlow che espone la lista dei parcheggi attivi, aggiornata in tempo reale
    val activeSessions: StateFlow<List<ActiveSessionDisplay>> = combine(
        // Unisce tre flussi: un timer al secondo, i parcheggi dal DB e la lista veicoli
        tickerFlow(),
        parkingRepository.getActiveSession(),
        vehicleRepository.getAllVehicles()
    ) {_, sessions, vehicles ->
        val now = System.currentTimeMillis()

        // Per ogni sessione attiva calcola i dati dinamici per la visualizzazione
        sessions.map { session ->
            // Cerca il nome del veicolo associato tramite l'id
            val vehicleName = vehicles.find { it.id == session.vehicleId }?.name ?: "Veicolo"
            
            // Calcola il tempo passato dall'avvio
            val elapsed = now - session.startTime

            // Calcolo del costo corrente in base al tipo di parcheggio
            val cost = when (session.type) {
                ParkingType.HOURLY -> session.hourlyRate?.let { rate ->
                    val hourElapesd = elapsed / 3600000.0
                    hourElapesd * rate
                }
                ParkingType.TICKET -> session.fixedCost
                ParkingType.FREE -> null
            }

            val remaining = session.expiryTime?.let { it - now }
            // Creazione  dell'oggetto per l'interfaccia
            ActiveSessionDisplay (
                session = session,
                vehicleName = vehicleName,
                elapsedMillis = elapsed,
                currentCost = cost,
                remainingMillis = remaining
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // Smette di aggiornare se l'UI non è visibile per 5s
        initialValue = emptyList()
    )

    // Termina una sessione di parcheggio
    fun endParking(session: ParkingSession) {
        viewModelScope.launch {
            // Aggiorna lo stato della sessione nel database
            parkingRepository.endSession(session)

            if (session.type == ParkingType.TICKET) {
                val workManager = WorkManager.getInstance(appContext)
                workManager.cancelUniqueWork("parking_expiry_final_${session.vehicleId}")
                workManager.cancelUniqueWork("parking_expiry_warning_${session.vehicleId}")
            }
        }
    }
}