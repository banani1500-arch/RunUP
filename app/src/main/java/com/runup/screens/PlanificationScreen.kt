package com.runup.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.runup.AppState
import com.runup.model.EntrenoDay
import androidx.navigation.NavController
import androidx.compose.material.icons.filled.ArrowBack

// ─── Modelos ─────────────────────────────────────────────────────────────────

enum class CalendarView { ANUAL, MENSUAL, SEMANAL, DIARIO }


// ─── Colores helper ──────────────────────────────────────────────────────────
private fun tipoColor(tipo: String) = when (tipo.lowercase()) {
    "series"   -> Color(0xFFE53935)
    "rodaje"   -> Color(0xFF43A047)
    "fartlek"  -> Color(0xFFFB8C00)
    "descanso" -> Color(0xFF9E9E9E)
    else       -> Color(0xFF9E9E9E)
}

private fun tipoChipBg(tipo: String) = when (tipo.lowercase()) {
    "series"   -> Color(0xFFFFEBEE)
    "rodaje"   -> Color(0xFFE8F5E9)
    "fartlek"  -> Color(0xFFFFF3E0)
    "descanso" -> Color(0xFFF5F5F5)
    else       -> Color(0xFFF5F5F5)
}

private fun tipoChipText(tipo: String) = when (tipo.lowercase()) {
    "series"   -> Color(0xFFC62828)
    "rodaje"   -> Color(0xFF2E7D32)
    "fartlek"  -> Color(0xFFE65100)
    "descanso" -> Color(0xFF616161)
    else       -> Color(0xFF616161)
}

// ─── Screen principal ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanificacionScreen(navController: NavController) {
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val token = remember {
        context.getSharedPreferences("runup_prefs", Context.MODE_PRIVATE)
            .getString("token", "") ?: ""
    }

    var currentView by remember { mutableStateOf(CalendarView.MENSUAL) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var currentYear by remember { mutableStateOf(LocalDate.now().year) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var entrenoAEliminar by remember { mutableStateOf<EntrenoDay?>(null) }
    var showEliminarPlanDialog by remember { mutableStateOf(false) }
    var weekStart by remember {
        mutableStateOf(
            LocalDate.now().with(DayOfWeek.MONDAY)
        )
    }

    val entrenoMap = remember { mutableStateMapOf<LocalDate, EntrenoDay>() }

    LaunchedEffect(Unit) {
        AppState.calendarioGlobal.forEach { entreno ->
            entrenoMap[entreno.fecha] = entreno
        }
    }

    // IA state
    var distancia by remember { mutableStateOf("21 km") }
    var nivelRitmo by remember { mutableStateOf("Intermedio") }
    var periodo by remember { mutableStateOf("Este mes") }
    var isGenerating by remember { mutableStateOf(false) }
    var planGenerado by remember { mutableStateOf<List<EntrenoDay>>(emptyList()) }
    var expandedDist by remember { mutableStateOf(false) }
    var expandedRitmo by remember { mutableStateOf(false) }
    var expandedPeriodo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {Text("Mi planificación", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 20.sp)},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Tabs de vista ────────────────────────────────────────────────
            item {
                ViewTabRow(currentView) { currentView = it }
            }

            // ── Cabecera de navegación ───────────────────────────────────────
            item {
                CalendarNavHeader(
                    view = currentView,
                    currentMonth = currentMonth,
                    currentYear = currentYear,
                    selectedDate = selectedDate,
                    weekStart = weekStart,
                    onPrev = {
                        when (currentView) {
                            CalendarView.MENSUAL -> currentMonth = currentMonth.minusMonths(1)
                            CalendarView.ANUAL   -> currentYear--
                            CalendarView.SEMANAL -> weekStart = weekStart.minusWeeks(1)
                            CalendarView.DIARIO  -> selectedDate = selectedDate.minusDays(1)
                        }
                    },
                    onNext = {
                        when (currentView) {
                            CalendarView.MENSUAL -> currentMonth = currentMonth.plusMonths(1)
                            CalendarView.ANUAL   -> currentYear++
                            CalendarView.SEMANAL -> weekStart = weekStart.plusWeeks(1)
                            CalendarView.DIARIO  -> selectedDate = selectedDate.plusDays(1)
                        }
                    }
                )
            }

            // ── Cuerpo del calendario ────────────────────────────────────────
            item {
                when (currentView) {
                    CalendarView.MENSUAL -> MonthlyView(
                        yearMonth = currentMonth,
                        entrenoMap = entrenoMap,
                        onDayClick = { date ->
                            selectedDate = date
                            currentView = CalendarView.DIARIO
                        }
                    )
                    CalendarView.SEMANAL -> WeeklyView(
                        weekStart = weekStart,
                        entrenoMap = entrenoMap,
                        onDayClick = { date ->
                            selectedDate = date
                            currentView = CalendarView.DIARIO
                        }
                    )
                    CalendarView.ANUAL -> AnnualView(
                        year = currentYear,
                        entrenoMap = entrenoMap,
                        onMonthClick = { month ->
                            currentMonth = YearMonth.of(currentYear, month)
                            currentView = CalendarView.MENSUAL
                        }
                    )
                    CalendarView.DIARIO -> DailyView(
                        date = selectedDate,
                        entreno = entrenoMap[selectedDate],
                        onDelete = { entreno -> entrenoAEliminar = entreno }
                    )
                }
            }

            // ── Panel IA ─────────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "Generar plan con IA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Fila 1: distancia + ritmo
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Dropdown Distancia
                                ExposedDropdownMenuBox(
                                    expanded = expandedDist,
                                    onExpandedChange = { expandedDist = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = distancia,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Distancia", fontSize = 11.sp) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedDist) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                                        shape = RoundedCornerShape(10.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                                    )
                                    ExposedDropdownMenu(expanded = expandedDist, onDismissRequest = { expandedDist = false }) {
                                        listOf("5 km","10 km","21 km","42 km").forEach {
                                            DropdownMenuItem(text = { Text(it) }, onClick = { distancia = it; expandedDist = false })
                                        }
                                    }
                                }
                                // Dropdown Ritmo
                                ExposedDropdownMenuBox(
                                    expanded = expandedRitmo,
                                    onExpandedChange = { expandedRitmo = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = nivelRitmo,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Nivel", fontSize = 11.sp) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedRitmo) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                                        shape = RoundedCornerShape(10.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                                    )
                                    ExposedDropdownMenu(expanded = expandedRitmo, onDismissRequest = { expandedRitmo = false }) {
                                        listOf("Principiante","Intermedio","Avanzado","Élite").forEach {
                                            DropdownMenuItem(text = { Text(it) }, onClick = { nivelRitmo = it; expandedRitmo = false })
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Dropdown Periodo
                            ExposedDropdownMenuBox(
                                expanded = expandedPeriodo,
                                onExpandedChange = { expandedPeriodo = it }
                            ) {
                                OutlinedTextField(
                                    value = periodo,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Periodo", fontSize = 11.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedPeriodo) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    shape = RoundedCornerShape(10.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                                )
                                ExposedDropdownMenu(expanded = expandedPeriodo, onDismissRequest = { expandedPeriodo = false }) {
                                    listOf("1 semana"," 2 semanas","4 semanas").forEach {
                                        DropdownMenuItem(text = { Text(it) }, onClick = { periodo = it; expandedPeriodo = false })
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Botón generar
                            Button(
                                onClick = {
                                    scope.launch {
                                        isGenerating = true
                                        planGenerado = emptyList()
                                        val result = generatePlanIA(distancia, nivelRitmo, periodo, token)
                                        isGenerating = false

                                        if (result.isNotEmpty()) {
                                            // Primero guardar en BD y obtener los IDs
                                            val resultConIds = guardarCalendarioEnBD(result, token)
                                            resultConIds.forEach { entrenoMap[it.fecha] = it }
                                            AppState.calendarioGlobal.addAll(resultConIds)
                                            planGenerado = resultConIds

                                            // Luego navegar usando resultConIds
                                            currentView = when (periodo) {
                                                "Esta semana" -> CalendarView.SEMANAL
                                                "2 semanas"   -> CalendarView.SEMANAL
                                                "4 semanas"   -> CalendarView.MENSUAL
                                                else          -> CalendarView.MENSUAL
                                            }
                                            if (periodo == "Esta semana" || periodo == "2 semanas") {
                                                weekStart = resultConIds.first().fecha.with(DayOfWeek.MONDAY)
                                            }
                                            if (periodo == "4 semanas") {
                                                currentMonth = YearMonth.from(resultConIds.first().fecha)
                                            }
                                        }
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
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Text("Generando plan...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                    } else {
                                        Text("Generar plan de entrenamiento", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Lista plan generado
                    if (planGenerado.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Plan generado — ${planGenerado.size} sesiones",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        planGenerado.forEach { PlanItem(it) }

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { showEliminarPlanDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eliminar plan completo", color = Color(0xFFFF5252), fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
// Eliminar día concreto
    entrenoAEliminar?.let { entreno ->
        AlertDialog(
            onDismissRequest = { entrenoAEliminar = null },
            containerColor = Color(0xFF185A9D),
            title = { Text("Eliminar entreno", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("¿Eliminar el entreno del ${entreno.fecha}?", color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    entrenoMap.remove(entreno.fecha)
                    AppState.calendarioGlobal.remove(entreno)
                    planGenerado = planGenerado.filter { it.fecha != entreno.fecha }
                    entrenoAEliminar = null
                    scope.launch { eliminarEntrenoEnBD(entreno.id, token) }
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

// Eliminar plan completo
    if (showEliminarPlanDialog) {
        AlertDialog(
            onDismissRequest = { showEliminarPlanDialog = false },
            containerColor = Color(0xFF185A9D),
            title = { Text("Eliminar plan completo", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("¿Eliminar todas las ${planGenerado.size} sesiones del plan?", color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    planGenerado.forEach { entreno ->
                        entrenoMap.remove(entreno.fecha)
                        AppState.calendarioGlobal.remove(entreno)
                    }
                    scope.launch {
                        planGenerado.forEach { eliminarEntrenoEnBD(it.id, token) }
                    }
                    planGenerado = emptyList()
                    showEliminarPlanDialog = false
                }) {
                    Text("Eliminar todo", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEliminarPlanDialog = false }) {
                    Text("Cancelar", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }
}

// ─── Componentes del calendario ───────────────────────────────────────────────

@Composable
private fun ViewTabRow(current: CalendarView, onSelect: (CalendarView) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        CalendarView.entries.forEach { view ->
            val label = when (view) {
                CalendarView.ANUAL   -> "Anual"
                CalendarView.MENSUAL -> "Mensual"
                CalendarView.SEMANAL -> "Semanal"
                CalendarView.DIARIO  -> "Diario"
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(view) }
                    .then(
                        if (view == current) Modifier.background(Color.Transparent)
                        else Modifier
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = if (view == current) FontWeight.Bold else FontWeight.Normal,
                        color = if (view == current) Color(0xFF185A9D) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (view == current) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.dp)
                                .background(Color(0xFF185A9D), RoundedCornerShape(1.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarNavHeader(
    view: CalendarView,
    currentMonth: YearMonth,
    currentYear: Int,
    selectedDate: LocalDate,
    weekStart: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val label = when (view) {
        CalendarView.MENSUAL -> "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale("es"))} ${currentMonth.year}"
            .replaceFirstChar { it.uppercase() }
        CalendarView.ANUAL   -> "$currentYear"
        CalendarView.SEMANAL -> {
            val end = weekStart.plusDays(6)
            "${weekStart.dayOfMonth} ${weekStart.month.getDisplayName(TextStyle.SHORT, Locale("es"))} – " +
                    "${end.dayOfMonth} ${end.month.getDisplayName(TextStyle.SHORT, Locale("es"))}"
        }
        CalendarView.DIARIO  -> {
            val dow = selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es")).replaceFirstChar { it.uppercase() }
            val mon = selectedDate.month.getDisplayName(TextStyle.FULL, Locale("es"))
            "$dow ${selectedDate.dayOfMonth} $mon"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
        }
        Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
        IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

// ── Vista mensual ─────────────────────────────────────────────────────────────

@Composable
private fun MonthlyView(
    yearMonth: YearMonth,
    entrenoMap: Map<LocalDate, EntrenoDay>,
    onDayClick: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val firstDay = yearMonth.atDay(1)
    val daysBefore = (firstDay.dayOfWeek.value - 1 + 7) % 7
    val daysInMonth = yearMonth.lengthOfMonth()

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        // Cabecera días semana
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("L","M","X","J","V","S","D").forEach { d ->
                Text(d, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        val totalCells = daysBefore + daysInMonth
        val rows = (totalCells + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    val dayNum = index - daysBefore + 1
                    val isCurrentMonth = dayNum in 1..daysInMonth
                    val date = if (isCurrentMonth) yearMonth.atDay(dayNum) else null
                    val entreno = date?.let { entrenoMap[it] }
                    val isToday = date == today

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .then(if (date != null) Modifier.clickable { onDayClick(date) } else Modifier)
                            .padding(2.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (date != null) {
                                if (isToday) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .background(Color(0xFF185A9D), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("$dayNum", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text(
                                        "$dayNum",
                                        fontSize = 12.sp,
                                        color = if (isCurrentMonth) MaterialTheme.colorScheme.onBackground
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
                                if (entreno != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(tipoColor(entreno.tipo), CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Vista semanal ─────────────────────────────────────────────────────────────

@Composable
private fun WeeklyView(
    weekStart: LocalDate,
    entrenoMap: Map<LocalDate, EntrenoDay>,
    onDayClick: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val shortDays = listOf("L","M","X","J","V","S","D")
        for (i in 0..6) {
            val date = weekStart.plusDays(i.toLong())
            val entreno = entrenoMap[date]
            val isToday = date == today
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onDayClick(date) },
                shape = RoundedCornerShape(10.dp),
                color = if (isToday) Color(0xFFE3F2FD) else MaterialTheme.colorScheme.surfaceVariant,
                border = if (entreno != null) androidx.compose.foundation.BorderStroke(1.dp, tipoColor(entreno.tipo))
                else null
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(shortDays[i], fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${date.dayOfMonth}", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = if (isToday) Color(0xFF185A9D) else MaterialTheme.colorScheme.onBackground)
                    if (entreno != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = tipoChipBg(entreno.tipo)
                        ) {
                            Text(
                                entreno.tipo.take(3),
                                fontSize = 9.sp,
                                color = tipoChipText(entreno.tipo),
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Vista anual ───────────────────────────────────────────────────────────────

@Composable
private fun AnnualView(
    year: Int,
    entrenoMap: Map<LocalDate, EntrenoDay>,
    onMonthClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(12) { month ->
            val count = entrenoMap.keys.count { it.year == year && it.monthValue == month + 1 }
            val monthName = LocalDate.of(year, month + 1, 1).month
                .getDisplayName(TextStyle.SHORT, Locale("es")).replaceFirstChar { it.uppercase() }
            Surface(
                modifier = Modifier.clickable { onMonthClick(month + 1) },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(monthName, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground)
                    Text("$count entrenos", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(minOf(1f, count / 10f))
                                .height(3.dp)
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFF43CEA2), Color(0xFF185A9D))),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

// ── Vista diaria ──────────────────────────────────────────────────────────────

@Composable
private fun DailyView(date: LocalDate, entreno: EntrenoDay?, onDelete: ((EntrenoDay) -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        if (entreno != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, tipoColor(entreno.tipo).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(tipoColor(entreno.tipo), CircleShape))
                            Surface(shape = RoundedCornerShape(20.dp), color = tipoChipBg(entreno.tipo)) {
                                Text(entreno.tipo, fontSize = 11.sp, color = tipoChipText(entreno.tipo),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                        IconButton(onClick = { onDelete?.invoke(entreno) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar",
                                tint = tipoColor(entreno.tipo).copy(alpha = 0.7f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(entreno.descripcion, fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground, lineHeight = 20.sp)
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventBusy, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Sin entreno planificado", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ── Item del plan generado ────────────────────────────────────────────────────

@Composable
private fun PlanItem(entreno: EntrenoDay) {
    val today = LocalDate.now()
    val diffDays = java.time.temporal.ChronoUnit.DAYS.between(today, entreno.fecha).toInt()
    val dayLabel = when (diffDays) {
        0 -> "Hoy"
        1 -> "Mañana"
        else -> "+${diffDays}d"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).background(tipoColor(entreno.tipo), CircleShape))
        Text(dayLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp))
        Text(entreno.descripcion, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Surface(shape = RoundedCornerShape(20.dp), color = tipoChipBg(entreno.tipo)) {
            Text(entreno.tipo, fontSize = 10.sp, color = tipoChipText(entreno.tipo),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Medium)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

// ─── Llamada a la IA ──────────────────────────────────────────────────────────

private suspend fun generatePlanIA(
    distancia: String,
    nivel: String,
    periodo: String,
    token: String
): List<EntrenoDay> = withContext(Dispatchers.IO) {

    android.util.Log.d("PLAN_DEBUG", ">>> FUNCIÓN LLAMADA")

    try {
        val url = URL("https://bsaldana.difadi.net/api/generate-plan")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", token)
            connectTimeout = 15000
            readTimeout = 30000
            doOutput = true
        }

        val body = JSONObject().apply {
            put("distancia", distancia)
            put("nivel", nivel)
            put("periodo", periodo)
        }.toString()

        android.util.Log.d("PLAN_DEBUG", ">>> Body: $body")
        conn.outputStream.use { it.write(body.toByteArray()) }

        val code = conn.responseCode
        android.util.Log.d("PLAN_DEBUG", ">>> HTTP code: $code")

        // Lee errorStream si hay error HTTP
        val response = if (code in 200..299) {
            conn.inputStream.bufferedReader().readText()
        } else {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "sin error stream"
            android.util.Log.e("PLAN_DEBUG", ">>> ERROR HTTP $code: $err")
            return@withContext emptyList()
        }

        android.util.Log.d("PLAN_DEBUG", ">>> Respuesta: $response")

        val jsonResp = JSONObject(response)
        if (!jsonResp.has("dias")) {
            android.util.Log.e("PLAN_DEBUG", ">>> Sin campo 'dias' en respuesta")
            return@withContext emptyList()
        }

        val dias = jsonResp.getJSONArray("dias")
        val today = LocalDate.now()

        (0 until dias.length()).map { i ->
            val d = dias.getJSONObject(i)
            EntrenoDay(
                fecha = today.plusDays(d.getInt("offset").toLong()),
                tipo = d.getString("tipo").lowercase(),
                descripcion = d.getString("descripcion")
            )
        }

    } catch (e: Exception) {
        android.util.Log.e("PLAN_DEBUG", ">>> EXCEPCIÓN ${e::class.simpleName}: ${e.message}")
        e.printStackTrace()
        emptyList()
    }
}

private suspend fun guardarCalendarioEnBD(
    entrenos: List<EntrenoDay>,
    token: String
): List<EntrenoDay> = withContext(Dispatchers.IO) {
    entrenos.map { entreno ->
        try {
            val url = URL("https://bsaldana.difadi.net/api/calendario")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", token)
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
            }
            val body = JSONObject().apply {
                put("titulo", entreno.tipo)
                put("tipo", entreno.tipo)
                put("fecha", entreno.fecha.toString())
                put("descripcion", entreno.descripcion)
            }.toString()
            conn.outputStream.use { it.write(body.toByteArray()) }
            val response = conn.inputStream.bufferedReader().readText()
            val id = JSONObject(response).optInt("id", 0)
            entreno.copy(id = id)  // ← devuelve con el id
        } catch (e: Exception) {
            android.util.Log.e("CALENDARIO", "Error: ${e.message}")
            entreno
        }
    }
}
private suspend fun eliminarEntrenoEnBD(id: Int, token: String) = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://bsaldana.difadi.net/api/calendario/$id")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            setRequestProperty("Authorization", token)
            connectTimeout = 10000
            readTimeout = 10000
        }
        android.util.Log.d("CALENDARIO", "Eliminado: ${conn.responseCode}")
    } catch (e: Exception) {
        android.util.Log.e("CALENDARIO", "Error delete: ${e.message}")
    }
}