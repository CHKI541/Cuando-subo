package com.ejemplo.cuandosubo.ui.colectivos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException

/** Error con un mensaje listo para mostrarle al usuario. */
class ColectivosApiException(message: String, val code: String = "") : Exception(message)

object ColectivosApiClient {
    // FIX (auditoría): la URL anterior apuntaba a
    // "us-central1-locksuite-nueva-default-rtdb.cloudfunctions.net" (mezcla del hostname
    // de Realtime Database con el de Cloud Functions) y devolvía 404 siempre.
    // Esta es la URL real y verificada de la Cloud Function "colectivosApi".
    private const val BASE_URL = "https://us-central1-locksuite-nueva.cloudfunctions.net/colectivosApi"

    // La función puede tardar unos segundos si tiene que probar varias fuentes (web de SUBE,
    // API del GCBA) o si arranca en frío: 12 s de lectura quedaba corto.
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 25_000

    /**
     * GET a la Cloud Function. Si responde con error, usa el mensaje "error" del JSON
     * (ya viene en castellano para el usuario) en vez de mostrar solo "HTTP 500".
     */
    private fun getJson(params: String): JSONObject {
        val connection = try {
            URL("$BASE_URL?$params").openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw ColectivosApiException("No se pudo conectar con el servidor.")
        }
        connection.requestMethod = "GET"
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/json")
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code in 200..299) {
                return try {
                    JSONObject(text)
                } catch (e: Exception) {
                    throw ColectivosApiException("Respuesta inválida del servidor.")
                }
            }
            val serverJson = try { JSONObject(text) } catch (e: Exception) { null }
            val message = serverJson?.optString("error").orEmpty()
            throw ColectivosApiException(
                if (message.isNotBlank()) message else "El servidor respondió con un error (HTTP $code).",
                serverJson?.optString("code").orEmpty()
            )
        } catch (e: ColectivosApiException) {
            throw e
        } catch (e: SocketTimeoutException) {
            throw ColectivosApiException("El servidor tardó demasiado en responder. Probá de nuevo.")
        } catch (e: UnknownHostException) {
            throw ColectivosApiException("Sin conexión a internet.")
        } catch (e: IOException) {
            throw ColectivosApiException("No se pudo conectar con el servidor.")
        } finally {
            connection.disconnect()
        }
    }

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")

    suspend fun buscarLinea(query: String): List<RouteInfo> = withContext(Dispatchers.IO) {
        val json = getJson("action=buscarLinea&query=${enc(query.trim())}")
        val routesJson = json.optJSONArray("routes") ?: return@withContext emptyList()
        val routes = mutableListOf<RouteInfo>()
        for (i in 0 until routesJson.length()) {
            val r = routesJson.getJSONObject(i)
            routes.add(
                RouteInfo(
                    id = r.getString("id"),
                    shortName = r.optString("shortName"),
                    longName = r.optString("longName")
                )
            )
        }
        routes
    }

    suspend fun obtenerParadas(routeId: String): List<StopInfo> = withContext(Dispatchers.IO) {
        val json = getJson("action=obtenerParadas&routeId=${enc(routeId)}")
        val stopsJson = json.optJSONArray("stops") ?: return@withContext emptyList()
        val stops = mutableListOf<StopInfo>()
        for (i in 0 until stopsJson.length()) {
            val s = stopsJson.getJSONObject(i)
            stops.add(
                StopInfo(
                    id = s.getString("id"),
                    name = s.optString("name", "Parada"),
                    direction = s.optInt("direction", -1),
                    headsign = s.optString("headsign")
                )
            )
        }
        stops
    }

    suspend fun obtenerArribos(stopId: String): ArrivalsResult = withContext(Dispatchers.IO) {
        val json = getJson("action=obtenerArribos&stopId=${enc(stopId)}")
        val arrivalsJson = json.optJSONArray("arrivals")
        val arrivals = mutableListOf<ArrivalInfo>()
        if (arrivalsJson != null) {
            for (i in 0 until arrivalsJson.length()) {
                val a = arrivalsJson.getJSONObject(i)
                arrivals.add(
                    ArrivalInfo(
                        route = a.optString("route"),
                        destination = a.optString("destination"),
                        arrivalTime = a.optString("arrivalTime"),
                        minutes = a.optInt("minutes", 0),
                        isLive = a.optBoolean("isLive", false)
                    )
                )
            }
        }
        ArrivalsResult(arrivals = arrivals, source = json.optString("source"))
    }

    suspend fun geocodeAddress(query: String): List<GeocodedAddress> = withContext(Dispatchers.IO) {
        val urlStr = "https://servicios.usig.buenosaires.gob.ar/normalizar/?direccion=${enc(query)}&geocodificar=true"
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
                throw ColectivosApiException("El buscador de direcciones del GCBA no respondió (HTTP ${connection.responseCode}).")
            }
        } catch (e: ColectivosApiException) {
            throw e
        } catch (e: SocketTimeoutException) {
            throw ColectivosApiException("El buscador de direcciones tardó demasiado en responder.")
        } catch (e: UnknownHostException) {
            throw ColectivosApiException("Sin conexión a internet.")
        } catch (e: IOException) {
            throw ColectivosApiException("No se pudo conectar con el buscador de direcciones.")
        } finally {
            connection.disconnect()
        }
    }
}

/** Quita el prefijo de agencia ("14_", "82_") para mostrar el código de la parada. */
fun codigoParada(stopId: String): String = stopId.replace(Regex("^\\d+_"), "")

data class RouteInfo(val id: String, val shortName: String, val longName: String)
data class StopInfo(val id: String, val name: String, val direction: Int = -1, val headsign: String = "")
data class ArrivalInfo(val route: String, val destination: String, val arrivalTime: String, val minutes: Int, val isLive: Boolean)
data class ArrivalsResult(val arrivals: List<ArrivalInfo>, val source: String)
data class GeocodedAddress(val name: String, val lat: Double, val lon: Double)
