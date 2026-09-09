package com.example.parkmate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "parkingSession",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ])
data class ParkingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val type: ParkingType,
    val latitude: Double,
    val longitude: Double,
    val startTime: Long,
    val endTime: Long? = null,
    val hourlyRate: Double? = null,
    val fixedCost: Double? = null,
    val expiryTime: Long? = null,
    val photoPath: String? = null
)