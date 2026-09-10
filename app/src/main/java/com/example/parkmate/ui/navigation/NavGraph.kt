package com.example.parkmate.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.parkmate.ui.vehicles.VehicleFormScreen
import com.example.parkmate.ui.vehicles.VehiclesScreen

// Navigazione fra le pagine
@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {

    // Destinazioni raggiungibili
    NavHost(
        navController = navController,
        startDestination = Routes.VehiclesList,
        modifier = modifier
    ) {
        // Schermata con lista dei veicoli
        composable<Routes.VehiclesList> {
            VehiclesScreen(
                onAddVehicle = {
                    // Form per l'aggiunta di un veicolo
                    navController.navigate(Routes.VehicleForm())
                },
                onEditVehicle = {
                    id -> navController.navigate(Routes.VehicleForm(vehicleId = id))
                }
            )
        }

        // Schermata per aggiungere e modificare un veicolo
        composable<Routes.VehicleForm> { backStackEntry ->
            // Recupera i parametri
            val args = backStackEntry.toRoute<Routes.VehicleForm>()
            VehicleFormScreen(
                vehicleId = args.vehicleId,
                onSaved = { navController.popBackStack() }
            )
        }

        composable<Routes.Map> {
            //TODO
            Text("Mappa")
        }

        composable<Routes.History> {
            //TODO
            Text("Storico")
        }

        composable<Routes.Stats> {
            //TODO
            Text("Statistiche")
        }
    }
}