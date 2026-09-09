package com.example.parkmate.ui.parking

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

enum class LocationPermissionStatus { NOT_REQUESTED, GRANTED, DENIED}

@Composable
fun rememberLocationPermissionStatus(): Pair<LocationPermissionStatus, () -> Unit> {
    val context = LocalContext.current

    var status by remember {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        mutableStateOf(if (alreadyGranted) LocationPermissionStatus.GRANTED else LocationPermissionStatus.NOT_REQUESTED)
    }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
        granted -> status = if (granted) LocationPermissionStatus.GRANTED else LocationPermissionStatus.DENIED
    }

    val requestPermission: () -> Unit = {
        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    return status to requestPermission
}