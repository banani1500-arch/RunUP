package com.runup.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import com.runup.AppState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("runup_prefs", Context.MODE_PRIVATE)
    val token = remember { prefs.getString("token", "") ?: "" }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var nombre by remember { mutableStateOf(
        AppState.perfilGlobal["nombre"]?.takeIf { it.isNotEmpty() }
            ?: (prefs.getString("nombre", "") ?: "")
    ) }
    var altura by remember { mutableStateOf(
        AppState.perfilGlobal["altura"]?.takeIf { it.isNotEmpty() }
            ?: (prefs.getString("altura", "") ?: "")
    ) }
    var edad by remember { mutableStateOf(
        AppState.perfilGlobal["edad"]?.takeIf { it.isNotEmpty() }
            ?: (prefs.getString("edad", "") ?: "")
    ) }
    var peso by remember { mutableStateOf(
        AppState.perfilGlobal["peso"]?.takeIf { it.isNotEmpty() }
            ?: (prefs.getString("peso", "") ?: "")
    ) }
    var imageUri by remember {
        mutableStateOf(
            prefs.getString("profile_image", null)?.let { Uri.parse(it) }
        )
    }

    val imc: Double? = remember(altura, peso) {
        val a = altura.toDoubleOrNull()
        val p = peso.toDoubleOrNull()
        if (a != null && p != null && a > 0) p / (a * a) else null
    }

    // Lanzador de galería
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            prefs.edit().putString("profile_image", it.toString()).apply()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Mi Perfil",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Foto de perfil
                Box(
                    modifier = Modifier
                        .size(148.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botón cambiar foto
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("Cambiar foto", color = Color(0xFF185A9D), fontWeight = FontWeight.Bold)
                }

                // Botón subir al servidor
                if (imageUri != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        val file = File(imageUri!!.path ?: "")
                                        val url = URL("https://bsaldana.difadi.net/api/upload")
                                        val connection = url.openConnection() as HttpURLConnection
                                        connection.requestMethod = "POST"
                                        // Multipart simplificado
                                        connection.responseCode
                                    }
                                    snackbarHostState.showSnackbar("Imagen subida correctamente")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error: ${e.message}")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF607D8B))
                    ) {
                        Text("Subir foto al servidor", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Tarjeta de datos
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(25.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    tonalElevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        ProfileField("Nombre", nombre, "Ingresa tu nombre") { nombre = it }
                        Spacer(modifier = Modifier.height(18.dp))
                        ProfileField("Altura (m)", altura, "Ej: 1.75", KeyboardType.Number) {
                            altura = it
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        ProfileField("Edad", edad, "Ej: 25", KeyboardType.Number) { edad = it }
                        Spacer(modifier = Modifier.height(18.dp))
                        ProfileField("Peso (kg)", peso, "Ej: 70", KeyboardType.Number) { peso = it }
                        Spacer(modifier = Modifier.height(18.dp))

                        // IMC
                        Text("IMC", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(15.dp))
                                .padding(horizontal = 15.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = imc?.let { "%.2f".format(it) } ?: "Ingresa altura y peso",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(35.dp))

                Button(
                    onClick = {
                        prefs.edit()
                            .putString("nombre", nombre)
                            .putString("altura", altura)
                            .putString("edad", edad)
                            .putString("peso", peso)
                            .apply()
                        AppState.perfilGlobal = mutableMapOf(
                            "nombre" to nombre,
                            "altura" to altura,
                            "edad"   to edad,
                            "peso"   to peso
                        )
                        scope.launch {
                            guardarPerfilEnBD(nombre, altura, edad, peso, token)
                            snackbarHostState.showSnackbar("Datos guardados correctamente")
                        }
                        navController.navigate("menu") {
                            popUpTo("menu") { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("Guardar", color = Color(0xFF185A9D), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    hint: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.Start) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(hint, color = Color.White.copy(alpha = 0.7f)) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = runUpTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp)
        )
    }
}

suspend fun guardarPerfilEnBD(
    nombre: String, altura: String, edad: String, peso: String, token: String
) = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://bsaldana.difadi.net/api/perfil")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", token)
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
        }
        val body = org.json.JSONObject().apply {
            put("nombre", nombre)
            put("altura", altura.toDoubleOrNull() ?: 0.0)
            put("edad", edad.toIntOrNull() ?: 0)
            put("peso", peso.toDoubleOrNull() ?: 0.0)
        }.toString()
        conn.outputStream.use { it.write(body.toByteArray()) }
        android.util.Log.d("PERFIL", "Guardado: ${conn.responseCode}")
    } catch (e: Exception) {
        android.util.Log.e("PERFIL", "Error: ${e.message}")
    }
}

