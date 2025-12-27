package com.example.covoitapp

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val bio: String = "" // ⭐ NOUVEAU CHAMP POUR LA BIO
)