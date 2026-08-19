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

@Database(entities = [Vehicle::class, ParkingSession::class, SavedLocation::class], version = 3, exportSchema = false)
abstract class ParkMateDatabase : RoomDatabase(){
    abstract fun vehicleDao(): VehicleDao

    abstract fun parkingSessionDao(): ParkingSessionDao

    abstract fun savedLocationDao(): SavedLocationDao
}