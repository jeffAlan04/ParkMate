package com.example.parkmate.ui.navigation

import kotlinx.serialization.Serializable

// Possibili destinazioni dell'app
sealed interface Routes {

    // Schermata con lista dei veicoli
    @Serializable
    data object VehiclesList : Routes

    // Form per creare o modificare un veicolo
    @Serializable
    data class VehicleForm(val vehicleId: Long? = null) : Routes

    // Form per avviare una nuova sessione di parcheggio
    @Serializable
    data object StartPark : Routes

    // Schermata della mappa
    @Serializable
    data object Map : Routes

    // Schermata dello storico
    @Serializable
    data object History : Routes

    // Schermata delle statistiche
    @Serializable
    data object Stats : Routes
}