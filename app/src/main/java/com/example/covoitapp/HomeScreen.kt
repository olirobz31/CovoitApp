package com.example.covoitapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.covoitapp.ui.theme.CovoitAppTheme
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

enum class SortOrder {
    NONE,       // Pas de tri
    PRICE_ASC,  // Prix croissant ↑
    PRICE_DESC  // Prix décroissant ↓
}

enum class FilterPeriod {
    ALL,           // Tous
    TODAY,         // Aujourd'hui
    THIS_WEEK,     // Cette semaine
    THIS_MONTH     // Ce mois
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,  // ⭐ AJOUTEZ CETTE LIGNE
    backStackEntry: NavBackStackEntry,
    onCreateTrip: () -> Unit,
    onLogout: () -> Unit,
    onViewReservations: (String) -> Unit,
    onProfile: () -> Unit,
    onNotifications: () -> Unit,
    onRateUser: (String, String) -> Unit,
    onMessages: () -> Unit,
    onContactUser: (String, String, String) -> Unit,
    onViewMap: (String) -> Unit,
    onViewDriverProfile: (String) -> Unit = {}
) {
    val repository = remember { FirebaseRepository() }
    val authManager = remember { AuthManager() }
    val currentUserEmail = authManager.getCurrentUser()?.email ?: ""

    var selectedTabIndex by remember { mutableStateOf(0) }
    var allTrajets by remember { mutableStateOf<List<Trajet>>(emptyList()) }
    var myTrajets by remember { mutableStateOf<List<Trajet>>(emptyList()) }
    var reservedTrajets by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf<SortOrder>(SortOrder.NONE) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var filterPeriod by remember { mutableStateOf<FilterPeriod>(FilterPeriod.ALL) }
    var filterMinPrice by remember { mutableStateOf(0f) }
    var filterMaxPrice by remember { mutableStateOf(100f) }
    var filterMinPlaces by remember { mutableStateOf(0) }
    var trajetToDelete by remember { mutableStateOf<Trajet?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var unreadCount by remember { mutableStateOf(0) }
    var unreadMessagesCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val tabs = listOf("Tous les trajets", "Mes trajets", "Historique")


    // Afficher le Snackbar quand il y a un message
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            snackbarMessage = null
        }
    }

    // Charger les trajets et vérifier les réservations
    LaunchedEffect(refreshTrigger, selectedTabIndex) {
        scope.launch {
            isLoading = true
            when (selectedTabIndex) {
                0 -> { // Tous les trajets
                    allTrajets = repository.getAllTrajets()
                    // Vérifier quels trajets sont réservés par l'utilisateur
                    val reserved = mutableSetOf<String>()
                    allTrajets.forEach { trajet ->
                        if (repository.hasUserReserved(trajet.id, currentUserEmail)) {
                            reserved.add(trajet.id)
                        }
                    }
                    reservedTrajets = reserved
                }
                1 -> { // Mes trajets
                    myTrajets = repository.getMyTrajets(currentUserEmail)
                }
                2 -> { // Historique
                    allTrajets = repository.getAllTrajets()
                }
            }
            isLoading = false
            isRefreshing = false
        }
    }

    // Charger le nombre de notifications non lues
    LaunchedEffect(refreshTrigger, selectedTabIndex, Unit) {
        scope.launch {
            unreadCount = repository.getUnreadNotificationCount(currentUserEmail)
        }
    }

    // Charger le nombre de messages non lus
    LaunchedEffect(refreshTrigger) {
        scope.launch {
            unreadMessagesCount = repository.getTotalUnreadMessageCount(currentUserEmail)
        }
    }

    // Filtrer et trier les trajets
    val currentTime = System.currentTimeMillis()

    val currentTrajets = when (selectedTabIndex) {
        0 -> allTrajets.filter { it.dateTrajet >= currentTime } // Tous les trajets à venir
        1 -> myTrajets.filter { it.dateTrajet >= currentTime }  // Mes trajets à venir
        2 -> allTrajets.filter { it.dateTrajet < currentTime }  // Trajets passés (historique)
        else -> emptyList()
    }

    val filteredTrajets = currentTrajets
        .filter { trajet ->
            // Filtre par recherche
            val matchesSearch = if (searchQuery.isEmpty()) {
                true
            } else {
                trajet.depart.contains(searchQuery, ignoreCase = true) ||
                        trajet.arrivee.contains(searchQuery, ignoreCase = true)
            }

            // Filtre par période
            val matchesPeriod = when (filterPeriod) {
                FilterPeriod.ALL -> true
                FilterPeriod.TODAY -> {
                    val calendar = java.util.Calendar.getInstance()
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    val todayStart = calendar.timeInMillis
                    calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
                    val todayEnd = calendar.timeInMillis
                    trajet.dateTrajet in todayStart until todayEnd
                }
                FilterPeriod.THIS_WEEK -> {
                    val calendar = java.util.Calendar.getInstance()
                    calendar.set(java.util.Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    val weekStart = calendar.timeInMillis
                    calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
                    val weekEnd = calendar.timeInMillis
                    trajet.dateTrajet in weekStart until weekEnd
                }
                FilterPeriod.THIS_MONTH -> {
                    val calendar = java.util.Calendar.getInstance()
                    calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    val monthStart = calendar.timeInMillis
                    calendar.add(java.util.Calendar.MONTH, 1)
                    val monthEnd = calendar.timeInMillis
                    trajet.dateTrajet in monthStart until monthEnd
                }
            }

            // Filtre par prix
            val matchesPrice = trajet.prixParPersonne >= filterMinPrice &&
                    trajet.prixParPersonne <= filterMaxPrice

            // Filtre par places
            val matchesPlaces = trajet.placesDisponibles >= filterMinPlaces

            matchesSearch && matchesPeriod && matchesPrice && matchesPlaces
        }
        .let { list ->
            when (sortOrder) {
                SortOrder.PRICE_ASC -> list.sortedBy { it.prixParPersonne }
                SortOrder.PRICE_DESC -> list.sortedByDescending { it.prixParPersonne }
                SortOrder.NONE -> list
            }
        }

    // Dialogue de confirmation de suppression
    if (showDeleteDialog && trajetToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmer la suppression") },
            text = { Text("Êtes-vous sûr de vouloir supprimer ce trajet ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val success = repository.deleteMyTrajet(trajetToDelete!!.id)
                            if (success) {
                                snackbarMessage = "✅ Trajet supprimé"
                                refreshTrigger++
                            } else {
                                snackbarMessage = "❌ Erreur lors de la suppression"
                            }
                            showDeleteDialog = false
                            trajetToDelete = null
                        }
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "🚗 CovoitApp",
                            fontWeight = FontWeight.Bold
                        )
                    },



                    actions = {
                        // Bouton Messages avec badge
                        BadgedBox(
                            badge = {
                                if (unreadMessagesCount > 0) {
                                    Badge {
                                        Text(unreadMessagesCount.toString())
                                    }
                                }
                            }
                        ) {
                            IconButton(onClick = { onMessages() }) {
                                Text("💬", fontSize = 20.sp)
                            }
                        }

                        // Bouton Notifications avec badge
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge {
                                        Text(unreadCount.toString())
                                    }
                                }
                            }
                        ) {
                            IconButton(onClick = { onNotifications() }) {
                                Text("🔔", fontSize = 20.sp)
                            }
                        }

                        // Bouton Mon profil
                        TextButton(
                            onClick = { onProfile() }
                        ) {
                            Text(
                                text = "👤 Profil",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        // Bouton Déconnexion
                        TextButton(
                            onClick = {
                                authManager.signOut()
                                onLogout()
                            }
                        ) {
                            Text(
                                text = "Déconnexion",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    color = if (selectedTabIndex == index)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onCreateTrip() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Créer un trajet",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
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
        } else if (currentTrajets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (selectedTabIndex) {
                        0 -> "Aucun trajet disponible\nCliquez sur + pour en créer un"
                        1 -> "Vous n'avez créé aucun trajet\nCliquez sur + pour en créer un"
                        2 -> "Aucun trajet dans l'historique"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Barre de recherche et filtres
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Barre de recherche
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Rechercher par lieu...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Rechercher")
                        },
                        singleLine = true
                    )

                    // Boutons tri et filtres
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredTrajets.size} trajet(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Bouton filtres
                            FilterChip(
                                selected = showFilters || filterPeriod != FilterPeriod.ALL ||
                                        filterMinPrice > 0f || filterMaxPrice < 100f || filterMinPlaces > 0,
                                onClick = { showFilters = !showFilters },
                                label = { Text("🔍 Filtres") }
                            )

                            // Bouton tri
                            FilterChip(
                                selected = sortOrder != SortOrder.NONE,
                                onClick = {
                                    sortOrder = when (sortOrder) {
                                        SortOrder.NONE -> SortOrder.PRICE_ASC
                                        SortOrder.PRICE_ASC -> SortOrder.PRICE_DESC
                                        SortOrder.PRICE_DESC -> SortOrder.NONE
                                    }
                                },
                                label = {
                                    Text(
                                        when (sortOrder) {
                                            SortOrder.NONE -> "Trier par prix"
                                            SortOrder.PRICE_ASC -> "Prix ↑"
                                            SortOrder.PRICE_DESC -> "Prix ↓"
                                        }
                                    )
                                }
                            )
                        }
                    }

                    // Panneau de filtres
                    if (showFilters) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Filtres",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // Filtre par période
                                Text("Période", fontWeight = FontWeight.Medium)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = filterPeriod == FilterPeriod.ALL,
                                        onClick = { filterPeriod = FilterPeriod.ALL },
                                        label = { Text("Tous") }
                                    )
                                    FilterChip(
                                        selected = filterPeriod == FilterPeriod.TODAY,
                                        onClick = { filterPeriod = FilterPeriod.TODAY },
                                        label = { Text("Auj.") }
                                    )
                                    FilterChip(
                                        selected = filterPeriod == FilterPeriod.THIS_WEEK,
                                        onClick = { filterPeriod = FilterPeriod.THIS_WEEK },
                                        label = { Text("Semaine") }
                                    )
                                    FilterChip(
                                        selected = filterPeriod == FilterPeriod.THIS_MONTH,
                                        onClick = { filterPeriod = FilterPeriod.THIS_MONTH },
                                        label = { Text("Mois") }
                                    )
                                }

                                HorizontalDivider()

                                // Filtre par prix
                                Text("Prix (${filterMinPrice.toInt()}€ - ${filterMaxPrice.toInt()}€)", fontWeight = FontWeight.Medium)
                                RangeSlider(
                                    value = filterMinPrice..filterMaxPrice,
                                    onValueChange = { range ->
                                        filterMinPrice = range.start
                                        filterMaxPrice = range.endInclusive
                                    },
                                    valueRange = 0f..100f,
                                    steps = 19
                                )

                                HorizontalDivider()

                                // Filtre par places
                                Text("Places minimum: ${filterMinPlaces}", fontWeight = FontWeight.Medium)
                                Slider(
                                    value = filterMinPlaces.toFloat(),
                                    onValueChange = { filterMinPlaces = it.toInt() },
                                    valueRange = 0f..5f,
                                    steps = 4
                                )

                                // Bouton réinitialiser
                                OutlinedButton(
                                    onClick = {
                                        filterPeriod = FilterPeriod.ALL
                                        filterMinPrice = 0f
                                        filterMaxPrice = 100f
                                        filterMinPlaces = 0
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Réinitialiser les filtres")
                                }
                            }
                        }
                    }
                }

                // Liste des trajets filtrés avec Pull-to-Refresh
                if (filteredTrajets.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucun trajet ne correspond à votre recherche",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

                    SwipeRefresh(
                        state = swipeRefreshState,
                        onRefresh = {
                            scope.launch {
                                isRefreshing = true
                                delay(500)
                                refreshTrigger++
                            }
                        }
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredTrajets) { trajet ->


                                TrajetCard(
                                    trajet = trajet,
                                    hasReserved = reservedTrajets.contains(trajet.id),
                                    currentUserEmail = currentUserEmail,
                                    showDeleteButton = selectedTabIndex == 1,
                                    isPastTrip = selectedTabIndex == 2,
                                    onReserver = {
                                        // Rediriger vers l'écran de paiement
                                        val trajetInfo = "${trajet.depart} → ${trajet.arrivee}"
                                        navController.navigate(
                                            "payment/${trajet.id}/${trajet.prixParPersonne}/${java.net.URLEncoder.encode(trajetInfo, "UTF-8")}"
                                        )
                                    },
                                    onAnnulerReservation = {
                                        scope.launch {
                                            val success = repository.annulerReservationAvecTracking(trajet.id, currentUserEmail)
                                            if (success) {
                                                snackbarMessage = "✅ Réservation annulée"
                                                refreshTrigger++
                                            } else {
                                                snackbarMessage = "❌ Erreur lors de l'annulation"
                                            }
                                        }
                                    },
                                    onDelete = {
                                        trajetToDelete = trajet
                                        showDeleteDialog = true
                                    },
                                    onViewReservations = { trajetId ->
                                        onViewReservations(trajetId)
                                    },
                                    onRateConductor = { trajetId, conducteurEmail ->
                                        onRateUser(trajetId, conducteurEmail)
                                    },
                                    onContactUser = { trajetId, userEmail, trajetInfo ->
                                        onContactUser(trajetId, userEmail, trajetInfo)
                                    },
                                    onViewMap = { trajetId ->
                                        onViewMap(trajetId)
                                    },
                                    onViewDriverProfile = { driverEmail ->
                                        onViewDriverProfile(driverEmail)
                                    }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrajetCard(
    trajet: Trajet,
    hasReserved: Boolean,
    currentUserEmail: String,
    showDeleteButton: Boolean = false,
    isPastTrip: Boolean = false,
    onReserver: () -> Unit = {},
    onAnnulerReservation: () -> Unit = {},
    onDelete: () -> Unit = {},
    onViewReservations: (String) -> Unit = {},
    onRateConductor: (String, String) -> Unit = { _, _ -> },
    onContactUser: (String, String, String) -> Unit = { _, _, _ -> },
    onViewMap: (String) -> Unit = {},
    onViewDriverProfile: (String) -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(false) }
    var reservationCount by remember { mutableStateOf<Int?>(null) }
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()

    // Charger le nombre de réservations
    LaunchedEffect(trajet.id) {
        reservationCount = repository.getReservationCount(trajet.id)
    }

    // Formater le prix
    val prixFormate = NumberFormat.getCurrencyInstance(Locale.FRANCE)
        .format(trajet.prixParPersonne)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isPastTrip) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Badge "Terminé" pour les trajets passés
            if (isPastTrip) {
                Text(
                    text = "✓ Terminé",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Photo du trajet
            if (trajet.photoUrl.isNotEmpty()) {
                AsyncImage(
                    model = trajet.photoUrl,
                    contentDescription = "Photo du trajet",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Horaire, date et prix
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = trajet.horaire,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE)
                            .format(java.util.Date(trajet.dateTrajet)),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = prixFormate,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Départ
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🟢", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = trajet.depart, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Arrivée
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🔴", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = trajet.arrivee, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(12.dp))

            // Conducteur et places (NOM CLIQUABLE)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onViewDriverProfile(trajet.conducteur) }
                ) {
                    UserAvatar(
                        email = trajet.conducteur,
                        size = 32.dp,
                        fontSize = 14
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getDisplayNameFromEmail(trajet.conducteur),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = if (trajet.placesDisponibles > 0)
                        "${trajet.placesDisponibles} place(s) dispo"
                    else
                        "Complet",
                    fontSize = 14.sp,
                    fontWeight = if (trajet.placesDisponibles == 0) FontWeight.Bold else FontWeight.Normal,
                    color = if (trajet.placesDisponibles > 0)
                        MaterialTheme.colorScheme.secondary
                    else
                        MaterialTheme.colorScheme.error
                )
            }

            // Afficher le nombre de réservations
            if (reservationCount != null && reservationCount!! > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👥 ",
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (reservationCount == 1)
                            "1 personne a déjà réservé"
                        else
                            "$reservationCount personnes ont déjà réservé",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))


            // Bouton Partager (visible pour tous)
            val context = androidx.compose.ui.platform.LocalContext.current

            OutlinedButton(
                onClick = {
                    val shareText = """
🚗 Covoiturage

📍 Départ : ${trajet.depart}
📍 Arrivée : ${trajet.arrivee}
🕐 Horaire : ${trajet.horaire}
📅 Date : ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE).format(java.util.Date(trajet.dateTrajet))}
💰 Prix : ${prixFormate}
🪑 Places disponibles : ${trajet.placesDisponibles}
👤 Conducteur : ${getDisplayNameFromEmail(email = trajet.conducteur)}

Réservez via CovoitApp !
        """.trimIndent()

                    // Créer l'intent de partage
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Partager ce trajet")
                    context.startActivity(shareIntent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📤 Partager ce trajet")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bouton Voir sur la carte
            OutlinedButton(
                onClick = { onViewMap(trajet.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🗺️ Voir sur la carte")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Boutons - Désactivés pour les trajets passés
            if (isPastTrip) {
                // Pour l'historique
                if (trajet.conducteur == currentUserEmail) {
                    // Si c'est notre trajet, on peut voir les réservations
                    Button(
                        onClick = { onViewReservations(trajet.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("👥 Voir les réservations (${reservationCount ?: 0})")
                    }
                } else if (hasReserved) {
                    // Si on a réservé ce trajet passé, on peut noter le conducteur
                    Button(
                        onClick = { onRateConductor(trajet.id, trajet.conducteur) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⭐ Noter le conducteur")
                    }
                } else {
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    ) {
                        Text("Trajet terminé")
                    }
                }
            } else {
                // Boutons normaux pour les trajets à venir
                if (showDeleteButton) {
                    // Boutons pour mes trajets
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Bouton voir les réservations
                        Button(
                            onClick = { onViewReservations(trajet.id) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Text("👥 Voir les réservations (${reservationCount ?: 0})")
                        }

                        // Bouton supprimer
                        OutlinedButton(
                            onClick = {
                                isLoading = true
                                onDelete()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Supprimer",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Supprimer ce trajet")
                            }
                        }
                    }
                } else {
                    // Boutons réserver/annuler (pour tous les trajets)
                    val isOwnTrip = trajet.conducteur == currentUserEmail

                    if (isOwnTrip) {
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false
                        ) {
                            Text("Votre trajet - Vous ne pouvez pas le réserver")
                        }
                    } else if (hasReserved) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Bouton Contacter
                            Button(
                                onClick = {
                                    onContactUser(
                                        trajet.id,
                                        trajet.conducteur,
                                        "${trajet.depart} → ${trajet.arrivee}"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("💬 Contacter le conducteur")
                            }

                            // Bouton Annuler réservation
                            OutlinedButton(
                                onClick = {
                                    isLoading = true
                                    onAnnulerReservation()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text("Annuler ma réservation")
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                isLoading = true
                                onReserver()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = trajet.placesDisponibles > 0 && !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = if (trajet.placesDisponibles > 0)
                                        "Réserver ce trajet"
                                    else
                                        "Complet"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    CovoitAppTheme {
        // Preview sans paramètres
    }
}