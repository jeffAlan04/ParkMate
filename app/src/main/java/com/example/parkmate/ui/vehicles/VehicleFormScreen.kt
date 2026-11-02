package com.example.parkmate.ui.vehicles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.data.local.entity.VehicleType
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.parkmate.data.local.entity.VehiclePlate
import com.example.parkmate.data.repository.VehicleSaveResult
import com.example.parkmate.ui.theme.Spacing
import kotlinx.coroutines.launch

// Schermata per l'aggiunta o la modifica di un veicolo
@Composable
fun VehicleFormScreen(
    vehicleId: Long? = null, // Se presente indica che si sta modificando un veicolo esistente
    viewModel: VehicleViewModel = viewModel(factory = VehicleViewModelFactory),
    onSaved: () -> Unit = {}, // Azione da eseguire dopo il salvataggio
    onBack: () -> Unit = onSaved // Azione per tornare indietro
) {

    val isEdit = vehicleId != null

    // Stato locale per i campi del form
    var name by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf(VehicleType.CAR) }
    var plateNumber by rememberSaveable { mutableStateOf("") }
    
    // Traccia se l'utente ha interagito con i campi per mostrare messaggi di errore mirati
    var nameTouched by rememberSaveable { mutableStateOf(false) }
    var plateTouched by rememberSaveable { mutableStateOf(false) }
    var loaded by rememberSaveable { mutableStateOf(!isEdit) }

    var saveError by remember { mutableStateOf<VehicleSaveResult?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Logica di derivazione dello stato per gestire la visibilità e gli errori
    val hasPlate = selectedType != VehicleType.BICYCLE
    val isPlateFormatValid = !hasPlate || VehiclePlate.isValid(selectedType, plateNumber)

    val nameErrorMessage = when {
        nameTouched && name.isBlank() -> "Nome obbligatorio"
        saveError == VehicleSaveResult.DuplicateBicycleName -> "Esiste già una bicicletta con questo nome"
        else -> null
    }
    val plateErrorMessage = when {
        !hasPlate -> null
        saveError == VehicleSaveResult.DuplicatePlate -> "Targa già registrata"
        plateTouched && plateNumber.isBlank() -> "Targa obbligatoria"
        plateTouched && !isPlateFormatValid -> "Formato targa non valido"
        else -> null
    }

    // Determina se il pulsante di salvataggio deve essere abilitato
    val canSave = name.isNotBlank() && isPlateFormatValid && !isSaving

    // Carica i dati di un veicolo esistente
    LaunchedEffect(vehicleId) {
        if (vehicleId != null && !loaded) {
            viewModel.getVehicleById(vehicleId)?.let { existing ->
                name = existing.name
                selectedType = existing.type
                plateNumber = existing.plateNumber ?: ""
            }
            loaded = true
        }
    }

    // Valida e salva i dati del veicolo tramite il ViewModel
    fun save() {
        val trimmedName = name.trim()
        val plateToSave = plateNumber.takeIf { hasPlate }
        isSaving = true
        coroutineScope.launch {
            val saveResult = viewModel.saveVehicle(vehicleId, trimmedName, selectedType, plateToSave)
            isSaving = false
            when(saveResult) {
                VehicleSaveResult.Success -> onSaved()
                VehicleSaveResult.InvalidPlate -> plateTouched = true
                else -> saveError = saveResult
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding() // Sposta l'UI quando appare la tastiera
    ) {
        FormHeader(
            title = if (isEdit) "Modifica veicolo" else "Nuovo veicolo",
            subtitle = "",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl)
        ) {
            // Selezione tipologia di veicolo
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionLabel("Tipo di veicolo")
                Row(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    VehicleType.entries.forEach { type ->
                        VehicleTypeCard(
                            type = type,
                            selected = type == selectedType,
                            onClick = {
                                selectedType = type
                                saveError = null },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Inserimento nome veicolo
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionLabel("Nome")
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameTouched = true
                        saveError = null
                    },
                    placeholder = { Text("es. Fiat Panda") },
                    singleLine = true,
                    isError = nameErrorMessage != null,
                    supportingText = nameErrorMessage?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction =
                            if (hasPlate)
                                ImeAction.Next
                            else
                                ImeAction.Done
                    ),
                    shape = MaterialTheme.shapes.medium,
                    colors = formFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Inserimento targa per auto e moto
            AnimatedVisibility(visible = hasPlate) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SectionLabel("Targa")
                    OutlinedTextField(
                        value = plateNumber,
                        onValueChange = { input ->
                            // Forza il maiuscolo e limita i caratteri
                            plateNumber = input.uppercase()
                                .filter { character -> character.isLetterOrDigit() }
                                .take(VehiclePlate.MAX_LENGTH)
                            plateTouched = true
                            saveError = null
                        },
                        placeholder = { Text("es. ${VehiclePlate.example(selectedType)}") },
                        singleLine = true,
                        isError = plateErrorMessage != null,
                        supportingText = plateErrorMessage?.let { message -> { Text(message) } },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        colors = formFieldColors(),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))
        }

        // Pulsante di azione per il salvataggio
        Button(
            onClick = { save() },
            enabled = canSave,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg)
                .height(Spacing.buttonHeight)
        ) {
            Text(
                if (isEdit) "Salva modifiche" else "Aggiungi veicolo",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun FormHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.sm, end = Spacing.screenHorizontal, top = Spacing.sm, bottom = Spacing.xl)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Indietro",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(modifier = Modifier.padding(start = Spacing.md)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// Card per la selezione del tipo di veicolo
@Composable
private fun VehicleTypeCard(
    type: VehicleType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.large
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val iconColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(shape)
            .background(container)
            .border(if (selected) 2.dp else 1.dp, borderColor, shape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = Spacing.lg)
    ) {
        Icon(
            type.icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            type.label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor
        )
    }
}

@Composable
private fun formFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    errorContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
)