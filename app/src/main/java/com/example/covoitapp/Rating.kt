package com.example.covoitapp

import com.google.firebase.firestore.DocumentId

data class Rating(
    @DocumentId
    val id: String = "",
    val ratedUserEmail: String = "",      // Email de la personne notée
    val raterUserEmail: String = "",      // Email de celui qui note
    val trajetId: String = "",            // ID du trajet concerné
    val rating: Int = 0,                  // Note de 1 à 5
    val comment: String = "",             // Commentaire optionnel
    val timestamp: Long = 0L              // Date de la note
)