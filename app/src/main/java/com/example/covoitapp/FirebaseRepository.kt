package com.example.covoitapp

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val trajetsRef = db.collection("trajets")
    private val reservationsRef = db.collection("reservations")

    // -- Début de la correction de performance --
    // Nouvelle fonction pour récupérer un seul trajet par son ID
    suspend fun getTrajetById(trajetId: String): Trajet? {
        return try {
            val doc = trajetsRef.document(trajetId).get().await()
            doc.toObject(Trajet::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            null
        }
    }
    // -- Fin de la correction --

    // Récupérer tous les trajets
    suspend fun getAllTrajets(): List<Trajet> {
        return try {
            val snapshot = trajetsRef.get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Trajet::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Récupérer mes trajets créés
    suspend fun getMyTrajets(email: String): List<Trajet> {
        return try {
            val snapshot = trajetsRef
                .whereEqualTo("conducteur", email)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Trajet::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Créer un nouveau trajet
    suspend fun createTrajet(trajet: Trajet): Boolean {
        return try {
            trajetsRef.add(trajet).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Supprimer un trajet ET toutes ses réservations associées
    suspend fun deleteMyTrajet(trajetId: String): Boolean {
        return try {
            // 1. Supprimer toutes les réservations liées à ce trajet
            val reservationsSnapshot = reservationsRef
                .whereEqualTo("trajetId", trajetId)
                .get()
                .await()

            // Supprimer chaque réservation en batch
            reservationsSnapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            // 2. Supprimer le trajet lui-même
            trajetsRef.document(trajetId).delete().await()

            true
        } catch (e: Exception) {
            false
        }
    }

    // Réserver un trajet avec tracking dans la collection reservations
    suspend fun reserverTrajetAvecTracking(trajetId: String, userEmail: String): Boolean {
        return try {
            // 1. Vérifier si l'utilisateur a déjà réservé ce trajet
            if (hasUserReserved(trajetId, userEmail)) {
                return false
            }

            // 2. Récupérer le trajet
            val trajetDoc = trajetsRef.document(trajetId).get().await()
            val trajet = trajetDoc.toObject(Trajet::class.java) ?: return false

            // 3. Vérifier qu'il reste des places
            if (trajet.placesDisponibles <= 0) {
                return false
            }

            // 4. Créer la réservation dans la collection reservations
            val reservation = Reservation(
                trajetId = trajetId,
                userEmail = userEmail,
                timestamp = System.currentTimeMillis()
            )
            reservationsRef.add(reservation).await()

            // 5. Décrémenter les places disponibles
            trajetsRef.document(trajetId).update(
                "placesDisponibles", trajet.placesDisponibles - 1
            ).await()

            // 6. NOUVEAU : Envoyer une notification au conducteur
            sendNotificationToDriver(trajet.conducteur, userEmail, trajet)

            true
        } catch (e: Exception) {
            false
        }
    }

    // Envoyer une notification au conducteur
    private suspend fun sendNotificationToDriver(
        conducteurEmail: String,
        passagerEmail: String,
        trajet: Trajet
    ) {
        try {
            val passagerName = passagerEmail.substringBefore("@")

            // Sauvegarder la notification dans Firestore
            db.collection("notifications").add(
                mapOf(
                    "userId" to conducteurEmail,
                    "title" to "🚗 Nouvelle réservation !",
                    "body" to "$passagerName a réservé votre trajet ${trajet.depart} → ${trajet.arrivee}",
                    "timestamp" to System.currentTimeMillis(),
                    "read" to false
                )
            ).await()

        } catch (e: Exception) {
            // Silencieux si l'envoi échoue
        }
    }

    // Annuler une réservation avec tracking
    suspend fun annulerReservationAvecTracking(trajetId: String, userEmail: String): Boolean {
        return try {
            // 1. Trouver la réservation
            val reservationSnapshot = reservationsRef
                .whereEqualTo("trajetId", trajetId)
                .whereEqualTo("userEmail", userEmail)
                .get()
                .await()

            if (reservationSnapshot.isEmpty) {
                return false
            }

            // 2. Supprimer la réservation
            reservationSnapshot.documents.first().reference.delete().await()

            // 3. Incrémenter les places disponibles
            val trajetDoc = trajetsRef.document(trajetId).get().await()
            val trajet = trajetDoc.toObject(Trajet::class.java) ?: return false

            trajetsRef.document(trajetId).update(
                "placesDisponibles", trajet.placesDisponibles + 1
            ).await()

            true
        } catch (e: Exception) {
            false
        }
    }

    // Vérifier si l'utilisateur a déjà réservé ce trajet
    suspend fun hasUserReserved(trajetId: String, userEmail: String): Boolean {
        return try {
            val snapshot = reservationsRef
                .whereEqualTo("trajetId", trajetId)
                .whereEqualTo("userEmail", userEmail)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }
    // Compter le nombre de réservations pour un trajet
    suspend fun getReservationCount(trajetId: String): Int {
        return try {
            val snapshot = reservationsRef
                .whereEqualTo("trajetId", trajetId)
                .get()
                .await()

            snapshot.size()
        } catch (e: Exception) {
            0
        }
    }
    // Récupérer toutes les réservations pour un trajet spécifique
    suspend fun getReservationsForTrajet(trajetId: String): List<Reservation> {
        return try {
            val snapshot = reservationsRef
                .whereEqualTo("trajetId", trajetId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Reservation::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    // Récupérer les notifications d'un utilisateur
    suspend fun getNotifications(userEmail: String): List<AppNotification> {
        return try {
            val snapshot = db.collection("notifications")
                .whereEqualTo("userId", userEmail)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                AppNotification(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    title = doc.getString("title") ?: "",
                    body = doc.getString("body") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    read = doc.getBoolean("read") ?: false
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Marquer une notification comme lue
    suspend fun markNotificationAsRead(notificationId: String): Boolean {
        return try {
            db.collection("notifications").document(notificationId)
                .update("read", true)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Compter les notifications non lues
    suspend fun getUnreadNotificationCount(userEmail: String): Int {
        return try {
            val snapshot = db.collection("notifications")
                .whereEqualTo("userId", userEmail)
                .whereEqualTo("read", false)
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            0
        }
    }
    // Ajouter une note
    suspend fun addRating(rating: Rating): Boolean {
        return try {
            db.collection("ratings").add(rating).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Vérifier si l'utilisateur a déjà noté quelqu'un pour ce trajet
    suspend fun hasUserRated(trajetId: String, raterEmail: String, ratedEmail: String): Boolean {
        return try {
            val snapshot = db.collection("ratings")
                .whereEqualTo("trajetId", trajetId)
                .whereEqualTo("raterUserEmail", raterEmail)
                .whereEqualTo("ratedUserEmail", ratedEmail)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    // Récupérer la note moyenne d'un utilisateur
    suspend fun getAverageRating(userEmail: String): Double {
        return try {
            val snapshot = db.collection("ratings")
                .whereEqualTo("ratedUserEmail", userEmail)
                .get()
                .await()

            if (snapshot.isEmpty) {
                return 0.0
            }

            val ratings = snapshot.documents.mapNotNull {
                it.getLong("rating")?.toInt()
            }

            if (ratings.isEmpty()) 0.0 else ratings.average()
        } catch (e: Exception) {
            0.0
        }
    }

    // Récupérer le nombre total de notes d'un utilisateur
    suspend fun getRatingCount(userEmail: String): Int {
        return try {
            val snapshot = db.collection("ratings")
                .whereEqualTo("ratedUserEmail", userEmail)
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            0
        }
    }

    // Récupérer toutes les notes reçues par un utilisateur
    suspend fun getRatingsForUser(userEmail: String): List<Rating> {
        return try {
            val snapshot = db.collection("ratings")
                .whereEqualTo("ratedUserEmail", userEmail)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                Rating(
                    id = doc.id,
                    ratedUserEmail = doc.getString("ratedUserEmail") ?: "",
                    raterUserEmail = doc.getString("raterUserEmail") ?: "",
                    trajetId = doc.getString("trajetId") ?: "",
                    rating = doc.getLong("rating")?.toInt() ?: 0,
                    comment = doc.getString("comment") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    // Envoyer un message
    suspend fun sendMessage(message: Message): Boolean {
        return try {
            db.collection("messages").add(message).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Récupérer les messages d'une conversation (entre 2 personnes pour un trajet)
    suspend fun getMessages(trajetId: String, userEmail1: String, userEmail2: String): List<Message> {
        return try {
            val snapshot = db.collection("messages")
                .whereEqualTo("trajetId", trajetId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val message = Message(
                    id = doc.id,
                    trajetId = doc.getString("trajetId") ?: "",
                    senderEmail = doc.getString("senderEmail") ?: "",
                    receiverEmail = doc.getString("receiverEmail") ?: "",
                    text = doc.getString("text") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    read = doc.getBoolean("read") ?: false
                )
                // Filtrer pour ne garder que les messages entre ces 2 utilisateurs
                if ((message.senderEmail == userEmail1 && message.receiverEmail == userEmail2) ||
                    (message.senderEmail == userEmail2 && message.receiverEmail == userEmail1)) {
                    message
                } else {
                    null
                }
            }.sortedBy { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Marquer les messages comme lus
    suspend fun markMessagesAsRead(trajetId: String, currentUserEmail: String, otherUserEmail: String): Boolean {
        return try {
            val snapshot = db.collection("messages")
                .whereEqualTo("trajetId", trajetId)
                .whereEqualTo("senderEmail", otherUserEmail)
                .whereEqualTo("receiverEmail", currentUserEmail)
                .whereEqualTo("read", false)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                doc.reference.update("read", true).await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // Récupérer toutes les conversations d'un utilisateur
    suspend fun getConversations(userEmail: String): List<Conversation> {
        return try {
            // Récupérer tous les messages où l'utilisateur est impliqué
            val sentMessages = db.collection("messages")
                .whereEqualTo("senderEmail", userEmail)
                .get()
                .await()

            val receivedMessages = db.collection("messages")
                .whereEqualTo("receiverEmail", userEmail)
                .get()
                .await()

            val allMessages = (sentMessages.documents + receivedMessages.documents).mapNotNull { doc ->
                Message(
                    id = doc.id,
                    trajetId = doc.getString("trajetId") ?: "",
                    senderEmail = doc.getString("senderEmail") ?: "",
                    receiverEmail = doc.getString("receiverEmail") ?: "",
                    text = doc.getString("text") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    read = doc.getBoolean("read") ?: false
                )
            }

            // Grouper par trajet et autre utilisateur
            val conversationsMap = mutableMapOf<Pair<String, String>, MutableList<Message>>()

            allMessages.forEach { message ->
                val otherUser = if (message.senderEmail == userEmail) message.receiverEmail else message.senderEmail
                val key = Pair(message.trajetId, otherUser)
                conversationsMap.getOrPut(key) { mutableListOf() }.add(message)
            }

            // Créer les objets Conversation
            conversationsMap.map { (key, messages) ->
                val (trajetId, otherUserEmail) = key
                val lastMessage = messages.maxByOrNull { it.timestamp }
                val unreadCount = messages.count {
                    it.receiverEmail == userEmail && !it.read
                }

                // Récupérer les infos du trajet
                val trajet = getTrajetById(trajetId)

                Conversation(
                    trajetId = trajetId,
                    otherUserEmail = otherUserEmail,
                    otherUserName = otherUserEmail.substringBefore("@"),
                    lastMessage = lastMessage?.text ?: "",
                    lastMessageTime = lastMessage?.timestamp ?: 0L,
                    unreadCount = unreadCount,
                    trajetInfo = if (trajet != null) "${trajet.depart} → ${trajet.arrivee}" else ""
                )
            }.sortedByDescending { it.lastMessageTime }

        } catch (e: Exception) {
            emptyList()
        }
    }

    // Compter le nombre total de messages non lus
    suspend fun getTotalUnreadMessageCount(userEmail: String): Int {
        return try {
            val snapshot = db.collection("messages")
                .whereEqualTo("receiverEmail", userEmail)
                .whereEqualTo("read", false)
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            0
        }
    }
}