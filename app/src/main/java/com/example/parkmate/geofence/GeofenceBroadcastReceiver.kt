package com.example.parkmate.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import com.example.parkmate.ParkMateApplication
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.local.entity.SavedLocation
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Intercetta l'ingresso e l'uscita da zone anche se l'app è chiusa
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Estrae l'evento di geofencing dall'Intent ricevuto
        val event = GeofencingEvent.fromIntent(intent) ?: return
        
        // Gestione di eventuali errori nella richiesta di geofencing
        if (event.hasError()) return

        // Tipo di transizione (ingresso o uscita) e lista delle zone che hanno attivato l'evento
        val transition = event.geofenceTransition
        val triggering = event.triggeringGeofences ?: return

        // Permette di completare le operazioni asincrone nel blocco coroutine.
        val pending = goAsync()
        
        // Avvia una coroutine
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Accesso alle dipendenze globali tramite il container dell'applicazione
                val container = (context.applicationContext as ParkMateApplication).container
                
                // Recupera luoghi salvati, sessioni attive e veicoli
                val locations = container.savedLocationRepository.getAllLocations().first()
                val sessions = container.parkingRepository.getActiveSession().first()
                val vehicles = container.vehicleRepository.getAllVehicles().first()

                // Cicla su tutti i Geofence attivati
                triggering.forEach { geofence ->
                    // Trova il luogo corrispondente salvato nel database usando l'ID univoco del Geofence
                    val location = locations.find { it.id.toString() == geofence.requestId }
                        ?: return@forEach

                    when (transition) {
                        // Caso Ingresso
                        Geofence.GEOFENCE_TRANSITION_ENTER ->
                            GeofenceNotifier.notifyEnter(context, location)

                        // Caso Uscita
                        Geofence.GEOFENCE_TRANSITION_EXIT -> {
                            val parked = sessions
                                .filter { isNear(it, location) }
                                .map { session ->
                                    GeofenceNotifier.ParkedVehicle(
                                        session = session,
                                        vehicleName = vehicles.find { it.id == session.vehicleId }?.name ?: "Veicolo"
                                    )
                                }
                            GeofenceNotifier.notifyExit(context, location, parked)
                        }
                    }
                }
            } finally {
                // Operazione completata
                pending.finish()
            }
        }
    }

    // Calcola se una sessione di parcheggio si trova all'interno del geofence radius
    private fun isNear(session: ParkingSession, location: SavedLocation): Boolean {
        val result = FloatArray(1)
        // Calcola la distanza in metri tra le coordinate del parcheggio e quelle del luogo salvato
        Location.distanceBetween(
            session.latitude, session.longitude, location.latitude, location.longitude, result
        )
        // Restituisce vero se il parcheggio è nel raggio del geofence
        return result[0] <= location.geofenceRadius * 2
    }
}