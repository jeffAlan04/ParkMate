package com.example.parkmate.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.ui.parking.ActiveSessionDisplay
import com.example.parkmate.ui.parking.ParkingViewModel
import com.example.parkmate.ui.parking.ParkingViewModelFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DEFAULT_POSITION = LatLng(44.4949, 11.3426)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: ParkingViewModel = viewModel(factory = ParkingViewModelFactory)
) {


    val activeSessions by viewModel.activeSessions.collectAsState()
    val savedLocation by viewModel.savedLocations.collectAsState()

    var selectedSession by remember { mutableStateOf<ActiveSessionDisplay?>(null) }

    val initialPosition = activeSessions.firstOrNull()?.let { LatLng(it.session.latitude, it.session.longitude) } ?: DEFAULT_POSITION

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 14f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        activeSessions.forEach { display ->
            Marker(
                state = MarkerState(position = LatLng(display.session.latitude, display.session.longitude)),
                title = display.vehicleName,
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                onClick = {
                    selectedSession = display
                    true
                }
            )
        }

        savedLocation.forEach { location ->
            Marker(
                state = MarkerState(position = LatLng(location.latitude, location.longitude)),
                title = location.name,
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
            )
        }
    }

    selectedSession?.let { display ->
        val sheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            onDismissRequest = { selectedSession = null },
            sheetState = sheetState
        ) {
            SessionDetailContent(display = display)
        }
    }
}

@Composable
private fun SessionDetailContent(display: ActiveSessionDisplay) {
    val startTimeFormatted = remember(display.session.startTime) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(display.session.startTime))
    }

    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Row {
            Icon(Icons.Default.DirectionsCar, contentDescription = null)
            Spacer(modifier = Modifier.height(0.dp))
            Text(
                display.vehicleName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Tipo: ${display.session.type.name}", style = MaterialTheme.typography.bodyLarge)
        Text("Iniziato alle: $startTimeFormatted", style = MaterialTheme.typography.bodyLarge)

        display.currentCost?.let { cost ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Costo attuale: €%.2f".format(cost),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
