package com.example.parkmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: VehicleType
)

enum class VehicleType{CAR, MOTORCYCLE, BICYCLE}