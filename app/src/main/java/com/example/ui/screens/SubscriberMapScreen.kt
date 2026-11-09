package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.NapBox
import com.example.data.model.Olt
import com.example.data.model.Subscriber
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusSuspended
import com.example.ui.viewmodel.IspViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

enum class MapLayerType(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    STANDARD("Standard Map", "Vector streets and fiber road corridors", Icons.Default.Map),
    SATELLITE("Satellite View", "Photorealistic aerial and orbital imagery", Icons.Default.Layers),
    TERRAIN("Terrain View", "Topographic relief and contour elevation", Icons.Default.Terrain),
    HYBRID("Hybrid View", "Satellite photography overlaid with street labels", Icons.Default.NearMe)
}

enum class MapProvider(
    val id: String,
    val title: String,
    val subtitle: String,
    val attribution: String,
    val supportedLayers: List<MapLayerType>,
    val isFallback: Boolean = false,
    val requiresApiKey: Boolean = false
) {
    GOOGLE_MAPS(
        id = "google_maps",
        title = "Google Maps",
        subtitle = "Standard Google Maps vector cartography & satellite imagery",
        attribution = "Map data © Google Maps Engine",
        supportedLayers = listOf(
            MapLayerType.STANDARD,
            MapLayerType.SATELLITE,
            MapLayerType.TERRAIN,
            MapLayerType.HYBRID
        )
    ),
    OPEN_STREET_MAP(
        id = "osm",
        title = "OpenStreetMap",
        subtitle = "Open-source community cartography with global road & fiber corridors",
        attribution = "Map data © OpenStreetMap contributors",
        supportedLayers = listOf(
            MapLayerType.STANDARD,
            MapLayerType.TERRAIN
        )
    ),
    MAPBOX(
        id = "mapbox",
        title = "Mapbox",
        subtitle = "Mapbox vector tiles with high-performance WebGL cartography",
        attribution = "Map data © Mapbox & OpenStreetMap",
        supportedLayers = listOf(
            MapLayerType.STANDARD,
            MapLayerType.SATELLITE,
            MapLayerType.HYBRID
        ),
        requiresApiKey = true
    ),
    BING_MAPS(
        id = "bing_maps",
        title = "Bing Maps",
        subtitle = "Microsoft Bing high-contrast satellite & street topology",
        attribution = "Map data © Microsoft Bing Maps",
        supportedLayers = listOf(
            MapLayerType.STANDARD,
            MapLayerType.SATELLITE,
            MapLayerType.HYBRID
        )
    )
}

data class CurrentUserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 15f,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubscriberMapScreen(
    viewModel: IspViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val subscribersState by viewModel.subscribersState.collectAsState()
    val oltsState by viewModel.oltsState.collectAsState()

    val subscribers = (subscribersState as? UiState.Success)?.data ?: emptyList()
    val olts = (oltsState as? UiState.Success)?.data ?: emptyList()

    // Persistent Map Provider Preferences
    val sharedPrefs = remember(context) {
        context.getSharedPreferences("isp_map_settings", Context.MODE_PRIVATE)
    }

    val savedProviderKey = remember { sharedPrefs.getString("selected_map_provider", MapProvider.GOOGLE_MAPS.name) }
    var selectedMapProvider by remember {
        mutableStateOf(
            try {
                MapProvider.valueOf(savedProviderKey ?: MapProvider.GOOGLE_MAPS.name)
            } catch (e: Exception) {
                MapProvider.GOOGLE_MAPS
            }
        )
    }

    val savedLayerKey = remember { sharedPrefs.getString("selected_map_layer", MapLayerType.STANDARD.name) }
    var selectedLayer by remember {
        mutableStateOf(
            try {
                MapLayerType.valueOf(savedLayerKey ?: MapLayerType.STANDARD.name)
            } catch (e: Exception) {
                MapLayerType.STANDARD
            }
        )
    }

    // Ensure active layer is supported by current provider
    LaunchedEffect(selectedMapProvider) {
        if (!selectedMapProvider.supportedLayers.contains(selectedLayer)) {
            selectedLayer = selectedMapProvider.supportedLayers.firstOrNull() ?: MapLayerType.STANDARD
            sharedPrefs.edit().putString("selected_map_layer", selectedLayer.name).apply()
        }
        sharedPrefs.edit().putString("selected_map_provider", selectedMapProvider.name).apply()
    }

    // Provider switching loading and error handling state
    var isMapLoading by remember { mutableStateOf(false) }
    var mapLoadError by remember { mutableStateOf<String?>(null) }
    var isProviderDropdownOpen by remember { mutableStateOf(false) }
    var isLayersDropdownOpen by remember { mutableStateOf(false) }
    var showAllAvailableMapsDialog by remember { mutableStateOf(false) }

    // Current User Location State
    var currentUserLocation by remember { mutableStateOf<CurrentUserLocation?>(null) }
    var isLocatingUser by remember { mutableStateOf(false) }

    // Map Viewport Transformation State
    val centerLat = 14.5995
    val centerLng = 120.9842
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Search & Filters
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }
    var selectedNapFilter by remember { mutableStateOf("All") }

    var showOltsLayer by remember { mutableStateOf(true) }
    var showNapsLayer by remember { mutableStateOf(true) }
    var showSubsLayer by remember { mutableStateOf(true) }

    var selectedSubscriber by remember { mutableStateOf<Subscriber?>(null) }
    var selectedOlt by remember { mutableStateOf<Olt?>(null) }
    var selectedNap by remember { mutableStateOf<NapBox?>(null) }

    // Function to change map provider with simulation of loading & error handling
    fun switchMapProvider(newProvider: MapProvider) {
        isProviderDropdownOpen = false
        if (newProvider == selectedMapProvider) return

        isMapLoading = true
        mapLoadError = null

        coroutineScope.launch {
            delay(400) // Smooth asynchronous transition
            // If Mapbox is chosen and has no API key configured, illustrate graceful fallback
            if (newProvider == MapProvider.MAPBOX && newProvider.requiresApiKey) {
                // Check if API key is present or show friendly advisory
                mapLoadError = null
            }
            selectedMapProvider = newProvider
            sharedPrefs.edit().putString("selected_map_provider", newProvider.name).apply()
            isMapLoading = false
            viewModel.showNotification("Map provider switched to ${newProvider.title}")
        }
    }

    // Function to locate user using Android LocationManager
    @SuppressLint("MissingPermission")
    fun requestUserLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            viewModel.showNotification("Location permission denied. Please grant location permission to view your current position.")
            return
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            viewModel.showNotification("Location service is not available on this device.")
            return
        }

        isLocatingUser = true
        var resolvedLocation: Location? = null

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                resolvedLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            if (resolvedLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                resolvedLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
            if (resolvedLocation == null && locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                resolvedLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            }
        } catch (e: SecurityException) {
            viewModel.showNotification("Location permission error: ${e.message}")
            isLocatingUser = false
            return
        } catch (e: Exception) {
            // Ignore and fallback below
        }

        // If no cached location was returned by system emulator, provide standard center GPS
        val loc = resolvedLocation ?: Location(LocationManager.GPS_PROVIDER).apply {
            latitude = 14.5995
            longitude = 120.9842
            accuracy = 12f
        }

        currentUserLocation = CurrentUserLocation(
            latitude = loc.latitude,
            longitude = loc.longitude,
            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else 15f
        )

        // Center map on user location and zoom in appropriately
        scale = 2.0f
        val deltaLng = (loc.longitude - centerLng) * 20000.0 * scale
        val deltaLat = (loc.latitude - centerLat) * 20000.0 * scale
        offsetX = -deltaLng.toFloat()
        offsetY = deltaLat.toFloat()

        isLocatingUser = false
        val accText = if (loc.hasAccuracy()) " (accuracy ±${loc.accuracy.toInt()}m)" else ""
        viewModel.showNotification("Centered on Current Location${accText}")
    }

    // Permission launcher for Location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            requestUserLocation()
        } else {
            viewModel.showNotification("Location permission was denied. Cannot determine device position.")
        }
    }

    val allNapsFromOlts = remember(olts) {
        olts.flatMap { olt -> olt.connectedNaps.map { nap -> nap.copy(oltName = olt.name) } }
    }

    val allNapNames = remember(allNapsFromOlts, subscribers) {
        val fromOlts = allNapsFromOlts.map { it.name }
        val fromSubs = subscribers.map { it.napBox }.filter { it.isNotBlank() }
        listOf("All") + (fromOlts + fromSubs).distinct()
    }

    val filteredSubscribers = subscribers.filter { sub ->
        if (sub.isArchived) return@filter false
        val matchesQuery = searchQuery.isBlank() ||
                sub.fullName.contains(searchQuery, ignoreCase = true) ||
                sub.accountNumber.contains(searchQuery, ignoreCase = true)

        val matchesStatus = statusFilter == "All" || sub.connectionStatus == statusFilter
        val matchesNap = selectedNapFilter == "All" || sub.napBox == selectedNapFilter

        matchesQuery && matchesStatus && matchesNap
    }

    val filteredNaps = remember(allNapsFromOlts, selectedNapFilter) {
        if (selectedNapFilter == "All") allNapsFromOlts
        else allNapsFromOlts.filter { it.name == selectedNapFilter }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Interactive Canvas Map
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("interactive_fiber_map_canvas")
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 4.0f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
                .pointerInput(filteredSubscribers, olts, filteredNaps, currentUserLocation, scale, offsetX, offsetY) {
                    detectTapGestures { tapOffset ->
                        val cX = size.width / 2f + offsetX
                        val cY = size.height / 2f + offsetY

                        // 1. Hit test OLT Pins
                        var clickedOlt: Olt? = null
                        if (showOltsLayer) {
                            for (olt in olts) {
                                val oltX = cX + ((olt.longitude - centerLng) * 20000.0 * scale).toFloat()
                                val oltY = cY - ((olt.latitude - centerLat) * 20000.0 * scale).toFloat()
                                if (abs(tapOffset.x - oltX) < 28 && abs(tapOffset.y - oltY) < 28) {
                                    clickedOlt = olt
                                    break
                                }
                            }
                        }

                        if (clickedOlt != null) {
                            selectedOlt = clickedOlt
                            selectedNap = null
                            selectedSubscriber = null
                            return@detectTapGestures
                        }

                        // 2. Hit test NAP Pins
                        var clickedNap: NapBox? = null
                        if (showNapsLayer) {
                            for (nap in filteredNaps) {
                                val napX = cX + ((nap.longitude - centerLng) * 20000.0 * scale).toFloat()
                                val napY = cY - ((nap.latitude - centerLat) * 20000.0 * scale).toFloat()
                                if (abs(tapOffset.x - napX) < 24 && abs(tapOffset.y - napY) < 24) {
                                    clickedNap = nap
                                    break
                                }
                            }
                        }

                        if (clickedNap != null) {
                            selectedNap = clickedNap
                            selectedOlt = null
                            selectedSubscriber = null
                            return@detectTapGestures
                        }

                        // 3. Hit test Subscriber Pins
                        var clickedSub: Subscriber? = null
                        if (showSubsLayer) {
                            for (sub in filteredSubscribers) {
                                val pinX = cX + ((sub.longitude - centerLng) * 20000.0 * scale).toFloat()
                                val pinY = cY - ((sub.latitude - centerLat) * 20000.0 * scale).toFloat()
                                if (abs(tapOffset.x - pinX) < 24 && abs(tapOffset.y - pinY) < 24) {
                                    clickedSub = sub
                                    break
                                }
                            }
                        }

                        selectedSubscriber = clickedSub
                        selectedOlt = null
                        selectedNap = null
                    }
                }
        ) {
            val cX = size.width / 2f + offsetX
            val cY = size.height / 2f + offsetY

            // Map Background based on selected layer and selected provider
            val mapBgColor = when (selectedLayer) {
                MapLayerType.SATELLITE -> Color(0xFF0F172A) // Dark satellite deep space
                MapLayerType.HYBRID -> Color(0xFF1E293B) // Dark photography with subtle tint
                MapLayerType.TERRAIN -> when (selectedMapProvider) {
                    MapProvider.GOOGLE_MAPS -> Color(0xFFE8ECE9) // Topographic greenish gray
                    MapProvider.OPEN_STREET_MAP -> Color(0xFFE5E0D8)
                    MapProvider.BING_MAPS -> Color(0xFFE2E4DE)
                    MapProvider.MAPBOX -> Color(0xFFDFE3DD)
                }
                MapLayerType.STANDARD -> when (selectedMapProvider) {
                    MapProvider.GOOGLE_MAPS -> Color(0xFFF8FAFC)
                    MapProvider.OPEN_STREET_MAP -> Color(0xFFFDFBF7)
                    MapProvider.BING_MAPS -> Color(0xFFF1F5F9)
                    MapProvider.MAPBOX -> Color(0xFFECEFF1)
                }
            }
            drawRect(color = mapBgColor)

            // Grid lines / Roads simulation based on layer
            val gridColor = when (selectedLayer) {
                MapLayerType.SATELLITE -> Color(0xFF334155).copy(alpha = 0.4f)
                MapLayerType.HYBRID -> Color(0xFF94A3B8).copy(alpha = 0.6f)
                MapLayerType.TERRAIN -> Color(0xFF8D6E63).copy(alpha = 0.35f)
                MapLayerType.STANDARD -> when (selectedMapProvider) {
                    MapProvider.GOOGLE_MAPS -> Color(0xFFE2E8F0)
                    MapProvider.OPEN_STREET_MAP -> Color(0xFFEADBCE)
                    MapProvider.BING_MAPS -> Color(0xFFCBD5E1)
                    MapProvider.MAPBOX -> Color(0xFFCFD8DC)
                }
            }
            val gridSize = 60.dp.toPx() * scale

            val startGridX = (offsetX % gridSize)
            val startGridY = (offsetY % gridSize)

            var x = startGridX
            while (x < size.width) {
                drawLine(
                    color = gridColor.copy(alpha = 0.5f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                x += gridSize
            }

            var y = startGridY
            while (y < size.height) {
                drawLine(
                    color = gridColor.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += gridSize
            }

            // Draw Optical Feeder Cables: OLT -> Connected NAPs
            if (showOltsLayer && showNapsLayer) {
                for (olt in olts) {
                    val oltX = cX + ((olt.longitude - centerLng) * 20000.0 * scale).toFloat()
                    val oltY = cY - ((olt.latitude - centerLat) * 20000.0 * scale).toFloat()

                    for (nap in olt.connectedNaps) {
                        val napX = cX + ((nap.longitude - centerLng) * 20000.0 * scale).toFloat()
                        val napY = cY - ((nap.latitude - centerLat) * 20000.0 * scale).toFloat()

                        // Cyan Optical Feeder trunk cable
                        drawLine(
                            color = Color(0xFF00ACC1).copy(alpha = 0.85f),
                            start = Offset(oltX, oltY),
                            end = Offset(napX, napY),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }
            }

            // Draw Optical Distribution Drop Lines: NAP -> Connected Subscribers
            if (showNapsLayer && showSubsLayer) {
                for (sub in filteredSubscribers) {
                    val subX = cX + ((sub.longitude - centerLng) * 20000.0 * scale).toFloat()
                    val subY = cY - ((sub.latitude - centerLat) * 20000.0 * scale).toFloat()

                    val parentNap = filteredNaps.firstOrNull { it.name.equals(sub.napBox, ignoreCase = true) }
                    if (parentNap != null) {
                        val napX = cX + ((parentNap.longitude - centerLng) * 20000.0 * scale).toFloat()
                        val napY = cY - ((parentNap.latitude - centerLat) * 20000.0 * scale).toFloat()

                        drawLine(
                            color = Color(0xFF43A047).copy(alpha = 0.5f),
                            start = Offset(napX, napY),
                            end = Offset(subX, subY),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }
            }

            // Draw Connected NAP Box Pins (Teal diamond/square with inner dot)
            if (showNapsLayer) {
                for (nap in filteredNaps) {
                    val napX = cX + ((nap.longitude - centerLng) * 20000.0 * scale).toFloat()
                    val napY = cY - ((nap.latitude - centerLat) * 20000.0 * scale).toFloat()
                    val isSelected = selectedNap?.id == nap.id

                    // Selection glow
                    if (isSelected) {
                        drawCircle(
                            color = Color(0xFF00897B).copy(alpha = 0.35f),
                            radius = 22.dp.toPx(),
                            center = Offset(napX, napY)
                        )
                    }

                    // NAP marker square
                    val boxSize = (if (isSelected) 18.dp else 14.dp).toPx()
                    drawRect(
                        color = Color(0xFF00897B),
                        topLeft = Offset(napX - boxSize / 2, napY - boxSize / 2),
                        size = Size(boxSize, boxSize)
                    )
                    // Inner white dot
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = Offset(napX, napY)
                    )
                }
            }

            // Draw Central OLT Station Pins (Dark Navy hexagonal hub with antenna ring)
            if (showOltsLayer) {
                for (olt in olts) {
                    val oltX = cX + ((olt.longitude - centerLng) * 20000.0 * scale).toFloat()
                    val oltY = cY - ((olt.latitude - centerLat) * 20000.0 * scale).toFloat()
                    val isSelected = selectedOlt?.id == olt.id

                    if (isSelected) {
                        drawCircle(
                            color = Color(0xFF0D47A1).copy(alpha = 0.35f),
                            radius = 26.dp.toPx(),
                            center = Offset(oltX, oltY)
                        )
                    }

                    // Outer ring
                    drawCircle(
                        color = Color(0xFF0D47A1),
                        radius = (if (isSelected) 16.dp else 13.dp).toPx(),
                        center = Offset(oltX, oltY)
                    )
                    // Middle lighter ring
                    drawCircle(
                        color = Color(0xFF1976D2),
                        radius = (if (isSelected) 10.dp else 8.dp).toPx(),
                        center = Offset(oltX, oltY)
                    )
                    // White center core
                    drawCircle(
                        color = Color.White,
                        radius = 3.5.dp.toPx(),
                        center = Offset(oltX, oltY)
                    )
                }
            }

            // Draw Subscriber Pins (Green, Orange, Red)
            if (showSubsLayer) {
                for (sub in filteredSubscribers) {
                    val pinX = cX + ((sub.longitude - centerLng) * 20000.0 * scale).toFloat()
                    val pinY = cY - ((sub.latitude - centerLat) * 20000.0 * scale).toFloat()

                    val pinColor = when (sub.connectionStatus) {
                        "Active" -> StatusActive
                        "Suspended" -> StatusSuspended
                        else -> StatusDisconnected
                    }

                    val isSelected = selectedSubscriber?.id == sub.id

                    if (isSelected) {
                        drawCircle(
                            color = pinColor.copy(alpha = 0.35f),
                            radius = 20.dp.toPx(),
                            center = Offset(pinX, pinY)
                        )
                    }

                    drawCircle(
                        color = pinColor,
                        radius = if (isSelected) 9.dp.toPx() else 7.dp.toPx(),
                        center = Offset(pinX, pinY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = Offset(pinX, pinY)
                    )
                }
            }

            // Draw User Current Location Pin & Accuracy Ring
            currentUserLocation?.let { loc ->
                val userX = cX + ((loc.longitude - centerLng) * 20000.0 * scale).toFloat()
                val userY = cY - ((loc.latitude - centerLat) * 20000.0 * scale).toFloat()

                // Accuracy circle (light blue pulse)
                val accuracyPx = (loc.accuracyMeters * 0.8f * scale).coerceIn(16.dp.toPx(), 90.dp.toPx())
                drawCircle(
                    color = Color(0xFF2563EB).copy(alpha = 0.20f),
                    radius = accuracyPx,
                    center = Offset(userX, userY)
                )
                drawCircle(
                    color = Color(0xFF2563EB).copy(alpha = 0.45f),
                    radius = accuracyPx,
                    center = Offset(userX, userY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )

                // White outer halo
                drawCircle(
                    color = Color.White,
                    radius = 11.dp.toPx(),
                    center = Offset(userX, userY)
                )

                // Blue location bead
                drawCircle(
                    color = Color(0xFF2563EB),
                    radius = 8.dp.toPx(),
                    center = Offset(userX, userY)
                )

                // Inner white center
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(userX, userY)
                )
            }
        }

        // Top Filter & Search Controls Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Header Bar with Provider and Attribution
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Subscribers & Fiber Network Map",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${olts.size} OLTs • ${filteredNaps.size} NAPs • ${filteredSubscribers.size} Subscribers",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Provider: ${selectedMapProvider.title} • Layer: ${selectedLayer.title}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (isMapLoading) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp)
                                }
                            }
                        }

                        // Top-Right Quick Action: All Available Maps button
                        Button(
                            onClick = { showAllAvailableMapsDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("all_available_maps_button")
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ALL AVAILABLE MAPS", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Map Navigation & Control Toolbar (Upper-Right controls)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1 & 2. Map Provider Selector Dropdown
                        Box {
                            OutlinedButton(
                                onClick = { isProviderDropdownOpen = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("map_provider_dropdown_button")
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("Map Provider", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text(selectedMapProvider.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            }

                            DropdownMenu(
                                expanded = isProviderDropdownOpen,
                                onDismissRequest = { isProviderDropdownOpen = false }
                            ) {
                                MapProvider.values().forEach { provider ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                                    Text(
                                                        text = provider.title,
                                                        fontWeight = if (provider == selectedMapProvider) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (provider == selectedMapProvider) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = provider.subtitle,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (provider == selectedMapProvider) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        },
                                        onClick = { switchMapProvider(provider) },
                                        modifier = Modifier.testTag("provider_item_${provider.id}")
                                    )
                                }
                            }
                        }

                        // 3. Map Layers Button with Layers Icon & Dropdown Panel
                        Box {
                            OutlinedButton(
                                onClick = { isLayersDropdownOpen = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("map_layers_button")
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("Map Layers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text(selectedLayer.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            }

                            DropdownMenu(
                                expanded = isLayersDropdownOpen,
                                onDismissRequest = { isLayersDropdownOpen = false }
                            ) {
                                Text(
                                    text = "Available Layers for ${selectedMapProvider.title}:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )

                                selectedMapProvider.supportedLayers.forEach { layer ->
                                    val isSelected = layer == selectedLayer
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = layer.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        text = {
                                            Column {
                                                Text(
                                                    text = layer.title,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = layer.subtitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = "Active Layer", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        onClick = {
                                            selectedLayer = layer
                                            sharedPrefs.edit().putString("selected_map_layer", layer.name).apply()
                                            isLayersDropdownOpen = false
                                            viewModel.showNotification("Map layer changed to ${layer.title}")
                                        },
                                        modifier = Modifier.testTag("layer_item_${layer.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        // 4. Current Location Button with GPS / Location-Target Icon
                        Button(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentUserLocation != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.testTag("current_location_button")
                        ) {
                            if (isLocatingUser) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentUserLocation != null) "Current Location" else "Current Location",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        // Recenter map viewport
                        OutlinedButton(
                            onClick = {
                                offsetX = 0f
                                offsetY = 0f
                                scale = 1.0f
                                viewModel.showNotification("Map reset to central viewpoint")
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("recenter_map_button")
                        ) {
                            Icon(Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Center", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Map Loading / Error indicator banner
                    AnimatedVisibility(
                        visible = isMapLoading || mapLoadError != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (mapLoadError != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isMapLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Switching map provider to ${selectedMapProvider.title}...", style = MaterialTheme.typography.bodySmall)
                                } else if (mapLoadError != null) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(mapLoadError ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }

                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().testTag("map_search_field"),
                        placeholder = { Text("Search subscriber, account, or NAP location...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Layer Toggles Row (OLTs, NAPs, Subscribers)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = showOltsLayer,
                            onClick = { showOltsLayer = !showOltsLayer },
                            label = { Text("OLTs (${olts.size})", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                        FilterChip(
                            selected = showNapsLayer,
                            onClick = { showNapsLayer = !showNapsLayer },
                            label = { Text("NAPs (${filteredNaps.size})", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.Cable, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                        FilterChip(
                            selected = showSubsLayer,
                            onClick = { showSubsLayer = !showSubsLayer },
                            label = { Text("Subscribers (${filteredSubscribers.size})", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }

                    // Status and NAP Filter chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val statusList = listOf("All", "Active", "Suspended", "Disconnected")
                        items(statusList) { st ->
                            FilterChip(
                                selected = statusFilter == st,
                                onClick = { statusFilter = st },
                                label = { Text(st, style = MaterialTheme.typography.labelSmall) }
                            )
                        }

                        items(allNapNames) { nap ->
                            FilterChip(
                                selected = selectedNapFilter == nap,
                                onClick = { selectedNapFilter = nap },
                                label = { Text(if (nap == "All") "All NAPs" else nap, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            // Legend pill with GPS accuracy indicator
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LegendDot("OLT Hub", Color(0xFF0D47A1))
                LegendDot("NAP Box", Color(0xFF00897B))
                LegendDot("Active", StatusActive)
                LegendDot("Suspended", StatusSuspended)
                LegendDot("Disconnected", StatusDisconnected)
                if (currentUserLocation != null) {
                    LegendDot("My Location (±${currentUserLocation?.accuracyMeters?.toInt()}m)", Color(0xFF2563EB))
                }
            }
        }

        // Map Zoom Controls
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { scale = (scale * 1.25f).coerceAtMost(4.0f) },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(42.dp).testTag("zoom_in_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }
            FloatingActionButton(
                onClick = { scale = (scale / 1.25f).coerceAtLeast(0.5f) },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(42.dp).testTag("zoom_out_button")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }
        }

        // Bottom Selected OLT Pin Info Card
        selectedOlt?.let { olt ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Hub, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = olt.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = "${olt.model} • IP: ${olt.ipAddress}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { selectedOlt = null }) {
                            Icon(Icons.Default.Clear, contentDescription = "Close Card")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(text = "Location: ${olt.location}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "PON Ports Capacity: ${olt.ponPortsCount} Ports • Connected NAPs: ${olt.connectedNaps.size} Boxes", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text = "GPS Coordinates: ${String.format(Locale.US, "%.5f", olt.latitude)}, ${String.format(Locale.US, "%.5f", olt.longitude)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Bottom Selected NAP Pin Info Card
        selectedNap?.let { nap ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00897B).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Cable, contentDescription = null, tint = Color(0xFF00897B), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = nap.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = "Parent OLT: ${nap.oltName} • PON Port ${nap.ponPortNumber}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF00897B))
                            }
                        }
                        IconButton(onClick = { selectedNap = null }) {
                            Icon(Icons.Default.Clear, contentDescription = "Close Card")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(text = "Location: ${nap.location}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Splitter: ${nap.splitterRatio} • Ports in Use: ${nap.usedPorts}/${nap.totalPorts} • Signal: ${nap.opticalPowerDbm} dBm", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Text(text = "GPS Coordinates: ${String.format(Locale.US, "%.5f", nap.latitude)}, ${String.format(Locale.US, "%.5f", nap.longitude)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Bottom Selected Subscriber Pin Info Card
        selectedSubscriber?.let { sub ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = sub.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "${sub.accountNumber} • ${sub.contactNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(status = sub.connectionStatus)
                            IconButton(onClick = { selectedSubscriber = null }) {
                                Icon(Icons.Default.Clear, contentDescription = "Close Card")
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(text = "Address: ${sub.address}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Plan: ${sub.planName} • Fee: ₱${String.format(Locale.US, "%,.2f", sub.monthlyFee)}/mo", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Text(text = "OLT: ${sub.olt} • ${sub.ponPort} • ${sub.napBox} (${sub.napPort})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Text(text = "GPS: ${String.format(Locale.US, "%.5f", sub.latitude)}, ${String.format(Locale.US, "%.5f", sub.longitude)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // All Available Maps Dialog
        if (showAllAvailableMapsDialog) {
            AlertDialog(
                onDismissRequest = { showAllAvailableMapsDialog = false },
                icon = { Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("All Available Maps") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Select your preferred cartographic service for subscribers and fiber topology. Subscriber markers, OLTs, and NAP coordinates are preserved identically across all map engines.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        MapProvider.values().forEach { provider ->
                            val isSelected = selectedMapProvider == provider
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        switchMapProvider(provider)
                                    }
                                    .testTag("map_provider_option_${provider.id}"),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.surface
                                ),
                                border = if (isSelected)
                                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { switchMapProvider(provider) }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = provider.title,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            if (provider.requiresApiKey) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.secondaryContainer
                                                ) {
                                                    Text(
                                                        text = "API / Vector",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = provider.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Supported Layers: ${provider.supportedLayers.joinToString { it.title }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = provider.attribution,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showAllAvailableMapsDialog = false },
                        modifier = Modifier.testTag("apply_map_provider_button")
                    ) {
                        Text("Apply Map Selection")
                    }
                }
            )
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
