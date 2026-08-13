package com.example.parkmate.ui.vehicles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.data.local.entity.Vehicle

@Composable
fun VehiclesScreen(
    viewModel: VehicleViewModel = viewModel(factory = VehicleViewModelFactory),
    onAddVehicle: () -> Unit = {},
    onEditVehicle: (Long) -> Unit = {}
) {
    val vehicles by viewModel.vehicles.collectAsState()

    var vehicleToDelete by remember { mutableStateOf<Vehicle?>(null) }
    LazyColumn {
        items(vehicles) { vehicle ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),

                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${vehicle.name} - ${vehicle.type}",
                    modifier = Modifier.clickable{ onEditVehicle(vehicle.id) }
                )

                IconButton(onClick = { viewModel.deleteVehicle(vehicle) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Elimina ${vehicle.name}"
                    )
                }
            }
        }
    }
    // Dialog compare quando vehicleToDelete non e' null
    vehicleToDelete?.let { vehicle ->
        AlertDialog(
            onDismissRequest = { vehicleToDelete = null }, // Chiude senza eliminare
            title = {Text("Eliminare veicolo?") },
            text = { Text("Sei sicuro di voler eliminare \"${vehicle.name}\"?")},
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteVehicle(vehicle) //elimina il veicolo
                    vehicleToDelete = null // Richiude il dialog
                }) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToDelete = null }) {
                    Text("Annulla")
                }
            }
        )
    }
}