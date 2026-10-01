package com.example.parkmate.ui.parking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.data.local.entity.Vehicle
import com.example.parkmate.data.local.entity.ParkingType
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.data.location.LocationProvider
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TextButton
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold

private enum class LocationSource { CURRENT, SAVED }

@Composable
fun StartParkScreen (
    viewModel: ParkingViewModel = viewModel(factory = ParkingViewModelFactory),
    onParkingStarted: () -> Unit = {} // Callback richiamata dopo il salvataggio
) {
    val vehicles by viewModel.vehicles.collectAsState()
    val savedLocations by viewModel.savedLocations.collectAsState()
    val activeSessions by viewModel.activeSessions.collectAsState()

    // Dati del form
    var selectedVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var selectedType by remember { mutableStateOf(ParkingType.FREE) }
    var hourlyRate by remember { mutableStateOf("") }
    var fixedCost by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    var locationSource by remember { mutableStateOf(LocationSource.CURRENT) }
    var selectedSavedLocation by remember { mutableStateOf<SavedLocation?>(null) }

    var baseCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    // Coordinate modificate manualmente
    var manualCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var showMapAdjustment by remember { mutableStateOf(false) }

    // Coordinate manuali preferite a quelle base
    val finalCoordinates = manualCoordinates ?: baseCoordinates

    // Flag per riprovare l'avvio dopo la richiesta di permesso
    var pendingStartAfterPermission by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val (permissionStatus, requestPermission) = rememberLocationPermissionStatus()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Verifica se il veicolo selezionato ha già un parcheggio in corso
    val activeSessionForSelectedVechicle = selectedVehicle?.let { vehicle ->
        activeSessions.find { it.session.vehicleId == vehicle.id }
    }

    // Aggiorna le coordinate base ogni vola che cambia la sorgente o i permessi
    LaunchedEffect(locationSource, selectedSavedLocation, permissionStatus) {
        manualCoordinates = null
        baseCoordinates = when (locationSource) {
            LocationSource.SAVED -> selectedSavedLocation?.let { it.latitude to it.longitude }

            LocationSource.CURRENT -> {
                if (permissionStatus != LocationPermissionStatus.GRANTED) {
                    null // Non può leggere il GPS senza permesso
                } else {
                    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                    // Chiama il provider GPS sospendendo la coroutine finché non riceve risposta
                    LocationProvider(fusedClient).getCurrentLocation()?.let { it.latitude to it.longitude }
                }
            }
        }
    }

    // Prova a salvare la sessione nel database
    fun attemptStartParking(scope: CoroutineScope) {
        val vehicle = selectedVehicle ?: return

        // Caso in cui la posizione è regolata manualmente sulla mappa
        manualCoordinates?.let { (lat, lng) ->
            viewModel.startParking(
                vehicleId = vehicle.id,
                type = selectedType,
                latitude = lat,
                longitude = lng,
                hourlyRate = hourlyRate.toDoubleOrNull(),
                fixedCost = fixedCost.toDoubleOrNull(),
                expiryTime = null,
                note = note.ifBlank { null }
            )
            return
        }

        // Caso in cui la posizione è di un luogo salvato
        when (locationSource) {
            LocationSource.SAVED -> {
                val location = selectedSavedLocation ?: return
                viewModel.startParking(
                    vehicleId = vehicle.id,
                    type = selectedType,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    hourlyRate = hourlyRate.toDoubleOrNull(),
                    fixedCost = fixedCost.toDoubleOrNull(),
                    expiryTime = null,
                    note = note.ifBlank { null }
                )
            }

            // Caso in cui la posizione è ricavata dal GPS attuale
            LocationSource.CURRENT -> {
                if (permissionStatus != LocationPermissionStatus.GRANTED) {
                    pendingStartAfterPermission = true
                    requestPermission() // Chiede i permessi se mancano
                    return
                }

                // Se si hanno già le coordinate, salva subito
                baseCoordinates?.let { (lat, lng) ->
                    viewModel.startParking(
                        vehicleId = vehicle.id,
                        type = selectedType,
                        latitude = lat,
                        longitude = lng,
                        hourlyRate = hourlyRate.toDoubleOrNull(),
                        fixedCost = fixedCost.toDoubleOrNull(),
                        expiryTime = null,
                        note = note.ifBlank { null }
                    )
                    return
                }

                // Se non si hanno le coordinate, forza una lettura del GPS
                scope.launch {
                    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                    val coordinates = LocationProvider(fusedClient).getCurrentLocation()
                    if (coordinates != null) {
                        viewModel.startParking(
                            vehicleId = vehicle.id,
                            type = selectedType,
                            latitude = coordinates.latitude,
                            longitude = coordinates.longitude,
                            hourlyRate = hourlyRate.toDoubleOrNull(),
                            fixedCost = fixedCost.toDoubleOrNull(),
                            expiryTime = null,
                            note = note.ifBlank { null }
                        )
                    } else {
                        snackbarHostState.showSnackbar("Impossibile ottenere posizione")
                    }
                }
            }
        }

    }

    // Fa partire il parcheggio automaticamente appena il permesso viene concesso
    LaunchedEffect(permissionStatus) {
        if (permissionStatus == LocationPermissionStatus.GRANTED && pendingStartAfterPermission) {
            pendingStartAfterPermission = false
            attemptStartParking(coroutineScope)
        }
    }
    
    // INTERFACCIA UTENTE
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Seleziona veicolo
            Text("Veicolo", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(vehicles) { vehicle ->
                    VehiclePickCard(
                        vehicle = vehicle,
                        selected = vehicle.id == selectedVehicle?.id,
                        onClick = { selectedVehicle = vehicle }
                    )
                }
            }
            
            // Se non c'è nessuna sessione attiva per l'auto scelta, mostra il form
            if (activeSessionForSelectedVechicle == null) {
                
                // Seleziona tipo di parcheggio
                Text("Tipo di parcheggio", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ParkingType.entries.forEach { type ->
                        ParkingTypeCard(
                            type = type,
                            selected = type == selectedType,
                            onClick = { selectedType = type },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                AnimatedVisibility(visible = selectedType == ParkingType.HOURLY, enter = fadeIn(), exit = fadeOut()) {
                    OutlinedTextField(
                        value = hourlyRate,
                        onValueChange = { hourlyRate = it },
                        label = { Text("Tariffa oraria (€)") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                AnimatedVisibility(visible = selectedType == ParkingType.TICKET, enter = fadeIn(), exit = fadeOut()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = fixedCost,
                            onValueChange = { fixedCost = it },
                            label = { Text("Costo ticket (€)") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Nota (opzionale)") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())

                // Scelta posizione
                Text("Posizione", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {

                    // Attuale
                    SegmentedButton(
                        selected = locationSource == LocationSource.CURRENT,
                        onClick = { locationSource = LocationSource.CURRENT },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    )  { Text("Posizione Attuale") }

                    // Salvata
                    SegmentedButton(
                        selected = locationSource == LocationSource.SAVED,
                        onClick = { locationSource = LocationSource.SAVED },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Luogo Salvato") }
                }

                // Lista dei luoghi salvati versione chip
                AnimatedVisibility(visible = locationSource == LocationSource.SAVED) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(savedLocations) { location ->
                            FilterChip(
                                selected = location.id == selectedSavedLocation?.id,
                                onClick = { selectedSavedLocation = location },
                                label = { Text(location.name) }
                            )
                        }
                    }
                }

                // Pulsante per mostrare la mappa e regolare la posizione GPS
                if (locationSource == LocationSource.CURRENT && baseCoordinates != null) {
                    TextButton(onClick = { showMapAdjustment = !showMapAdjustment }) {
                        Text(if (showMapAdjustment) "Nascondi mappa" else "Regola posizione sulla mappa")
                    }
                    AnimatedVisibility(visible = showMapAdjustment) {
                        LocationAdjustmentMap(
                            coordinates = finalCoordinates,
                            isOverriden = manualCoordinates != null,
                            onMapClick = { lat, lng -> manualCoordinates = lat to lng },
                            onResetOverride = { manualCoordinates = null }
                        )
                    }
                }
            } else {
                // Se c'è già una sessione attiva, mostra i dettagli del timer e la mappa del parcheggio esistente
                ActiveSessionMap(display = activeSessionForSelectedVechicle)
                ParkingTrackerInfo(display = activeSessionForSelectedVechicle)
            }

            // Bottone di avvio/termine
            val canSave = selectedVehicle != null && (locationSource == LocationSource.CURRENT || selectedSavedLocation != null)

            Button(
                onClick = {
                    if (activeSessionForSelectedVechicle != null) {
                        viewModel.endParking(activeSessionForSelectedVechicle.session)
                    } else {
                        attemptStartParking(coroutineScope) }
                    },
                enabled = activeSessionForSelectedVechicle != null || canSave,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    if (activeSessionForSelectedVechicle != null)
                        "Termina parcheggio"
                    else
                        "Avvia parcheggio",
                    style = MaterialTheme.typography.titleMedium)
            }

        }
    }
}

// Mappa che permette di visualizzare e regolare manualmente la posizione
@Composable
private fun LocationAdjustmentMap(
    coordinates: Pair<Double, Double>?,
    isOverriden: Boolean, // Indica se l'utente ha spostato manualmente il marker
    onMapClick: (Double, Double) -> Unit,
    onResetOverride: () -> Unit
) {
    val markerPosition = coordinates?.let { LatLng(it.first, it.second) }

    val cameraPositionState = rememberCameraPositionState {
        markerPosition?.let {
            position = CameraPosition.fromLatLngZoom(it, 16f)
        }
    }

    Column {
        GoogleMap(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng -> onMapClick(latLng.latitude, latLng.longitude) }
        ) {
            markerPosition?.let {
                Marker(state = MarkerState(position = it))
            }
        }

        if (isOverriden) {
            TextButton(onClick = onResetOverride) {
                Text("Ripristina posizione")
            }
        }
    }
}

// Visualizza la posizione di un parcheggio attivo su una mappa
@Composable
private fun ActiveSessionMap(display: ActiveSessionDisplay) {
    val markerPosition = LatLng(display.session.latitude, display.session.longitude)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(markerPosition, 16f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        cameraPositionState = cameraPositionState
    ) {
        Marker(state = MarkerState(position = markerPosition))
    }
}

// Mostra le informazioni in tempo reale di un parcheggio attivo (tempo e costo).
@Composable
private fun ParkingTrackerInfo(display: ActiveSessionDisplay) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Tempo trascorso
        Text(
            text = formatElapsedDuration(display.elapsedMillis),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold
        )
        // Costo accumulato finora
        display.currentCost?.let { cost ->
            Text(
                text = String.format(Locale.ITALY, "€ %.2f", cost),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Formatta un valore in millisecondi in una stringa leggibile
private fun formatElapsedDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.ITALY, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ITALY, "%02d:%02d", minutes, seconds)
    }
}

// Card per la selezione di un veicolo
@Composable
private fun VehiclePickCard(vehicle: Vehicle, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(vehicle.name, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// Card per selezionare il tipo di parcheggio
@Composable
private fun ParkingTypeCard(type: ParkingType, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                type.name,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
