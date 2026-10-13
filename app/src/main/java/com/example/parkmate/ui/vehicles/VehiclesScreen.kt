package com.example.parkmate.ui.vehicles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.data.local.entity.VehicleType
import com.example.parkmate.ui.theme.Spacing

@Composable
fun VehiclesScreen(
    viewModel: VehicleViewModel = viewModel(factory = VehicleViewModelFactory),
    onAddVehicle: () -> Unit = {},
    onEditVehicle: (Long) -> Unit = {}
) {
    val vehicleItems by viewModel.vehicleItems.collectAsState()
    var vehicleToDelete by remember { mutableStateOf<VehicleListItem?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        VehiclesHeader(items = vehicleItems, onAddVehicle = onAddVehicle)

        val list = vehicleItems
        when {
            list == null -> Unit
            list.isEmpty() -> EmptyVehicles(modifier = Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = Spacing.screenHorizontal,
                    end = Spacing.screenHorizontal,
                    bottom = Spacing.xl
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(list, key = { it.vehicle.id}) { item ->
                    SwipeToDeleteVehicle(
                        item = item,
                        onEditVehicle = { onEditVehicle(item.vehicle.id) },
                        onDeleteRequest = { vehicleToDelete = item }
                    )
                }
            }
        }
    }
    vehicleToDelete?.let { item ->
        DeleteVehicleDialog(
            item = item,
            onConfirm = {
                viewModel.deleteVehicle(item.vehicle)
                vehicleToDelete = null
            },
            onDismiss = { vehicleToDelete = null }
        )

    }
}

@Composable
private fun VehiclesHeader(items: List<VehicleListItem>?, onAddVehicle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(top = Spacing.xl, bottom = Spacing.lg)
    ) {
        Text(
            "I tuoi veicoli",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(Spacing.lg))

        Button(
            onClick = onAddVehicle,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().height(Spacing.buttonHeight)
        ) {
            Text("Aggiungi veicolo", style = MaterialTheme.typography.labelLarge)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteVehicle(
    item: VehicleListItem,
    onEditVehicle: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val currentOnDeleteRequest by rememberUpdatedState(onDeleteRequest)

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart)
                currentOnDeleteRequest()
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val dragging = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        if (dragging)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            Color.Transparent)
                    .padding(horizontal = Spacing.xl),
                contentAlignment = Alignment.CenterEnd
            ) {
              if (dragging) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                          "Elimina",
                          style = MaterialTheme.typography.labelLarge,
                          color = MaterialTheme.colorScheme.onErrorContainer
                      )
                      Spacer(Modifier.width(Spacing.sm))
                      Icon(
                          Icons.Default.Delete,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.onErrorContainer
                      )
                  }
              }
            }
        }
    ) {
        VehicleCard(item = item, onClick = onEditVehicle)
    }
}

@Composable
private fun VehicleCard(item: VehicleListItem, onClick: () -> Unit) {
    val vehicle = item.vehicle

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VehicleTypeIcon(type = vehicle.type)

            Spacer(Modifier.width(Spacing.lg))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    vehicle.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                vehicle.plateNumber?.takeIf { it.isNotBlank() }?.let { plate->
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        plate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(Spacing.md))

            ParkingStatusChip(isParked = item.isParked)
        }
    }
}

@Composable
private fun VehicleTypeIcon(type: VehicleType) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            type.icon,
            contentDescription = type.label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

// Stato con testo + pallino: il colore non è l'unico indicatore
@Composable
private fun ParkingStatusChip(isParked: Boolean) {
    val container =
        if (isParked) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    val content =
        if (isParked) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(container)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (isParked) "In sosta" else "Libero",
            style = MaterialTheme.typography.labelMedium,
            color = content
        )
    }
}


@Composable
private fun EmptyVehicles(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(Spacing.xl))
        Text(
            "Nessun veicolo",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}


@Composable
private fun DeleteVehicleDialog(
    item: VehicleListItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        },
        title = { Text("Eliminare ${item.vehicle.name}?") },
        text = {
            Text(
                if (item.isParked)
                    "Il veicolo ha un parcheggio in corso. Verrà eliminato anche il parcheggio attivo"
                else
                    "Verranno eliminate anche le sue soste nello storico. L'operazione non può essere annullata."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Elimina") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface
    )
}
