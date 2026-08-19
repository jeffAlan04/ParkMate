package com.example.parkmate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.parkmate.data.local.dao.VehicleDao
import com.example.parkmate.data.local.entity.Vehicle

@Database(entities = [Vehicle::class], version = 2, exportSchema = false)
abstract class ParkMateDatabase : RoomDatabase(){
    abstract fun vehicleDao(): VehicleDao
}