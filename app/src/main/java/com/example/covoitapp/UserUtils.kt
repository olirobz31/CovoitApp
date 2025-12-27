package com.example.covoitapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.abs

// Extraire le prénom de l'email (version robuste)
fun getDisplayNameFromEmail(email: String): String {
    // Si l'email est vide, retourner un nom par défaut
    if (email.isBlank()) {
        return "Anonyme"
    }
    // Extraire la partie avant le @
    val username = email.substringBefore("@")

    // Capitaliser la première lettre
    return username.replaceFirstChar { it.uppercase() }
}

// Obtenir les initiales (première lettre)
fun getInitialsFromEmail(email: String): String {
    val displayName = getDisplayNameFromEmail(email)
    return displayName.firstOrNull()?.uppercase() ?: "?"
}

// Générer une couleur unique basée sur le nom
fun getColorFromEmail(email: String): Color {
    val colors = listOf(
        Color(0xFF6200EE), // Violet
        Color(0xFF03DAC5), // Cyan
        Color(0xFFFF6B6B), // Rouge
        Color(0xFF4ECDC4), // Turquoise
        Color(0xFF45B7D1), // Bleu clair
        Color(0xFFFFA07A), // Saumon
        Color(0xFF98D8C8), // Menthe
        Color(0xFFFFB6C1), // Rose
        Color(0xFF9B59B6), // Violet foncé
        Color(0xFF3498DB), // Bleu
        Color(0xFFE67E22), // Orange
        Color(0xFF2ECC71)  // Vert
    )

    // Utiliser le hashCode de l'email pour choisir une couleur
    val index = abs(email.hashCode()) % colors.size
    return colors[index]
}

// Composant pour afficher l'avatar avec initiales
@Composable
fun UserAvatar(
    email: String,
    size: Dp = 40.dp,
    fontSize: Int = 16
) {
    val profilePhotoManager = remember { ProfilePhotoManager() }
    var photoUrl by remember { mutableStateOf<String?>(null) }

    // Charger la photo de profil
    LaunchedEffect(email) {
        photoUrl = profilePhotoManager.getProfilePhotoUrl(email)
    }

    if (photoUrl != null) {
        // Afficher la vraie photo
        AsyncImage(
            model = photoUrl,
            contentDescription = "Photo de profil",
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        // Afficher l'avatar avec initiales (ancien comportement)
        val initials = getInitialsFromEmail(email)
        val backgroundColor = getColorFromEmail(email)

        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}