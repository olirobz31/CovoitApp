package com.example.covoitapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Modèle de données pour la réponse OSRM
data class OSRMResponse(
    val routes: List<Route>
)

data class Route(
    val geometry: Geometry
)

data class Geometry(
    val coordinates: List<List<Double>>
)

// Interface Retrofit pour l'API OSRM
interface OSRMApi {
    @GET("route/v1/driving/{coordinates}")
    suspend fun getRoute(
        @Path("coordinates") coordinates: String,
        @Query("geometries") geometries: String = "geojson"  // ⭐ AJOUTÉ
    ): OSRMResponse
}

// Service pour récupérer l'itinéraire
object OSRMService {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://router.project-osrm.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(OSRMApi::class.java)

    suspend fun getRoute(startLon: Double, startLat: Double, endLon: Double, endLat: Double): List<List<Double>>? {
        return try {
            val coordinates = "$startLon,$startLat;$endLon,$endLat"
            println("=== OSRM REQUEST ===")  // ⭐ DEBUG
            println("Coordinates: $coordinates")
            println("URL: https://router.project-osrm.org/route/v1/driving/$coordinates?geometries=geojson")

            val response = api.getRoute(coordinates)

            println("=== OSRM RESPONSE ===")  // ⭐ DEBUG
            println("Routes found: ${response.routes.size}")
            if (response.routes.isNotEmpty()) {
                println("Coordinates count: ${response.routes[0].geometry.coordinates.size}")
            }

            response.routes.firstOrNull()?.geometry?.coordinates
        } catch (e: Exception) {
            println("=== OSRM ERROR ===")  // ⭐ DEBUG
            println("Error: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}