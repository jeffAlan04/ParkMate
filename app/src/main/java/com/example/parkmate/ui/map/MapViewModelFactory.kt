package com.example.parkmate.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.parkmate.ParkMateApplication

// Factory per creare il LocationViewModel con le dipendenze necessarie
object MapViewModelFactory: ViewModelProvider.Factory {

    // Recupera l'istanza dell'Application
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ParkMateApplication
        @Suppress("UNCHECKED_CAST")

        // Crea il ViewModel fornendo le repository necessarie
        return MapViewModel(app.container.savedLocationRepository, app.container.geofenceManager) as T
    }
}