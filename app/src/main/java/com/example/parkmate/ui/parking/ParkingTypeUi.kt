package com.example.parkmate.ui.parking

import com.example.parkmate.data.local.entity.ParkingType
import java.text.NumberFormat
import java.util.Locale

val ParkingType.label: String
    get() = when (this) {
        ParkingType.FREE -> "Gratis"
        ParkingType.HOURLY -> "Orario"
        ParkingType.TICKET -> "Ticket"
    }

fun formatPrice(value: Double): String = NumberFormat.getCurrencyInstance(Locale.ITALY).format(value)