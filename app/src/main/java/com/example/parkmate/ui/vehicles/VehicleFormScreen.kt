package com.example.parkmate.ui.vehicles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.data.local.entity.VehicleType
import com.example.parkmate.ui.vehicles.VehicleViewModelFactory

@Composable
fun VehicleFormScreen(
    vehicleId: Long? = null,
    viewModel: VehicleViewModel = viewModel(factory = VehicleViewModelFactory)
) {

    // Contiene il nome del veicolo inserito dall'utente (inzialmente vuoto)
    var name by remember { mutableStateOf("") }

    // Conteiente il tipo di veicolo selezionato dall'utente
    var selectedType by remember { mutableStateOf(VehicleType.CAR) }

    // Carica i dati di un veicolo esistente
    LaunchedEffect(vehicleId) {
        if (vehicleId != null) {

            //Recupera il veicolo con l'ID specificato
            val existingVehcle = viewModel.getVehicleById(vehicleId)

            // Controlla che il veicolo esista
            if (existingVehcle != null) {
                name = existingVehcle.name
                selectedType = existingVehcle.type
            }
        }
    }
}