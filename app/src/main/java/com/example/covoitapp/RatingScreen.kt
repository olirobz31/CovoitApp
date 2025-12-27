package com.example.covoitapp

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingScreen(
    trajetId: String,
    ratedUserEmail: String,
    ratedUserName: String,
    onBack: () -> Unit = {}
) {
    val repository = remember { FirebaseRepository() }
    val authManager = remember { AuthManager() }
    val currentUserEmail = authManager.getCurrentUser()?.email ?: ""
    val scope = rememberCoroutineScope()

    var selectedRating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var hasAlreadyRated by remember { mutableStateOf(false) }

    // Vérifier si l'utilisateur a déjà noté
    LaunchedEffect(Unit) {
        hasAlreadyRated = repository.hasUserRated(trajetId, currentUserEmail, ratedUserEmail)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Noter le trajet") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        if (showSuccess) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "✅",
                        fontSize = 64.sp
                    )
                    Text(
                        text = "Merci pour votre avis !",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = onBack) {
                        Text("Retour")
                    }
                }
            }
        } else if (hasAlreadyRated) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "ℹ️",
                        fontSize = 64.sp
                    )
                    Text(
                        text = "Vous avez déjà noté ce trajet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Button(onClick = onBack) {
                        Text("Retour")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Comment s'est passé le trajet avec $ratedUserName ?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Étoiles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 1..5) {
                        IconButton(
                            onClick = { selectedRating = i }
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "$i étoiles",
                                modifier = Modifier.size(48.dp),
                                tint = if (i <= selectedRating)
                                    Color(0xFFFFD700)
                                else
                                    Color.LightGray
                            )
                        }
                    }
                }

                Text(
                    text = when (selectedRating) {
                        0 -> "Sélectionnez une note"
                        1 -> "Très mauvais"
                        2 -> "Mauvais"
                        3 -> "Moyen"
                        4 -> "Bon"
                        5 -> "Excellent"
                        else -> ""
                    },
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Commentaire optionnel
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Commentaire (optionnel)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    maxLines = 5
                )

                Spacer(modifier = Modifier.weight(1f))

                // Bouton valider
                Button(
                    onClick = {
                        scope.launch {
                            if (selectedRating > 0) {
                                isLoading = true
                                val rating = Rating(
                                    ratedUserEmail = ratedUserEmail,
                                    raterUserEmail = currentUserEmail,
                                    trajetId = trajetId,
                                    rating = selectedRating,
                                    comment = comment,
                                    timestamp = System.currentTimeMillis()
                                )
                                val success = repository.addRating(rating)
                                isLoading = false
                                if (success) {
                                    showSuccess = true
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = selectedRating > 0 && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Valider ma note", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}