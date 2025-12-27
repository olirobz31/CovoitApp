package com.example.covoitapp

import com.google.firebase.firestore.DocumentId

data class Message(
    @DocumentId
    val id: String = "",
    val trajetId: String = "",           // ID du trajet concerné
    val senderEmail: String = "",        // Email de l'expéditeur
    val receiverEmail: String = "",      // Email du destinataire
    val text: String = "",               // Contenu du message
    val timestamp: Long = 0L,            // Date d'envoi
    val read: Boolean = false            // Lu ou non
)

data class Conversation(
    val trajetId: String = "",
    val otherUserEmail: String = "",
    val otherUserName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val unreadCount: Int = 0,
    val trajetInfo: String = ""          // "Paris → Lyon"
)