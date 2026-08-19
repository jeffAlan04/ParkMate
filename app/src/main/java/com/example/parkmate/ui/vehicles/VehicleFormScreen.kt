package com.example.parkmate.ui.vehicles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.data.local.entity.VehicleType
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.material3.Card
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleFormScreen(
    vehicleId: Long? = null,
    viewModel: VehicleViewModel = viewModel(factory = VehicleViewModelFactory),
    onSaved: () -> Unit = {}
) {

    // Contiene il nome del veicolo inserito dall'utente (inzialmente vuoto)
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(VehicleType.CAR) }
    var plateNumber by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Carica i dati di un veicolo esistente
    LaunchedEffect(vehicleId) {
        if (vehicleId != null) {
            viewModel.getVehicleById(vehicleId)?.let { existing ->
                name = existing.name
                selectedType = existing.type
                plateNumber = existing.plateNumber ?: ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (vehicleId != null)
                            "Modifica veicolo"
                        else
                            "Nuovo veicolo",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {

            Text("Tipo di veicolo", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VehicleType.entries.forEach { type ->
                    VehicleTypeCard(
                        type = type,
                        selected = type == selectedType,
                        onClick = { selectedType = type },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = {Text("Nome veicolo")},
                placeholder = {
                    Text("es. Fiat Panda")},
                isError = name.isBlank(),
                supportingText = { if (name.isBlank())
                    Text("Nome obbligatorio")
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedVisibility(
                visible = selectedType != VehicleType.BICYCLE,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                OutlinedTextField(
                    value = plateNumber,
                    onValueChange = { plateNumber = it.uppercase() },
                    label = {Text("Targa")},
                    placeholder = {Text("es. AB123CD")},
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val plate = plateNumber.ifBlank { null }

                    if (vehicleId != null) {
                        viewModel.updateVehicle(vehicleId, name, selectedType, plate)
                    } else {
                        viewModel.addVehicle(name, selectedType, plate)

                        name = ""
                        selectedType = VehicleType.CAR
                        plateNumber = ""

                        coroutineScope.launch { snackbarHostState.showSnackbar("Veicolo aggiunto") }
                    }
                    onSaved()
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Salva", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun VehicleTypeCard(
    type: VehicleType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (type) {
        VehicleType.CAR -> Icons.Default.DirectionsCar
        VehicleType.MOTORCYCLE -> Icons.Default.TwoWheeler
        VehicleType.BICYCLE -> Icons.Default.PedalBike
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (selected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth()
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint =
                    if (selected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
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