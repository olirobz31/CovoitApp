package com.example.covoitapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    onBack: () -> Unit = {},
    onOpenChat: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val repository = remember { FirebaseRepository() }
    val authManager = remember { AuthManager() }
    val currentUserEmail = authManager.getCurrentUser()?.email ?: ""
    val scope = rememberCoroutineScope()

    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Charger les conversations
    LaunchedEffect(Unit) {
        scope.launch {
            val allConversations = repository.getConversations(currentUserEmail)

            // ⭐ Debug - Afficher toutes les conversations
            println("=== DEBUG CONVERSATIONS ===")
            println("Total conversations: ${allConversations.size}")
            allConversations.forEachIndexed { index, conv ->
                println("Conv $index:")
                println("  trajetId: '${conv.trajetId}'")
                println("  otherUserEmail: '${conv.otherUserEmail}'")
                println("  trajetInfo: '${conv.trajetInfo}'")
                println("  lastMessage: '${conv.lastMessage}'")
            }

            // ⭐ FILTRE ASSOUPLI - Garde plus de conversations
            conversations = allConversations.filter { conversation ->
                // Garder si email est valide
                conversation.otherUserEmail.isNotBlank()
            }

            println("Conversations après filtre: ${conversations.size}")

            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Aucune conversation",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Réservez un trajet pour commencer à discuter ! 💬",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(conversations) { conversation ->
                    ConversationCard(
                        conversation = conversation,
                        onClick = {
                            // ⭐ PROTECTION AVEC LOGS
                            try {
                                println("=== CLIC SUR CONVERSATION ===")
                                println("trajetId: '${conversation.trajetId}'")
                                println("otherUserEmail: '${conversation.otherUserEmail}'")
                                println("trajetInfo: '${conversation.trajetInfo}'")

                                // Utiliser trajetId tel quel si c'est "direct", sinon vérifier
                                val finalTrajetId = when {
                                    conversation.trajetId == "direct" -> "direct"
                                    conversation.trajetId.isBlank() -> "direct"
                                    else -> conversation.trajetId
                                }

                                println("Navigation avec trajetId: '$finalTrajetId'")

                                onOpenChat(
                                    finalTrajetId,
                                    conversation.otherUserEmail,
                                    conversation.trajetInfo.ifBlank { "Chat" }
                                )
                            } catch (e: Exception) {
                                println("ERREUR lors de l'ouverture du chat: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun ConversationCard(
    conversation: Conversation,
    onClick: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE)
    val timeStr = try {
        dateFormat.format(Date(conversation.lastMessageTime))
    } catch (e: Exception) {
        "Date inconnue"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Avatar
                UserAvatar(
                    email = conversation.otherUserEmail,
                    size = 48.dp,
                    fontSize = 20
                )

                // Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = conversation.otherUserName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = conversation.trajetInfo.ifBlank { "Chat" },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = conversation.lastMessage.ifBlank { "Pas de message" },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = timeStr,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (conversation.unreadCount > 0) {
                    Badge {
                        Text(conversation.unreadCount.toString())
                    }
                }
            }
        }
    }
}