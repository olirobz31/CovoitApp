package com.example.covoitapp

import android.util.Log

object NetworkErrorHandler {

    fun <T> handleNetworkCall(
        onError: (String) -> Unit,
        call: suspend () -> T
    ): suspend () -> T? {
        return suspend {
            try {
                call()
            } catch (e: Exception) {
                Log.e("NetworkError", "Error: ${e.message}", e)
                val message = when {
                    e.message?.contains("Unable to resolve host") == true ->
                        "Pas de connexion internet"
                    e.message?.contains("timeout") == true ->
                        "Délai d'attente dépassé"
                    else ->
                        "Erreur réseau: ${e.message}"
                }
                onError(message)
                null
            }
        }
    }
}