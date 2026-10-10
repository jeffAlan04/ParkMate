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

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val transition = event.geofenceTransition
        val triggering = event.triggeringGeofences ?: return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as ParkMateApplication).container
                val locations = container.locationRepository.getAllLocations().first()
                val sessions = container.parkingRepository.getActiveSession().first()
                val vehicles = container.vehicleRepository.getAllVehicles().first()

                triggering.forEach { geofence ->
                    val location = locations.find { it.id.toString() == geofence.requestId }
                        ?: return@forEach

                    when (transition) {
                        Geofence.GEOFENCE_TRANSITION_ENTER ->
                            GeofenceNotifier.notifyEnter(context, location)

                        Geofence.GEOFENCE_TRANSITION_EXIT ->
                            sessions.filter { isNear(it, location) }.forEach { session ->
                                val vehicleName = vehicles.find { it.id == session.vehicleId }?.name ?: "Veicolo"
                                GeofenceNotifier.notifyExit(context, location, session, vehicleName)
                            }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun isNear(session: ParkingSession, location: SavedLocation): Boolean {
        val result = FloatArray(1)
        Location.distanceBetween(
            session.latitude, session.longitude, location.latitude, location.longitude, result
        )
        return result[0] <= location.geofenceRadius * 2
    }
}