package com.example.parkmate.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.parkmate.data.local.entity.SavedLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLocationDao {

    // Recupera tutte le posizioni salvate
    @Query("SELECT * FROM savedLocations ORDER BY name")
    fun getAll(): Flow<List<SavedLocation>>

    // Inserisce una nuova posizione
    @Insert
    suspend fun insert(location: SavedLocation): Long

    // Aggiorna una posizione già esistente
    @Update
    suspend fun update(location: SavedLocation)

    // Elimina una posizione presente
    @Delete
    suspend fun delete(location: SavedLocation)
}