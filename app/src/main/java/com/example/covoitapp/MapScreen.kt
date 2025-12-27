package com.example.covoitapp

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    trajet: Trajet,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var routePoints by remember { mutableStateOf<List<GeoPoint>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Initialiser OSMDroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
    }

    // Charger l'itinéraire routier
    LaunchedEffect(trajet) {
        scope.launch {
            val departPoint = getApproximateLocation(trajet.depart)
            val arriveePoint = getApproximateLocation(trajet.arrivee)

            // Récupérer l'itinéraire routier depuis OSRM
            val coordinates = OSRMService.getRoute(
                departPoint.longitude,
                departPoint.latitude,
                arriveePoint.longitude,
                arriveePoint.latitude
            )

            routePoints = coordinates?.map { coord ->
                GeoPoint(coord[1], coord[0]) // OSRM retourne [lon, lat]
            }

            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${trajet.depart} → ${trajet.arrivee}") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)

                            val departPoint = getApproximateLocation(trajet.depart)
                            val arriveePoint = getApproximateLocation(trajet.arrivee)

                            // Marqueur départ (vert)
                            val departMarker = Marker(this).apply {
                                position = departPoint
                                title = "Départ: ${trajet.depart}"
                                snippet = trajet.horaire
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            overlays.add(departMarker)

                            // Marqueur arrivée (rouge)
                            val arriveeMarker = Marker(this).apply {
                                position = arriveePoint
                                title = "Arrivée: ${trajet.arrivee}"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            overlays.add(arriveeMarker)

                            // Tracer l'itinéraire routier si disponible
                            if (routePoints != null && routePoints!!.isNotEmpty()) {
                                val line = Polyline().apply {
                                    setPoints(routePoints)
                                    outlinePaint.color = android.graphics.Color.BLUE
                                    outlinePaint.strokeWidth = 8f
                                }
                                overlays.add(line)
                            } else {
                                // Fallback : ligne droite si l'API échoue
                                val line = Polyline().apply {
                                    addPoint(departPoint)
                                    addPoint(arriveePoint)
                                    outlinePaint.color = android.graphics.Color.BLUE
                                    outlinePaint.strokeWidth = 5f
                                }
                                overlays.add(line)
                            }

                            // Centrer la carte
                            val centerLat = (departPoint.latitude + arriveePoint.latitude) / 2
                            val centerLon = (departPoint.longitude + arriveePoint.longitude) / 2
                            controller.setCenter(GeoPoint(centerLat, centerLon))
                            controller.setZoom(7.0)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// Fonction pour obtenir des coordonnées approximatives
private fun getApproximateLocation(city: String): GeoPoint {
    return when (city.lowercase()) {
        "paris" -> GeoPoint(48.8566, 2.3522)
        "lyon" -> GeoPoint(45.7640, 4.8357)
        "marseille" -> GeoPoint(43.2965, 5.3698)
        "toulouse" -> GeoPoint(43.6047, 1.4442)
        "nice" -> GeoPoint(43.7102, 7.2620)
        "nantes" -> GeoPoint(47.2184, -1.5536)
        "bordeaux" -> GeoPoint(44.8378, -0.5792)
        "lille" -> GeoPoint(50.6292, 3.0573)
        "rennes" -> GeoPoint(48.1173, -1.6778)
        "strasbourg" -> GeoPoint(48.5734, 7.7521)
        "montpellier" -> GeoPoint(43.6108, 3.8767)
        "reims" -> GeoPoint(49.2583, 4.0317)
        "cahors" -> GeoPoint(44.4479, 1.4410)
        "figeac" -> GeoPoint(44.6088, 2.0319)
        "rome" -> GeoPoint(41.9028, 12.4964)
        "naples" -> GeoPoint(40.8518, 14.2681)
        else -> GeoPoint(46.2276, 2.2137) // Centre de la France par défaut
    }
}