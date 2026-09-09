package com.example.parkmate.ui.parking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.parkmate.ParkMateApplication

object ParkingViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ParkMateApplication
        @Suppress("UNCHECKED_CAST")
        return ParkingViewModel(
            parkingRepository = app.container.parkingRepository,
            vehicleRepository = app.container.vehicleRepository,
            locationRepository = app.container.locationRepository
        ) as T
    }
}