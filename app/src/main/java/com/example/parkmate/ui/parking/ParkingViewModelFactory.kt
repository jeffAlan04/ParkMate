package com.example.parkmate.ui.parking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.parkmate.ParkMateApplication

// Factory per creare il ParkingViewModel con le dipendenze necessarie
object ParkingViewModelFactory : ViewModelProvider.Factory {

    // Recupera l'istanza dell'Application
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ParkMateApplication

        @Suppress("UNCHECKED_CAST")
        // Crea il ViewModel fornendo le repository necessarie
        return ParkingViewModel(
            parkingRepository = app.container.parkingRepository,
            vehicleRepository = app.container.vehicleRepository,
            locationRepository = app.container.locationRepository,
            appContext = app.applicationContext
        ) as T
    }
}