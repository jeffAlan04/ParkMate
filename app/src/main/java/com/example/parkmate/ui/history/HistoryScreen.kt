package com.example.parkmate.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.data.local.entity.ParkingType
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.parkmate.ui.photo.FullScreenPhotoDialog
import com.example.parkmate.ui.photo.PhotoThumbnail
import com.example.parkmate.ui.theme.Spacing
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import com.example.parkmate.ui.common.formatPrice
import com.example.parkmate.ui.parking.label
import java.util.Date

//Categorie di filtro per lo storico.
private enum class FilterCategory(val label: String) {
    NONE("Nessuno"), VEHICLE("Veicolo"), TYPE("Tipo"), PERIOD("Periodo")
}

// Range temporali 
private val PERIODS = listOf(
    TimeRange.TODAY to "Oggi",
    TimeRange.WEEK to "Settimana",
    TimeRange.MONTH to "Mese"
)

//Schermata dello storico dei parcheggi conclusi.
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory)) {
    // Osservazione dello stato dal ViewModel
    val entries by viewModel.filteredHistory.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState()
    val filter by viewModel.filter.collectAsState()

    // Stato per gestire quale categoria di filtro è attualmente selezionata
    var activeCategory by remember { mutableStateOf(FilterCategory.NONE) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            "Storico parcheggi",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(horizontal = Spacing.screenHorizontal)
                .padding(top = Spacing.xl, bottom = Spacing.lg)
        )

        // ChipCategory per i filtri
        ChipRow {
            items(FilterCategory.entries) { category ->
                ParkFilterChip(
                    label = category.label,
                    selected = activeCategory == category,
                    onClick = {
                        activeCategory = category
                        // Resetta il filtro effettivo quando si cambia categoria
                        viewModel.setFilter(HistoryFilter.None)
                    }
                )
            }
        }

        // Sotto filtri che appaiono in base alla categoria selezionata
        AnimatedVisibility(visible = activeCategory != FilterCategory.NONE) {
            Column {
                Spacer(Modifier.height(Spacing.sm))
                ChipRow {
                    when (activeCategory) {
                        // Filtro per veicolo
                        FilterCategory.VEHICLE -> items(vehicles, key = { it.id }) { vehicle ->
                            val selected = (filter as? HistoryFilter.ByVehicle)?.vehicleId == vehicle.id
                            ParkFilterChip(vehicle.name, selected) {
                                viewModel.setFilter(if (selected) HistoryFilter.None else HistoryFilter.ByVehicle(vehicle.id))
                            }
                        }

                        // Filtro per tipo di parcheggio
                        FilterCategory.TYPE -> items(ParkingType.entries) { type ->
                            val selected = (filter as? HistoryFilter.ByType)?.type == type
                            ParkFilterChip(type.label, selected) {
                                viewModel.setFilter(
                                    if (selected) HistoryFilter.None else HistoryFilter.ByType(type)
                                )
                            }
                        }

                        // Filtro per periodo
                        FilterCategory.PERIOD -> items(PERIODS) { (range, label) ->
                            val selected = (filter as? HistoryFilter.ByPeriod)?.range == range
                            ParkFilterChip(label, selected) {
                                viewModel.setFilter(
                                    if (selected) HistoryFilter.None else HistoryFilter.ByPeriod(range)
                                )
                            }
                        }
                        FilterCategory.NONE -> Unit
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.lg))

            if (entries.isEmpty()) {
                EmptyHistory(
                    filtered = filter != HistoryFilter.None,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = Spacing.screenHorizontal,
                        end = Spacing.screenHorizontal,
                        bottom = Spacing.xl
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(entries, key = { it.session.id }) { entry ->
                        HistoryEntryCard(entry)
                    }
                }
            }
    }
}

// Riga orizzontale a scorrimento per i filtri.
@Composable
private fun ChipRow(content: LazyListScope.() -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        content = content
    )
}

// Singolo chip di filtro per lo storico.
@Composable
private fun ParkFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

// Card per singola sessione con nome del veicolo, costo, data tipo e foto
@Composable
private fun HistoryEntryCard(entry: HistoryEntry) {
    val shape = MaterialTheme.shapes.large
    var showPhoto by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
        .padding(Spacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nome del veicolo
            Text(
                entry.vehicleName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(Spacing.md))
            // Costo finale
            Text(
                if (entry.session.type == ParkingType.FREE)
                    "Gratis"
                else
                    formatPrice(entry.cost),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(Spacing.xxs))

        // Data e ora di inizio e fine
        Text(
            formatSessionInterval(entry.session.startTime, entry.session.endTime),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.sm))

        // Tipo di parcheggio
        TypeTag(entry.session.type)

        // Foto
        entry.session.photoPath?.let { path ->
            Spacer(Modifier.height(Spacing.md))

            PhotoThumbnail(
                path = path,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(MaterialTheme.shapes.medium),
                onClick = { showPhoto = true }
            )
            // Dialog per visualizzare la foto a schermo intero
            if (showPhoto) {
                FullScreenPhotoDialog(path = path, onDismiss = { showPhoto = false })
            }
        }
    }
}

private fun formatSessionInterval(start: Long, end: Long?): String {
    val dateTime = SimpleDateFormat("dd/MM/yyyy 'alle' HH:mm", Locale.ITALY)
    if (end == null)
        return dateTime.format(Date(start))

    val day = SimpleDateFormat("yyyyMMdd", Locale.ITALY)
    val time = SimpleDateFormat("HH:mm", Locale.ITALY)
    val shortDateTime = SimpleDateFormat("dd/MM HH:mm", Locale.ITALY)

    return if (day.format(Date(start)) == day.format(Date(end))) {
        "${SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(Date(start))}\n${time.format(Date(start))} – ${time.format(Date(end))}"
    } else {
        "${shortDateTime.format(Date(start))} – ${shortDateTime.format(Date(end))}"
    }
}

// Tag per il tipo di parcheggio.
@Composable
private fun TypeTag(type: ParkingType) {
    Text(
        type.label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
    )
}

// Schermata per quando non ci sono sessioni salvate o filtrate.
@Composable
private fun EmptyHistory(filtered: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (filtered) "Nessun risultato" else "Nessun parcheggio",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            if (filtered) "Prova a cambiare filtro" else "Le sessioni concluse appariranno qui",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
