package com.anglr.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight\nimport androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import android.util.Base64\nimport java.security.KeyStore\nimport javax.crypto.Cipher\nimport javax.crypto.KeyGenerator\nimport javax.crypto.SecretKey\nimport javax.crypto.spec.GCMParameterSpec\nimport javax.crypto.spec.SecretKeySpec\nimport java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

private val Navy = Color(0xFF071A2D)
private val Sea = Color(0xFF00AFC0)
private val Aqua = Color(0xFF65D7C5)
private val Sand = Color(0xFFF3F6F7)
private val Ink = Color(0xFF10283B)

data class Spot(val name: String, val type: String, val lat: Double, val lon: Double, val distance: Double)

class MainActivity : ComponentActivity() {
    private var locationGranted by mutableStateOf(false)
    private var currentLocation by mutableStateOf<Location?>(null)
    private val requestLocation = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        locationGranted = result.values.any { it }
        if (locationGranted) currentLocation = readLocation()
    }
    private fun readLocation(): Location? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).mapNotNull {
                try { manager.getLastKnownLocation(it) } catch (_: Exception) { null }
            }.maxByOrNull { it.time }
        } catch (_: Exception) { null }
    }
    private fun requestFreshLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocation.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)); return
        }
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter {
            try { manager.isProviderEnabled(it) } catch (_: Exception) { false }
        }
        if (providers.isEmpty()) { currentLocation = readLocation(); return }
        var received = false
        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(loc: Location) {
                if (!received) { received = true; currentLocation = loc; try { manager.removeUpdates(this) } catch (_: Exception) {} }
            }
            @Deprecated("Deprecated")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        try {
            providers.forEach { manager.requestLocationUpdates(it, 0L, 0f, listener) }
            currentLocation = readLocation()
        } catch (_: Exception) { currentLocation = readLocation() }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (locationGranted) currentLocation = readLocation()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Sea, secondary = Aqua, background = Sand)) {
                AnglrApp(location = currentLocation, hasLocation = locationGranted,
                    askLocation = { requestFreshLocation() },
                    refreshLocation = { requestFreshLocation() })
            }
        }
    }
}

@Composable
fun AnglrApp(location: Location?, hasLocation: Boolean, askLocation: () -> Unit, refreshLocation: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var latText by remember { mutableStateOf("") }
    var lonText by remember { mutableStateOf("") }
    LaunchedEffect(location) {
        if (location != null) { latText = location.latitude.toString(); lonText = location.longitude.toString() }
    }
    Surface(Modifier.fillMaxSize(), color = Sand) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().background(Navy).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Waves, null, tint = Aqua, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(10.dp))
                Column { Text("ANGLR", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text("AUSTRALIAN FISHING INTELLIGENCE", color = Aqua, style = MaterialTheme.typography.labelSmall) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = refreshLocation) { Icon(Icons.Default.MyLocation, "Refresh location", tint = Color.White) }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    0 -> HomeScreen(location, hasLocation, askLocation, onTab = { tab = it })
                    1 -> SpotsScreen(location, latText, lonText, { latText = it }, { lonText = it }, askLocation)
                    2 -> TidesScreen(location, latText, lonText, { latText = it }, { lonText = it })
                    3 -> WeatherScreen(location)
                    4 -> GuideScreen()
                    5 -> ReportsScreen()
                    else -> FavoritesScreen()
                }
            }
            NavigationBar(containerColor = Navy) {
                val labels = listOf("Home", "Spots", "Tides", "Weather", "Guide", "Reports", "Saved")
                val icons = listOf(Icons.Default.Home, Icons.Default.Map, Icons.Default.Waves, Icons.Default.Cloud, Icons.Default.MenuBook, Icons.Default.Article, Icons.Default.Favorite)
                labels.forEachIndexed { i, label ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(icons[i], null) }, label = { Text(label, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Aqua, selectedTextColor = Aqua, unselectedIconColor = Color.LightGray, unselectedTextColor = Color.LightGray))
                }
            }
        }
    }
}

@Composable
fun Page(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column {
                Text(title, style = MaterialTheme.typography.headlineMedium, color = Ink, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.Gray)
            }
        }
        item { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }
    }
}
@Composable
fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
@Composable
fun HomeScreen(location: Location?, hasLocation: Boolean, askLocation: () -> Unit, onTab: (Int) -> Unit) {
    Page("Read the water.", "Your local fishing dashboard") {
        if (!hasLocation || location == null) Panel {
            Text("Enable location to find fishing spots near you.", color = Ink)
            Button(onClick = askLocation) { Text("Enable GPS") }
        } else Panel {
            Text("YOUR LOCATION", fontWeight = FontWeight.Bold, color = Ink)
            Text("%.5f, %.5f".format(Locale.US, location.latitude, location.longitude))
            Text("Use the tabs below to load live local data.", color = Color.Gray)
        }
        Panel {
            val phase = moonPhase(Date())
            Text("MOON PHASE", fontWeight = FontWeight.Bold, color = Ink)
            Text("${phase.first} · ${phase.second} illuminated", color = Sea, fontWeight = FontWeight.SemiBold)
            Text("Lunar timing is a planning signal, not a guarantee of fish activity.", color = Color.Gray)
        }
        Panel {
            Text("LIVE FISHING INTEL", fontWeight = FontWeight.Bold, color = Ink)
            listOf("Nearby fishing spots" to 1, "Tide predictions" to 2, "Live weather & marine forecast" to 3, "Species, bait & rig guide" to 4, "Fishing reports" to 5).forEach { (name, page) ->
                Row(Modifier.fillMaxWidth().clickable { onTab(page) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ChevronRight, null, tint = Sea)
                    Text(name, Modifier.weight(1f), color = Ink)
                    Icon(Icons.Default.ArrowForward, null, tint = Sea)
                }
            }
        }
        Panel {
            Text("DATA TRANSPARENCY", fontWeight = FontWeight.Bold, color = Ink)
            Text("Weather is retrieved from Open-Meteo. Fishing spot discovery uses OpenStreetMap community data. Tide predictions require a WorldTides API key; ANGLR will not invent tide times.", color = Color.Gray)
        }
    }
}

fun moonPhase(date: Date): Pair<String, String> {
    val synodicMonth = 29.530588853
    val newMoonEpoch = 947182440000L
    val age = ((date.time - newMoonEpoch) / 86400000.0 % synodicMonth + synodicMonth) % synodicMonth
    val fraction = age / synodicMonth
    val illumination = (50.0 * (1.0 - cos(2.0 * PI * fraction))).roundToInt()
    val phaseName = when {
        fraction < 0.03 || fraction > 0.97 -> "New moon"
        fraction < 0.22 -> "Waxing crescent"
        fraction < 0.28 -> "First quarter"
        fraction < 0.47 -> "Waxing gibbous"
        fraction < 0.53 -> "Full moon"
        fraction < 0.72 -> "Waning gibbous"
        fraction < 0.78 -> "Last quarter"
        else -> "Waning crescent"
    }
    return phaseName to "$illumination%"
}

@Composable
fun SpotsScreen(location: Location?, latText: String, lonText: String, setLat: (String)->Unit, setLon: (String)->Unit, askLocation: ()->Unit) {
    var spots by remember { mutableStateOf<List<Spot>>(emptyList()) }
    var status by remember { mutableStateOf("Enter coordinates or enable GPS, then search.") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val favorites = remember { context.getSharedPreferences("anglr_favorites", Context.MODE_PRIVATE) }
    var favoritesTick by remember { mutableIntStateOf(0) }
    val lat = latText.toDoubleOrNull()
    val lon = lonText.toDoubleOrNull()
    Page("Fishing spots", "Automatically use your GPS to find mapped fishing access") {
        Panel {
            if (location == null) {
                Text("Location needed", fontWeight = FontWeight.Bold, color = Ink)
                Text("Allow location access and refresh GPS to search around you.", color = Color.Gray)
                Button(onClick = askLocation) { Text("Enable / refresh GPS") }
            } else {
                Text("SEARCH CENTRE", fontWeight = FontWeight.Bold, color = Ink)
                Text("%.5f, %.5f".format(Locale.US, location.latitude, location.longitude), color = Ink)
                Text("Search radius: 30 km · OpenStreetMap community data", color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = askLocation) { Text("Refresh GPS") }
                    Button(enabled = !loading, onClick = {
                        loading = true; status = "Searching OpenStreetMap…"
                        scope.launch {
                            try { spots = withContext(Dispatchers.IO) { fetchSpots(location.latitude, location.longitude) }; status = if (spots.isEmpty()) "No mapped fishing access found within 30 km. Try again later or check the map." else "Found ${spots.size} mapped places within 30 km."
                            catch (e: Exception) { status = "Could not load spots: ${e.message}" }
                            loading = false
                        }
                    }) { Text(if (loading) "Searching…" else "Find nearby spots") }
                }
            }
        }
        Panel { Text(status, color = Ink) }
        spots.forEach { spot ->
            Panel {
                Text(spot.name, fontWeight = FontWeight.Bold, color = Ink)
                Text(spot.type, color = Sea)
                Text("%.1f km away".format(Locale.getDefault(), spot.distance / 1000.0), color = Color.Gray)
                Text("%.5f, %.5f".format(Locale.US, spot.lat, spot.lon), color = Color.Gray)
                val favoriteKey = "${spot.name}|${spot.lat}|${spot.lon}"
                val saved = favoritesTick >= 0 && favorites.getStringSet("spots", emptySet())?.contains(favoriteKey) == true
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val current = favorites.getStringSet("spots", emptySet())?.toMutableSet() ?: mutableSetOf()
                        if (saved) current.remove(favoriteKey) else current.add(favoriteKey)
                        favorites.edit().putStringSet("spots", current).apply()
                    }) { Text(if (saved) "★ Saved" else "☆ Save spot") }
                    Button(onClick = {
                        val uri = Uri.parse("geo:${spot.lat},${spot.lon}?q=${spot.lat},${spot.lon}(${URLEncoder.encode(spot.name, "UTF-8")})")
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${spot.lat},${spot.lon}")))
                        }
                    }) { Text("Directions") }
                }
            }
        }
    }
}
fun fetchSpots(lat: Double, lon: Double): List<Spot> {
    val radius = 30000
    val query = "[out:json][timeout:25];(node(around:$radius,$lat,$lon)[leisure=fishing];way(around:$radius,$lat,$lon)[leisure=fishing];relation(around:$radius,$lat,$lon)[leisure=fishing];node(around:$radius,$lat,$lon)[man_made=pier];way(around:$radius,$lat,$lon)[man_made=pier];node(around:$radius,$lat,$lon)[leisure=slipway];way(around:$radius,$lat,$lon)[leisure=slipway];node(around:$radius,$lat,$lon)[amenity=fishing];way(around:$radius,$lat,$lon)[amenity=fishing];node(around:$radius,$lat,$lon)[sport=fishing];way(around:$radius,$lat,$lon)[sport=fishing];node(around:$radius,$lat,$lon)[waterway=boat_ramp];way(around:$radius,$lat,$lon)[waterway=boat_ramp];node(around:$radius,$lat,$lon)[leisure=marina];way(around:$radius,$lat,$lon)[leisure=marina];);out center tags;"
    val encoded = java.net.URLEncoder.encode(query, "UTF-8")
    val json = try { getJson("https://overpass-api.de/api/interpreter?data=$encoded") } catch (_: Exception) { getJson("https://overpass.kumi.systems/api/interpreter?data=$encoded") }
    val elements = json.optJSONArray("elements") ?: JSONArray()
    val result = mutableListOf<Spot>()
    for (i in 0 until elements.length()) {
        val item = elements.getJSONObject(i); val tags = item.optJSONObject("tags") ?: JSONObject()
        val name = tags.optString("name", "").ifBlank { tags.optString("leisure", tags.optString("man_made", "Fishing access")) }
        val slat = item.optDouble("lat", item.optJSONObject("center")?.optDouble("lat") ?: Double.NaN)
        val slon = item.optDouble("lon", item.optJSONObject("center")?.optDouble("lon") ?: Double.NaN)
        if (slat.isNaN() || slon.isNaN()) continue
        val out = FloatArray(1); Location.distanceBetween(lat, lon, slat, slon, out)
        result.add(Spot(name, tags.optString("leisure", tags.optString("man_made", "Fishing access")), slat, slon, out[0].toDouble()))
    }
    return result.distinctBy { "${it.name}:${it.lat}:${it.lon}" }.sortedBy { it.distance }.take(60)
}

@Composable
fun FavoritesScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("anglr_favorites", Context.MODE_PRIVATE) }
    var tick by remember { mutableIntStateOf(0) }
    val saved = remember(tick) { prefs.getStringSet("spots", emptySet())?.toList()?.sorted() ?: emptyList() }
    Page("Saved fishing spots", "Your personal fishing locations") {
        if (saved.isEmpty()) Panel {
            Text("No saved spots yet", fontWeight = FontWeight.Bold, color = Ink)
            Text("Go to Spots, search nearby and tap Save spot. Your saved locations will appear here.", color = Color.Gray)
        }
        saved.forEach { key ->
            val parts = key.split("|")
            if (parts.size >= 3) {
                val name = parts[0]; val lat = parts[1].toDoubleOrNull(); val lon = parts[2].toDoubleOrNull()
                Panel {
                    Text(name, fontWeight = FontWeight.Bold, color = Ink)
                    if (lat != null && lon != null) Text("%.5f, %.5f".format(Locale.US, lat, lon), color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = lat != null && lon != null, onClick = {
                            val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon")
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lon")))
                        }
                        }) { Text("Directions") }
                        OutlinedButton(onClick = { val set = prefs.getStringSet("spots", emptySet())?.toMutableSet() ?: mutableSetOf(); set.remove(key); prefs.edit().putStringSet("spots", set).apply(); tick++ }) { Text("Remove") }
                    }
                }
            }
        }
    }
}

@Composable
fun TidesScreen(location: Location?, latText: String, lonText: String, setLat: (String)->Unit, setLon: (String)->Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var apiKey by remember { mutableStateOf(loadWorldTidesKey(context)) }
    var data by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Tide predictions need a WorldTides API key.") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lat = latText.toDoubleOrNull() ?: location?.latitude
    val lon = lonText.toDoubleOrNull() ?: location?.longitude
    Page("Tides", "Predicted high and low water for your selected location") {
        Panel {
            Text("TIDE DATA CONNECTION", fontWeight = FontWeight.Bold, color = Ink)
            Text("Create a WorldTides API key and paste it below. ANGLR stores the key encrypted with Android Keystore on this device; it is not embedded in the app or repository.", color = Color.Gray)\n            OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.worldtides.info/"))) }) { Text("Get a WorldTides API key") }
            OutlinedTextField(apiKey, { apiKey = it; saveWorldTidesKey(context, it) }, label = { Text("WorldTides API key") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(lat?.toString() ?: "", setLat, label = { Text("Latitude") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(lon?.toString() ?: "", setLon, label = { Text("Longitude") }, modifier = Modifier.fillMaxWidth())
            Button(enabled = apiKey.isNotBlank() && lat != null && lon != null && !loading, onClick = {
                loading = true; status = "Loading tide predictions…"; data = ""
                scope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) { getJson("https://www.worldtides.info/api/v3?extremes&lat=${lat}&lon=${lon}&key=${URLEncoder.encode(apiKey, "UTF-8")}") }
                        val extremes = result.optJSONArray("extremes") ?: JSONArray()
                        val fmt = SimpleDateFormat("EEE d MMM, h:mm a", Locale.getDefault())
                        data = (0 until extremes.length()).joinToString("\n") { i ->
                            val e = extremes.getJSONObject(i); val date = fmt.format(Date(e.optLong("dt") * 1000))
                            "${e.optString("type")} — $date — ${e.optDouble("height")} m"
                        }
                        status = if (data.isBlank()) "No tide extremes returned for this location." else "Predicted tide extremes"
                        if (result.has("error")) status = result.optString("error")
                    } catch (e: Exception) { status = "Tide request failed: ${e.message}" }
                    loading = false
                }
            }) { Text(if (loading) "Loading…" else "Get tide times") }
        }
        Panel { Text(status, fontWeight = FontWeight.Bold, color = Ink); if (data.isNotBlank()) Text(data, color = Ink) }
    }
}

@Composable
fun WeatherScreen(location: Location?) {
    var latText by remember { mutableStateOf(location?.latitude?.toString() ?: "") }
    var lonText by remember { mutableStateOf(location?.longitude?.toString() ?: "") }
    var report by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Load a live forecast for your location.") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Page("Weather & marine", "Live forecast from Open-Meteo") {
        Panel {
            OutlinedTextField(latText, { latText = it }, label = { Text("Latitude") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(lonText, { lonText = it }, label = { Text("Longitude") }, modifier = Modifier.fillMaxWidth())
            Button(enabled = latText.toDoubleOrNull() != null && lonText.toDoubleOrNull() != null && !loading, onClick = {
                loading = true; status = "Loading forecast…"; report = ""
                scope.launch {
                    try {
                        val json = withContext(Dispatchers.IO) { getJson("https://api.open-meteo.com/v1/forecast?latitude=${latText}&longitude=${lonText}&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m&hourly=wind_speed_10m,wind_direction_10m,precipitation_probability&daily=sunrise,sunset&timezone=auto&forecast_days=2") }
                        val c = json.getJSONObject("current"); val u = json.getJSONObject("current_units")
                        report = listOf(
                            "Temperature: ${c.optDouble("temperature_2m")} ${u.optString("temperature_2m")}",
                            "Feels like: ${c.optDouble("apparent_temperature")}",
                            "Wind: ${c.optDouble("wind_speed_10m")} ${u.optString("wind_speed_10m")} from ${c.optInt("wind_direction_10m")}°",
                            "Wind gusts: ${c.optDouble("wind_gusts_10m")}",
                            "Rain: ${c.optDouble("precipitation")} mm",
                            "Humidity: ${c.optInt("relative_humidity_2m")}%",
                            "Pressure: ${c.optDouble("pressure_msl")} hPa",
                            "Sunrise: ${json.getJSONObject("daily").getJSONArray("sunrise").optString(0)}",
                            "Sunset: ${json.getJSONObject("daily").getJSONArray("sunset").optString(0)}"
                        ).joinToString("\n")
                        status = "Live forecast updated: ${c.optString("time")}"
                    } catch (e: Exception) { status = "Forecast unavailable: ${e.message}" }
                    loading = false
                }
            }) { Text(if (loading) "Loading…" else "Refresh forecast") }
        }
        Panel { Text(status, color = Ink); if (report.isNotBlank()) Text(report, color = Ink) }
        Panel { Text("Fishing note", fontWeight = FontWeight.Bold, color = Ink); Text("Wind and gusts are important for exposed piers and boats. Open-Meteo provides forecast conditions, not a guaranteed fishing prediction. Check local marine warnings before departure.", color = Color.Gray) }
    }
}

@Composable
fun GuideScreen() {
    var species by remember { mutableStateOf("Snapper") }
    val advice = when (species) {
        "Flathead" -> "Soft plastics (3–4 inch) or pilchard strips. 6–10 lb braid, 10–15 lb leader, 1/8–1/4 oz jighead. Work sandy edges and channels with slow hops."
        "Kingfish" -> "Live squid or slimy mackerel, or stickbaits. 30–60 lb braid and 40–60 lb leader. Look for current lines, bait schools and structure."
        "Bream" -> "Small soft plastics, worms or prawn. 4–6 lb braid, 6–8 lb leader. Target structure, weed edges and snags with light jigheads."
        "Whiting" -> "Worms, pipis or small soft plastics. 4–6 lb braid, light leader. Drift or cast onto sand flats and channels."
        else -> "Soft plastics, pilchards or squid strips. 8–15 lb braid, 12–20 lb leader. Fish structure, reefs, channels and current lines."
    }
    Page("Tackle & rig guide", "Practical starting setups — adjust to local rules and conditions") {
        Panel {
            Text("Choose target species", fontWeight = FontWeight.Bold, color = Ink)
            listOf("Snapper", "Flathead", "Kingfish", "Bream", "Whiting").forEach { item ->
                Row(Modifier.fillMaxWidth().clickable { species = item }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = species == item, onClick = { species = item })
                    Text(item, color = Ink)
                }
            }
        }
        Panel { Text("$species setup", fontWeight = FontWeight.Bold, color = Ink); Text(advice, color = Ink) }
        Panel { Text("Before you fish", fontWeight = FontWeight.Bold, color = Ink); Text("Check current Victorian or relevant state size and bag limits, closed seasons, marine park rules and local access restrictions. This is general guidance, not a guarantee of catch.", color = Color.Gray) }
    }
}

@Composable
fun ReportsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Page("Fishing reports", "Open regional reports from established fishing communities") {
        Panel {
            Text("Live report sources", fontWeight = FontWeight.Bold, color = Ink)
            Text("These links open the source websites directly. ANGLR does not claim to have aggregated or verified their reports inside the app.", color = Color.Gray)
            val sources = listOf(
                "Fishing Victoria — regional community reports" to "https://www.fishing-victoria.com/",
                "FishingMad — Melbourne & Victoria reports" to "https://fishingmad.com.au/reports/",
                "Victorian Fisheries Authority — fishing information" to "https://www.vfa.vic.gov.au/"
            )
            sources.forEach { (label, url) ->
                OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }, modifier = Modifier.fillMaxWidth()) { Text(label) }
            }
        }
        Panel {
            Text("Local fishing log", fontWeight = FontWeight.Bold, color = Ink)
            Text("Save your own catch notes on this device from the Notes app for now. An online community feed and report moderation still require a backend service.", color = Color.Gray)
        }
    }
}

fun getJson(address: String): JSONObject {
    val connection = URL(address).openConnection() as HttpURLConnection
    connection.connectTimeout = 15000; connection.readTimeout = 20000
    connection.setRequestProperty("User-Agent", "ANGLR-Android/0.2")
    return try {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) throw IllegalStateException("HTTP $code: $body")
        JSONObject(body)
    } finally { connection.disconnect() }
}
