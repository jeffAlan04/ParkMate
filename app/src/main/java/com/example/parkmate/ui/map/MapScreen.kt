package com.example.parkmate.ui.map

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.data.local.entity.ParkingType
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.ui.location.LocationViewModel
import com.example.parkmate.ui.location.LocationViewModelFactory
import com.example.parkmate.ui.parking.ActiveSessionDisplay
import com.example.parkmate.ui.parking.ParkingViewModel
import com.example.parkmate.ui.parking.ParkingViewModelFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    locationViewModel: LocationViewModel = viewModel(factory = LocationViewModelFactory)
) {

    val activeSessions by parkingViewModel.activeSessions.collectAsState()
    val savedLocation by locationViewModel.locations.collectAsState()

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

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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

        Text(
            text =
                if (mapMode == MapMode.SAVED_LOCATIONS)
                    "Luoghi salvati"
                else
                    "Parcheggi attivi",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
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
                            Marker(
                                state = MarkerState(
                                    position = LatLng(
                                        display.session.latitude,
                                        display.session.longitude
                                    )
                                ),
                                title = display.vehicleName,
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                                onClick = {
                                    selectedSession = display
                                    true
                                }
                            )
                        }
                    }

                    MapMode.SAVED_LOCATIONS -> {
                        savedLocation.forEach { location ->
                            Marker(
                                state = MarkerState(
                                    position = LatLng(
                                        location.latitude,
                                        location.longitude
                                    )
                                ),
                                title = location.parkingType.name,
                                icon = BitmapDescriptorFactory.defaultMarker(markerHueFor(location.parkingType)),
                                onClick = {
                                    formMode = LocationFormMode.Edit(location)
                                    true
                                }
                            )
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
            onSave = { name, lat, lng, type, hourlyRate, fixedCost ->
                when (mode) {
                    is LocationFormMode.Add -> locationViewModel.addLocation(name, lat, lng, type, hourlyRate, fixedCost)
                    is LocationFormMode.Edit -> locationViewModel.updateLocation(mode.location.id, name, lat, lng, type, hourlyRate, fixedCost)
                }
                formMode = null
            },
            onDelete = (mode as? LocationFormMode.Edit)?.let { editMode ->
                {
                    locationViewModel.deleteLocation(editMode.location)
                    formMode = null
                }
            }
        )
    }
}

private fun markerHueFor(type: ParkingType): Float = when (type) {
    ParkingType.FREE -> BitmapDescriptorFactory.HUE_GREEN
    ParkingType.HOURLY -> BitmapDescriptorFactory.HUE_CYAN
    ParkingType.TICKET -> BitmapDescriptorFactory.HUE_ORANGE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationFormSheet(
    mode: LocationFormMode,
    onDismiss: () -> Unit,
    onSave: (name: String, lat: Double, lng: Double, type: ParkingType, hourlyRate: Double?, fixedCost: Double?) -> Unit,
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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
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

            GoogleMap(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    latitudeText = latLng.latitude.toString()
                    longitudeText = latLng.longitude.toString()
                }
            ) {
                markerPosition?.let {
                    Marker(state = MarkerState(position = it))
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
                OutlinedTextField(
                    value = hourlyRateText,
                    onValueChange = { hourlyRateText = it },
                    label = { Text("Prezzo orario") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedVisibility(
                visible = selectedType == ParkingType.TICKET,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                OutlinedTextField(
                    value = fixedCostText,
                    onValueChange = { fixedCostText = it },
                    label = { Text("Costo ticker") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val canSave = name.isNotBlank() && latitudeText.toDoubleOrNull() != null && longitudeText.toDoubleOrNull() != null

            Button(
                onClick = {
                    val lat = latitudeText.toDoubleOrNull() ?: return@Button
                    val lng = longitudeText.toDoubleOrNull() ?: return@Button
                    onSave(name, lat, lng, selectedType, hourlyRateText.toDoubleOrNull(), fixedCostText.toDoubleOrNull())
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
                type.name,
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
    val startTimeFormatted = remember(display.session.startTime) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(display.session.startTime))
    }

    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Row {
            Icon(Icons.Default.DirectionsCar, contentDescription = null)
            Spacer(modifier = Modifier.height(0.dp))
            Text(
                display.vehicleName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Tipo: ${display.session.type.name}", style = MaterialTheme.typography.bodyLarge)
        Text("Iniziato alle: $startTimeFormatted", style = MaterialTheme.typography.bodyLarge)

        display.currentCost?.let { cost ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Costo attuale: €%.2f".format(cost),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
