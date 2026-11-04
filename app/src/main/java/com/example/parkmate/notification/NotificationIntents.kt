package com.example.parkmate.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.parkmate.MainActivity

object NotificationIntents {

    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    // Crea un intent per aprire la schermata di avvio parcheggio con un luogo pre-selezionato
    fun startParking(context: Context, locationId: Long): PendingIntent =
        build(context, requestCode = 10000 + locationId.toInt()) {
            putExtra(MainActivity.EXTRA_START_LOCATION_ID, locationId)
        }

    // Crea un intent per aprire il riepilogo di una sessione attiva per uno specifico veicolo
    fun activeSession(context: Context, vehicleId: Long): PendingIntent =
        build(context, requestCode = 20000 + vehicleId.toInt()) {
            putExtra(MainActivity.EXTRA_SUMMARY_VEHICLE_ID, vehicleId)
        }

    private fun build(context: Context, requestCode: Int, extras: Intent.() -> Unit): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            // FLAG_ACTIVITY_SINGLE_TOP evita di riaprire l'app se è già in primo piano
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            extras()
        }
        return PendingIntent.getActivity(context, requestCode, intent, FLAGS)
    }
}