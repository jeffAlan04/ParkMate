package com.example.parkmate.ui.visualization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.parkmate.ParkMateApplication

object DataViewModelFactory: ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ParkMateApplication
        @Suppress("UNCHECKED_CAST")
        return DataViewModel(app.container.parkingRepository) as T
    }
}