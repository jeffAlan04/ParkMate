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

private enum class LocationSource { CURRENT, SAVED }

@Composable
fun StartParkScreen (
    viewModel: ParkingViewModel = viewModel(factory = ParkingViewModelFactory),
    onParkingStarted: () -> Unit = {}
) {
    val vehicles by viewModel.vehicles.collectAsState()
    val savedLocations by viewModel.savedLocations.collectAsState()

    var selectedVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var selectedType by remember { mutableStateOf(ParkingType.FREE) }
    var hourlyRate by remember { mutableStateOf("") }
    var fixedCost by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    var locationSource by remember { mutableStateOf(LocationSource.CURRENT) }
    var selecteSavedLocation by remember { mutableStateOf<SavedLocation?>(null) }

    var pendingStartAfterPermission by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val (permissionStatus, requestPermission) = rememberLocationPermissionStatus()

    val activeSessions by viewModel.activeSessions.collectAsState()

    val activeSessionForSelectedVechicle = selectedVehicle?.let { vehicle ->
        activeSessions.find { it.session.vehicleId == vehicle.id }
    }

    fun attemptStartParking(scope: CoroutineScope) {
        val vehicle = selectedVehicle ?: return

        scope.launch {
            val coordinates = when (locationSource) {
                LocationSource.SAVED -> selecteSavedLocation?.let {
                    it.latitude to it.longitude
                }

                LocationSource.CURRENT -> {
                    if (permissionStatus != LocationPermissionStatus.GRANTED) {
                        pendingStartAfterPermission = true
                        requestPermission()
                        null
                    } else {
                        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                        LocationProvider(fusedClient).getCurrentLocation()
                            ?.let { it.latitude to it.longitude }
                    }
                }
            }

            if (coordinates != null) {
                val (lat, lng) = coordinates
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
            }
        }
    }

    LaunchedEffect(permissionStatus) {
        if (permissionStatus == LocationPermissionStatus.GRANTED && pendingStartAfterPermission) {
            pendingStartAfterPermission = false
            attemptStartParking(this)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
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
        AnimatedVisibility(
            visible = selectedType == ParkingType.HOURLY,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            OutlinedTextField(
                value = hourlyRate,
                onValueChange = { hourlyRate = it },
                label = { Text("Tariffa oraria (€)") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        AnimatedVisibility(
            visible = selectedType == ParkingType.TICKET,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
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

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Nota (opzionale)") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Scelta posizione
        Text("Posizione", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = locationSource == LocationSource.CURRENT,
                onClick = { locationSource = LocationSource.CURRENT },
                shape = SegmentedButtonDefaults.itemShape(0, 2)
            ) { Text("Posizione Attuale") }

            SegmentedButton(
                selected = locationSource == LocationSource.SAVED,
                onClick = { locationSource = LocationSource.SAVED },
                shape = SegmentedButtonDefaults.itemShape(1, 2)
            ) { Text("Luogo Salvato") }
        }

        AnimatedVisibility(visible = locationSource == LocationSource.SAVED) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(savedLocations) { location ->
                    FilterChip(
                        selected = location.id == selecteSavedLocation?.id,
                        onClick = { selecteSavedLocation = location },
                        label = { Text(location.name) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        val canSave = selectedVehicle != null && (locationSource == LocationSource.CURRENT || selecteSavedLocation != null)

        AnimatedVisibility(
            visible = activeSessionForSelectedVechicle != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            activeSessionForSelectedVechicle?.let { display ->
                ParkingTrackerCard(display = display)
            }
        }

        Button(
            onClick = {
                val activeSession = activeSessionForSelectedVechicle
                if (activeSession != null) {
                    viewModel.endParking(activeSession.session)
                } else {
                    attemptStartParking(coroutineScope)
                }
            },
            enabled = activeSessionForSelectedVechicle != null || canSave,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(
                if (activeSessionForSelectedVechicle != null)
                    "Termina Parcheggio"
                else
                    "Avvia Parcheggio",

                style = MaterialTheme.typography.titleMedium)
        }


    }
}

@Composable
private fun ParkingTrackerCard(display: ActiveSessionDisplay) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatElapsedDuration(display.elapsedMillis),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold
        )

        display.currentCost?.let { cost ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = String.format(Locale.ITALY, "€ %.2f", cost),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

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
