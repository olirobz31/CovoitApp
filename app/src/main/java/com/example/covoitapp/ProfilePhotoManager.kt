package com.example.covoitapp

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProfilePhotoManager {
    private val storage = FirebaseStorage.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val usersRef = db.collection("users")

    // Uploader une photo de profil
    suspend fun uploadProfilePhoto(userEmail: String, imageUri: Uri): Result<String> {
        return try {
            // Créer une référence unique pour cette photo
            val photoRef = storage.reference
                .child("profile_photos/${userEmail.replace(".", "_")}.jpg")

            // Uploader l'image
            photoRef.putFile(imageUri).await()

            // Récupérer l'URL de téléchargement
            val downloadUrl = photoRef.downloadUrl.await().toString()

            // Sauvegarder l'URL dans Firestore
            usersRef.document(userEmail).set(
                mapOf("photoUrl" to downloadUrl)
            ).await()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Récupérer l'URL de la photo de profil d'un utilisateur
    suspend fun getProfilePhotoUrl(userEmail: String): String? {
        return try {
            val doc = usersRef.document(userEmail).get().await()
            doc.getString("photoUrl")
        } catch (e: Exception) {
            null
        }
    }

    // Supprimer la photo de profil
    suspend fun deleteProfilePhoto(userEmail: String): Boolean {
        return try {
            // Supprimer du Storage
            val photoRef = storage.reference
                .child("profile_photos/${userEmail.replace(".", "_")}.jpg")
            photoRef.delete().await()

            // Supprimer de Firestore
            usersRef.document(userEmail).delete().await()

            true
        } catch (e: Exception) {
            false
        }
    }
}