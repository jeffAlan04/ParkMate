package com.example.parkmate

import android.app.Application
import com.example.parkmate.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ParkMateApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        CoroutineScope(Dispatchers.IO).launch {
            val locations = container.locationRepository.getAllLocations().first()
            container.geofenceManager.registerAll(locations)
        }
    }
}