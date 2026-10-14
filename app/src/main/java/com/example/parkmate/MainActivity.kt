package com.example.parkmate

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Activity principale
class MainActivity : ComponentActivity() {
    // ID della posizione da cui avviare un parcheggio
    private var startLocationId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Abilita la visualizzazione a tutto schermo
        enableEdgeToEdge()
        
        // Se l'attività viene creata da zero, controlla se c'è un luogo preselezionato nell'Intent
        if (savedInstanceState == null) readStartLocation(intent)
        
        setContent {
            ParkMateTheme {
                // Richiede il permesso per le notifiche
                RequestNotificationPermission()
                
                // Punto di ingresso dell'interfaccia Compose
                ParkMateApp(startLocationId = startLocationId)
            }
        }
    }

    // Gestisce il caso in cui l'app sia già aperta e venga cliccata una notifica
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readStartLocation(intent)
    }

    // Estrae l'ID della posizione dagli
    private fun readStartLocation(intent: Intent) {
        startLocationId = intent.getLongExtra(EXTRA_START_LOCATION_ID, -1).takeIf { it != -1L }
    }

    companion object { 
        // Chiave utilizzata per passare l'ID del luogo tramite Intent
        const val EXTRA_START_LOCATION_ID = "startLocationId" 
    }
}

// Modello dati per gli elementi della barra di navigazione inferiore
private data class BottonNavItem(val route: Routes, val label: String, val icon: ImageVector)

// Composable che definisce la struttura dell'app con navigazione e BottomBar
@Composable
fun ParkMateApp(startLocationId: Long? = null) {
    RequestNotificationPermission()

    val navController = rememberNavController()

    // Effect per navigare alla schermata Parcheggi se è stato passato un luogo specifico
    LaunchedEffect(startLocationId) {
        if (startLocationId != null)
            navController.navigate(Routes.StartPark)
    }

    // Elenco delle destinazioni principali nella barra di navigazione
    val bottomNavItems = listOf(
        BottonNavItem(Routes.VehiclesList, "Veicoli", Icons.Default.DirectionsCar),
        BottonNavItem(Routes.StartPark, "Parcheggi", Icons.Default.LocalParking),
        BottonNavItem(Routes.Map, "Mappa", Icons.Default.Map),
        BottonNavItem(Routes.History, "Storico", Icons.Default.History),
        BottonNavItem(Routes.Stats, "Statistiche", Icons.Default.BarChart)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                // Osserva la destinazione corrente per evidenziare l'icona corretta
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = currentBackStackEntry?.destination

                bottomNavItems.forEach { item ->
                    // Controlla se la rotta dell'item è presente nella gerarchia di navigazione attuale
                    val selected = currentDestination?.hierarchy?.any {
                        it.hasRoute(item.route::class)
                    } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                // Evita di accumulare istanze della stessa schermata nel backstack
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label)},
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Componente che gestisce le transizioni tra le diverse schermate dell'app
        NavGraph(
            navController = navController,
            startLocationId = startLocationId,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

// Composable per la richiesta del permesso POST_NOTIFICATIONS
@Composable
fun RequestNotificationPermission() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* L'app funzionerà senza avvisi */ }

    LaunchedEffect(Unit) {
        // Permesso per le notifiche
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}