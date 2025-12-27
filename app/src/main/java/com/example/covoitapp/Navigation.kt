package com.example.covoitapp

import androidx.compose.runtime.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch

// Définition des routes (chemins) pour chaque écran
object Routes {
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
    const val HOME = "home"
    const val CREATE_TRIP = "create_trip"
    const val RESERVATIONS = "reservations/{trajetId}"
    const val PROFILE = "profile"
    const val NOTIFICATIONS = "notifications"
    const val RATING = "rating/{trajetId}/{ratedUserEmail}/{ratedUserName}"
    const val CONVERSATIONS = "conversations"
    const val CHAT = "chat/{trajetId}/{otherUserEmail}/{trajetInfo}"
    const val CHAT_DIRECT = "chat/direct/{otherUserEmail}"
    const val MAP = "map/{trajetId}"
    const val DRIVER_PROFILE = "driver_profile/{driverEmail}"
    const val PAYMENT = "payment/{trajetId}/{montant}/{trajetInfo}"

    fun reservations(trajetId: String) = "reservations/$trajetId"
    fun rating(trajetId: String, ratedUserEmail: String, ratedUserName: String) =
        "rating/$trajetId/$ratedUserEmail/$ratedUserName"
    fun chat(trajetId: String, otherUserEmail: String, trajetInfo: String) =
        "chat/$trajetId/$otherUserEmail/$trajetInfo"
    fun chatDirect(otherUserEmail: String) = "chat/direct/$otherUserEmail"
    fun map(trajetId: String) = "map/$trajetId"
    fun driverProfile(driverEmail: String) = "driver_profile/$driverEmail"
    fun payment(trajetId: String, montant: Double, trajetInfo: String) =
        "payment/$trajetId/$montant/${java.net.URLEncoder.encode(trajetInfo, "UTF-8")}"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authManager = remember { AuthManager() }
    val scope = rememberCoroutineScope()

    // Déterminer la destination de départ en fonction de l'état de connexion
    val startDestination = if (authManager.getCurrentUser() != null) {
        Routes.HOME
    } else {
        Routes.LOGIN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Écran de connexion
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Routes.SIGN_UP)
                }
            )
        }

        // Écran d'inscription
        composable(Routes.SIGN_UP) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Écran de profil
        composable(Routes.PROFILE) {
            val authManager = remember { AuthManager() }
            val userEmail = authManager.getCurrentUser()?.email ?: ""

            ProfileScreen(
                userEmail = userEmail,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Écran des notifications
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Écran de notation
        composable(
            route = Routes.RATING,
            arguments = listOf(
                navArgument("trajetId") { type = NavType.StringType },
                navArgument("ratedUserEmail") { type = NavType.StringType },
                navArgument("ratedUserName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val trajetId = backStackEntry.arguments?.getString("trajetId") ?: ""
            val ratedUserEmail = backStackEntry.arguments?.getString("ratedUserEmail") ?: ""
            val ratedUserName = backStackEntry.arguments?.getString("ratedUserName") ?: ""

            RatingScreen(
                trajetId = trajetId,
                ratedUserEmail = ratedUserEmail,
                ratedUserName = ratedUserName,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Écran de conversations
        composable(Routes.CONVERSATIONS) {
            ConversationsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onOpenChat = { trajetId, otherUserEmail, trajetInfo ->
                    // ⭐ CORRIGÉ - Vérifier si c'est un chat direct
                    if (trajetId == "direct" || trajetId.isBlank()) {
                        navController.navigate(Routes.chatDirect(otherUserEmail))
                    } else {
                        navController.navigate(Routes.chat(trajetId, otherUserEmail, trajetInfo))
                    }
                }
            )
        }

        // Écran de chat avec trajet
        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("trajetId") { type = NavType.StringType },
                navArgument("otherUserEmail") { type = NavType.StringType },
                navArgument("trajetInfo") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val trajetId = backStackEntry.arguments?.getString("trajetId") ?: ""
            val otherUserEmail = backStackEntry.arguments?.getString("otherUserEmail") ?: ""
            val trajetInfo = backStackEntry.arguments?.getString("trajetInfo") ?: ""

            ChatScreen(
                trajetId = trajetId,
                otherUserEmail = otherUserEmail,
                trajetInfo = trajetInfo,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ⭐ NOUVEAU - Chat direct (sans trajet)
        composable(
            route = Routes.CHAT_DIRECT,
            arguments = listOf(
                navArgument("otherUserEmail") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val otherUserEmail = backStackEntry.arguments?.getString("otherUserEmail") ?: ""

            ChatScreen(
                trajetId = "direct",
                otherUserEmail = otherUserEmail,
                trajetInfo = "Chat avec ${getDisplayNameFromEmail(otherUserEmail)}",
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Écran de carte
        composable(
            route = Routes.MAP,
            arguments = listOf(
                navArgument("trajetId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val trajetId = backStackEntry.arguments?.getString("trajetId") ?: ""

            val repository = remember { FirebaseRepository() }
            var trajet by remember { mutableStateOf<Trajet?>(null) }

            LaunchedEffect(trajetId) {
                trajet = repository.getTrajetById(trajetId)
            }

            trajet?.let {
                MapScreen(
                    trajet = it,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // Écran de profil conducteur
        composable(
            route = Routes.DRIVER_PROFILE,
            arguments = listOf(
                navArgument("driverEmail") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val driverEmail = backStackEntry.arguments?.getString("driverEmail") ?: ""

            DriverProfileScreen(
                driverEmail = driverEmail,
                onBack = {
                    navController.popBackStack()
                },
                onChatClick = {
                    navController.navigate(Routes.chatDirect(driverEmail))
                }
            )
        }

        // Écran de paiement
        composable(
            route = Routes.PAYMENT,
            arguments = listOf(
                navArgument("trajetId") { type = NavType.StringType },
                navArgument("montant") { type = NavType.StringType },
                navArgument("trajetInfo") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val trajetId = backStackEntry.arguments?.getString("trajetId") ?: ""
            val montantStr = backStackEntry.arguments?.getString("montant") ?: "0.0"
            val montant = montantStr.toDoubleOrNull() ?: 0.0
            val trajetInfoEncoded = backStackEntry.arguments?.getString("trajetInfo") ?: ""
            val trajetInfo = java.net.URLDecoder.decode(trajetInfoEncoded, "UTF-8")

            PaymentScreen(
                trajetId = trajetId,
                montant = montant,
                trajetInfo = trajetInfo,
                onPaymentSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Écran d'accueil (liste des trajets)
        composable(Routes.HOME) { backStackEntry ->
            HomeScreen(
                navController = navController,
                backStackEntry = backStackEntry,
                onCreateTrip = {
                    navController.navigate(Routes.CREATE_TRIP)
                },
                onLogout = {
                    scope.launch {
                        authManager.signOut()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    }
                },
                onViewReservations = { trajetId ->
                    navController.navigate(Routes.reservations(trajetId))
                },
                onProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onNotifications = {
                    navController.navigate(Routes.NOTIFICATIONS)
                },
                onRateUser = { trajetId, userEmail ->
                    val userName = userEmail.substringBefore("@")
                    navController.navigate(Routes.rating(trajetId, userEmail, userName))
                },
                onMessages = {
                    navController.navigate(Routes.CONVERSATIONS)
                },
                onContactUser = { trajetId, userEmail, trajetInfo ->
                    navController.navigate(Routes.chat(trajetId, userEmail, trajetInfo))
                },
                onViewMap = { trajetId ->
                    navController.navigate(Routes.map(trajetId))
                },
                onViewDriverProfile = { driverEmail ->
                    navController.navigate(Routes.driverProfile(driverEmail))
                }
            )
        }

        // Écran de création de trajet
        composable(Routes.CREATE_TRIP) {
            CreateTripScreen(
                onTripCreated = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("trip_created", true)
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Écran des réservations
        composable(
            route = Routes.RESERVATIONS,
            arguments = listOf(
                navArgument("trajetId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val trajetId = backStackEntry.arguments?.getString("trajetId") ?: ""

            val repository = remember { FirebaseRepository() }
            var trajet by remember { mutableStateOf<Trajet?>(null) }

            LaunchedEffect(trajetId) {
                trajet = repository.getTrajetById(trajetId)
            }

            trajet?.let {
                ReservationsScreen(
                    trajetId = trajetId,
                    trajet = it,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}