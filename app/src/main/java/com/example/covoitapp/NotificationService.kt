package com.example.covoitapp

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Extraire les données de la notification
        val title = remoteMessage.notification?.title ?: "CovoitApp"
        val body = remoteMessage.notification?.body ?: ""

        // Afficher la notification
        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Sauvegarder le token pour l'utilisateur
        saveTokenToFirestore(token)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "trajets_channel" // Utiliser le même ID que dans MainActivity
        val notificationId = System.currentTimeMillis().toInt()

        // Intent pour ouvrir l'app quand on clique sur la notification
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Créer la notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Utiliser une icône de l'application
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Afficher la notification (avec la permission)
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            try {
                NotificationManagerCompat.from(this).notify(notificationId, notification)
            } catch (e: SecurityException) {
                // Gérer le cas où la permission est révoquée entre temps
            }
        }
    }

    private fun saveTokenToFirestore(token: String) {
        val authManager = AuthManager()
        val userEmail = authManager.getCurrentUser()?.email ?: return

        // Sauvegarder le token dans Firestore
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("users").document(userEmail)
            .update("fcmToken", token)
            .addOnFailureListener {
                // Si le document n'existe pas, le créer
                db.collection("users").document(userEmail)
                    .set(mapOf("fcmToken" to token))
            }
    }
}