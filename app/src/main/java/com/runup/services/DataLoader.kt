package com.runup.services

import com.runup.AppState
import com.runup.model.Marcador
import com.runup.model.EntrenoDay
import com.runup.screens.Zapa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

object DataLoader {

    private const val BASE = "https://bsaldana.difadi.net/api"

    suspend fun cargarTodo(token: String) = withContext(Dispatchers.IO) {
        cargarMarkers(token)
        cargarCalendario(token)
        cargarZapatillas(token)
        cargarPerfil(token)
    }

    private fun get(endpoint: String, token: String): JSONArray? {
        return try {
            val conn = (URL("$BASE/$endpoint").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", token)
                connectTimeout = 10000
                readTimeout = 10000
            }
            if (conn.responseCode == 200) {
                JSONArray(conn.inputStream.bufferedReader().readText())
            } else null
        } catch (e: Exception) {
            android.util.Log.e("DATALOADER", "Error GET $endpoint: ${e.message}")
            null
        }
    }

    private fun cargarPerfil(token: String) {
        try {
            val conn = (URL("$BASE/perfil").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", token)
                connectTimeout = 10000
                readTimeout = 10000
            }
            if (conn.responseCode == 200) {
                val j = org.json.JSONObject(conn.inputStream.bufferedReader().readText())
                AppState.perfilGlobal = mutableMapOf(
                    "nombre" to (j.optString("name") ?: ""),
                    "altura" to (j.optString("altura") ?: ""),
                    "edad"   to (j.optString("edad") ?: ""),
                    "peso"   to (j.optString("peso") ?: "")
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("DATALOADER", "Error perfil: ${e.message}")
        }
    }

    private fun cargarMarkers(token: String) {
        val arr = get("markers", token) ?: return
        AppState.entrenosGlobal.clear()
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            AppState.entrenosGlobal.add(
                Marcador(
                    id          = j.optString("id"),
                    nombre      = j.optString("title"),
                    tiempo      = j.optString("tiempo"),
                    tipoEntreno = j.optString("tipoEntreno"),
                    sensaciones = j.optString("sensaciones"),
                    kilometros  = j.optDouble("kilometros", 0.0),
                    posicion    = com.google.android.gms.maps.model.LatLng(
                        j.optDouble("lat", 0.0),
                        j.optDouble("lng", 0.0)
                    )
                )
            )
        }
        android.util.Log.d("DATALOADER", "Markers cargados: ${AppState.entrenosGlobal.size}")
    }

    private fun cargarCalendario(token: String) {
        val arr = get("calendario", token) ?: return
        AppState.calendarioGlobal.clear()
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            try {
                AppState.calendarioGlobal.add(
                    EntrenoDay(
                        id          = j.optInt("id"),
                        fecha       = LocalDate.parse(j.optString("fecha")),
                        tipo        = j.optString("tipo"),
                        descripcion = j.optString("descripcion")
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("DATALOADER", "Error parseando fecha: ${e.message}")
            }
        }
        android.util.Log.d("DATALOADER", "Calendario cargado: ${AppState.calendarioGlobal.size}")
    }

    private fun cargarZapatillas(token: String) {
        val arr = get("zapatillas", token) ?: return
        AppState.zapatillasGlobal.clear()
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            AppState.zapatillasGlobal.add(
                Zapa(
                    id     = j.optInt("id"),
                    titulo   = "${j.optString("marca")} ${j.optString("modelo")}".trim(),
                    imageUri = null,
                    km       = j.optString("kilometros_acumulados"),
                    fecha    = j.optString("fecha_compra")
                )
            )
        }
        android.util.Log.d("DATALOADER", "Zapatillas cargadas: ${AppState.zapatillasGlobal.size}")
    }
}