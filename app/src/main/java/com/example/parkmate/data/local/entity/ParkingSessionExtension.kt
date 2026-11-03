package com.example.parkmate.data.local.entity
// Calcola il costo in base al tipo di sessione di parcheggio
fun ParkingSession.computeCost(now: Long = System.currentTimeMillis()): Double = when (type) {
    ParkingType.FREE -> 0.0
    ParkingType.TICKET -> fixedCost ?: 0.0
    ParkingType.HOURLY -> {
        val end = endTime ?: now
        val hours = (end - startTime) / 3600000.0
        (hourlyRate ?: 0.0) * hours
    }
}