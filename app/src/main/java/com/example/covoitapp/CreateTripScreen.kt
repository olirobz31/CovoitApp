package com.example.covoitapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripScreen(
    onTripCreated: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val authManager = remember { AuthManager() }
    val currentUserEmail = authManager.getCurrentUser()?.email ?: ""

    var depart by remember { mutableStateOf("") }
    var arrivee by remember { mutableStateOf("") }
    var dateTrajet by remember { mutableStateOf("") }
    var horaire by remember { mutableStateOf("") }
    var prixParPersonne by remember { mutableStateOf("") }
    var placesDisponibles by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Date picker state
    val datePickerState = rememberDatePickerState()

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = millis
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                        dateTrajet = dateFormat.format(calendar.time)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Créer un trajet") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Informations du trajet",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )

            // Départ
            OutlinedTextField(
                value = depart,
                onValueChange = { depart = it },
                label = { Text("Lieu de départ") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Arrivée
            OutlinedTextField(
                value = arrivee,
                onValueChange = { arrivee = it },
                label = { Text("Lieu d'arrivée") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date
            OutlinedTextField(
                value = dateTrajet,
                onValueChange = { },
                label = { Text("Date du trajet") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Text("📅")
                    }
                }
            )

            // Horaire
            OutlinedTextField(
                value = horaire,
                onValueChange = { horaire = it },
                label = { Text("Horaire (ex: 08:00)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Prix
            OutlinedTextField(
                value = prixParPersonne,
                onValueChange = { prixParPersonne = it },
                label = { Text("Prix par personne (€)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Places
            OutlinedTextField(
                value = placesDisponibles,
                onValueChange = { placesDisponibles = it },
                label = { Text("Nombre de places disponibles") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Message d'erreur
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bouton Créer
            Button(
                onClick = {
                    scope.launch {
                        // Validation
                        if (depart.isBlank() || arrivee.isBlank() || dateTrajet.isBlank() ||
                            horaire.isBlank() || prixParPersonne.isBlank() || placesDisponibles.isBlank()
                        ) {
                            errorMessage = "Veuillez remplir tous les champs"
                            return@launch
                        }

                        try {
                            isLoading = true
                            errorMessage = null

                            // Parser la date
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                            val date = dateFormat.parse(dateTrajet)
                            val dateMillis = date?.time ?: System.currentTimeMillis()

                            val prix = prixParPersonne.toDoubleOrNull() ?: 0.0
                            val places = placesDisponibles.toIntOrNull() ?: 0

                            // Créer le trajet SANS photo (temporaire)
                            val nouveauTrajet = Trajet(
                                conducteur = currentUserEmail,
                                depart = depart,
                                arrivee = arrivee,
                                dateTrajet = dateMillis,
                                horaire = horaire,
                                prixParPersonne = prix,
                                placesDisponibles = places,
                                placesTotales = places,
                                photoUrl = ""  // Pas de photo pour le moment
                            )

                            val success = repository.createTrajet(nouveauTrajet)

                            isLoading = false

                            if (success) {
                                onTripCreated()
                            } else {
                                errorMessage = "Erreur lors de la création du trajet"
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Erreur: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Créer le trajet")
                }
            }

            // Bouton Annuler
            OutlinedButton(
                onClick = { onNavigateBack() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("Annuler")
            }
        }
    }
}