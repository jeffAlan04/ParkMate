package com.example.parkmate.ui.parking

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

// Possibili stati del permesso di localizzazione
enum class LocationPermissionStatus { NOT_REQUESTED, GRANTED, DENIED}

// Composable che gestisce lo stato del permsso di localizzazione
// Restituisce lo stato attuale del permesso e una funzione per richiedere il permesso
@Composable
fun rememberLocationPermissionStatus(): Pair<LocationPermissionStatus, () -> Unit> {

    // Context dell'applicazione
    val context = LocalContext.current

    // Mantiene lo stato del permesso durante la composizione
    var status by remember {

        // Controlla se il permesso è già stato concesso
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // Se permesso già presente, lo stato è GRANTED, altrimenti NOT REQUESTED
        mutableStateOf(if (alreadyGranted) LocationPermissionStatus.GRANTED else LocationPermissionStatus.NOT_REQUESTED)
    }

    // Launcher per mostrare la richiesta di permesso all'utente
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
        // Aggiorna lo stato in base alla risposta
        granted -> status = if (granted) LocationPermissionStatus.GRANTED else LocationPermissionStatus.DENIED
    }

    // Avvia la richiesta del permesso
    val requestPermission: () -> Unit = {
        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    // Restituisce lo stato e la funzione per richiedere il permesso
    return status to requestPermission
}