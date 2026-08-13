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
@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Routes.VehiclesList,
        modifier = modifier
    ) {
        composable<Routes.VehiclesList> {
            VehiclesScreen(
                onAddVehicle = {
                    navController.navigate(Routes.VehicleForm())
                },
                onEditVehicle = {
                    id -> navController.navigate(Routes.VehicleForm(vehicleId = id))
                }
            )
        }

        composable<Routes.VehicleForm> { backStackEntry ->
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