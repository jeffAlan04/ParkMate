package com.example.parkmate.di

import android.content.Context
import androidx.room.Room
import com.example.parkmate.data.local.ParkMateDatabase
import com.example.parkmate.data.repository.LocationRepository
import com.example.parkmate.data.repository.LocationRepositoryImpl
import com.example.parkmate.data.repository.ParkingRepository
import com.example.parkmate.data.repository.ParkingRepositoryImpl
import com.example.parkmate.data.repository.VehicleRepository
import com.example.parkmate.data.repository.VehicleRepositoryImpl

class AppContainer(context: Context) {

    private val database = Room.databaseBuilder(context, ParkMateDatabase::class.java, "parkmate.db")
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    val vehicleRepository: VehicleRepository by lazy {
        VehicleRepositoryImpl(database.vehicleDao())
    }

    val parkingRepository: ParkingRepository by lazy {
        ParkingRepositoryImpl(database.parkingSessionDao())
    }

    val locationRepository: LocationRepository by lazy {
        LocationRepositoryImpl(database.savedLocationDao())
    }
}