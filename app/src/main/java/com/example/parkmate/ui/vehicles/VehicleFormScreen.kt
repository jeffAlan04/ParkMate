package com.example.parkmate.ui.vehicles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.data.local.entity.VehicleType


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleFormScreen(
    vehicleId: Long? = null,
    viewModel: VehicleViewModel = viewModel(factory = VehicleViewModelFactory),
    onSaved: () -> Unit = {}
) {

    // Contiene il nome del veicolo inserito dall'utente (inzialmente vuoto)
    var name by remember { mutableStateOf("") }

    // Conteiente il tipo di veicolo selezionato dall'utente
    var selectedType by remember { mutableStateOf(VehicleType.CAR) }

    // Carica i dati di un veicolo esistente
    LaunchedEffect(vehicleId) {
        if (vehicleId != null) {
            val existingVehcle = viewModel.getVehicleById(vehicleId)
            if (existingVehcle != null) {
                name = existingVehcle.name
                selectedType = existingVehcle.type
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {

        // Campo di testo per inserire il nome del veicolo
        TextField(
            value = name,
            onValueChange = {name = it},
            label = {Text("Nome veicolo")},
            isError = name.isBlank(),
            supportingText = {
                if (name.isBlank()) {
                    Text("Nome obbligatorio")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Creazione di una riga di pulasanti tra cui se ne puo scegliere solo uno
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            // Crea un pulsante per ogni tipo presente in VehicleType
            VehicleType.entries.forEachIndexed { index, type ->
                SegmentedButton(

                    // Il pulsante e' selezionato se corrisponde al tipo scelto
                    selected = type == selectedType,

                    // Aggiorna il tipo selezionato quando viene cliccato
                    onClick = { selectedType = type },

                    // Gestisce i bordi dei pulsanti in base alla posizione nella riga
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = VehicleType.entries.size
                    )
                ) {
                    Text(type.name)
                }
            }
        }

        Button(
            onClick = {
                if (vehicleId != null) {
                    viewModel.updateVehicle(vehicleId, name, selectedType)
                } else {
                    viewModel.addVehicle(name, selectedType)
                }
                onSaved()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Salva")
        }
    }
}