package com.runup.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.location.LocationServices
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(navController: NavController) {

    val context = LocalContext.current
    val role = context.getSharedPreferences("runup_prefs", Context.MODE_PRIVATE)
        .getString("role", "user") ?: "user"
    android.util.Log.d("MENU_DEBUG", "Role leído: $role")
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    //Variable para que el puntero de localización vaya a nuestra posición
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var selectedMarcador by remember { mutableStateOf<Marcador?>(null) }
    var showFormDialog by remember { mutableStateOf(false) }
    var tappedPosition by remember { mutableStateOf<LatLng?>(null) }
    var fcmActive by remember { mutableStateOf(true) }

    val initialPosition = LatLng(40.4168, -3.7038)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 14f)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.background(gradientBrush),
                drawerContainerColor = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradientBrush)
                        .padding(vertical = 40.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(55.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tus movidas",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                DrawerItem(icon = Icons.Default.Person, label = "Perfil") {
                    scope.launch { drawerState.close() }
                    navController.navigate("profile")
                }
                DrawerItem(icon = Icons.Default.FitnessCenter, label = "Zapas") {
                    scope.launch { drawerState.close() }
                    navController.navigate("zapas")
                }
                DrawerItem(icon = Icons.Default.FitnessCenter, label = "Entrenos") {
                    scope.launch { drawerState.close() }
                    navController.navigate("entrenos")
                }
                DrawerItem(icon = Icons.Default.DirectionsRun, label = "Carreras") {
                    scope.launch { drawerState.close() }
                    navController.navigate("carreras")
                }
                DrawerItem(icon = Icons.Default.DateRange, label = "Planificación") {
                    scope.launch { drawerState.close() }
                    navController.navigate("planificacion")
                }
                DrawerItem(icon = Icons.Default.Eco, label = "Alimentación") {
                    scope.launch { drawerState.close() }
                    navController.navigate("alimentacion")
                }

                if (role == "admin") {
                    DrawerItem(icon = Icons.Default.AdminPanelSettings, label = "Admin") {
                        scope.launch { drawerState.close() }
                        navController.navigate("admin")
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = Color.White.copy(alpha = 0.3f)
                )
                DrawerItem(icon = Icons.Default.Logout, label = "Cerrar sesión") {
                    scope.launch { drawerState.close() }
                    // Limpiar datos locales
                    context.getSharedPreferences("runup_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                    AppState.entrenosGlobal.clear()
                    AppState.zapatillasGlobal.clear()
                    AppState.calendarioGlobal.clear()
                    AppState.perfilGlobal = mutableMapOf()
                    // Navegar al login
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                 }
                }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("RunUp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.background(gradientBrush)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        if (hasLocationPermission) {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                location?.let {
                                    val userLatLng = LatLng(it.latitude, it.longitude)
                                    scope.launch {
                                        cameraPositionState.animate(
                                            update = com.google.android.gms.maps.CameraUpdateFactory
                                                .newCameraPosition(
                                                    CameraPosition.fromLatLngZoom(userLatLng, 16f)
                                                ),
                                            durationMs = 800
                                        )
                                    }
                                }
                            }
                        }else {
                            locationPermissionLauncher.launch(
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        }
                    },
                    containerColor = Color(0xFF43CEA2),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Mapa (70%)
                Box(modifier = Modifier.weight(0.7f)) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            mapType = mapType,
                            isMyLocationEnabled = hasLocationPermission
                        ),
                        uiSettings = MapUiSettings(
                            myLocationButtonEnabled = false,
                            zoomControlsEnabled = false
                        ),
                        onMapClick = { latLng ->
                            tappedPosition = latLng
                            showFormDialog = true
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
                                ),
                                onClick = {
                                    selectedMarcador = marcador
                                    false
                                }
                            )
                        }
                    } //

                    // Botón satélite — dentro del Box, fuera del GoogleMap
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                mapType = if (mapType == MapType.NORMAL) MapType.SATELLITE else MapType.NORMAL
                            },
                            modifier = Modifier.background(gradientBrush, RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                if (mapType == MapType.NORMAL) Icons.Default.Satellite else Icons.Default.Map,
                                contentDescription = "Tipo de mapa",
                                tint = Color.White
                            )
                        }
                    }

                    // Badge FCM — dentro del Box, fuera del GoogleMap no necesito que aparezca
                   /* if (fcmActive) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .background(gradientBrush, RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("FCM activo", color = Color.White, fontSize = 12.sp)
                        }
                    }*/
                } // ← cierre Box mapa

                // Panel inferior (30%)
                Box(modifier = Modifier.weight(0.3f)) {
                    if (selectedMarcador == null) {
                        EmptyPanel()
                    } else {
                        MarcadorPanel(
                            marcador = selectedMarcador!!,
                            onClose = { selectedMarcador = null }
                        )
                    }
                }
            }
        }
    }

    // Diálogo nuevo entreno
    if (showFormDialog && tappedPosition != null) {
        NuevoEntrenoDialog(
            posicion = tappedPosition!!,
            onDismiss = { showFormDialog = false },
            onGuardar = { marcador ->
                AppState.entrenosGlobal.add(marcador)
                showFormDialog = false
                val token = context.getSharedPreferences("runup_prefs", Context.MODE_PRIVATE)
                    .getString("token", "") ?: ""

                android.util.Log.d("RUNUP", "Token: $token")  // ← añade esto
                android.util.Log.d("RUNUP", "Guardando: ${marcador.nombre}")  // ← y esto

                scope.launch {
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
                                put("sensaciones", marcador.sensaciones)
                                put("kilometros", marcador.kilometros)
                                put("lat", marcador.posicion.latitude)
                                put("lng", marcador.posicion.longitude)
                            }.toString()
                            connection.outputStream.use { it.write(body.toByteArray()) }
                            val responseCode = connection.responseCode
                            android.util.Log.d("RUNUP", "Respuesta servidor: $responseCode")  // ← y esto
                        } catch (e: Exception) {
                            android.util.Log.e("RUNUP", "Error: ${e.message}")  // ← y esto
                            e.printStackTrace()
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null, tint = Color.White) },
        label = { Text(label, color = Color.White, fontWeight = FontWeight.SemiBold) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent
        )
    )
}

@Composable
private fun EmptyPanel() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Toca el mapa para añadir un entreno", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("O pulsa un marcador para ver su info", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun MarcadorPanel(marcador: Marcador, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    marcador.nombre,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = chipColor(marcador.tipoEntreno).copy(alpha = 0.8f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        marcador.tipoEntreno,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(marcador.tiempo.ifEmpty { "Sin tiempo" }, color = Color.White, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SentimentSatisfied, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(marcador.sensaciones.ifEmpty { "Sin sensaciones" }, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevoEntrenoDialog(
    posicion: LatLng,
    onDismiss: () -> Unit,
    onGuardar: (Marcador) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var tiempo by remember { mutableStateOf("") }
    var tipoSeleccionado by remember { mutableStateOf("Rodaje") }
    var sensaciones by remember { mutableStateOf("Buenas") }
    var expandedTipo by remember { mutableStateOf(false) }
    var expandedSensaciones by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF185A9D),
        title = { Text("Nuevo entreno", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del entreno", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White) },
                    colors = runUpTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = expandedTipo,
                    onExpandedChange = { expandedTipo = it }
                ) {
                    OutlinedTextField(
                        value = tipoSeleccionado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de entreno", color = Color.White) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                        colors = runUpTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(15.dp)
                    )
                    ExposedDropdownMenu(expanded = expandedTipo, onDismissRequest = { expandedTipo = false }) {
                        listOf("Rodaje", "Series", "Fartlek").forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(tipo) },
                                onClick = { tipoSeleccionado = tipo; expandedTipo = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = tiempo,
                    onValueChange = { tiempo = it },
                    label = { Text("Tiempo (ej: 45 min)", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White) },
                    colors = runUpTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = expandedSensaciones,
                    onExpandedChange = { expandedSensaciones = it }
                ) {
                    OutlinedTextField(
                        value = sensaciones,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sensaciones", color = Color.White) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSensaciones) },
                        colors = runUpTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(15.dp)
                    )
                    ExposedDropdownMenu(expanded = expandedSensaciones, onDismissRequest = { expandedSensaciones = false }) {
                        listOf("Malas", "Regulares", "Buenas", "Muy buenas").forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = { sensaciones = s; expandedSensaciones = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nombre.isNotEmpty()) {
                        onGuardar(
                            Marcador(
                                id = System.currentTimeMillis().toString(),
                                nombre = nombre,
                                tiempo = tiempo,
                                tipoEntreno = tipoSeleccionado,
                                sensaciones = sensaciones,
                                kilometros = 0.0,
                                posicion = posicion
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color(0xFF185A9D))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Guardar entreno", color = Color(0xFF185A9D), fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun chipColor(tipo: String): Color = when (tipo) {
    "Series"  -> Color.Red
    "Rodaje"  -> Color.Green
    "Fartlek" -> Color(0xFFFFA500)
    else      -> Color(0xFF9C27B0)
}