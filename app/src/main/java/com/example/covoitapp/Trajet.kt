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
    val photoUrl: String = "",  // ⭐ VIRGULE ICI !
    val departLat: Double = 0.0,
    val departLon: Double = 0.0,
    val arriveeLat: Double = 0.0,
    val arriveeLon: Double = 0.0
)