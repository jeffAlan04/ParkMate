package com.example.parkmate.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.parkmate.ParkMateApplication
import com.example.parkmate.data.local.entity.ParkingType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Gestisce le azioni rapide provenienti dalle notifiche
class GeofenceActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Verifica che l'intent ricevuto sia quello per terminare un parcheggio
        if (intent.action != ACTION_END_PARKING)
            return

        // Estrae i parametri necessari dalla notifica (ID sessione e ID notifica)
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        if (sessionId == -1L)
            return

        // Permette di eseguire operazioni lunghe senza bloccare il thread principale
        val pending = goAsync()
        
        // Avvia una coroutine per aggiornare il database
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Recupera il container delle dipendenze per accedere ai repository e scheduler
                val container = (context.applicationContext as ParkMateApplication).container
                
                // Cerca la sessione attiva corrispondente all'ID ricevuto
                val session = container.parkingRepository.getActiveSession().first()
                    .find { it.id == sessionId }

                if (session != null) {
                    // Chiude la sessione di parcheggio nel database
                    container.parkingRepository.endSession(session)
                    // Cancella eventuali allarmi di scadenza programmati per questa sessione
                    container.parkingAlarmScheduler.cancelFor(session)
                }

                // Chiude la notifica di sistema se è stato fornito un ID valido
                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }
            } finally {
                // Operazione completata
                pending.finish()
            }
        }
    }

    companion object {
        // Stringa di azione per identificare l'intent di chiusura parcheggio
        const val ACTION_END_PARKING = "com.example.parkmate.action.END_PARKING"
        
        // Chiavi per gli extra contenuti nell'Intent
        const val EXTRA_SESSION_ID = "sessionId"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
    }
}