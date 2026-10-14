package com.example.parkmate

import android.app.Application
import com.example.parkmate.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Classe  per l'inizializzazione dell'app
class ParkMateApplication : Application() {
    
    // Container per la Dependency Injection manuale.
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Inizializza il container delle dipendenze passando il contesto dell'applicazione
        container = AppContainer(this)

        // Ripristina i Geofence all'avvio dell'applicazione.
        CoroutineScope(Dispatchers.IO).launch {
            // Recupera la lista corrente dei luoghi salvati dal database
            val locations = container.locationRepository.getAllLocations().first()
            
            // Registra geofence salvati dall'utente
            container.geofenceManager.registerAll(locations)
        }
    }
}