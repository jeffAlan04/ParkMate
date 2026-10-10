package com.example.parkmate.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.parkmate.data.local.entity.SavedLocation
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {
    private val client = LocationServices.getGeofencingClient(context)

    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    @SuppressLint("MissingPermission")
    fun register(location: SavedLocation) {
        val geofence = Geofence.Builder()
            .setRequestId(location.id.toString())
            .setCircularRegion(location.latitude, location.longitude, location.geofenceRadius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(0)
            .addGeofence(geofence)
            .build()

        try {
            client.addGeofences(request, pendingIntent)
        } catch (e: SecurityException) { }
    }

    fun unregister(locationId: Long) {
        client.removeGeofences(listOf(locationId.toString()))
    }

    fun registerAll(locations: List<SavedLocation>) {
        locations.filter { it.geofenceEnabled }.forEach { register(it) }
    }
}