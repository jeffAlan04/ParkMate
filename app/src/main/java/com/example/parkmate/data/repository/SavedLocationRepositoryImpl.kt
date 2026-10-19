package com.example.parkmate.data.repository

import com.example.parkmate.data.local.dao.SavedLocationDao
import com.example.parkmate.data.local.entity.SavedLocation
import kotlinx.coroutines.flow.Flow

class SavedLocationRepositoryImpl(private val dao: SavedLocationDao) : SavedLocationRepository {
    // Restituisce tutte le posizioni salvate
    override fun getAllLocations(): Flow<List<SavedLocation>> = dao.getAll()

    override suspend fun getLocationById(locationId: Long): SavedLocation? = dao.getById(locationId)

    // Aggiunge una posizione al DB
    override suspend fun addLocation(location: SavedLocation): Long = dao.insert(location)

    // Aggiorna una posizione al DB
    override suspend fun updateLocation(location: SavedLocation) {
        dao.update(location)
    }

    // Rimuove una posizione dal DB
    override suspend fun deleteLocation(location: SavedLocation) {
        dao.delete(location)
    }
}