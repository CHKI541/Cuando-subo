package com.ejemplo.cuandosubo.ui.colectivos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object ColectivosApiClient {
    private const val BASE_URL = "https://us-central1-locksuite-nueva-default-rtdb.cloudfunctions.net/colectivosApi"

    suspend fun buscarLinea(query: String): List<RouteInfo> = withContext(Dispatchers.IO) {
        val urlStr = "$BASE_URL?action=buscarLinea&query=${URLEncoder.encode(query, "UTF-8")}"
        val connection = URL(urlStr).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 12000
        connection.readTimeout = 12000
        
        try {
            if (connection.responseCode == 200) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                val routesJson = json.getJSONArray("routes")
                val routes = mutableListOf<RouteInfo>()
                for (i in 0 until routesJson.length()) {
                    val r = routesJson.getJSONObject(i)
                    routes.add(RouteInfo(
                        id = r.getString("id"),
                        shortName = r.getString("shortName"),
                        longName = r.getString("longName")
                    ))
                }
                return@withContext routes
            } else {
                throw Exception("HTTP ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun obtenerParadas(routeId: String): List<StopInfo> = withContext(Dispatchers.IO) {
        val urlStr = "$BASE_URL?action=obtenerParadas&routeId=${URLEncoder.encode(routeId, "UTF-8")}"
        val connection = URL(urlStr).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 12000
        connection.readTimeout = 12000
        
        try {
            if (connection.responseCode == 200) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                val stopsJson = json.getJSONArray("stops")
                val stops = mutableListOf<StopInfo>()
                for (i in 0 until stopsJson.length()) {
                    val s = stopsJson.getJSONObject(i)
                    stops.add(StopInfo(
                        id = s.getString("id"),
                        name = s.getString("name")
                    ))
                }
                return@withContext stops
            } else {
                throw Exception("HTTP ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun obtenerArribos(stopId: String): List<ArrivalInfo> = withContext(Dispatchers.IO) {
        val urlStr = "$BASE_URL?action=obtenerArribos&stopId=${URLEncoder.encode(stopId, "UTF-8")}"
        val connection = URL(urlStr).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 12000
        connection.readTimeout = 12000
        
        try {
            if (connection.responseCode == 200) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                val arrivalsJson = json.getJSONArray("arrivals")
                val arrivals = mutableListOf<ArrivalInfo>()
                for (i in 0 until arrivalsJson.length()) {
                    val a = arrivalsJson.getJSONObject(i)
                    arrivals.add(ArrivalInfo(
                        route = a.getString("route"),
                        destination = a.getString("destination"),
                        arrivalTime = a.getString("arrivalTime"),
                        minutes = a.getInt("minutes"),
                        isLive = a.getBoolean("isLive")
                    ))
                }
                return@withContext arrivals
            } else {
                throw Exception("HTTP ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }
    
    suspend fun geocodeAddress(query: String): List<GeocodedAddress> = withContext(Dispatchers.IO) {
        val urlStr = "https://servicios.usig.buenosaires.gob.ar/normalizar/?direccion=${URLEncoder.encode(query, "UTF-8")}&geocodificar=true"
        val connection = URL(urlStr).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        try {
            if (connection.responseCode == 200) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                val addresses = mutableListOf<GeocodedAddress>()
                val direccionesNormalizadas = json.optJSONArray("direccionesNormalizadas") ?: return@withContext emptyList()
                for (i in 0 until direccionesNormalizadas.length()) {
                    val dir = direccionesNormalizadas.getJSONObject(i)
                    val coordenadas = dir.optJSONObject("coordenadas")
                    if (coordenadas != null) {
                        addresses.add(GeocodedAddress(
                            name = dir.getString("direccion"),
                            lat = coordenadas.optDouble("y", 0.0),
                            lon = coordenadas.optDouble("x", 0.0)
                        ))
                    }
                }
                return@withContext addresses
            } else {
                throw Exception("HTTP ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }
}

data class RouteInfo(val id: String, val shortName: String, val longName: String)
data class StopInfo(val id: String, val name: String)
data class ArrivalInfo(val route: String, val destination: String, val arrivalTime: String, val minutes: Int, val isLive: Boolean)
data class GeocodedAddress(val name: String, val lat: Double, val lon: Double)
