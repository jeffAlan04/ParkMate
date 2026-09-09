package com.example.parkmate.ui.parking

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.data.local.entity.Vehicle
import com.example.parkmate.data.local.entity.ParkingType
import com.example.parkmate.data.local.entity.SavedLocation
import com.example.parkmate.data.location.LocationProvider
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

private enum class LocationSource { CURRENT, SAVED }

@Composable
fun StartParkScreen (viewModel: ParkingViewModel = viewModel(factory = ParkingViewModelFactory)
) {
    val vehicles by viewModel.vehicles.collectAsState()
    val savedLocations by viewModel.savedLocations.collectAsState()

    var selectedVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var selectedType by remember { mutableStateOf(ParkingType.FREE) }
    var hourlyRate by remember { mutableStateOf("") }
    var fixedCost by remember { mutableStateOf("") }

    var locationSource by remember { mutableStateOf(LocationSource.CURRENT) }
    var selecteSavedLocation by remember { mutableStateOf<SavedLocation?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val (permissionStatus, requestPermission) = rememberLocationPermissionStatus()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
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

        Text("Posizione", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth())  {
            SegmentedButton (
                selected = locationSource == LocationSource.CURRENT,
                onClick = { locationSource = LocationSource.CURRENT },
                shape = SegmentedButtonDefaults.itemShape(0, 2)
            ) { Text("Posizione Attuale")}

            SegmentedButton (
                selected = locationSource == LocationSource.SAVED,
                onClick = { locationSource = LocationSource.SAVED },
                shape = SegmentedButtonDefaults.itemShape(1, 2)
            ) { Text("Luogo Salvato") }
        }

        Spacer(modifier = Modifier.weight(1f))

        val canSave = selectedVehicle != null && (locationSource == LocationSource.CURRENT || selecteSavedLocation != null)

        Button(
            onClick = {
                val vehicle = selectedVehicle ?: return@Button

                coroutineScope.launch {
                    val coordinates = when (locationSource) {
                        LocationSource.SAVED -> selecteSavedLocation?.let {
                            it.latitude to it.longitude
                        }

                        LocationSource.CURRENT -> {
                            if (permissionStatus != LocationPermissionStatus.GRANTED) {
                                requestPermission()
                                null
                            } else {
                                val fusedClient =
                                    LocationServices.getFusedLocationProviderClient(context)
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
                            expiryTime = null
                        )
                    }
                }
            },
            enabled = canSave,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Avvia Parcheggio", style = MaterialTheme.typography.titleMedium)}
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