package com.example.parkmate.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.parkmate.data.local.entity.SavedLocation
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

// Gestisce la registrazione e la rimozione dei Geofence
class GeofenceManager(private val context: Context) {
    
    // Client per interagire con il servizio di Geofencing di sistema
    private val client = LocationServices.getGeofencingClient(context)

    // PendingIntent per inviare il broadcast quando si verifica una transizione di Geofence
    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    // Registra un nuovo Geofence
    @SuppressLint("MissingPermission")
    fun register(location: SavedLocation) {
        // Caratteristiche del Geofence (area, raggio, durata)
        val geofence = Geofence.Builder()
            .setRequestId(location.id.toString()) // ID univoco del Geofence
            .setCircularRegion(location.latitude, location.longitude, location.geofenceRadius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE) // Il monitoraggio non scade mai
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        // Richiesta di attivazione
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(0) // Non attiva il trigger immediatamente al momento della registrazione
            .addGeofence(geofence)
            .build()

        try {
            // Aggiungere il geofence
            client.addGeofences(request, pendingIntent)
        } catch (e: SecurityException) {
            // Gestione in caso di permessi revocati improvvisamente
        }
    }

    // Rimozione di un Geofence specifico
    fun unregister(locationId: Long) {
        client.removeGeofences(listOf(locationId.toString()))
    }

    // Registrazione di tutti i luoghi che hanno il geofence abilitato.
    fun registerAll(locations: List<SavedLocation>) {
        locations.filter { it.geofenceEnabled }.forEach { register(it) }
    }
}