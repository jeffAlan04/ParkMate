package com.example.parkmate.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.local.entity.ParkingType
import java.util.concurrent.TimeUnit

// Gestisce la pianificazione delle notifiche di scadenza dei parcheggi tramite WorkManager.

class ParkingAlarmScheduler(context: Context) {

    private val workManager = WorkManager.getInstance(context)

    // Pianifica due avvisi per la scadenza di un parcheggio: un warning e un final
    fun scheduleExpiry(vehicleId: Long, vehicleName: String, expiryTime: Long, warningMinutesBefore: Int) {
        val now = System.currentTimeMillis()

        // Calcola il ritardo per la notifica finale
        val finalDelay = expiryTime - now
        if (finalDelay > 0) {
            val finalRequest = OneTimeWorkRequestBuilder<ParkingExpiryWorker>()
                .setInitialDelay(finalDelay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("vehicleName" to vehicleName, "isWarning" to false))
                .build()
            
            // Aggiorna eventuali pianificazioni esistenti per lo stesso veicolo
            workManager.enqueueUniqueWork(finalWorkName(vehicleId), ExistingWorkPolicy.REPLACE, finalRequest)
        }

        // Calcola il ritardo per la notifica di preavviso
        val warningDelay = expiryTime - now - (warningMinutesBefore * 60000L)
        if (warningDelay > 0) {
            val warningRequest = OneTimeWorkRequestBuilder<ParkingExpiryWorker>()
                .setInitialDelay(warningDelay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("vehicleName" to vehicleName, "isWarning" to true))
                .build()
            workManager.enqueueUniqueWork(warningWorkName(vehicleId), ExistingWorkPolicy.REPLACE, warningRequest)
        }
    }

    // Cancella tutte le notifiche pianificate per un determinato veicolo.
    fun cancelExpiry(vehicleId: Long) {
        workManager.cancelUniqueWork(finalWorkName(vehicleId))
        workManager.cancelUniqueWork(warningWorkName(vehicleId))
    }

    // Cancellare le notifiche associate a una specifica sessione se è di tipo TICKET
    fun cancelFor(session: ParkingSession) {
        if (session.type == ParkingType.TICKET) {
            cancelExpiry(session.vehicleId)
        }
    }

    // Genera nomi univoci per i task in modo da non sovrapporre i veicoli
    private fun finalWorkName(vehicleId: Long) = "parking_expiry_final_$vehicleId"
    private fun warningWorkName(vehicleId: Long) = "parking_expiry_warning_$vehicleId"
}