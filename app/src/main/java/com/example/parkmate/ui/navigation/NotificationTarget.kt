package com.example.parkmate.ui.navigation

// Schermata da aprire quando l'utente tocca una notifica
sealed interface NotificationTarget {
    // Form di avvio parcheggio
    data class StartPark(val locationId: Long) : NotificationTarget

    // Riepilogo del parcheggio in corso
    data class ActiveSession(val vehicleId: Long): NotificationTarget
}