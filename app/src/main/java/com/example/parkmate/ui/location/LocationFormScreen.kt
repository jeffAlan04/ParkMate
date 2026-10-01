package com.example.parkmate.ui.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.ui.parking.rememberLocationPermissionStatus
import kotlinx.coroutines.launch
import com.example.parkmate.ui.parking.LocationPermissionStatus
import com.google.android.gms.location.LocationServices
import com.example.parkmate.data.location.LocationProvider

// Schermata per l'inserimento o la modifica di un luogo salvato.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationFormScreen(
    locationId: Long? = null,
    viewModel: LocationViewModel = viewModel(factory = LocationViewModelFactory),
    onSaved: () -> Unit = {}  // Callback richiamata dopo il salvataggio
) {
    // Stati locali per i campi di testo del modulo
    var name by remember { mutableStateOf("") }
    var latitudeText by remember { mutableStateOf("") }
    var longitudeText by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val (permissionStatus, requestPermission) = rememberLocationPermissionStatus()

    // Carica i dati di un veicolo esistente
    LaunchedEffect(locationId) {
        if (locationId != null) {
            viewModel.getLocationById(locationId)?.let { existing ->
                name = existing.name
                latitudeText = existing.latitude.toString()
                longitudeText = existing.longitude.toString()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (locationId != null ) "Modifica luogo" else "Nuovo luogo") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Campo di input per il nome
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome luogo") },
                placeholder = { Text("es. Casa, Ufficio") },
                isError = name.isBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            // TODO: sostituire con selezione tap-to-pick su GoogleMap una volta
            // integrato Maps Compose (punto 0 della scaletta) — per ora GPS + manuale
            Button(
                onClick = {
                    coroutineScope.launch {
                        if (permissionStatus != LocationPermissionStatus.GRANTED) {
                            requestPermission() // Chiede i permessi se mancano
                        } else {
                            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                            // Tenta di ottenere la posizione GPS attuale
                            LocationProvider(fusedClient).getCurrentLocation()?.let {
                                latitudeText = it.latitude.toString()
                                longitudeText = it.longitude.toString()
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Usa posizione attuale")
            }

            // Visualizzazione della Latitudine
            OutlinedTextField(
                value = latitudeText,
                onValueChange = { latitudeText = it },
                label = { Text("Latitudine") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Visualizzazione della Longitudine
            OutlinedTextField(
                value = longitudeText,
                onValueChange = { longitudeText = it },
                label = { Text("Longitudine") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            // Abilita il salvataggio solo se i campi sono compilati correttamente
            val lat = latitudeText.toDoubleOrNull()
            val lng = longitudeText.toDoubleOrNull()
            val canSave = name.isNotBlank() && lat != null && lng != null

            // Bottone finale per confermare l'inserimento o la modifica nel database
            Button(
                onClick = {
                    if (lat != null && lng != null) {
                        if (locationId != null) {
                            // Modalità Modifica
                            viewModel.updateLocation(locationId, name, lat, lng)
                        } else {
                            // Modalità Nuovo Inserimento
                            viewModel.addLocation(name, lat, lng)
                        }
                        onSaved()
                    }
                },
                enabled = canSave,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Salva", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}