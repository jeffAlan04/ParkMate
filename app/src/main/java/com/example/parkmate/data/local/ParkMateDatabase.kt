package com.example.parkmate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.parkmate.data.local.dao.ParkingSessionDao
import com.example.parkmate.data.local.dao.SavedLocationDao
import com.example.parkmate.data.local.dao.VehicleDao
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.data.local.entity.Vehicle

// Database Room dell'app
@Database(entities = [Vehicle::class, ParkingSession::class, SavedLocation::class], version = 6, exportSchema = false)
abstract class ParkMateDatabase : RoomDatabase(){

    // Restituisce il DAO utilizzato per accedere alla tabella dei veicoli
    abstract fun vehicleDao(): VehicleDao

    // Restituisce il DAO utilizzato per accedere alla tabella delle sessioni di parcheggio
    abstract fun parkingSessionDao(): ParkingSessionDao

    // Restituisce il DAO utilizzato per accedere alla tabella delle posizioni salvate
    abstract fun savedLocationDao(): SavedLocationDao
}