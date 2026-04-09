package com.runup.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.runup.AppState
import com.runup.model.Marcador
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun MapScreen(
    marcadoresIniciales: List<Marcador> = emptyList(),
    onGuardarEntrenos: ((List<Marcador>) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initialPosition = LatLng(42.343316, -3.758292)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 12f)
    }

    var markers by remember {
        mutableStateOf(AppState.entrenosGlobal.map { it.posicion to it.id }.toSet())
    }
    var showDialog by remember { mutableStateOf(false) }
    var tappedPosition by remember { mutableStateOf<LatLng?>(null) }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(mapType = MapType.HYBRID),
        onMapClick = { latLng ->
            tappedPosition = latLng
            showDialog = true
        }
    ) {
        AppState.entrenosGlobal.forEach { marcador ->
            Marker(
                state = MarkerState(position = marcador.posicion),
                title = marcador.nombre,
                icon = BitmapDescriptorFactory.defaultMarker(
                    when (marcador.tipoEntreno) {
                        "Series"  -> BitmapDescriptorFactory.HUE_RED
                        "Rodaje"  -> BitmapDescriptorFactory.HUE_GREEN
                        "Fartlek" -> BitmapDescriptorFactory.HUE_ORANGE
                        else      -> BitmapDescriptorFactory.HUE_VIOLET
                    }
                )
            )
        }
    }

    if (showDialog && tappedPosition != null) {
        AddMarkerDialog(
            posicion = tappedPosition!!,
            onDismiss = { showDialog = false },
            onGuardar = { marcador ->
                AppState.entrenosGlobal.add(marcador)
                markers = AppState.entrenosGlobal.map { it.posicion to it.id }.toSet()
                showDialog = false
                onGuardarEntrenos?.invoke(AppState.entrenosGlobal)

                val token = context.getSharedPreferences("runup_prefs", Context.MODE_PRIVATE)
                    .getString("token", "") ?: ""

                scope.launch { guardarMarcadorEnServidor(marcador, token) }
            }
        )
    }
}

@Composable
fun AddMarkerDialog(
    posicion: LatLng,
    onDismiss: () -> Unit,
    onGuardar: (Marcador) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var tiempo by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("") }
    var km by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Marcador") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tiempo,
                    onValueChange = { tiempo = it },
                    label = { Text("Tiempo") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tipo,
                    onValueChange = { tipo = it },
                    label = { Text("Tipo de entreno") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = km,
                    onValueChange = { km = it },
                    label = { Text("Kilómetros") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nombre.isNotEmpty()) {
                    val marcador = Marcador(
                        id = System.currentTimeMillis().toString(),
                        nombre = nombre,
                        tiempo = tiempo,
                        tipoEntreno = tipo,
                        kilometros = km.toDoubleOrNull() ?: 0.0,
                        posicion = posicion,
                        sensaciones = ""
                    )
                    onGuardar(marcador)
                }
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private suspend fun guardarMarcadorEnServidor(marcador: Marcador, token: String) =
    withContext(Dispatchers.IO) {
        try {
            val url = URL("https://bsaldana.difadi.net/api/markers")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", token)
                doOutput = true
            }
            val body = JSONObject().apply {
                put("title", marcador.nombre)
                put("tiempo", marcador.tiempo)
                put("tipoEntreno", marcador.tipoEntreno)
                put("kilometros", marcador.kilometros)
                put("lat", marcador.posicion.latitude)
                put("lng", marcador.posicion.longitude)
            }.toString()
            connection.outputStream.use { it.write(body.toByteArray()) }
            connection.responseCode
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }