package com.example.parkmate.ui.parking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.parkmate.ui.photo.createPhotoFile
import java.text.SimpleDateFormat
import java.util.Calendar
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import com.example.parkmate.ui.photo.FullScreenPhotoDialog
import com.example.parkmate.ui.photo.PhotoThumbnail


private enum class LocationSource { CURRENT, SAVED }

@Composable
fun StartParkScreen (
    viewModel: ParkingViewModel = viewModel(factory = ParkingViewModelFactory),
    initialLocationId: Long? = null,
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
    var expiryTime by remember { mutableStateOf<Long?>(null) }
    var warningMinutesBefore by remember { mutableStateOf(15) }
    var note by remember { mutableStateOf("") }

    var locationSource by remember { mutableStateOf(LocationSource.CURRENT) }
    var selectedSavedLocation by remember { mutableStateOf<SavedLocation?>(null) }

    var baseCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    // Coordinate modificate manualmente
    var manualCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var showMapAdjustment by remember { mutableStateOf(false) }

    // Coordinate manuali preferite a quelle base
    val finalCoordinates = manualCoordinates ?: baseCoordinates

    val effectiveType = when (locationSource) {
        LocationSource.SAVED -> selectedSavedLocation?.parkingType ?: selectedType
        LocationSource.CURRENT -> selectedType
    }

    val effectiveHourlyRate = when (locationSource) {
        LocationSource.SAVED -> selectedSavedLocation?.hourlyRate
        LocationSource.CURRENT -> hourlyRate.toDoubleOrNull()
    }

    val effectiveFixedCost = when (locationSource) {
        LocationSource.SAVED -> selectedSavedLocation?.fixedCost
        LocationSource.CURRENT -> fixedCost.toDoubleOrNull()
    }

    // Flag per riprovare l'avvio dopo la richiesta di permesso
    var pendingStartAfterPermission by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val (permissionStatus, requestPermission) = rememberLocationPermissionStatus()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var fullScreenPhoto by remember { mutableStateOf<String?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
        success -> val newPath = pendingPhotoPath
        if (success && newPath != null) {
            photoPath?.let { File(it).delete() }
            photoPath = newPath
        } else {
            newPath?.let { File(it).delete() }
        }
        pendingPhotoPath = null
    }

    fun launchCamera() {
        val file = createPhotoFile(context)
        pendingPhotoPath = file.absolutePath
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        takePictureLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted ->
        if (granted)
            launchCamera()
        else
            coroutineScope.launch { snackbarHostState.showSnackbar("Permesso fotocamera negato") }
    }

    fun onTakePhoto() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted)
            launchCamera()
        else
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Verifica se il veicolo selezionato ha già un parcheggio in corso
    val activeSessionForSelectedVechicle = selectedVehicle?.let { vehicle ->
        activeSessions.find { it.session.vehicleId == vehicle.id }
    }

    var summaryVehicleId by remember { mutableStateOf<Long?>(null) }

    val summarySession = summaryVehicleId?.let { id ->
        activeSessions.find { it.session.vehicleId == id }
    }

    var handledLocationId by remember { mutableStateOf<Long?>(null) }

    fun hasActiveSession(vehicle: Vehicle) = activeSessions.any { it.session.vehicleId == vehicle.id }

    fun resetForm() {
        selectedType = ParkingType.FREE
        hourlyRate = ""
        fixedCost = ""
        expiryTime = null
        note = ""
        manualCoordinates = null
        showMapAdjustment = false
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

    LaunchedEffect(initialLocationId, savedLocations) {
        if (initialLocationId != null && initialLocationId != handledLocationId) {
            savedLocations.find { it.id == initialLocationId }?.let { location ->
                selectedSavedLocation = location
                locationSource = LocationSource.SAVED
                handledLocationId = initialLocationId
            }
        }
    }

    LaunchedEffect(vehicles) {
        if (selectedVehicle == null && vehicles.size == 1)
            selectedVehicle = vehicles.first()
    }

    fun startSession(vehicleId: Long, lat: Double, lng: Double) {
        viewModel.startParking(
            vehicleId = vehicleId,
            type = effectiveType,
            latitude = lat,
            longitude = lng,
            hourlyRate = effectiveHourlyRate,
            fixedCost = effectiveFixedCost,
            expiryTime = expiryTime.takeIf { effectiveType == ParkingType.TICKET },
            warningMinutesBefore = warningMinutesBefore,
            note = note.ifBlank { null },
            photoPath = photoPath
        )
        photoPath = null
    }

    // Prova a salvare la sessione nel database
    fun attemptStartParking(scope: CoroutineScope) {
        val vehicle = selectedVehicle ?: return

        summaryVehicleId = vehicle.id

        // Caso in cui la posizione è regolata manualmente sulla mappa
        manualCoordinates?.let { (lat, lng) ->
            startSession(vehicle.id, lat, lng)
            return
        }

        // Caso in cui la posizione è di un luogo salvato
        when (locationSource) {
            LocationSource.SAVED -> {
                val location = selectedSavedLocation ?: return
                startSession(vehicle.id, location.latitude, location.longitude)
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
                    startSession(vehicle.id, lat, lng)
                    return
                }

                // Se non si hanno le coordinate, forza una lettura del GPS
                scope.launch {
                    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                    val coordinates = LocationProvider(fusedClient).getCurrentLocation()
                    if (coordinates != null) {
                        startSession(vehicle.id, coordinates.latitude, coordinates.longitude)
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
            if (summarySession != null) {
                ActiveSessionSummary(
                    display = summarySession,
                    allSessions = activeSessions,
                    canStartNew = vehicles.any { !hasActiveSession(it) },
                    onSelectSession = { vehicleId -> summaryVehicleId = vehicleId },
                    onEndParking = {
                        viewModel.endParking(summarySession.session)
                        summaryVehicleId = null
                    },
                    onNewParking = {
                        summaryVehicleId = null
                        selectedVehicle = vehicles.firstOrNull { !hasActiveSession(it) }
                        resetForm()
                    }
                )
            } else {
                // Seleziona veicolo
                Text("Veicolo", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(vehicles) { vehicle ->
                        VehiclePickCard(
                            vehicle = vehicle,
                            selected = vehicle.id == selectedVehicle?.id,
                            onClick = {
                                selectedVehicle = vehicle
                                if (hasActiveSession(vehicle)) summaryVehicleId = vehicle.id
                            }
                        )
                    }
                }

                // Form del parcheggio
                if (locationSource == LocationSource.CURRENT) {
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
                        OutlinedTextField(
                            value = fixedCost,
                            onValueChange = { fixedCost = it },
                            label = { Text("Costo ticket (€)") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    selectedSavedLocation?.let { location ->
                        SavedLocationCostSumamry(location)
                    }
                }

                AnimatedVisibility(visible = effectiveType == ParkingType.TICKET, enter = fadeIn(), exit = fadeOut()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExpiryTimePicker(expiryTime = expiryTime, onExpirySelected = { expiryTime = it })

                        val expiry = expiryTime
                        if (expiry == null) {
                            Text(
                                "Imposta la scadenza per avviare il parcheggio",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (expiry <= System.currentTimeMillis()) {
                            Text(
                                "La scadenza deve essere successiva all'ora attuale",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text("Avvisami prima della scadenza", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 10, 15, 30).forEach { minutes ->
                                FilterChip(
                                    selected = warningMinutesBefore == minutes,
                                    onClick = { warningMinutesBefore = minutes },
                                    label = { Text("$minutes min") }
                                )
                            }
                        }
                    }
                }

                // Nota
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Nota (opzionale)") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())

                // Foto
                Text("Foto (opzionale)", style = MaterialTheme.typography.labelLarge)
                val currentPhoto = photoPath
                if (currentPhoto == null) {
                    OutlinedButton(
                        onClick = { onTakePhoto() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scatta foto")
                    }
                } else {
                    PhotoThumbnail(
                        path = currentPhoto,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        onClick = { fullScreenPhoto = currentPhoto }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onTakePhoto() }) {
                            Text("Rifai foto")
                        }
                        TextButton(onClick = {
                            File(currentPhoto).delete()
                            photoPath = null
                        }) { Text("Rimuovi") }
                    }
                }

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

                if (activeSessionForSelectedVechicle != null) {
                    Text(
                        "Questo veicolo ha già un parcheggio in corso",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = { summaryVehicleId = selectedVehicle?.id }) {
                        Text("Vedi parcheggio in corso")
                    }
                }

                val ticketExpiryValid = effectiveType != ParkingType.TICKET || (expiryTime?.let { it > System.currentTimeMillis() } == true)
                // Bottone di avvio/termine
                val canSave = selectedVehicle != null && activeSessionForSelectedVechicle == null && ticketExpiryValid && (locationSource == LocationSource.CURRENT || selectedSavedLocation != null)

                Button(
                    onClick = { attemptStartParking(coroutineScope) },
                    enabled =  canSave,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Avvia parcheggio", style = MaterialTheme.typography.titleMedium)
                }

            }

        }
    }
    fullScreenPhoto?.let { path ->
        FullScreenPhotoDialog(path = path, onDismiss =  { fullScreenPhoto = null })
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
            markerPosition?.let { position ->
                val markerState = remember(position) { MarkerState(position = position) }
                Marker(state = markerState)
            }
        }

        if (isOverriden) {
            TextButton(onClick = onResetOverride) {
                Text("Ripristina posizione")
            }
        }
    }
}

// Mostra le informazioni in tempo reale di un parcheggio attivo (tempo e costo).
@Composable
private fun ParkingTrackerInfo(display: ActiveSessionDisplay) {
    val isTicket = display.session.type == ParkingType.TICKET
    val remaining = display.remainingMillis

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isTicket && remaining != null) {
            val isExpired = remaining <= 0
            val color =
                if (isExpired)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurface
            Text(
                text =
                    if (isExpired)
                        "Ticket scaduto"
                    else
                        "Tempo rimanente",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                text = formatElapsedDuration(remaining.coerceAtLeast(0)),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        } else {
            // Tempo trascorso
            Text(
                text = formatElapsedDuration(display.elapsedMillis),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
            )
        }
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

@Composable
private fun SavedLocationCostSumamry(location: SavedLocation) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Tipo di parcheggio: ${location.parkingType.name}", style = MaterialTheme.typography.bodyMedium)
            when (location.parkingType) {
                ParkingType.HOURLY -> Text(
                    "Tariffa oraria: ${location.hourlyRate}€",
                    style = MaterialTheme.typography.bodyMedium
                )

                ParkingType.TICKET -> Text(
                    "Costo ticket: €%.2f".format(location.fixedCost ?: 0.0),
                    style = MaterialTheme.typography.bodyMedium
                )
                ParkingType.FREE -> Text("Parcheggio gratuito", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ExpiryTimePicker(expiryTime: Long?, onExpirySelected: (Long) -> Unit) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    TextButton(onClick = {
        DatePickerDialog(context, { _, year, month, day ->
            calendar.set(year, month, day)
            TimePickerDialog(context, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                onExpirySelected(calendar.timeInMillis)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }) {
        val label = expiryTime?.let {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(it)
        } ?: "Imposta scadenza"
        Text(
            if (expiryTime != null)
                "Scade il: $label"
            else
                label
        )
    }
}

@Composable
private fun ActiveSessionSummary(
    display: ActiveSessionDisplay,
    allSessions: List<ActiveSessionDisplay>,
    canStartNew: Boolean,
    onSelectSession: (Long) -> Unit,
    onEndParking: () -> Unit,
    onNewParking: () -> Unit
) {
    val session = display.session
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.ITALY) }
    val dateTimeFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        if (allSessions.size > 1) {
            Text("Parcheggi in corso", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allSessions, key = { it.session.id }) { other ->
                    FilterChip(
                        selected = other.session.vehicleId == session.vehicleId,
                        onClick = { onSelectSession(other.session.vehicleId) },
                        label = { Text(other.vehicleName)}
                    )
                }
            }
        }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(display.vehicleName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Tipo: ${parkingTypeLabel(session.type)}", style = MaterialTheme.typography.bodyLarge)
                Text("Iniziato alle ${timeFormat.format(session.startTime)}", style = MaterialTheme.typography.bodyMedium)

                if (session.type == ParkingType.TICKET) {
                    session.expiryTime?.let { expiry ->
                        Text("Scade il ${dateTimeFormat.format(expiry)}", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                session.note?.takeIf { it.isNotBlank() }?.let { text ->
                    Text("Nota: $text", style = MaterialTheme.typography.bodyMedium)
                }
                session.photoPath?.let { path ->
                    var showPhoto by remember { mutableStateOf(false) }
                    PhotoThumbnail(
                        path = path,
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        onClick = { showPhoto = true }
                    )
                    if (showPhoto) {
                        FullScreenPhotoDialog(path = path, onDismiss = { showPhoto = false })
                    }
                }
            }
        }

        ParkingTrackerInfo(display = display)

        Button(
            onClick = onEndParking,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Termina parcheggio", style = MaterialTheme.typography.titleMedium)
        }

        OutlinedButton(
            onClick = onNewParking,
            enabled = canStartNew,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Nuovo parcheggio")
        }

        if (!canStartNew) {
            Text(
                "Tutti i veicoli hanno già un parcheggio in corso",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun parkingTypeLabel(type: ParkingType): String = when (type) {
    ParkingType.FREE -> "Gratuito"
    ParkingType.HOURLY -> "A ore"
    ParkingType.TICKET -> "Ticket"
}