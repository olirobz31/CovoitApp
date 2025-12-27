package com.example.covoitapp

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userEmail: String,
    onBack: () -> Unit = {}
) {
    val photoManager = remember { ProfilePhotoManager() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val firestore = Firebase.firestore

    var photoUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var bio by remember { mutableStateOf("") }
    var isEditingBio by remember { mutableStateOf(false) }
    var tempBio by remember { mutableStateOf("") }

    // Chargement de la photo de profil et de la bio
    LaunchedEffect(userEmail) {
        photoUrl = photoManager.getProfilePhotoUrl(userEmail)

        // Charger la bio depuis Firestore
        firestore.collection("users").document(userEmail).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    bio = document.getString("bio") ?: ""
                }
            }
    }

    // Sélecteur d'image
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isLoading = true
                photoManager.uploadProfilePhoto(userEmail, it)
                photoUrl = photoManager.getProfilePhotoUrl(userEmail)
                isLoading = false

                snackbarHostState.showSnackbar("✅ Photo mise à jour")
            }
        }
    }

    // Dialogue de confirmation de suppression
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer la photo ?") },
            text = { Text("Êtes-vous sûr de vouloir supprimer votre photo de profil ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            photoManager.deleteProfilePhoto(userEmail)
                            photoUrl = null
                            isLoading = false
                            showDeleteDialog = false

                            snackbarHostState.showSnackbar("✅ Photo supprimée")
                        }
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialogue d'édition de la bio
    if (isEditingBio) {
        AlertDialog(
            onDismissRequest = {
                isEditingBio = false
                tempBio = ""
            },
            title = { Text("Modifier ma bio") },
            text = {
                OutlinedTextField(
                    value = tempBio,
                    onValueChange = {
                        if (it.length <= 150) {
                            tempBio = it
                        }
                    },
                    placeholder = { Text("Parlez de vous...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    supportingText = {
                        Text("${tempBio.length}/150 caractères")
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            // Sauvegarder la bio dans Firestore
                            firestore.collection("users").document(userEmail)
                                .set(mapOf("bio" to tempBio), com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener {
                                    bio = tempBio
                                    isEditingBio = false
                                    tempBio = ""
                                    scope.launch {
                                        snackbarHostState.showSnackbar("✅ Bio mise à jour")
                                    }
                                }
                        }
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    isEditingBio = false
                    tempBio = ""
                }) {
                    Text("Annuler")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mon profil") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Photo de profil
            Box(
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Photo de profil",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    UserAvatar(
                        email = userEmail,
                        size = 120.dp,
                        fontSize = 48
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(120.dp)
                    )
                }
            }

            Text(
                text = getDisplayNameFromEmail(userEmail),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = userEmail,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Note moyenne
            var averageRating by remember { mutableStateOf(0.0) }
            var ratingCount by remember { mutableStateOf(0) }
            val repository = remember { FirebaseRepository() }

            LaunchedEffect(userEmail) {
                averageRating = repository.getAverageRating(userEmail)
                ratingCount = repository.getRatingCount(userEmail)
            }

            if (ratingCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⭐",
                        fontSize = 24.sp
                    )
                    Text(
                        text = String.format("%.1f", averageRating),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "($ratingCount avis)",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Aucun avis pour le moment",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section Bio
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "À propos de moi",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                tempBio = bio
                                isEditingBio = true
                            }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Modifier la bio",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = bio.ifEmpty { "Parlez de vous, de vos passions, de vos habitudes de voyage..." },
                        fontSize = 14.sp,
                        color = if (bio.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.onSurface,
                        style = if (bio.isEmpty())
                            MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        else
                            MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Boutons
            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (photoUrl != null) "Changer la photo" else "Ajouter une photo")
            }

            if (photoUrl != null) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Supprimer la photo")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}