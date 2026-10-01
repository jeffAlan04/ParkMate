package com.example.parkmate.data.local.entity

fun ParkingSession.computeCost(): Double = when (type) {
    ParkingType.FREE -> 0.0
    ParkingType.TICKET -> fixedCost ?: 0.0
    ParkingType.HOURLY -> {
        val end = endTime ?: System.currentTimeMillis()
        val hours = (end - startTime) / 3600000.0
        (hourlyRate ?: 0.0) * hours
    }
}