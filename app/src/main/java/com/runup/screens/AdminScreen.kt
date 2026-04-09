package com.runup.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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

data class UsuarioAdmin(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val createdAt: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(navController: NavController) {
    val context = LocalContext.current
    val token = remember {
        context.getSharedPreferences("runup_prefs", Context.MODE_PRIVATE)
            .getString("token", "") ?: ""
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var usuarios by remember { mutableStateOf<List<UsuarioAdmin>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var usuarioSeleccionado by remember { mutableStateOf<UsuarioAdmin?>(null) }
    var usuarioAEliminar by remember { mutableStateOf<UsuarioAdmin?>(null) }
    var showDatosDialog by remember { mutableStateOf(false) }
    var showRolDialog by remember { mutableStateOf(false) }
    var datosUsuario by remember { mutableStateOf<JSONObject?>(null) }

    // Cargar usuarios al entrar
    LaunchedEffect(Unit) {
        isLoading = true
        usuarios = cargarUsuarios(token)
        isLoading = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Panel de administración", color = Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    Text("${usuarios.size} usuarios registrados",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                items(usuarios) { usuario ->
                    UsuarioCard(
                        usuario = usuario,
                        onVerDatos = {
                            usuarioSeleccionado = usuario
                            showDatosDialog = true
                            scope.launch {
                                datosUsuario = cargarDatosUsuario(usuario.id, token)
                            }
                        },
                        onCambiarRol = {
                            usuarioSeleccionado = usuario
                            showRolDialog = true
                        },
                        onEliminar = { usuarioAEliminar = usuario }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    // Diálogo ver datos del usuario
    if (showDatosDialog && usuarioSeleccionado != null) {
        AlertDialog(
            onDismissRequest = { showDatosDialog = false; datosUsuario = null },
            containerColor = Color(0xFF185A9D),
            title = {
                Text("Datos de ${usuarioSeleccionado!!.name}",
                    color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                if (datosUsuario == null) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    }
                } else {
                    val datos = datosUsuario!!
                    val markers = datos.optJSONArray("markers")?.length() ?: 0
                    val zapas = datos.optJSONArray("zapatillas")?.length() ?: 0
                    val calendario = datos.optJSONArray("calendario")?.length() ?: 0
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DatoRow("Email", usuarioSeleccionado!!.email)
                        DatoRow("Rol", usuarioSeleccionado!!.role)
                        DatoRow("Registro", usuarioSeleccionado!!.createdAt.take(10))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                        DatoRow("Entrenos en mapa", "$markers")
                        DatoRow("Zapatillas", "$zapas")
                        DatoRow("Días de calendario", "$calendario")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDatosDialog = false; datosUsuario = null }) {
                    Text("Cerrar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Diálogo cambiar rol
    if (showRolDialog && usuarioSeleccionado != null) {
        val nuevoRol = if (usuarioSeleccionado!!.role == "admin") "user" else "admin"
        AlertDialog(
            onDismissRequest = { showRolDialog = false },
            containerColor = Color(0xFF185A9D),
            title = { Text("Cambiar rol", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "¿Cambiar el rol de ${usuarioSeleccionado!!.name} de '${usuarioSeleccionado!!.role}' a '$nuevoRol'?",
                    color = Color.White
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val uid = usuarioSeleccionado!!.id
                    showRolDialog = false
                    scope.launch {
                        val ok = cambiarRolUsuario(uid, nuevoRol, token)
                        if (ok) {
                            usuarios = cargarUsuarios(token)
                            snackbarHostState.showSnackbar("Rol actualizado correctamente")
                        } else {
                            snackbarHostState.showSnackbar("Error al cambiar el rol")
                        }
                    }
                }) {
                    Text("Confirmar", color = Color(0xFF43CEA2), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRolDialog = false }) {
                    Text("Cancelar", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }

    // Diálogo eliminar usuario
    usuarioAEliminar?.let { usuario ->
        AlertDialog(
            onDismissRequest = { usuarioAEliminar = null },
            containerColor = Color(0xFF185A9D),
            title = { Text("Eliminar usuario", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "¿Eliminar a ${usuario.name} (${usuario.email})?\nSe eliminarán todos sus datos.",
                    color = Color.White
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val uid = usuario.id
                    usuarioAEliminar = null
                    scope.launch {
                        val ok = eliminarUsuario(uid, token)
                        if (ok) {
                            usuarios = cargarUsuarios(token)
                            snackbarHostState.showSnackbar("Usuario eliminado")
                        } else {
                            snackbarHostState.showSnackbar("Error al eliminar")
                        }
                    }
                }) {
                    Text("Eliminar", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { usuarioAEliminar = null }) {
                    Text("Cancelar", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }
}

@Composable
private fun UsuarioCard(
    usuario: UsuarioAdmin,
    onVerDatos: () -> Unit,
    onCambiarRol: () -> Unit,
    onEliminar: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (usuario.role == "admin") Color(0xFF185A9D) else Color(0xFF43CEA2),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        usuario.name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(usuario.name, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground)
                    Text(usuario.email, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (usuario.role == "admin") Color(0xFF185A9D) else Color(0xFF43CEA2).copy(alpha = 0.2f)
                ) {
                    Text(
                        usuario.role,
                        fontSize = 11.sp,
                        color = if (usuario.role == "admin") Color.White else Color(0xFF0F6E56),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Ver datos
                OutlinedButton(
                    onClick = onVerDatos,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null,
                        modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Datos", fontSize = 11.sp)
                }
                // Cambiar rol
                OutlinedButton(
                    onClick = onCambiarRol,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null,
                        modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rol", fontSize = 11.sp)
                }
                // Eliminar
                OutlinedButton(
                    onClick = onEliminar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null,
                        modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Borrar", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun DatoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Llamadas a la API ─────────────────────────────────────────────────────────

private suspend fun cargarUsuarios(token: String): List<UsuarioAdmin> = withContext(Dispatchers.IO) {
    try {
        val conn = (URL("https://bsaldana.difadi.net/api/users").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", token)
            connectTimeout = 10000
            readTimeout = 10000
        }
        if (conn.responseCode == 200) {
            val arr = JSONArray(conn.inputStream.bufferedReader().readText())
            (0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                UsuarioAdmin(
                    id        = j.optInt("id"),
                    name      = j.optString("name"),
                    email     = j.optString("email"),
                    role      = j.optString("role"),
                    createdAt = j.optString("created_at")
                )
            }
        } else emptyList()
    } catch (e: Exception) {
        android.util.Log.e("ADMIN", "Error cargar usuarios: ${e.message}")
        emptyList()
    }
}

private suspend fun cargarDatosUsuario(id: Int, token: String): JSONObject? = withContext(Dispatchers.IO) {
    try {
        val conn = (URL("https://bsaldana.difadi.net/api/users/$id/datos").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", token)
            connectTimeout = 10000
            readTimeout = 10000
        }
        if (conn.responseCode == 200) JSONObject(conn.inputStream.bufferedReader().readText())
        else null
    } catch (e: Exception) {
        android.util.Log.e("ADMIN", "Error datos usuario: ${e.message}")
        null
    }
}

private suspend fun cambiarRolUsuario(id: Int, nuevoRol: String, token: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val conn = (URL("https://bsaldana.difadi.net/api/users/$id/role").openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", token)
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
        }
        val body = JSONObject().apply { put("role", nuevoRol) }.toString()
        conn.outputStream.use { it.write(body.toByteArray()) }
        conn.responseCode == 200
    } catch (e: Exception) {
        android.util.Log.e("ADMIN", "Error cambiar rol: ${e.message}")
        false
    }
}

private suspend fun eliminarUsuario(id: Int, token: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val conn = (URL("https://bsaldana.difadi.net/api/users/$id").openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            setRequestProperty("Authorization", token)
            connectTimeout = 10000
            readTimeout = 10000
        }
        conn.responseCode == 200
    } catch (e: Exception) {
        android.util.Log.e("ADMIN", "Error eliminar usuario: ${e.message}")
        false
    }
}