package com.example.parkmate.ui.location

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.example.parkmate.data.local.entity.SavedLocation
import androidx.compose.foundation.lazy.items

// Schermata che visualizza l'elenco dei luoghi salvati dall'utente.
@Composable
fun LocationListScreen(
    viewModel: LocationViewModel = viewModel(factory = LocationViewModelFactory),
    onAddLocation: () -> Unit = {},      // Callback per navigare alla creazione di un nuovo luogo
    onEditLocation: (Long) -> Unit = {}  // Callback per navigare alla modifica di un luogo esistente
) {
    // Osserva la lista dei luoghi dal database tramite lo StateFlow del ViewModel
    val locations by viewModel.locations.collectAsState()
    
    // Stato locale per gestire quale luogo l'utente intende eliminare
    var locationToDelete by remember { mutableStateOf<SavedLocation?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddLocation) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi luogo")
            }
        }
    ) { innerPadding ->
        if (locations.isEmpty()) {
            // Se la lista è vuota mostra un messaggio
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Nessun luogo salvato ancora", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            // Elenco per visualizzare i luoghi salvati
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(locations, key = { it.id }) { location ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onEditLocation(location.id) }
                        ) {
                            // Nome
                            Text(location.name, style = MaterialTheme.typography.titleMedium)
                            // Coordinate
                            Text(
                                String.format("%.5f, %.5f", location.latitude, location.longitude),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { locationToDelete = location }) {
                            Icon(Icons.Default.Delete, contentDescription = "Elimina ${location.name}")
                        }
                    }
                }
            }
        }
    }

    // Dialog di conferma per l'eliminazione di un luogo.
    locationToDelete?.let { location ->
        AlertDialog(
            onDismissRequest = { locationToDelete = null },
            title = { Text("Eliminare luogo?") },
            text = { Text("Sei sicuro di voler eliminare \"${location.name}\"?") },
            confirmButton = {
                // Elimina il luogo
                TextButton(onClick =  {
                    viewModel.deleteLocation(location)
                    locationToDelete = null
                }) { Text("Elimina") }
            },
            dismissButton = {
                // Chiude semplicemente il dialogo
                TextButton(onClick = { locationToDelete = null }) { Text("Annulla") }
            }
        )
    }
}
