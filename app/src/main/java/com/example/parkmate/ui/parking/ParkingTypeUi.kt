package com.example.parkmate.ui.parking

import com.example.parkmate.data.local.entity.ParkingType

val ParkingType.label: String
    get() = when (this) {
        ParkingType.FREE -> "Gratis"
        ParkingType.HOURLY -> "Orario"
        ParkingType.TICKET -> "Ticket"
    }

