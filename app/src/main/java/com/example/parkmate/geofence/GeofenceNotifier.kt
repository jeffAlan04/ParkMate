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

object GeofenceNotifier {

    private const val CHANNEL_ID = "geofence"
    private const val EXIT_NOTIFICATION_OFFSET = 1000000
    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun notifyEnter(context: Context, location: SavedLocation) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_START_LOCATION_ID, location.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val startPendingIntent = PendingIntent.getActivity(context, location.id.toInt(), openIntent, FLAGS)

        val notification = baseBuilder(
            context,
            title = "Sei arrivato a ${location.name}",
            text = "Vuoi avviare il parcheggio?"
        )
            .setContentIntent(startPendingIntent)
            .addAction(0, "Avvia parcheggio", startPendingIntent)
            .build()

        show(context, location.id.toInt(), notification)
    }

    fun notifyExit(context: Context, location: SavedLocation, session: ParkingSession, vehicleName: String) {

        NotificationManagerCompat.from(context).cancel(location.id.toInt())

        val notificationId = EXIT_NOTIFICATION_OFFSET + session.id.toInt()

        val endIntent = Intent(context, GeofenceActionReceiver::class.java).apply {
            action = GeofenceActionReceiver.ACTION_END_PARKING
            putExtra(GeofenceActionReceiver.EXTRA_SESSION_ID, session.id)
            putExtra(GeofenceActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val endPendingIntent = PendingIntent.getBroadcast(context, session.id.toInt(), endIntent, FLAGS)

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            FLAGS
        )

        val notification = baseBuilder(
            context,
            title = "Hai lasciato ${location.name}",
            text = "Il parcheggio di $vehicleName è ancora attivo"
        )
            .setContentIntent(openApp)
            .addAction(0, "Termina parcheggio", endPendingIntent)
            .build()

        show(context, notificationId, notification)
    }

    private fun baseBuilder(context: Context, title: String, text: String): NotificationCompat.Builder {
        ensureChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Posizioni salvate", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun show(context: Context, id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) { }
    }
}