package com.example.parkmate.data.repository

import com.example.parkmate.data.local.entity.SavedLocation
import kotlinx.coroutines.flow.Flow

interface SavedLocationRepository {
    fun getAllLocations(): Flow<List<SavedLocation>>

    suspend fun getLocationById(locationId: Long): SavedLocation?
    suspend fun addLocation(location: SavedLocation): Long
    suspend fun updateLocation(location: SavedLocation)
    suspend fun deleteLocation(location: SavedLocation)
}