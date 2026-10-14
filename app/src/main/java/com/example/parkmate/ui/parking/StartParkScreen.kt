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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.parkmate.ui.photo.FullScreenPhotoDialog
import com.example.parkmate.ui.photo.PhotoThumbnail
import com.example.parkmate.ui.theme.Spacing
import com.example.parkmate.ui.theme.extendedColors
import com.example.parkmate.ui.vehicles.icon

private enum class LocationSource { CURRENT, SAVED }

@OptIn(ExperimentalMaterial3Api::class)
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
        LocationSource.CURRENT -> hourlyRate.toPriceOrNull()
    }

    val effectiveFixedCost = when (locationSource) {
        LocationSource.SAVED -> selectedSavedLocation?.fixedCost
        LocationSource.CURRENT -> fixedCost.toPriceOrNull()
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

    BackHandler(enabled = summarySession != null) {
        summaryVehicleId = null
    }

    // INTERFACCIA UTENTE
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        if (summarySession != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
            ) {
                ScreenHeader(
                    title = "Parcheggio in corso",
                    onBack = { summaryVehicleId = null }
                )
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
                    },
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal).padding(bottom = Spacing.xl)
                )
            }
        } else {
            val ticketExpiryValid = effectiveType != ParkingType.TICKET || (expiryTime?.let { it > System.currentTimeMillis() } == true)
            // Bottone di avvio/termine

            val canSave = selectedVehicle != null &&
                    activeSessionForSelectedVechicle == null &&
                    ticketExpiryValid &&
                    (locationSource == LocationSource.CURRENT || selectedSavedLocation != null)

            val blockingReason = when {
                vehicles.isEmpty() -> "Aggiungi un veicolo"
                selectedVehicle == null -> "Seleziona un veicolo"
                activeSessionForSelectedVechicle != null -> null
                locationSource == LocationSource.SAVED && selectedSavedLocation == null -> "Scegli un luogo salvato"
                !ticketExpiryValid -> "Imposta una scadenza valida"
                else -> null
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    ScreenHeader(title = "Nuovo parcheggio")
                    Column(
                        modifier = Modifier
                            .padding(horizontal = Spacing.screenHorizontal)
                            .padding(bottom = Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
                    ) {
                        FormSection(title = "Veicolo") {
                            if (vehicles.isEmpty()) {
                                Text(
                                    "Non hai ancora veicoli. Aggiungine uno",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(vehicles) { vehicle ->
                                        VehicleOption(
                                            vehicle = vehicle,
                                            selected = vehicle.id == selectedVehicle?.id,
                                            isParked = hasActiveSession(vehicle),
                                            onClick = {
                                                selectedVehicle = vehicle
                                                if (hasActiveSession(vehicle))
                                                    summaryVehicleId = vehicle.id
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        // Posizione
                        FormSection(title = "Posizione") {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {

                                // Attuale
                                SegmentedButton(
                                    selected = locationSource == LocationSource.CURRENT,
                                    onClick = { locationSource = LocationSource.CURRENT },
                                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                                )  { Text("Attuale", maxLines = 1) }

                                // Salvata
                                SegmentedButton(
                                    selected = locationSource == LocationSource.SAVED,
                                    onClick = { locationSource = LocationSource.SAVED },
                                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                                ) { Text("Luogo Salvato", maxLines = 1) }
                            }
                            // Pulsante per mostrare la mappa e regolare la posizione GPS
                            if (locationSource == LocationSource.CURRENT) {
                                when {
                                    permissionStatus != LocationPermissionStatus.GRANTED -> LocationStatusRow(
                                        text = "Il permesso verrà chiesto all'avvio",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    baseCoordinates == null -> LocationStatusRow(
                                        text = "Rilevamento della posizione",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (baseCoordinates != null) {
                                    TextButton(
                                        onClick = {showMapAdjustment = !showMapAdjustment},
                                        contentPadding = PaddingValues(horizontal = 0.dp)
                                    ) {
                                        Text(
                                            if (showMapAdjustment)
                                                "Nascondi mappa"
                                            else
                                                "Regola sulla mappa"
                                        )
                                    }
                                    AnimatedVisibility(visible = showMapAdjustment) {
                                        LocationAdjustmentMap(
                                            coordinates = finalCoordinates,
                                            isOverriden = manualCoordinates != null,
                                            onMapClick = { lat, lng ->
                                                manualCoordinates = lat to lng
                                            },
                                            onResetOverride = { manualCoordinates = null }
                                        )
                                    }
                                }
                            } else {
                                if (savedLocations.isEmpty()) {
                                    Text(
                                        "Non hai ancora salvato nessun luogo",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                        items(savedLocations, key = { it.id }) { location ->
                                            FilterChip(
                                                selected = location.id == selectedSavedLocation?.id,
                                                onClick = { selectedSavedLocation = location },
                                                label = { Text(location.name) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // Tariffa
                        FormSection(title = "Tariffa") {
                            if (locationSource == LocationSource.CURRENT) {
                                Row(
                                    modifier = Modifier.selectableGroup(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    ParkingType.entries.forEach { type ->
                                        ParkingTypeOption(
                                            type = type,
                                            selected = type == selectedType,
                                            onClick = { selectedType = type },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                // Tariffa oraria
                                AnimatedVisibility(visible = selectedType == ParkingType.HOURLY) {
                                    PriceField(
                                        value = hourlyRate,
                                        onValueChange = { hourlyRate = it },
                                        label = "Tariffa oraria (€)",
                                    )
                                }
                                // Ticket
                                AnimatedVisibility(visible = selectedType == ParkingType.TICKET) {
                                    PriceField(
                                        value = fixedCost,
                                        onValueChange = { fixedCost = it },
                                        label = "Costo ticket (€)",
                                    )
                                }
                            } else {
                                val location = selectedSavedLocation
                                if (location == null) {
                                    Text(
                                        "Scegli un luogo salvato per vedere la sua tariffa",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    SavedLocationCostSumamry(location)
                                }
                            }
                        }
                        // Scadenza
                        AnimatedVisibility(visible = effectiveType == ParkingType.TICKET) {
                            FormSection(title = "Scadenza del ticket") {
                                ExpiryTimePicker(expiryTime = expiryTime, onExpirySelected = { expiryTime = it})

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
                                Text(
                                    "Avvisami prima della scadenza",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    listOf(1, 10, 15, 30).forEach { minutes ->
                                        FilterChip(
                                            selected = warningMinutesBefore == minutes,
                                            onClick = { warningMinutesBefore = minutes },
                                            label = { Text("$minutes min") },
                                            shape = MaterialTheme.shapes.small
                                        )
                                    }
                                }
                            }
                        }
                        // Dettagli
                        FormSection(
                            title = "Dettagli",
                            subtitle = "Facoltativi"
                        ) {
                            // Nota
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                label = { Text("Nota") },
                                maxLines = 3,
                                shape = MaterialTheme.shapes.medium,
                                colors = formFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Foto
                            val currentPhoto = photoPath
                            if (currentPhoto == null) {
                                OutlinedButton(
                                    onClick = { onTakePhoto() },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(Spacing.buttonHeight)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(Spacing.sm))
                                    Text("Aggiungi una foto", style = MaterialTheme.typography.labelLarge)
                                }
                            } else {
                                PhotoThumbnail(
                                    path = currentPhoto,
                                    modifier = Modifier.fillMaxWidth().height(180.dp),
                                    onClick = { fullScreenPhoto = currentPhoto }
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                    TextButton(onClick = { onTakePhoto() }) {
                                        Text("Rifai foto")
                                    }
                                    TextButton(
                                        onClick = {
                                            File(currentPhoto).delete()
                                            photoPath = null
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) { Text("Rimuovi") }
                                }
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    if (blockingReason != null) {
                        Text(
                            blockingReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = { attemptStartParking(coroutineScope) },
                        enabled = canSave,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().height(Spacing.buttonHeight)
                    ) { Text("Avvia parcheggio", style = MaterialTheme.typography.labelLarge) }
                }
            }
        }
    }
    fullScreenPhoto?.let { path ->
        FullScreenPhotoDialog(path = path, onDismiss =  { fullScreenPhoto = null })
    }
}
@Composable
private fun ScreenHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {

    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(
                start = if (onBack != null) Spacing.xs else Spacing.screenHorizontal,
                end = Spacing.screenHorizontal
            )
            .padding(top = Spacing.xl, bottom = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Indietro")
            }
            Spacer(Modifier.width(Spacing.xs))
        }
        Column{
            Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) {
                Spacer(Modifier.height(Spacing.xs))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Spacer(Modifier.width(Spacing.sm))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        content()
    }
}
@Composable
private fun LocationStatusRow(text: String, tint: Color) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = tint)
}

@Composable
private fun VehicleOption(vehicle: Vehicle, selected: Boolean, isParked: Boolean, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape
            )
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            vehicle.type.icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(Spacing.sm))
        Column {
            Text(
                vehicle.name,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isParked) {
                Text("In sosta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

// Card per selezionare il tipo di parcheggio
@Composable
private fun ParkingTypeOption(type: ParkingType, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.medium
    Column (
        modifier = modifier
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape
            )
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            type.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PriceField(value: String, onValueChange: (String) -> Unit, label: String, suffix: String? = null) {
    OutlinedTextField(
        value = value,
        // Solo cifre e un separatore decimale
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() || it == ',' || it == '.' }.take(7)) },
        label = { Text(label) },
        prefix = { Text("€ ") },
        suffix = suffix?.let { s -> { Text(s) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = MaterialTheme.shapes.medium,
        colors = formFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SavedLocationCostSumamry(location: SavedLocation) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Spacing.md),
    ) {
        Text(
            location.parkingType.label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            when (location.parkingType) {
                ParkingType.HOURLY -> location.hourlyRate?.let { "${formatPrice(it)} / ora" } ?: "Tariffa non indicata"
                ParkingType.TICKET -> location.fixedCost?.let { formatPrice(it) } ?: "Costo non indicato"
                ParkingType.FREE -> "Nessun costo" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExpiryTimePicker(expiryTime: Long?, onExpirySelected: (Long) -> Unit) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    val label = expiryTime?.let {
        "Scade il " + SimpleDateFormat("dd/MM/yyyy 'alle' HH:mm", Locale.ITALY).format(it)
    } ?: "Imposta data e ora di scadenza"

    OutlinedButton(
        onClick = {
            DatePickerDialog(context, { _, year, month, day ->
                calendar.set(year, month, day)
                TimePickerDialog(context, { _, hour, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    onExpirySelected(calendar.timeInMillis)
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .height(Spacing.buttonHeight)
    ) {
        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Spacing.sm))
        Text(label, style = MaterialTheme.typography.labelLarge)
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
            TextButton(
                onClick = onResetOverride,
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                Text("Ripristina posizione")
            }
        }
    }
}

@Composable
private fun ActiveSessionSummary(
    display: ActiveSessionDisplay,
    allSessions: List<ActiveSessionDisplay>,
    canStartNew: Boolean,
    onSelectSession: (Long) -> Unit,
    onEndParking: () -> Unit,
    onNewParking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session = display.session
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.ITALY) }
    val dateTimeFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY) }
    var showPhoto by remember(session.id) { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {

        // Selettore tra più parcheggi in corso
        if (allSessions.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(allSessions, key = { it.session.id }) { other ->
                    FilterChip(
                        selected = other.session.vehicleId == session.vehicleId,
                        onClick = { onSelectSession(other.session.vehicleId) },
                        label = { Text(other.vehicleName) },
                        shape = MaterialTheme.shapes.small
                    )
                }
            }
        }

        // Card principale: veicolo, tempo, costo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(vertical = Spacing.xl, horizontal = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${display.vehicleName} · ${session.type.label}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(Spacing.lg))
            ParkingTrackerInfo(display = display)
        }

        // Dettagli
        val shape = MaterialTheme.shapes.large
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            DetailRow(
                Icons.Default.Schedule,
                "Inizio",
                "Oggi alle ${timeFormat.format(session.startTime)}"
            )

            if (session.type == ParkingType.TICKET) {
                session.expiryTime?.let { expiry ->
                    DetailRow(Icons.Default.Event, "Scadenza", dateTimeFormat.format(expiry))
                }
            }

            session.note?.takeIf { it.isNotBlank() }?.let { text ->
                DetailRow(Icons.Default.EditNote, "Nota", text)
            }

            session.photoPath?.let { path ->
                PhotoThumbnail(
                    path = path,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    onClick = { showPhoto = true }
                )
                if (showPhoto) {
                    FullScreenPhotoDialog(path = path, onDismiss = { showPhoto = false })
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Button(
                onClick = onEndParking,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.buttonHeight)
            ) {
                Text("Termina parcheggio", style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
                onClick = onNewParking,
                enabled = canStartNew,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.buttonHeight)
            ) {
                Text("Nuovo parcheggio", style = MaterialTheme.typography.labelLarge)
            }

            if (!canStartNew) {
                Text(
                    "Tutti i veicoli hanno già un parcheggio in corso",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


// Mostra le informazioni in tempo reale di un parcheggio attivo (tempo e costo).
@Composable
private fun ParkingTrackerInfo(display: ActiveSessionDisplay) {
    val isTicket = display.session.type == ParkingType.TICKET
    val remaining = display.remainingMillis
    val onHero = MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isTicket && remaining != null) {
            val isExpired = remaining <= 0
            val isEndingSoon = !isExpired && remaining <= 15 * 60000L
            val accent = when {
                isExpired -> MaterialTheme.colorScheme.error
                isEndingSoon -> MaterialTheme.extendedColors.warning
                else -> onHero
            }
            Text(
                when {
                    isExpired -> "Ticket scaduto"
                    isEndingSoon -> "In scadenza"
                    else -> "Tempo rimanente"
                },
                style = MaterialTheme.typography.labelLarge,
                color = accent
            )

            Text(
                text = formatElapsedDuration(remaining.coerceAtLeast(0)),
                style = MaterialTheme.typography.displayLarge,
                color = accent
            )
        } else {
            // Tempo trascorso
            Text(
                text = formatElapsedDuration(display.elapsedMillis),
                style = MaterialTheme.typography.displayLarge,
                color = onHero
            )
        }
        // Costo accumulato finora
        display.currentCost?.let { cost ->
            Text(
                formatPrice(cost),
                style = MaterialTheme.typography.titleLarge,
                color = onHero
            )
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Spacing.md))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun formFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    errorContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline
)

private fun String.toPriceOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

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