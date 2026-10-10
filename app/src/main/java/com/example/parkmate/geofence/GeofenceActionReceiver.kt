package com.example.parkmate.geofence

import android.app.Notification.EXTRA_NOTIFICATION_ID
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller.EXTRA_SESSION_ID
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import androidx.work.impl.utils.ForceStopRunnable
import com.example.parkmate.ParkMateApplication
import com.example.parkmate.data.local.entity.ParkingType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GeofenceActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_END_PARKING)
            return

        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        if (sessionId == -1L)
            return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as ParkMateApplication).container
                val session = container.parkingRepository.getActiveSession().first()
                    .find { it.id == sessionId }

                if (session != null) {
                    container.parkingRepository.endSession(session)

                    if (session.type == ParkingType.TICKET) {
                        val workManager = WorkManager.getInstance(context)
                        workManager.cancelUniqueWork("parking_expiry_final_${session.vehicleId}")
                        workManager.cancelUniqueWork("parking_expiry_warning_${session.vehicleId}")
                    }
                }

                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_END_PARKING = "com.example.parkmate.action.END_PARKING"
        const val EXTRA_SESSION_ID = "sessionId"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
    }
}