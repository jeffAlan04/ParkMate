package com.example.parkmate.ui.visualization

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.heatmaps.HeatmapTileProvider

private val DEFAULT_POSITION = LatLng(44.4949, 11.3426)

private val TYPE_COLORS = mapOf(
    "FREE" to Color(0xFF4CAF50),
    "HOURLY" to Color(0xFF2196F3),
    "TICKET" to Color(0xFFFF9800)
)
@Composable
fun DataVisualizationScreen(
    viewModel: DataViewModel = viewModel(factory = DataViewModelFactory)
) {
    val heatmapPoints by viewModel.heatmapPoints.collectAsState()
    val costByType by viewModel.costByType.collectAsState()
    val totalCost by viewModel.totalCost.collectAsState()
    val countByType by viewModel.countByType.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Distribuzione dei parcheggi", style = MaterialTheme.typography.titleLarge)
        ParkingHeatMap(
            points = heatmapPoints,
            modifier = Modifier.fillMaxWidth().height(250.dp)
        )

        Text("Numero di parcheggi per tipo", style = MaterialTheme.typography.titleLarge)
        ParkingCountBarChart(
            data = countByType,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Spesa totale", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "€%.2f".format(totalCost),
            style = MaterialTheme.typography.headlineMedium
        )

        Text("Spesa per tipo di parcheggio", style = MaterialTheme.typography.titleLarge)
        SpendingPieChart(
            data = costByType,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ParkingHeatMap(points: List<LatLng>, modifier: Modifier = Modifier) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(points.firstOrNull() ?: DEFAULT_POSITION, 13f)
    }

    val tileProvider = remember(points) {
        if (points.isNotEmpty())
            HeatmapTileProvider.Builder().data(points).build()
        else
            null
    }

    GoogleMap(modifier = modifier, cameraPositionState = cameraPositionState) {
        tileProvider?.let { provider ->
            TileOverlay(tileProvider = provider)
        }
    }
}

@Composable
private fun ParkingCountBarChart(data: List<TypeCount>, modifier: Modifier = Modifier) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxCount = data.maxOfOrNull { it.count } ?: 0.0

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        if (data.isNotEmpty()) {
                            val barWidth = size.width / data.size
                            selectedIndex = (offset.x / barWidth).toInt().coerceIn(0, data.size - 1)
                        }
                    }
                }
        ) {
            if (data.isEmpty() || maxCount == 0.0)
                return@Canvas

            val barWidth = size.width / data.size
            data.forEachIndexed { index, item ->
                val ratio = item.count.toFloat() / maxCount.toFloat()
                val barHeight = size.height * ratio
                val isSelected = index == selectedIndex

                drawRect(
                    color = (TYPE_COLORS[item.label] ?: Color.Gray).let {if (isSelected) it else it.copy(alpha = 0.6f) },
                    topLeft = Offset(x = index * barWidth + barWidth * -.15f, y = size.height - barHeight),
                    size = Size(width = barWidth * 0.7f, height = barHeight)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            data.forEach { item -> Text(item.label, style = MaterialTheme.typography.labelSmall) }
        }

        selectedIndex?.let { index ->
            data.getOrNull(index)?.let { item ->
                Text(
                    "${item.label}: ${item.count} parcheggi",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SpendingPieChart(data: List<CostAggregation>, modifier: Modifier = Modifier) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val total = data.sumOf { it.totalCost }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (data.isEmpty() || total == 0.0) {
            Text("Nessuna spesa registrata", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        Canvas(
            modifier = Modifier.size(220.dp).pointerInput(data) {
                detectTapGestures { offset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = offset.x - center.x
                    val dy = offset.y - center.y
                    var angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble()))

                    if (angle < 0)
                        angle += 360.0

                    var acc = 0.0
                    for ((index, item) in data.withIndex()) {
                        val sweep = (item.totalCost / total) * 360.0
                        if (angle >= acc && angle < acc + sweep) {
                            selectedIndex = index
                            break
                        }
                        acc += sweep
                    }
                }
            }
        ) {
            val strokeWidth = size.minDimension * 0.28f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            var startAngle = -90f

            data.forEachIndexed { index, item ->
                val sweep = ((item.totalCost / total) * 360f).toFloat()
                val isSelected = index == selectedIndex
                drawArc(
                    color = (TYPE_COLORS[item.label] ?: Color.Gray).let { if (isSelected) it else it.copy(alpha = 0.7f) },
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = if (isSelected) strokeWidth * 1.5f else strokeWidth,
                        cap = StrokeCap.Butt
                    )
                )
                startAngle += sweep
            }
        }

        Spacer(Modifier.height(12.dp))

        data.forEachIndexed { index, item ->
            val percent = (item.totalCost / total) * 100
            Text(
                "${item.label}: €%.2f (%.0f%%)".format(item.totalCost, percent),
                style = if (index == selectedIndex) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                color = TYPE_COLORS[item.label] ?: Color.Gray
            )
        }
    }

}