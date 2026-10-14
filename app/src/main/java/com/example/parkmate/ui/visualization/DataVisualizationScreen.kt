package com.example.parkmate.ui.visualization

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.data.local.entity.ParkingType
import com.example.parkmate.ui.common.formatPrice
import com.example.parkmate.ui.parking.label
import com.example.parkmate.ui.theme.Spacing
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.heatmaps.Gradient
import com.google.maps.android.heatmaps.HeatmapTileProvider
import kotlin.math.roundToInt

private val DEFAULT_POSITION = LatLng(44.4949, 11.3426)


@Composable
fun DataVisualizationScreen(
    viewModel: DataViewModel = viewModel(factory = DataViewModelFactory)
) {
    val heatmapPoints by viewModel.heatmapPoints.collectAsState()
    val costByType by viewModel.costByType.collectAsState()
    val totalCost by viewModel.totalCost.collectAsState()
    val countByType by viewModel.countByType.collectAsState()

    val totalCount = countByType.sumOf { it.count }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "Statistiche",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(top = Spacing.xl, bottom = Spacing.lg)
        )
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            TotalSpendingHero(totalCost = totalCost, totalCount = totalCount)

            StatSection(title = "Dove parcheggi") {
                if (heatmapPoints.isEmpty()) {
                    EmptyText("Nessun parcheggio registrato")
                } else {
                    ParkingHeatMap(
                        points = heatmapPoints,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                }
            }

            StatSection(title = "Parcheggi per tipo") {
                if (countByType.isEmpty() || totalCount == 0) {
                    EmptyText("Nessun parcheggio registrato")
                } else {
                    ParkingCountBarChart(data = countByType)
                }
            }

            StatSection(title = "Spesa per tipo") {
                if (costByType.isEmpty() || costByType.sumOf { it.totalCost } == 0.0) {
                    EmptyText("Nessuna spesa registrata")
                } else {
                    SpendingBreakdown(data = costByType)
                }
            }
        }
    }
}


@Composable
private fun TotalSpendingHero(totalCost: Double, totalCount: Int) {
    val onHero = MaterialTheme.colorScheme.onPrimaryContainer
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(vertical = Spacing.xl, horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Spesa totale", style = MaterialTheme.typography.labelLarge, color = onHero)
        Spacer(Modifier.height(Spacing.xs))
        Text(formatPrice(totalCost), style = MaterialTheme.typography.displaySmall, color = onHero)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            if (totalCount == 1)
                "1 parcheggio"
            else
                "$totalCount parcheggi",
            style = MaterialTheme.typography.bodyMedium,
            color = onHero
        )
    }
}
@Composable
private fun StatSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        content()
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ParkingHeatMap(points: List<LatLng>, modifier: Modifier = Modifier) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(points.firstOrNull() ?: DEFAULT_POSITION, 13f)
    }

    val tileProvider = remember(points) {
        HeatmapTileProvider.Builder()
            .data(points)
            .radius(40)
            .gradient(
                Gradient(
                    intArrayOf(
                        Color(0xFF39A9FF).toArgb(),
                        Color(0xFF1457D9).toArgb(),
                        Color(0xFF0B1C48).toArgb()
                    ),
                    floatArrayOf(0.2f, 0.6f, 1f)
                )
            )
            .build()
    }

    GoogleMap(modifier = modifier, cameraPositionState = cameraPositionState) {
        tileProvider?.let { provider ->
            TileOverlay(tileProvider = provider)
        }
    }
}

@Composable
private fun ParkingCountBarChart(data: List<TypeCount>, modifier: Modifier = Modifier) {
    val maxCount = data.maxOf { it.count }
    val maxBarHeight = 140.dp

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { item ->
                val ratio = item.count.toFloat() / maxCount.toFloat()
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        item.count.toInt().toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height((maxBarHeight * ratio).coerceAtLeast(2.dp))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(typeColor(item.label))

                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(Spacing.sm))

        Row(modifier = Modifier.fillMaxWidth()) {
            data.forEach { item ->
                Text(
                    typeLabel(item.label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SpendingBreakdown(data: List<CostAggregation>) {
    val total = data.sumOf { it.totalCost }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        data.sortedByDescending { it.totalCost }.forEach { item ->
            val share = (item.totalCost / total).toFloat()
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        typeLabel(item.label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        formatPrice(item.totalCost),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "  ${(share * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(share)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(typeColor(item.label))
                    )
                }
            }
        }
    }
}
@Composable
private fun typeColor(label: String): Color {
    val dark = isSystemInDarkTheme()
    return when (label) {
        "FREE" -> if (dark) Color(0xFF1FAE55) else Color(0xFF16A34A)
        "HOURLY" -> if (dark) Color(0xFF5B8DEF) else Color(0xFF1457D9)
        "TICKET" -> if (dark) Color(0xFFC97808) else Color(0xFFD97706)
        else -> MaterialTheme.colorScheme.outline
    }
}

private fun typeLabel(label: String): String = runCatching { ParkingType.valueOf(label).label }.getOrDefault(label)