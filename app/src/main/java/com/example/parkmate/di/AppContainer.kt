package com.example.parkmate.di

import android.content.Context
import androidx.room.Room
import com.example.parkmate.data.local.ParkMateDatabase
import com.example.parkmate.data.location.LocationProvider
import com.example.parkmate.data.repository.LocationRepository
import com.example.parkmate.data.repository.LocationRepositoryImpl
import com.example.parkmate.data.repository.ParkingRepository
import com.example.parkmate.data.repository.ParkingRepositoryImpl
import com.example.parkmate.data.repository.VehicleRepository
import com.example.parkmate.data.repository.VehicleRepositoryImpl
import com.example.parkmate.geofence.GeofenceManager
import com.example.parkmate.notification.ParkingAlarmScheduler
import com.google.android.gms.location.LocationServices

// Container che crea e fornisce le dipendenze necessarie
class AppContainer(context: Context) {

    // Database Room dell'app
    private val database = Room.databaseBuilder(context, ParkMateDatabase::class.java, "parkmate.db")
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    // Crea il repository dei veicoli quando viene utilizzato la prima volta
    val vehicleRepository: VehicleRepository by lazy {
        VehicleRepositoryImpl(database.vehicleDao())
    }

    // Crea il repository dei luoghi salvati quando viene utilizzato la prima volta
    val parkingRepository: ParkingRepository by lazy {
        ParkingRepositoryImpl(database.parkingSessionDao())
    }

    // Provider della posizione GPS del dispositivo
    val locationProvider: LocationProvider by lazy {
        LocationProvider(LocationServices.getFusedLocationProviderClient(context))
    }
    val locationRepository: LocationRepository by lazy {
        LocationRepositoryImpl(database.savedLocationDao())
    }

    // Pianificatore delle notifiche di scadenza
    val parkingAlarmScheduler: ParkingAlarmScheduler by lazy {
        ParkingAlarmScheduler(context)
    }

    val geofenceManager: GeofenceManager by lazy {
        GeofenceManager(context)
    }
}