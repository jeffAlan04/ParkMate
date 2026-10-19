package com.example.parkmate.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.parkmate.MainActivity
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.notification.NotificationIntents

// Gestisce la creazione e la visualizzazione delle notifiche relative al Geofencing.
object GeofenceNotifier {

    private const val CHANNEL_ID = "geofence"
    // Offset per le notifiche di uscita per evitare conflitti di ID con quelle di ingresso
    private const val EXIT_NOTIFICATION_OFFSET = 1000000
    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    // Veicolo parcheggiato vicino al luogo
    data class ParkedVehicle(val session: ParkingSession, val vehicleName: String)

    // Notifica quando l'utente entra in un'area salvata
    fun notifyEnter(context: Context, location: SavedLocation) {

        val openStartPark = NotificationIntents.startParking(context, location.id)

        val notification = baseBuilder(
            context,
            title = "Sei arrivato a ${location.name}",
            text = "Vuoi avviare il parcheggio?"
        )
            .setContentIntent(openStartPark)
            .addAction(0, "Avvia parcheggio", openStartPark)
            .build()

        show(context, location.id.toInt(), notification)
    }

    // Invia una notifica quando l'utente esce da un'area salvata mentre ha ancora un parcheggio attivo
    fun notifyExit(context: Context, location: SavedLocation, parked: List<ParkedVehicle>) {

        if (parked.isEmpty()) return

        NotificationManagerCompat.from(context).cancel(location.id.toInt())

        val notificationId = EXIT_NOTIFICATION_OFFSET + location.id.toInt()
        val first = parked.first()

        val builder = if (parked.size == 1) {
            baseBuilder(
                context,
                title = "Hai lasciato ${location.name}",
                text = "Il parcheggio di ${first.vehicleName} è ancora attivo"
            ).addAction(0, "Termina parcheggio", endPakingIntent(context, first.session.id, notificationId))
        } else {
            baseBuilder(
                context,
                title = "Hai lasciato ${location.name}",
                text = "Hai ${parked.size} parcheggi ancora attivi"
            )
        }

        val notification = builder
            .setContentIntent(NotificationIntents.activeSession(context, first.session.vehicleId))
            .build()

        show(context, notificationId, notification)
    }

    private fun endPakingIntent(context: Context, sessionId: Long, notificationId: Int): PendingIntent {
        val intent = Intent(context, GeofenceActionReceiver::class.java).apply {
            action = GeofenceActionReceiver.ACTION_END_PARKING
            putExtra(GeofenceActionReceiver.EXTRA_SESSION_ID, sessionId)
            putExtra(GeofenceActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(context, notificationId, intent, FLAGS)
    }

    // Configurazione base comune per tutte le notifiche di Geofencing.
    private fun baseBuilder(context: Context, title: String, text: String): NotificationCompat.Builder {
        ensureChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
    }

    // Crea il canale di notifica
    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Posizioni salvate", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    // Mostra la notifica verificando i permessi
    @SuppressLint("MissingPermission")
    private fun show(context: Context, id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) { }
    }
}