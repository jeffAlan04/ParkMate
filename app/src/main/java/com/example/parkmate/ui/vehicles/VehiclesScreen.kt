package com.example.parkmate.ui.vehicles

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun VehiclesScreen(viewModel: VehicleViewModel = viewModel(factory = VehicleViewModelFactory)) {
    val vehicles by viewModel.vehicles.collectAsState()

    LazyColumn {
        items(vehicles) { vehicle ->
            Text("${vehicle.name} - ${vehicle.type}")
        }
    }
}