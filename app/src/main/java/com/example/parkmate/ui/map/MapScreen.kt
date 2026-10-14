package com.example.parkmate.ui.map

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.data.local.entity.ParkingType
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.ui.parking.ActiveSessionDisplay
import com.example.parkmate.ui.parking.ParkingViewModel
import com.example.parkmate.ui.parking.ParkingViewModelFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.input.KeyboardType
import com.example.parkmate.ui.common.formatPrice
import com.example.parkmate.ui.common.toPriceOrNull
import com.example.parkmate.ui.parking.label
import com.example.parkmate.ui.theme.Spacing

private enum class MapMode {SAVED_LOCATIONS, ACTIVE_SESSIONS}
private val DEFAULT_POSITION = LatLng(44.4949, 11.3426)

private sealed class LocationFormMode {
    data class Add(val latitude: Double, val longitude: Double) : LocationFormMode()
    data class Edit(val location: SavedLocation) : LocationFormMode()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    parkingViewModel: ParkingViewModel = viewModel(factory = ParkingViewModelFactory),
    mapViewModel: MapViewModel = viewModel(factory = MapViewModelFactory)
) {

    val activeSessions by parkingViewModel.activeSessions.collectAsState()
    val savedLocation by mapViewModel.locations.collectAsState()

    var selectedSession by remember { mutableStateOf<ActiveSessionDisplay?>(null) }
    var formMode by remember { mutableStateOf<LocationFormMode?>(null) }
    var mapMode by remember { mutableStateOf(MapMode.SAVED_LOCATIONS) }

    val initialPosition = when (mapMode) {
        MapMode.ACTIVE_SESSIONS -> activeSessions.firstOrNull()?.let { LatLng(it.session.latitude, it.session.longitude) } ?: DEFAULT_POSITION
        MapMode.SAVED_LOCATIONS -> savedLocation.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
    } ?: DEFAULT_POSITION

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 14f)
    }

    LaunchedEffect(mapMode) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(initialPosition, 14f)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            "Mappa",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(top = Spacing.xl, bottom = Spacing.lg)
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(bottom = Spacing.md)
        ) {
            SegmentedButton(
                selected = mapMode == MapMode.SAVED_LOCATIONS,
                onClick = { mapMode = MapMode.SAVED_LOCATIONS },
                shape = SegmentedButtonDefaults.itemShape(0, 2)
            ) { Text("Luoghi salvati") }

            SegmentedButton(
                selected = mapMode == MapMode.ACTIVE_SESSIONS,
                onClick = { mapMode = MapMode.ACTIVE_SESSIONS },
                shape = SegmentedButtonDefaults.itemShape(1, 2)
            ) { Text("Sessioni attive") }
        }
        if (formMode == null) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapLongClick = { latLng ->
                    if (mapMode == MapMode.SAVED_LOCATIONS) {
                        formMode = LocationFormMode.Add(latLng.latitude, latLng.longitude)
                    }
                }
            ) {
                when (mapMode) {
                    MapMode.ACTIVE_SESSIONS -> {
                        activeSessions.forEach { display ->
                            val position = LatLng(display.session.latitude, display.session.longitude)
                            val markerState = remember(display.session.id, position) { MarkerState(position = position) }
                            Marker(
                                state = markerState,
                                title = display.vehicleName,
                                icon = BitmapDescriptorFactory.defaultMarker(2f),
                                onClick = {
                                    selectedSession = display
                                    true
                                }
                            )
                        }
                    }

                    MapMode.SAVED_LOCATIONS -> {
                        savedLocation.forEach { location ->
                            val position = LatLng(location.latitude, location.longitude)
                            val markerState = remember(location.id, position) { MarkerState(position = position) }
                            Marker(
                                state = markerState,
                                title = location.name,
                                icon = BitmapDescriptorFactory.defaultMarker(markerHueFor(location.parkingType)),
                                onClick = {
                                    formMode = LocationFormMode.Edit(location)
                                    true
                                }
                            )

                            if (location.geofenceEnabled) {
                                Circle(
                                    center = LatLng(location.latitude, location.longitude),
                                    radius = location.geofenceRadius.toDouble(),
                                    strokeColor = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2f,
                                    fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedSession?.let { display ->
        val sheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            onDismissRequest = { selectedSession = null },
            sheetState = sheetState
        ) {
            SessionDetailContent(display = display)
        }
    }

    formMode?.let { mode ->
        LocationFormSheet(
            mode = mode,
            onDismiss = { formMode = null },
            onSave = { name, lat, lng, type, hourlyRate, fixedCost, geofenceEnabled, geofenceRadius ->
                when (mode) {
                    is LocationFormMode.Add -> mapViewModel.addLocation(name, lat, lng, type, hourlyRate, fixedCost, geofenceEnabled, geofenceRadius)
                    is LocationFormMode.Edit -> mapViewModel.updateLocation(mode.location.id, name, lat, lng, type, hourlyRate, fixedCost, geofenceEnabled, geofenceRadius)
                }
                formMode = null
            },
            onDelete = (mode as? LocationFormMode.Edit)?.let { editMode ->
                {
                    mapViewModel.deleteLocation(editMode.location)
                    formMode = null
                }
            }
        )
    }
}

private fun markerHueFor(type: ParkingType): Float = when (type) {
    ParkingType.FREE -> 140f
    ParkingType.HOURLY -> 220f
    ParkingType.TICKET -> 38f
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationFormSheet(
    mode: LocationFormMode,
    onDismiss: () -> Unit,
    onSave: (name: String, lat: Double, lng: Double, type: ParkingType, hourlyRate: Double?, fixedCost: Double?, geofenceEnabled: Boolean, geofenceRadius: Float) -> Unit,
    onDelete: (() -> Unit)?
) {
    val initialLatitude = when (mode) {
        is LocationFormMode.Add -> mode.latitude
        is LocationFormMode.Edit -> mode.location.latitude
    }

    val initialLongitude = when (mode) {
        is LocationFormMode.Add -> mode.longitude
        is LocationFormMode.Edit -> mode.location.longitude
    }

    var name by remember { mutableStateOf((mode as? LocationFormMode.Edit)?.location?.name ?: "") }
    var latitudeText by remember { mutableStateOf(initialLatitude.toString()) }
    var longitudeText by remember { mutableStateOf(initialLongitude.toString()) }
    var selectedType by remember { mutableStateOf((mode as? LocationFormMode.Edit)?.location?.parkingType ?: ParkingType.FREE) }

    var hourlyRateText by remember { mutableStateOf((mode as? LocationFormMode.Edit)?.location?.hourlyRate?.toString() ?: "") }
    var fixedCostText by remember { mutableStateOf((mode as? LocationFormMode.Edit)?.location?.fixedCost?.toString() ?: "") }

    var geofenceEnabled by remember { mutableStateOf((mode as? LocationFormMode.Edit)?.location?.geofenceEnabled ?: false) }
    var geofenceRadius by remember { mutableStateOf((mode as? LocationFormMode.Edit)?.location?.geofenceRadius ?: 100f) }

    var hourlyTouched by remember { mutableStateOf(false) }
    var ticketTouched by remember { mutableStateOf(false) }

    val requestGeofencePermission = rememberGeofencePermissionRequester { granted ->
        geofenceEnabled = granted
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(bottom = Spacing.xl)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                if (mode is LocationFormMode.Add) "Nuovo luogo" else "Modifica luogo",
                style = MaterialTheme.typography.titleLarge
            )

            val markerPosition = remember(latitudeText, longitudeText) {
                val lat = latitudeText.toDoubleOrNull()
                val lng = longitudeText.toDoubleOrNull()
                if (lat != null && lng != null)
                    LatLng(lat, lng)
                else
                    null
            }

            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(markerPosition ?: DEFAULT_POSITION, 16f)
            }

            val hourlyRate = hourlyRateText.toPriceOrNull()
            val fixedCost = fixedCostText.toPriceOrNull()

            val priceValid = when (selectedType) {
                ParkingType.FREE -> true
                ParkingType.HOURLY -> hourlyRate != null && hourlyRate > 0
                ParkingType.TICKET -> fixedCost != null && fixedCost > 0
            }


            GoogleMap(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    latitudeText = latLng.latitude.toString()
                    longitudeText = latLng.longitude.toString()
                }
            ) {
                markerPosition?.let { position ->
                    val markerState = remember(position) { MarkerState(position = position) }
                    Marker(state = markerState)
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = latitudeText,
                    onValueChange = { latitudeText = it },
                    label = { Text("Latitudine") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = longitudeText,
                    onValueChange = { longitudeText = it },
                    label = { Text("Longitudine") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Tipo di parcheggio", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ParkingType.entries.forEach { type ->
                    LocationTypeCard(
                        type = type,
                        selected = type == selectedType,
                        onClick = { selectedType = type },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            AnimatedVisibility(
                visible = selectedType == ParkingType.HOURLY,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val showError = hourlyTouched && (hourlyRate == null || hourlyRate <= 0)

                OutlinedTextField(
                    value = hourlyRateText,
                    onValueChange = { input ->
                        hourlyRateText = input.filter { it.isDigit() || it == ',' || it == '.' }.take(7)
                        hourlyTouched = true
                    },
                    label = { Text("Prezzo orario") },
                    isError = showError,
                    supportingText = if (showError) { { Text("Inserisci una tariffa") } } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().offset(y = (-5).dp)
                )
            }

            AnimatedVisibility(
                visible = selectedType == ParkingType.TICKET,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val showError = ticketTouched && (fixedCost == null || fixedCost <= 0)
                OutlinedTextField(
                    value = fixedCostText,
                    onValueChange = { input ->
                        fixedCostText = input.filter { it.isDigit() || it == ',' || it == '.' }.take(7)
                        ticketTouched = true
                    },
                    label = { Text("Costo ticket") },
                    isError = showError,
                    supportingText = if (showError) { { Text("Inserisci il costo del ticket") } } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().offset(y = (-5).dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Avvisami quando entro/esco", modifier = Modifier.weight(1f))
                Switch(
                    checked = geofenceEnabled,
                    onCheckedChange = { wantsOn ->
                        if (wantsOn)
                            requestGeofencePermission()
                        else
                            geofenceEnabled = false
                    }
                )
            }

            AnimatedVisibility(visible = geofenceEnabled, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    Text("Raggio: ${geofenceRadius.toInt()} m", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = geofenceRadius,
                        onValueChange = { geofenceRadius = it },
                        valueRange = 100f..500f,
                        steps = 7,
                    )
                }
            }


            val canSave = name.isNotBlank() && latitudeText.toDoubleOrNull() != null && longitudeText.toDoubleOrNull() != null && priceValid

            Button(
                onClick = {
                    val lat = latitudeText.toDoubleOrNull() ?: return@Button
                    val lng = longitudeText.toDoubleOrNull() ?: return@Button
                    onSave(
                        name,
                        lat,
                        lng,
                        selectedType,
                        hourlyRate.takeIf { selectedType == ParkingType.HOURLY},
                        fixedCost.takeIf { selectedType == ParkingType.TICKET},
                        geofenceEnabled,
                        geofenceRadius)
                },
                enabled = canSave,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    if (mode is LocationFormMode.Add)
                        "Salva luogo"
                    else
                        "Salva modifiche"
                )
            }
            onDelete?.let { delete ->
                TextButton(
                    onClick = delete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Elimina luogo")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LocationTypeCard(
    type: ParkingType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (selected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                type.label,
                style = MaterialTheme.typography.labelLarge,
                color =
                    if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
private fun SessionDetailContent(display: ActiveSessionDisplay) {
    val session = display.session
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.ITALY) }
    val dateTimeFormat = remember { SimpleDateFormat("dd/MM/yyyy 'alle' HH:mm", Locale.ITALY) }


    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = Spacing.screenHorizontal)
        .padding(bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Column {
            Text(
                display.vehicleName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                session.type.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SessionInfoRow("Inizio", "Oggi alle ${timeFormat.format(Date(session.startTime))}")

        // Scadenza solo per i ticket
        if (session.type == ParkingType.TICKET) {
            session.expiryTime?.let { expiry ->
                val expired = expiry <= System.currentTimeMillis()
                SessionInfoRow(
                    label = if (expired) "Scaduto il" else "Scadenza",
                    value = dateTimeFormat.format(Date(expiry)),
                    valueColor = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        display.currentCost?.let { cost ->
            SessionInfoRow(
                label = if (session.type == ParkingType.TICKET) "Costo" else "Costo attuale",
                value = formatPrice(cost),
                valueColor = MaterialTheme.colorScheme.primary
            )

        }
    }
}

@Composable
private fun SessionInfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor)
    }
}

@Composable
private fun rememberGeofencePermissionRequester(onResult: (Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val fine = Manifest.permission.ACCESS_FINE_LOCATION
    val background = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    fun isGranted(permission: String) = ContextCompat.checkSelfPermission(context, permission) ==
        PackageManager.PERMISSION_GRANTED

    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> onResult(isGranted(background)) }

    val fineLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        when{
            !granted -> onResult(false)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> backgroundLauncher.launch(background)

            else -> onResult(true)
        }
    }

    return {
        when {
            !isGranted(fine) -> fineLauncher.launch(fine)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isGranted(background) -> backgroundLauncher.launch(background)
            else -> onResult(true)
        }
    }
}

