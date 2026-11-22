package com.anglr.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private val Navy = Color(0xFF071A2D)
private val Sea = Color(0xFF0B8E9A)
private val Aqua = Color(0xFF65D7C5)
private val Sand = Color(0xFFF3F6F7)
private val Ink = Color(0xFF10283B)

class MainActivity : ComponentActivity() {
    private var locationGranted by mutableStateOf(false)
    private val requestLocation = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        locationGranted = result.values.any { it }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Sea, secondary = Aqua, background = Sand)) {
                AnglrHome(locationGranted) { requestLocation.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) }
            }
        }
    }
}

@Composable
fun AnglrHome(locationGranted: Boolean, askLocation: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    Surface(modifier = Modifier.fillMaxSize(), color = Sand) {
        Column {
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 20.dp)) {
                item {
                    Column(Modifier.fillMaxWidth().background(Navy).padding(22.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Waves, null, tint = Aqua, modifier = Modifier.size(30.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("ANGLR", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                            Spacer(Modifier.weight(1f))
                            Text("BETA", color = Aqua, style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.height(22.dp))
                        Text("READ THE WATER.", color = Aqua, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("Find your next bite.", color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Local intel for smarter fishing.", color = Color(0xFFB7C8D5))
                    }
                }
                item {
                    Column(Modifier.padding(16.dp)) {
                        if (!locationGranted) {
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, tint = Sea, modifier = Modifier.size(32.dp))
                                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                        Text("Turn on nearby fishing intel", fontWeight = FontWeight.Bold, color = Ink)
                                        Text("Allow location to tailor spots, weather and tides.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Button(onClick = askLocation) { Text("Enable") }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                        }
                        Text("YOUR FISHING SNAPSHOT", color = Ink, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Snapshot("BITE WINDOW", "Dawn", "Best low-light", Icons.Default.WbTwilight, Modifier.weight(1f))
                            Snapshot("TIDE", "Incoming", "Check local tide", Icons.Default.Waves, Modifier.weight(1f))
                            Snapshot("MOON", "Waxing", "Lunar phase", Icons.Default.DarkMode, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("TODAY'S GAME PLAN", color = Ink, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        FeatureCard(Icons.Default.Place, "Find fishing spots", "Piers, beaches, estuaries and boat access nearby", "Explore spots")
                        FeatureCard(Icons.Default.Tsunami, "Tides & conditions", "Tide times, wind, swell, rain and water conditions", "Conditions")
                        FeatureCard(Icons.Default.Fishing, "Tackle & rig guide", "Species-specific setups, bait, lures and knots", "Rig up")
                        FeatureCard(Icons.Default.Article, "Local fishing reports", "Recent reports and bite updates from multiple sources", "Reports")
                        Spacer(Modifier.height(8.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.padding(18.dp)) {
                                Text("ANGLR FIELD NOTE", color = Aqua, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Text("Fishing forecasts will combine weather, tide movement, moon phase, season and verified local reports. Always check live data before heading out.", color = Color.White)
                            }
                        }
                    }
                }
            }
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Map, null) }, label = { Text("Spots") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.Waves, null) }, label = { Text("Tides") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.MenuBook, null) }, label = { Text("Guide") })
            }
        }
    }
}

@Composable
fun Snapshot(title: String, value: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, null, tint = Sea, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(value, fontWeight = FontWeight.Bold, color = Ink)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun FeatureCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String, action: String) {
    Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Color(0xFFE3F4F2), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Sea, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, color = Ink, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Text(action, color = Sea, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        }
    }
}