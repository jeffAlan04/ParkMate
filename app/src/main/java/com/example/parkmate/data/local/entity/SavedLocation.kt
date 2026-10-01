package com.example.parkmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savedLocations")
data class SavedLocation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val parkingType: ParkingType,
    val hourlyRate: Double? = null,
    val fixedCost: Double? = null
)
