package com.example.covoitapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverProfileScreen(
    driverEmail: String,
    onBack: () -> Unit,
    onChatClick: () -> Unit
) {
    val photoManager = remember { ProfilePhotoManager() }
    val firestore = Firebase.firestore
    val repository = remember { FirebaseRepository() }

    var photoUrl by remember { mutableStateOf<String?>(null) }
    var bio by remember { mutableStateOf("") }
    var averageRating by remember { mutableStateOf(0.0) }
    var ratingCount by remember { mutableStateOf(0) }
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }

    // Charger les données du conducteur
    LaunchedEffect(driverEmail) {
        photoUrl = photoManager.getProfilePhotoUrl(driverEmail)
        averageRating = repository.getAverageRating(driverEmail)
        ratingCount = repository.getRatingCount(driverEmail)

        // Charger les avis depuis Firestore
        firestore.collection("reviews")
            .whereEqualTo("ratedUserEmail", driverEmail)
            .get()
            .addOnSuccessListener { documents ->
                reviews = documents.mapNotNull { doc ->
                    try {
                        Review(
                            id = doc.id,
                            trajetId = doc.getString("trajetId") ?: "",
                            reviewerEmail = doc.getString("reviewerEmail") ?: "",
                            ratedUserEmail = doc.getString("ratedUserEmail") ?: "",
                            rating = doc.getLong("rating")?.toInt() ?: 0,
                            comment = doc.getString("comment") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }

        // Charger la bio
        firestore.collection("users").document(driverEmail).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    bio = document.getString("bio") ?: ""
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil du conducteur") },
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
                    email = driverEmail,
                    size = 120.dp,
                    fontSize = 48
                )
            }

            Text(
                text = getDisplayNameFromEmail(driverEmail),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = driverEmail,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Note moyenne
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

            // Bio
            if (bio.isNotEmpty()) {
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
                        Text(
                            text = "À propos",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = bio,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Bouton Contacter
            Button(
                onClick = onChatClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Contacter le conducteur")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Avis
            if (reviews.isNotEmpty()) {
                Text(
                    text = "Avis (${reviews.size})",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                reviews.forEach { review ->
                    ReviewCard(review)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ReviewCard(review: Review) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getDisplayNameFromEmail(review.reviewerEmail),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "⭐".repeat(review.rating),
                    fontSize = 16.sp
                )
            }

            if (review.comment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = review.comment,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}