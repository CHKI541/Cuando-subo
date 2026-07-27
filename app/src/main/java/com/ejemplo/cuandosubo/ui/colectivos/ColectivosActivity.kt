package com.ejemplo.cuandosubo.ui.colectivos

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

class ColectivosActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cargar base de datos de paradas
        StopsDatabase.initialize(this)

        setContent {
            ColectivosAppTheme {
                ColectivosMainScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

// Colores modernos (Estilo Premium Dark Mode)
private val DarkBackground = Color(0xFF0B192C)
private val CardBackground = Color(0xFF1E3E62)
private val AccentColor = Color(0xFFF1C40F) // Dorado
private val TextWhite = Color(0xFFF5F5F7)
private val TextGray = Color(0xFF9E9E9E)
private val LiveGreen = Color(0xFF2ECC71) // Verde para tiempo real

@Composable
fun ColectivosAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DarkBackground,
            surface = CardBackground,
            primary = AccentColor,
            onBackground = TextWhite,
            onSurface = TextWhite
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColectivosMainScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Favoritos", "Por Línea", "Por Dirección")

    // Estado global de la parada seleccionada para ver arribos
    var activeStopId by remember { mutableStateOf<String?>(null) }
    var activeStopName by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Cuando SUBO CABA",
                        fontWeight = FontWeight.Bold,
                        color = AccentColor,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkBackground,
                    contentColor = AccentColor,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AccentColor
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                        0 -> FavoritosTab(onStopClick = { id, name ->
                            activeStopId = id
                            activeStopName = name
                        })
                        1 -> LineasTab(onStopClick = { id, name ->
                            activeStopId = id
                            activeStopName = name
                        })
                        2 -> DireccionesTab(onStopClick = { id, name ->
                            activeStopId = id
                            activeStopName = name
                        })
                    }
                }
            }

            // Diálogo/Popup animado de arribos en tiempo real al seleccionar una parada
            if (activeStopId != null) {
                ArribosDialog(
                    stopId = activeStopId!!,
                    stopName = activeStopName ?: "Parada",
                    onDismiss = { activeStopId = null }
                )
            }
        }
    }
}

// ==================== TABS DE NAVEGACIÓN ====================

@Composable
fun FavoritosTab(onStopClick: (String, String) -> Unit) {
    val context = LocalContext.current
    var favorites by remember { mutableStateOf(getFavoriteStops(context)) }

    // Escuchar cambios al enfocar el tab
    LaunchedEffect(Unit) {
        favorites = getFavoriteStops(context)
    }

    if (favorites.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.StarOutline,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    "No tienes paradas favoritas.",
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
                Text(
                    "Busca una línea o dirección y toca la estrella.",
                    color = TextGray.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(favorites) { stop ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStopClick(stop.id, stop.name) },
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stop.name, fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 16.sp)
                            Text("Código: ${stop.id.removePrefix("14_")}", color = TextGray, fontSize = 12.sp)
                        }
                        IconButton(onClick = {
                            toggleFavorite(context, stop.id, stop.name)
                            favorites = getFavoriteStops(context)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Eliminar de favoritos",
                                tint = AccentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LineasTab(onStopClick: (String, String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var routesList by remember { mutableStateOf<List<RouteInfo>>(emptyList()) }
    var selectedRouteId by remember { mutableStateOf<String?>(null) }
    var selectedRouteName by remember { mutableStateOf<String?>(null) }
    var stopsList by remember { mutableStateOf<List<StopInfo>>(emptyList()) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (selectedRouteId == null) {
            // Buscador de líneas
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar Línea (ej. 132)", color = TextGray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        if (searchQuery.trim().isNotEmpty()) {
                            isLoading = true
                            scope.launch {
                                try {
                                    routesList = ColectivosApiClient.buscarLinea(searchQuery)
                                    if (routesList.isEmpty()) {
                                        Toast.makeText(context, "No se encontraron ramales.", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = AccentColor)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentColor,
                    unfocusedBorderColor = TextGray,
                    focusedLabelColor = AccentColor,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentColor)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(routesList) { route ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedRouteId = route.id
                                    selectedRouteName = "${route.shortName} - ${route.longName}"
                                    isLoading = true
                                    scope.launch {
                                        try {
                                            stopsList = ColectivosApiClient.obtenerParadas(route.id)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error cargando paradas: ${e.message}", Toast.LENGTH_LONG).show()
                                            selectedRouteId = null
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(route.shortName, fontWeight = FontWeight.Bold, color = AccentColor, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(route.longName, color = TextWhite, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // Listado de paradas de la línea seleccionada
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedRouteId = null }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás", tint = AccentColor)
                }
                Text(
                    selectedRouteName ?: "Recorrido",
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontSize = 16.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentColor)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(stopsList) { stop ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStopClick(stop.id, stop.name) },
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(AccentColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsBus,
                                        contentDescription = null,
                                        tint = DarkBackground,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(stop.name, fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 14.sp)
                                    Text("Código: ${stop.id.removePrefix("14_")}", color = TextGray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DireccionesTab(onStopClick: (String, String) -> Unit) {
    var addressQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var addressesList by remember { mutableStateOf<List<GeocodedAddress>>(emptyList()) }
    var nearestStops by remember { mutableStateOf<List<StaticStopInfo>>(emptyList()) }
    var selectedAddressName by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (selectedAddressName == null) {
            // Buscador de dirección
            OutlinedTextField(
                value = addressQuery,
                onValueChange = { addressQuery = it },
                label = { Text("Buscar Dirección (ej. Corrientes 2000)", color = TextGray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        if (addressQuery.trim().isNotEmpty()) {
                            isLoading = true
                            scope.launch {
                                try {
                                    addressesList = ColectivosApiClient.geocodeAddress(addressQuery)
                                    if (addressesList.isEmpty()) {
                                        Toast.makeText(context, "No se encontró la dirección.", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = AccentColor)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentColor,
                    unfocusedBorderColor = TextGray,
                    focusedLabelColor = AccentColor,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentColor)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(addressesList) { addr ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAddressName = addr.name
                                    isLoading = true
                                    scope.launch {
                                        try {
                                            // Calcular paradas más cercanas localmente
                                            nearestStops = StopsDatabase.getNearestStops(addr.lat, addr.lon)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error al calcular paradas: ${e.message}", Toast.LENGTH_SHORT).show()
                                            selectedAddressName = null
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = AccentColor)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(addr.name, color = TextWhite, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // Listado de paradas cercanas a la dirección seleccionada
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedAddressName = null }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás", tint = AccentColor)
                }
                Text(
                    "Paradas cerca de: $selectedAddressName",
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontSize = 15.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(nearestStops) { stop ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStopClick("14_${stop.id}", stop.name) },
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(AccentColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsBus,
                                    contentDescription = null,
                                    tint = DarkBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(stop.name, fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 14.sp)
                                Text("Código Parada: ${stop.id}", color = TextGray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== DIÁLOGO DE ARRIBOS EN TIEMPO REAL ====================

@Composable
fun ArribosDialog(
    stopId: String,
    stopName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isFavorite by remember { mutableStateOf(isStopFavorite(context, stopId)) }
    var arrivalsList by remember { mutableStateOf<List<ArrivalInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // Consulta y auto-refresco cada 30 segundos
    LaunchedEffect(refreshTrigger) {
        isLoading = true
        try {
            arrivalsList = ColectivosApiClient.obtenerArribos(stopId)
        } catch (e: Exception) {
            Toast.makeText(context, "Error cargando arribos: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            isLoading = false
        }
    }

    // Auto-refresco en segundo plano
    LaunchedEffect(Unit) {
        while (true) {
            delay(30000)
            refreshTrigger++
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stopName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextWhite)
                    Text("Código Parada: ${stopId.removePrefix("14_")}", fontSize = 12.sp, color = TextGray)
                }
                Row {
                    IconButton(onClick = {
                        toggleFavorite(context, stopId, stopName)
                        isFavorite = !isFavorite
                    }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorito",
                            tint = AccentColor
                        )
                    }
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refrescar", tint = AccentColor)
                    }
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentColor)
                    }
                } else if (arrivalsList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No hay colectivos próximos a arribar a esta parada en este momento.",
                            color = TextGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(arrivalsList) { arrival ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkBackground.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                arrival.route,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = AccentColor
                                            )
                                            if (arrival.isLive) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(LiveGreen.copy(alpha = 0.2f))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text("VIVO", color = LiveGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(arrival.destination, fontSize = 12.sp, color = TextWhite)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            if (arrival.minutes == 0) "Arribando" else "${arrival.minutes} min",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (arrival.isLive) LiveGreen else TextWhite
                                        )
                                        Text(arrival.arrivalTime, fontSize = 11.sp, color = TextGray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = AccentColor, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp)
    )
}

// ==================== FUNCIONES AUXILIARES DE FAVORITOS ====================

data class FavoriteStop(val id: String, val name: String)

private fun getFavoriteStops(context: Context): List<FavoriteStop> {
    val prefs = context.getSharedPreferences("colectivos_prefs", Context.MODE_PRIVATE)
    val favSet = prefs.getStringSet("favorites", emptySet()) ?: emptySet()
    return favSet.map {
        val parts = it.split("|")
        FavoriteStop(parts[0], if (parts.size > 1) parts[1] else "Parada")
    }.sortedBy { it.name }
}

private fun isStopFavorite(context: Context, stopId: String): Boolean {
    val prefs = context.getSharedPreferences("colectivos_prefs", Context.MODE_PRIVATE)
    val favSet = prefs.getStringSet("favorites", emptySet()) ?: emptySet()
    return favSet.any { it.startsWith("$stopId|") }
}

private fun toggleFavorite(context: Context, stopId: String, stopName: String) {
    val prefs = context.getSharedPreferences("colectivos_prefs", Context.MODE_PRIVATE)
    val favSet = prefs.getStringSet("favorites", emptySet())?.toMutableSet() ?: mutableSetOf()

    val item = "$stopId|$stopName"
    val existing = favSet.firstOrNull { it.startsWith("$stopId|") }

    if (existing != null) {
        favSet.remove(existing)
    } else {
        favSet.add(item)
    }

    prefs.edit().putStringSet("favorites", favSet).apply()
}
