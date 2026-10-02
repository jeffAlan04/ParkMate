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

class ParkingExpiryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params){

    override suspend fun doWork(): Result {
        val vehicleName = inputData.getString("vehicleName") ?: "Veicolo"
        val isWarning = inputData.getBoolean("isWarning", false)
        showNotification(vehicleName, isWarning)
        return Result.success()
    }

    @SuppressLint("ServiceCast", "MissingPermission")
    private fun showNotification(vehicleName: String, isWarning: Boolean) {
        val channelId =
            if (isWarning)
                "parking_expiry_warning"
            else
                "parking_expiry_final"

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                return
            }
        }

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

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(!isWarning)
            .build()

        val notificationId = (vehicleName + isWarning).hashCode()
        NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
    }
}