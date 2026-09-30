package com.example.parkmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.parkmate.ui.navigation.NavGraph
import com.example.parkmate.ui.navigation.Routes
import com.example.parkmate.ui.theme.ParkMateTheme
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.filled.LocalParking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParkMateTheme {
                ParkMateApp()
            }
        }
    }
}

private data class BottonNavItem(val route: Routes, val label: String, val icon: ImageVector)
@Composable
fun ParkMateApp() {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottonNavItem(Routes.VehiclesList, "Veicoli", Icons.Default.DirectionsCar),
        BottonNavItem(Routes.StartPark, "Parcheggi", Icons.Default.LocalParking),
        BottonNavItem(Routes.Map, "Mappa", Icons.Default.Map),
        BottonNavItem(Routes.History, "Storico", Icons.Default.History),
        BottonNavItem(Routes.Stats, "Statistiche", Icons.Default.BarChart)
    )

    Scaffold(
        bottomBar = {
            NavigationBar{
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = currentBackStackEntry?.destination

                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.hasRoute(item.route::class)
                    } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label)},
                        label = {Text(item.label)}
                    )
                }
            }
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding))
    }
}