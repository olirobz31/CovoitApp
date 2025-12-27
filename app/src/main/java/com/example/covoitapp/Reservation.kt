package com.example.covoitapp

import com.google.firebase.firestore.DocumentId

// Modèle de données pour une réservation
data class Reservation(
    @DocumentId
    val id: String = "",
    val trajetId: String = "",
    val userEmail: String = "",
    val timestamp: Long = System.currentTimeMillis()
)