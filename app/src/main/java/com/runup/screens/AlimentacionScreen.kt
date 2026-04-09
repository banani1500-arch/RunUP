package com.runup.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class MenuAlimentacion(
    val id: Int = 0,
    val distancia: String,
    val nombre: String,
    val desayuno: String,
    val comida: String,
    val suplementacion: String,
    val cena: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlimentacionScreen(navController: NavController) {
    val context = LocalContext.current
    val token = remember {
        context.getSharedPreferences("runup_prefs", Context.MODE_PRIVATE)
            .getString("token", "") ?: ""
    }
    val scope = rememberCoroutineScope()

    val distancias = listOf("5 km", "10 km", "21 km", "42 km")
    var distanciaSeleccionada by remember { mutableStateOf<String?>(null) }
    var menus by remember { mutableStateOf<List<MenuAlimentacion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isGenerating by remember { mutableStateOf(false) }
    var menuDetalle by remember { mutableStateOf<MenuAlimentacion?>(null) }
    var menuAEliminar by remember { mutableStateOf<MenuAlimentacion?>(null) }

    LaunchedEffect(Unit) {
        menus = cargarMenus(token)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (distanciaSeleccionada == null) "Mi Alimentación"
                        else "Menús para ${distanciaSeleccionada}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (distanciaSeleccionada != null) distanciaSeleccionada = null
                        else navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(gradientBrush)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (distanciaSeleccionada == null) {
            // ── Vista principal: 4 tarjetas de distancia ──────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(distancias) { distancia ->
                    val menusDist = menus.filter { it.distancia == distancia }
                    DistanciaCard(
                        distancia = distancia,
                        numMenus = menusDist.size,
                        onClick = { distanciaSeleccionada = distancia }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        } else {
            // ── Vista menús de una distancia ──────────────────────────────
            val menusFiltrados = menus.filter { it.distancia == distanciaSeleccionada }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Menús predefinidos si no hay ninguno generado
                if (menusFiltrados.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Menú base ${distanciaSeleccionada}",
                                    fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Pulsa el botón para generar tu menú personalizado con IA",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                items(menusFiltrados) { menu ->
                    MenuCard(
                        menu = menu,
                        onClick = { menuDetalle = menu },
                        onDelete = { menuAEliminar = menu }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isGenerating = true
                                val nuevo = generarMenuIA(distanciaSeleccionada!!, token)
                                if (nuevo != null) {
                                    val id = guardarMenuEnBD(nuevo, token)
                                    val conId = nuevo.copy(id = id)
                                    menus = menus + conId
                                }
                                isGenerating = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        enabled = !isGenerating
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(gradientBrush, RoundedCornerShape(10.dp))
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isGenerating) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CircularProgressIndicator(color = Color.White,
                                        modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("Generando menú...", color = Color.White,
                                        fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null,
                                        tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text("Generar menú con IA", color = Color.White,
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Diálogo detalle del menú
    menuDetalle?.let { menu ->
        AlertDialog(
            onDismissRequest = { menuDetalle = null },
            containerColor = Color(0xFF185A9D),
            title = {
                Text(menu.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MenuSeccion("🌅 Desayuno", menu.desayuno)
                    MenuSeccion("☀️ Comida", menu.comida)
                    MenuSeccion("💊 Suplementación", menu.suplementacion)
                    MenuSeccion("🌙 Cena", menu.cena)
                }
            },
            confirmButton = {
                TextButton(onClick = { menuDetalle = null }) {
                    Text("Cerrar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Diálogo eliminar menú
    menuAEliminar?.let { menu ->
        AlertDialog(
            onDismissRequest = { menuAEliminar = null },
            containerColor = Color(0xFF185A9D),
            title = { Text("Eliminar menú", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("¿Eliminar el menú '${menu.nombre}'?", color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    val id = menu.id
                    menus = menus.filter { it.id != id }
                    menuAEliminar = null
                    scope.launch { eliminarMenuEnBD(id, token) }
                }) {
                    Text("Eliminar", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { menuAEliminar = null }) {
                    Text("Cancelar", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }
}

@Composable
private fun DistanciaCard(distancia: String, numMenus: Int, onClick: () -> Unit) {
    val (emoji, color, descripcion) = when (distancia) {
        "5 km"  -> Triple("🏃", Color(0xFF43A047), "Ritmo rápido, recuperación ágil")
        "10 km" -> Triple("🏃", Color(0xFF1976D2), "Resistencia y velocidad")
        "21 km" -> Triple("🏃", Color(0xFF7B1FA2), "Media maratón, carga de carbos")
        else    -> Triple("🏃", Color(0xFFE53935), "Maratón, máxima preparación")
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(56.dp).background(color.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 28.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(distancia, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(descripcion, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (numMenus > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$numMenus menú${if (numMenus > 1) "s" else ""} guardado${if (numMenus > 1) "s" else ""}",
                        fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MenuCard(menu: MenuAlimentacion, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(menu.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Toca para ver el menú completo", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun MenuSeccion(titulo: String, contenido: String) {
    Column {
        Text(titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(3.dp))
        Text(contenido, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 18.sp)
    }
}

// ── API calls ─────────────────────────────────────────────────────────────────

private suspend fun cargarMenus(token: String): List<MenuAlimentacion> = withContext(Dispatchers.IO) {
    try {
        val conn = (URL("https://bsaldana.difadi.net/api/alimentacion").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", token)
            connectTimeout = 10000
            readTimeout = 10000
        }
        if (conn.responseCode == 200) {
            val arr = JSONArray(conn.inputStream.bufferedReader().readText())
            (0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                MenuAlimentacion(
                    id             = j.optInt("id"),
                    distancia      = j.optString("distancia"),
                    nombre         = j.optString("nombre"),
                    desayuno       = j.optString("desayuno"),
                    comida         = j.optString("comida"),
                    suplementacion = j.optString("suplementacion"),
                    cena           = j.optString("cena")
                )
            }
        } else emptyList()
    } catch (e: Exception) {
        android.util.Log.e("ALIMENTACION", "Error cargar: ${e.message}")
        emptyList()
    }
}

private suspend fun generarMenuIA(distancia: String, token: String): MenuAlimentacion? = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://bsaldana.difadi.net/api/generate-menu")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", token)
            connectTimeout = 15000
            readTimeout = 30000
            doOutput = true
        }
        val body = JSONObject().apply { put("distancia", distancia) }.toString()
        conn.outputStream.use { it.write(body.toByteArray()) }

        val code = conn.responseCode
        android.util.Log.d("ALIMENTACION", ">>> HTTP code: $code")

        if (code in 200..299) {
            val response = conn.inputStream.bufferedReader().readText()
            android.util.Log.d("ALIMENTACION", ">>> Respuesta: $response")
            val j = JSONObject(response)
            MenuAlimentacion(
                distancia      = distancia,
                nombre         = j.optString("nombre", "Menú para $distancia"),
                desayuno       = j.optString("desayuno"),
                comida         = j.optString("comida"),
                suplementacion = j.optString("suplementacion"),
                cena           = j.optString("cena")
            )
        } else {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "sin error"
            android.util.Log.e("ALIMENTACION", ">>> ERROR $code: $err")
            null
        }
    } catch (e: Exception) {
        android.util.Log.e("ALIMENTACION", ">>> EXCEPCIÓN: ${e.message}")  // ← añade esto
        null
    }
}

private suspend fun guardarMenuEnBD(menu: MenuAlimentacion, token: String): Int = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://bsaldana.difadi.net/api/alimentacion")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", token)
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
        }
        val body = JSONObject().apply {
            put("distancia",      menu.distancia)
            put("nombre",         menu.nombre)
            put("desayuno",       menu.desayuno)
            put("comida",         menu.comida)
            put("suplementacion", menu.suplementacion)
            put("cena",           menu.cena)
        }.toString()
        conn.outputStream.use { it.write(body.toByteArray()) }
        val response = conn.inputStream.bufferedReader().readText()
        JSONObject(response).optInt("id", 0)
    } catch (e: Exception) {
        android.util.Log.e("ALIMENTACION", "Error guardar: ${e.message}")
        0
    }
}

private suspend fun eliminarMenuEnBD(id: Int, token: String) = withContext(Dispatchers.IO) {
    try {
        val conn = (URL("https://bsaldana.difadi.net/api/alimentacion/$id").openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            setRequestProperty("Authorization", token)
            connectTimeout = 10000
            readTimeout = 10000
        }
        android.util.Log.d("ALIMENTACION", "Eliminado: ${conn.responseCode}")
    } catch (e: Exception) {
        android.util.Log.e("ALIMENTACION", "Error eliminar: ${e.message}")
    }
}