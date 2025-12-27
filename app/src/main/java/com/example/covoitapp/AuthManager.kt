package com.example.covoitapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthManager {
    private val auth = FirebaseAuth.getInstance()

    // Obtenir l'utilisateur actuellement connecté
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    // Inscription d'un nouvel utilisateur
    suspend fun signUp(email: String, password: String): AuthResult {
        return try {
            // Vérifier la longueur du mot de passe
            if (password.length < 6) {
                return AuthResult.Error("Le mot de passe doit contenir au moins 6 caractères")
            }

            // Créer le compte
            auth.createUserWithEmailAndPassword(email, password).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Erreur lors de l'inscription")
        }
    }

    // Connexion d'un utilisateur existant
    suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            if (email.isEmpty() || password.isEmpty()) {
                return AuthResult.Error("Veuillez remplir tous les champs")
            }

            auth.signInWithEmailAndPassword(email, password).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error("Email ou mot de passe incorrect")
        }
    }

    // Déconnexion
    fun signOut() {
        auth.signOut()
    }

    // Vérifier si l'utilisateur est connecté
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}

// Résultat de l'authentification
sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}