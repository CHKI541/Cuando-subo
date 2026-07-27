package com.ejemplo.cuandosubo.ui.colectivos

import android.content.Context
import org.json.JSONArray
import java.io.InputStreamReader

object StopsDatabase {
    private var stops: List<StaticStopInfo>? = null

    fun initialize(context: Context) {
        if (stops != null) return
        try {
            context.assets.open("stops.json").use { stream ->
                InputStreamReader(stream, "UTF-8").use { reader ->
                    val text = reader.readText()
                    val array = JSONArray(text)
                    val list = ArrayList<StaticStopInfo>(array.length())
                    for (i in 0 until array.length()) {
                        val stopVal = array.getJSONArray(i)
                        list.add(StaticStopInfo(
                            id = stopVal.getString(0),
                            name = stopVal.getString(1),
                            lat = stopVal.getDouble(2),
                            lon = stopVal.getDouble(3)
                        ))
                    }
                    stops = list
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getNearestStops(lat: Double, lon: Double, limit: Int = 12): List<StaticStopInfo> {
        val currentStops = stops ?: return emptyList()
        return currentStops
            .map { stop ->
                val dLat = stop.lat - lat
                val dLon = stop.lon - lon
                val distSq = dLat * dLat + dLon * dLon
                Pair(stop, distSq)
            }
            .sortedBy { it.second }
            .take(limit)
            .map { it.first }
    }
}

data class StaticStopInfo(val id: String, val name: String, val lat: Double, val lon: Double)
