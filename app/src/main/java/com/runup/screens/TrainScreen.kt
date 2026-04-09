package com.runup.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.runup.AppState
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrenosScreen(navController: NavController) {
    val context = LocalContext.current
    val token = remember {
        context.getSharedPreferences("runup_prefs", Context.MODE_PRIVATE)
            .getString("token", "") ?: ""
    }
    val scope = rememberCoroutineScope()
    var entrenos by remember { mutableStateOf(AppState.entrenosGlobal.toList()) }
    var entrenoAEliminar by remember { mutableStateOf<com.runup.model.Marcador?>(null) }

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
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Mis Entrenos", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))

                if (entrenos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Surface(shape = RoundedCornerShape(25.dp), color = Color.White.copy(alpha = 0.15f)) {
                            Column(modifier = Modifier.padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.DirectionsRun, contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No hay entrenos", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("Pulsa + para añadir uno", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(entrenos) { e ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.DirectionsRun, contentDescription = null,
                                            tint = Color.White, modifier = Modifier.size(26.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = e.nombre, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            StatChip("⏱ ${e.tiempo}")
                                            StatChip("📍 ${e.kilometros} km")
                                            StatChip("🏃 ${e.tipoEntreno}")
                                        }
                                    }
                                    // ← BOTÓN ELIMINAR
                                    IconButton(onClick = { entrenoAEliminar = e }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar",
                                            tint = Color.White.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // ← DIÁLOGO CONFIRMACIÓN — dentro del composable
    entrenoAEliminar?.let { entreno ->
        AlertDialog(
            onDismissRequest = { entrenoAEliminar = null },
            containerColor = Color(0xFF185A9D),
            title = { Text("Eliminar entreno", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("¿Seguro que quieres eliminar ${entreno.nombre}?", color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    AppState.entrenosGlobal.remove(entreno)
                    entrenos = AppState.entrenosGlobal.toList()
                    entrenoAEliminar = null
                    scope.launch {
                        eliminarMarcadorEnBD(entreno.id, token)
                    }
                }) {
                    Text("Eliminar", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { entrenoAEliminar = null }) {
                    Text("Cancelar", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }
}

@Composable
private fun StatChip(text: String) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private suspend fun eliminarMarcadorEnBD(id: String, token: String) = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://bsaldana.difadi.net/api/markers/$id")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            setRequestProperty("Authorization", token)
            connectTimeout = 10000
            readTimeout = 10000
        }
        android.util.Log.d("ENTRENOS", "Eliminado: ${conn.responseCode}")
    } catch (e: Exception) {
        android.util.Log.e("ENTRENOS", "Error delete: ${e.message}")
    }
}