package com.example.parkmate.ui.vehicles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.parkmate.data.local.entity.VehicleType

val VehicleType.icon: ImageVector
    get() = when (this) {
        VehicleType.CAR -> Icons.Default.DirectionsCar
        VehicleType.MOTORCYCLE -> Icons.Default.TwoWheeler
        VehicleType.BICYCLE -> Icons.Default.PedalBike
    }

val VehicleType.label: String
    get() = when (this) {
        VehicleType.CAR -> "Automobile"
        VehicleType.MOTORCYCLE -> "Motociclo"
        VehicleType.BICYCLE -> "Bicicletta"
    }