package com.example.parkmate.data.local.entity

object VehiclePlate {
    private val carPlatePattern = Regex("^[A-Z]{2}[0-9]{3}[A-Z]{2}$") // Formato targa auto
    private val motorcyclePlatePattern = Regex("^[A-Z]{2}[0-9]{5}$") // Formato targa moto

    const val MAX_LENGTH = 7

    fun isValid(type: VehicleType, plateNumber: String): Boolean = when (type) {
        VehicleType.CAR -> plateNumber.matches(carPlatePattern)
        VehicleType.MOTORCYCLE -> plateNumber.matches(motorcyclePlatePattern)
        VehicleType.BICYCLE -> true
    }

    fun example(type: VehicleType): String = when (type) {
        VehicleType.CAR -> "AB123CD"
        VehicleType.MOTORCYCLE -> "AB12345"
        VehicleType.BICYCLE -> ""
    }
}