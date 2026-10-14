package com.example.parkmate.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.local.entity.ParkingType
import java.util.concurrent.TimeUnit

class ParkingAlarmScheduler(context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun scheduleExpiry(vehicleId: Long, vehicleName: String, expiryTime: Long, warningMinutesBefore: Int) {
        val now = System.currentTimeMillis()

        val finalDelay = expiryTime - now
        if (finalDelay > 0) {
            val finalRequest = OneTimeWorkRequestBuilder<ParkingExpiryWorker>()
                .setInitialDelay(finalDelay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("vehicleName" to vehicleName, "isWarning" to false))
                .build()
            workManager.enqueueUniqueWork(finalWorkName(vehicleId), ExistingWorkPolicy.REPLACE, finalRequest)
        }

        val warningDelay = expiryTime - now - warningMinutesBefore * 60000L
        if (warningDelay > 0) {
            val warningRequest = OneTimeWorkRequestBuilder<ParkingExpiryWorker>()
                .setInitialDelay(finalDelay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("vehicleName" to vehicleName, "isWarning" to true))
                .build()
            workManager.enqueueUniqueWork(warningWorkName(vehicleId), ExistingWorkPolicy.REPLACE, warningRequest)
        }
    }

    fun cancelExpiry(vehicleId: Long) {
        workManager.cancelUniqueWork(finalWorkName(vehicleId))
        workManager.cancelUniqueWork(warningWorkName(vehicleId))
    }

    fun cancelFor(session: ParkingSession) {
        if (session.type == ParkingType.TICKET) {
            cancelExpiry(session.vehicleId)
        }
    }

    private fun finalWorkName(vehicleId: Long) = "parking_expiry_final_$vehicleId"
    private fun warningWorkName(vehicleId: Long) = "parking_expiry_warning_$vehicleId"
}