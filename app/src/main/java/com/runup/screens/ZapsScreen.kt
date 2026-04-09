package com.runup.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import android.content.Context
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.platform.LocalContext
import com.runup.AppState
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
import androidx.navigation.NavController
import androidx.compose.material.icons.filled.ArrowBack

data class Zapa(
    val id: Int = 0,
    val titulo: String,
    val imageUri: Uri?,
    val km: String = "",
    val fecha: String = ""
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZapasScreen(navController: NavController) {
    val context = LocalContext.current
    val token = remember {
        context.getSharedPreferences("runup_prefs", Context.MODE_PRIVATE)
            .getString("token", "") ?: ""
    }

    val scope = rememberCoroutineScope()
    var zapas by remember { mutableStateOf(AppState.zapatillasGlobal.toList()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedZapa by remember { mutableStateOf<Zapa?>(null) }
    var zapaAEliminar by remember { mutableStateOf<Zapa?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Mis Zapatillas!",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(25.dp))

                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF185A9D))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Crear nueva tarjeta",
                        color = Color(0xFF185A9D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(25.dp))

                zapas.forEach { zapa ->
                    ZapaCard(
                        zapa = zapa,
                        onClick = { selectedZapa = zapa },
                        onDelete = {zapaAEliminar = zapa }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateZapaDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { nueva ->
                showCreateDialog = false
                scope.launch {
                    val id = guardarZapaEnBD(nueva, token)
                    val zapaConId = nueva.copy(id = id)
                    AppState.zapatillasGlobal.add(zapaConId)
                    zapas = AppState.zapatillasGlobal.toList()
                }
            }
        )
    }

    selectedZapa?.let { zapa ->
        ZapaDetailsDialog(
            zapa = zapa,
            onDismiss = { selectedZapa = null },
            onGuardar = { km, fecha ->
                val zapaActualizada = zapa.copy(km = km, fecha = fecha)
                val idx = AppState.zapatillasGlobal.indexOf(zapa)
                if (idx >= 0) AppState.zapatillasGlobal[idx] = zapaActualizada
                zapas = AppState.zapatillasGlobal.toList()
                selectedZapa = null
                scope.launch {
                    actualizarZapaEnBD(zapaActualizada, token)  // ← PUT en vez de POST
                }
            }
        )
    }

    zapaAEliminar?.let { zapa ->
        AlertDialog(
            onDismissRequest = { zapaAEliminar = null },
            containerColor = Color(0xFF185A9D),
            title = { Text("Eliminar zapatilla", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("¿Seguro que quieres eliminar ${zapa.titulo}?", color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    AppState.zapatillasGlobal.remove(zapa)
                    zapas = AppState.zapatillasGlobal.toList()
                    zapaAEliminar = null
                    scope.launch {
                        eliminarZapaEnBD(zapa.id, token)
                    }
                }) {
                    Text("Eliminar", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { zapaAEliminar = null }) {
                    Text("Cancelar", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }

}

@Composable
fun ZapaCard(zapa: Zapa, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(25.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Column {
            if (zapa.imageUri != null) {
                AsyncImage(
                    model = zapa.imageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .background(Color.White.copy(alpha = 0.24f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(50.dp))
                }
            }
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(zapa.titulo, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.White.copy(alpha = 0.7f))
                    }
                }
                if (zapa.km.isNotEmpty() || zapa.fecha.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Text("Km: ${zapa.km.ifEmpty { "-" }}, Fecha: ${zapa.fecha.ifEmpty { "-" }}",
                        color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun CreateZapaDialog(onDismiss: () -> Unit, onCreate: (Zapa) -> Unit) {
    var titulo by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        imageUri = it
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF185A9D),
        title = { Text("Nueva tarjeta", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título", color = Color.White) },
                    placeholder = { Text("Ej: Nike VaporFly 3", color = Color.White.copy(alpha = 0.7f)) },
                    colors = runUpTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp)
                )

                // Vista previa imagen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(15.dp))
                        )
                    } else {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                }

                Button(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("Seleccionar foto", color = Color(0xFF185A9D), fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (titulo.isNotEmpty() && imageUri != null) {
                    onCreate(Zapa(titulo = titulo, imageUri = imageUri))
                }
            }) { Text("Crear", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.White.copy(alpha = 0.7f)) }
        }
    )
}

@Composable
fun ZapaDetailsDialog(zapa: Zapa, onDismiss: () -> Unit, onGuardar: (String, String) -> Unit) {
    var km by remember { mutableStateOf(zapa.km) }
    var fecha by remember { mutableStateOf(zapa.fecha) }
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF185A9D),
        title = { Text("Detalles de la tarjeta", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = km,
                    onValueChange = { km = it },
                    label = { Text("Kilómetros", color = Color.White) },
                    placeholder = { Text("Ej: 150", color = Color.White.copy(alpha = 0.7f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = runUpTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp)
                )
                OutlinedTextField(
                    value = fecha,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha de compra", color = Color.White) },
                    placeholder = { Text("Selecciona fecha", color = Color.White.copy(alpha = 0.7f)) },
                    trailingIcon = {
                        IconButton(onClick = {
                            val cal = Calendar.getInstance()
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    fecha = "%04d-%02d-%02d".format(year, month + 1, day)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.White)
                        }
                    },
                    colors = runUpTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onGuardar(km, fecha) }) {
                Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

suspend fun guardarZapaEnBD(zapa: Zapa, token: String): Int =
    withContext(Dispatchers.IO) {
        try {
            val url = URL("https://bsaldana.difadi.net/api/zapatillas")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", token)
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
            }
            val partes = zapa.titulo.split(" ", limit = 2)
            val body = JSONObject().apply {
                put("marca", partes.getOrElse(0) { zapa.titulo })
                put("modelo", partes.getOrElse(1) { "" })
                put("kilometros_acumulados", zapa.km.toDoubleOrNull() ?: 0.0)
                put("fecha_compra", zapa.fecha.ifEmpty { null })
                put("notas", "")
            }.toString()
            conn.outputStream.use { it.write(body.toByteArray()) }
            val response = conn.inputStream.bufferedReader().readText()
            android.util.Log.d("ZAPAS", "Guardada: ${conn.responseCode}")
            JSONObject(response).optInt("id", 0)  // ← devuelve el id
        } catch (e: Exception) {
            android.util.Log.e("ZAPAS", "Error: ${e.message}")
            0  // ← devuelve 0 si hay error
        }
    }

suspend fun actualizarZapaEnBD(zapa: Zapa, token: String) = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://bsaldana.difadi.net/api/zapatillas/${zapa.id}")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", token)
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
        }
        val body = JSONObject().apply {
            put("kilometros_acumulados", zapa.km.toDoubleOrNull() ?: 0.0)
            put("fecha_compra", zapa.fecha.ifEmpty { null })
        }.toString()
        conn.outputStream.use { it.write(body.toByteArray()) }
        android.util.Log.d("ZAPAS", "Actualizada: ${conn.responseCode}")
    } catch (e: Exception) {
        android.util.Log.e("ZAPAS", "Error update: ${e.message}")
    }
}

suspend fun eliminarZapaEnBD(id: Int, token: String) = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://bsaldana.difadi.net/api/zapatillas/$id")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            setRequestProperty("Authorization", token)
            connectTimeout = 10000
            readTimeout = 10000
        }
        android.util.Log.d("ZAPAS", "Eliminada: ${conn.responseCode}")
    } catch (e: Exception) {
        android.util.Log.e("ZAPAS", "Error delete: ${e.message}")
    }
}


