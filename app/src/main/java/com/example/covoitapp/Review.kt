package com.example.covoitapp

data class Review(
    val id: String = "",
    val trajetId: String = "",
    val reviewerEmail: String = "",
    val ratedUserEmail: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)