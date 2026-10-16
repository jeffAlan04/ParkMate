package com.example.parkmate.notification

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.Manifest
import android.app.NotificationChannel

// Mostra le notifiche di scadenza del parcheggio.
class ParkingExpiryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params){

    //Recupera i dati di input e invoca la visualizzazione della notifica.
    override suspend fun doWork(): Result {
        // Recupera l'id, il nome del veicolo e il flag di preavviso dai dati passati al worker
        val vehicleId = inputData.getLong("vehicleId", -1L)
        val vehicleName = inputData.getString("vehicleName") ?: "Veicolo"
        val isWarning = inputData.getBoolean("isWarning", false)
        
        showNotification(vehicleId, vehicleName, isWarning)
        return Result.success()
    }

    // Costruisce e visualizza la notifica
    @SuppressLint("ServiceCast", "MissingPermission")
    private fun showNotification(vehicleId: Long, vehicleName: String, isWarning: Boolean) {
        // Identificativo del canale di notifica
        val channelId =
            if (isWarning)
                "parking_expiry_warning"
            else
                "parking_expiry_final"

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Creazione del canale di notifica
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName =
                if (isWarning)
                    "Promemoria scadenza ticket"
                else
                    "Ticket scaduto"

            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        // Controllo dei permessi per le notifiche
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Titolo e testo della notifica
        val title =
            if (isWarning)
                "Ticket in scadenza"
            else
                "Ticket scaduto"

        val text =
            if (isWarning)
                "Il parcheggio di $vehicleName è in scadenza"
            else
                "Il parcheggio di $vehicleName è scaduto"

        // Costruzione della notifica
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // Se è un avviso finale la notifica rimane finché l'utente non interagisce
            .setAutoCancel(true)

        if (vehicleId != -1L) {
            builder.setContentIntent(NotificationIntents.activeSession(applicationContext, vehicleId))
        }

        // Generazione di un ID univoco
        val notificationId = (vehicleId * 2 + if (isWarning) 1 else 0).toInt()
        NotificationManagerCompat.from(applicationContext).notify(notificationId, builder.build())
    }
}