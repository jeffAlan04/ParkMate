package com.example.parkmate.ui.vehicles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun VehiclesScreen(
    viewModel: VehicleViewModel = viewModel(factory = VehicleViewModelFactory),
    onAddVehicle: () -> Unit = {},
    onEditVehicle: (Long) -> Unit = {}
) {
    val vehicles by viewModel.vehicles.collectAsState()

    LazyColumn {
        items(vehicles) { vehicle ->
            Text(
                "${vehicle.name} - ${vehicle.type}",

                modifier = Modifier.clickable{ onEditVehicle(vehicle.id) }
            )
        }
    }
}