package com.example.parkmate.data.location

import android.annotation.SuppressLint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationProvider(private val fusedClient: FusedLocationProviderClient) {

    // Metodo che richiede il permesso di accesso alla posizione
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationCoordinates? {

        // Annulla la richiesta di posizione se la coroutine viene cancellata
        val cancellationTokenSource = CancellationTokenSource()

        return suspendCancellableCoroutine { continuation ->

            // Chiede la posizione attutale del dispositivo
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                // token per annullare la richiesta
                cancellationTokenSource.token

                // Callback quando la richiesta termina con successo
            ).addOnSuccessListener { location ->

                // Posizione trovata
                if (location != null) {
                    // Riprende la coroutine e restituisce lat e long
                    continuation.resume(LocationCoordinates(location.latitude, location.longitude))
                } else {
                    // Nessuna posizione disponibile
                    continuation.resume(null)
                }

                // Callback in caso di errore
            }.addOnFailureListener {
                continuation.resume(null)
            }

            // Coroutine cancellata e richiesta di localizzazione annullata
            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }
}

// Classe dati per la posizione
data class LocationCoordinates(val latitude: Double, val longitude: Double)