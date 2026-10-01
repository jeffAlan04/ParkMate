package com.example.parkmate.ui.history

import android.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.example.parkmate.data.local.entity.ParkingType
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.lazy.items

private enum class FilterCategory(val label: String) {
    NONE("Nessuno"), VEHICLE("Veicolo"), TYPE("Tipo"), PERIOD("Periodo")
}

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory)) {
    val entries by viewModel.filteredHistory.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState()

    var activeCategory by remember { mutableStateOf(FilterCategory.NONE) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Storico parcheggi", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Box {
            OutlinedButton(onClick = { categoryMenuExpanded = true }) {
                Text("Filtro: ${activeCategory.label}")
            }
            DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                FilterCategory.entries.forEach { category ->
                    DropdownMenuItem(text = { Text(category.label) }, onClick = {
                        activeCategory = category
                        categoryMenuExpanded = false
                        if (category == FilterCategory.NONE)
                            viewModel.setFilter(HistoryFilter.None)
                    }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        when(activeCategory) {
            FilterCategory.VEHICLE -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vehicles.forEach { vehicle ->
                        FilterChip(
                            selected = (viewModel.filter.collectAsState().value as? HistoryFilter.ByVehicle)?.vehicleId == vehicle.id,
                            onClick = { viewModel.setFilter(HistoryFilter.ByVehicle(vehicle.id)) },
                            label = { Text(vehicle.name) }
                        )
                    }
                }
            }

            FilterCategory.TYPE -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ParkingType.entries.forEach { type ->
                        FilterChip(
                            selected = (viewModel.filter.collectAsState().value as? HistoryFilter.ByType)?.type == type,
                            onClick = { viewModel.setFilter(HistoryFilter.ByType(type)) },
                            label = { Text(type.name) }
                        )
                    }
                }
            }

            FilterCategory.PERIOD -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val labels = mapOf(TimeRange.TODAY to "Oggi", TimeRange.WEEK to "Settimana", TimeRange.MONTH to "Mese")
                    labels.forEach { (range, label) ->
                        FilterChip(
                            selected = (viewModel.filter.collectAsState().value as? HistoryFilter.ByPeriod)?.range == range,
                            onClick = { viewModel.setFilter(HistoryFilter.ByPeriod(range)) },
                            label = { Text(label) }
                        )
                    }
                }
            }
            FilterCategory.NONE -> Unit
        }
        Spacer(Modifier.height(16.dp))

        if (entries.isEmpty()) {
            Text("Nessuna session trovata", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(entries, key = { it.session.id }) { entry ->
                    HistoryEntryCard(entry)
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(entry: HistoryEnter) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY) }

    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(entry.vehicleName, style = MaterialTheme.typography.titleMedium)
                Text(entry.session.type.name, style = MaterialTheme.typography.labelMedium)
            }
            Text(dateFormat.format(entry.session.startTime), style = MaterialTheme.typography.bodySmall)
            Text(
                "€%.2f".format(entry.cost),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
