package com.example.parkmate.data.repository

import com.example.parkmate.data.local.dao.SavedLocationDao
import com.example.parkmate.data.local.entity.SavedLocation
import kotlinx.coroutines.flow.Flow

class LocationRepositoryImpl(private val dao: SavedLocationDao) : LocationRepository {
    override fun getAllLocations(): Flow<List<SavedLocation>> = dao.getAll()

    override suspend fun addLocation(location: SavedLocation) {
        dao.insert(location)
    }

    override suspend fun updateLocation(location: SavedLocation) {
        dao.update(location)
    }

    override suspend fun deleteLocation(location: SavedLocation) {
        dao.delete(location)
    }
}