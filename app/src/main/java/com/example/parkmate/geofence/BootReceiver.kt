package com.example.parkmate.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.parkmate.ParkMateApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED)
            return

        val app = context.applicationContext as ParkMateApplication
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val locations = app.container.locationRepository.getAllLocations().first()
                app.container.geofenceManager.registerAll(locations)
            } finally {
                pendingResult.finish()
            }
        }
    }
}