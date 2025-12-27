package com.example.covoitapp

data class Trajet(
    val id: String = "",
    val conducteur: String = "",
    val depart: String = "",
    val arrivee: String = "",
    val dateTrajet: Long = 0,
    val horaire: String = "",
    val prixParPersonne: Double = 0.0,
    val placesDisponibles: Int = 0,
    val placesTotales: Int = 0,
    val photoUrl: String = "" // ⭐ NOUVEAU CHAMP
)