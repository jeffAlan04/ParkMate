package com.example.parkmate.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Routes {

    @Serializable
    data object VehiclesList : Routes

    @Serializable
    data class VehicleForm(val vehicleId: Long? = null) : Routes

    @Serializable
    data object ActiveParking : Routes

    @Serializable
    data object Map : Routes

    @Serializable
    data object History : Routes

    @Serializable
    data object Stats : Routes
}