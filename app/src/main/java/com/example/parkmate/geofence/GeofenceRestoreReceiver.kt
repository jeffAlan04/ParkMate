package com.example.parkmate.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.parkmate.ParkMateApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Ripristinare i Geofence al riavvio del dispositivo.
class GeofenceRestoreReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Verifica che l'evento ricevuto sia effettivamente il completamento dell'avvio
        if (intent.action != Intent.ACTION_BOOT_COMPLETED)
            return

        // Recupera l'istanza dell'applicazione per accedere al container delle dipendenze
        val app = context.applicationContext as ParkMateApplication
        
        // Permette al Receiver di continuare l'esecuzione in background
        val pendingResult = goAsync()

        // Avvia una coroutine
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Recupera la lista aggiornata di tutti i luoghi salvati dall'utente
                val locations = app.container.locationRepository.getAllLocations().first()
                
                // Registra nuovamente tutti i geofence associati ai luoghi salvati
                app.container.geofenceManager.registerAll(locations)
            } finally {
                // Segnala che l'operazione è terminata
                pendingResult.finish()
            }
        }
    }
}