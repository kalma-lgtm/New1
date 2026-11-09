package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.NapBox
import com.example.data.model.Olt
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusSuspended
import java.util.Locale

@Composable
fun OltManagementTab(
    olts: List<Olt>,
    onOpenAddOlt: () -> Unit,
    onOpenAddNap: (oltId: String, ponPortNumber: Int) -> Unit,
    onDeleteOlt: (String) -> Unit,
    onDeleteNap: (oltId: String, napId: String) -> Unit,
    onViewOnMap: (latitude: Double, longitude: Double) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    if (olts.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Text(
                    text = "No OLTs Registered",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Register your central Optical Line Terminal to configure PON ports and connected NAP boxes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onOpenAddOlt,
                    modifier = Modifier.testTag("add_first_olt_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Register First OLT")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Optical Line Terminals (OLT) & PON Infrastructure",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        val totalNaps = olts.sumOf { it.connectedNaps.size }
                        val totalPonPorts = olts.sumOf { it.ponPortsCount }
                        Text(
                            text = "${olts.size} OLTs • $totalPonPorts Total PON Ports • $totalNaps Connected NAPs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onOpenAddOlt,
                        modifier = Modifier.testTag("add_olt_header_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add OLT")
                    }
                }
            }

            items(olts, key = { it.id }) { olt ->
                OltCard(
                    olt = olt,
                    onOpenAddNap = { ponPortNumber -> onOpenAddNap(olt.id, ponPortNumber) },
                    onDeleteOlt = { onDeleteOlt(olt.id) },
                    onDeleteNap = { napId -> onDeleteNap(olt.id, napId) },
                    onViewOnMap = { onViewOnMap(olt.latitude, olt.longitude) }
                )
            }
        }
    }
}

@Composable
fun OltCard(
    olt: Olt,
    onOpenAddNap: (ponPortNumber: Int) -> Unit,
    onDeleteOlt: () -> Unit,
    onDeleteNap: (napId: String) -> Unit,
    onViewOnMap: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    var selectedPonFilter by remember { mutableIntStateOf(0) } // 0 = all ports

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Hub,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = olt.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${olt.model} • ${olt.ipAddress}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status = olt.status)
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                }
            }

            // Location & GPS row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = olt.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { onViewOnMap() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.4f", olt.latitude)}, ${String.format(Locale.US, "%.4f", olt.longitude)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Quick Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "PON Ports Capacity",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${olt.ponPortsCount} PON Ports",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        text = "Connected NAPs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${olt.connectedNaps.size} Boxes Pinned",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = "Total Drops / Ports",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val totalPorts = olt.connectedNaps.sumOf { it.totalPorts }
                    val usedPorts = olt.connectedNaps.sumOf { it.usedPorts }
                    Text(
                        text = "$usedPorts / $totalPorts Used",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalPorts > 0 && usedPorts >= totalPorts) StatusDisconnected else StatusActive
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = "PON Ports & Connected NAP Boxes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Each PON port drives optical distribution cables to NAP splitter boxes pinned on the map.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // PON Port Selector / Loop
                    for (portIndex in 1..olt.ponPortsCount) {
                        val napsOnThisPort = olt.connectedNaps.filter { it.ponPortNumber == portIndex }
                        PonPortItemCard(
                            oltId = olt.id,
                            ponPortNumber = portIndex,
                            naps = napsOnThisPort,
                            onAddNap = { onOpenAddNap(portIndex) },
                            onDeleteNap = onDeleteNap,
                            onViewNapOnMap = onViewOnMap
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDeleteOlt,
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete OLT")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PonPortItemCard(
    oltId: String,
    ponPortNumber: Int,
    naps: List<NapBox>,
    onAddNap: () -> Unit,
    onDeleteNap: (String) -> Unit,
    onViewNapOnMap: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "P$ponPortNumber",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "PON Port $ponPortNumber",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${naps.size} connected NAP(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onAddNap,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_nap_port_${ponPortNumber}_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Connect NAP")
                }
            }

            if (naps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    naps.forEach { nap ->
                        NapBoxRowItem(
                            nap = nap,
                            onDelete = { onDeleteNap(nap.id) },
                            onViewOnMap = onViewNapOnMap
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NapBoxRowItem(
    nap: NapBox,
    onDelete: () -> Unit,
    onViewOnMap: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Cable,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = nap.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF00695C).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = nap.splitterRatio,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00695C),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "📍 ${nap.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "GPS: ${String.format(Locale.US, "%.4f", nap.latitude)}, ${String.format(Locale.US, "%.4f", nap.longitude)} • Ports: ${nap.usedPorts}/${nap.totalPorts} (${nap.opticalPowerDbm} dBm)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onViewOnMap, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "View on Map",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove NAP",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddOltDialog(
    onDismiss: () -> Unit,
    onSave: (Olt) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("Huawei SmartAX EA5800") }
    var ipAddress by remember { mutableStateOf("192.168.10.1") }
    var ponPortsCountText by remember { mutableStateOf("8") }
    var location by remember { mutableStateOf("Central Office Hub, Data Center") }
    var latitudeText by remember { mutableStateOf("14.5995") }
    var longitudeText by remember { mutableStateOf("120.9842") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Hub, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Register New OLT")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Configure the Optical Line Terminal chassis, PON port count, and central coordinates to be pinned on the Map.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("OLT Name / Label *") },
                    placeholder = { Text("e.g. OLT-Main-Downtown") },
                    modifier = Modifier.fillMaxWidth().testTag("olt_name_input")
                )

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("OLT Model & Type") },
                    placeholder = { Text("e.g. Huawei EA5800-X7 / ZTE C320") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = { ipAddress = it },
                    label = { Text("Management IP Address") },
                    placeholder = { Text("192.168.10.1") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Input field for Number of PON Ports
                OutlinedTextField(
                    value = ponPortsCountText,
                    onValueChange = { ponPortsCountText = it.filter { char -> char.isDigit() } },
                    label = { Text("Number of PON Ports *") },
                    placeholder = { Text("e.g. 4, 8, 16") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("olt_pon_ports_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("4", "8", "16").forEach { count ->
                        FilterChip(
                            selected = ponPortsCountText == count,
                            onClick = { ponPortsCountText = count },
                            label = { Text("$count Ports") }
                        )
                    }
                }

                // Input field for Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Installation Location *") },
                    placeholder = { Text("e.g. Central Tower Exchange, 3rd Floor") },
                    modifier = Modifier.fillMaxWidth().testTag("olt_location_input")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latitudeText,
                        onValueChange = { latitudeText = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = longitudeText,
                        onValueChange = { longitudeText = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "This OLT will be pinned on the app's interactive map as the central fiber optic distribution root.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val ports = ponPortsCountText.toIntOrNull() ?: 8
                        val lat = latitudeText.toDoubleOrNull() ?: 14.5995
                        val lng = longitudeText.toDoubleOrNull() ?: 120.9842
                        val newOlt = Olt(
                            name = name,
                            model = model,
                            ipAddress = ipAddress,
                            ponPortsCount = maxOf(1, ports),
                            location = location,
                            latitude = lat,
                            longitude = lng,
                            status = "Online"
                        )
                        onSave(newOlt)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_olt_button")
            ) {
                Text("Register OLT")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddNapDialog(
    olt: Olt,
    preselectedPonPort: Int,
    onDismiss: () -> Unit,
    onSave: (ponPortNumber: Int, NapBox) -> Unit
) {
    var napName by remember { mutableStateOf("NAP-P${preselectedPonPort}-0${olt.connectedNaps.count { it.ponPortNumber == preselectedPonPort } + 1}") }
    var selectedPonPort by remember { mutableIntStateOf(preselectedPonPort) }
    var location by remember { mutableStateOf("") }
    var latitudeText by remember { mutableStateOf(String.format(Locale.US, "%.4f", olt.latitude + 0.003)) }
    var longitudeText by remember { mutableStateOf(String.format(Locale.US, "%.4f", olt.longitude + 0.003)) }
    var totalPortsText by remember { mutableStateOf("8") }
    var splitterRatio by remember { mutableStateOf("1:8 PLC") }
    var opticalPowerText by remember { mutableStateOf("-19.2") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Cable, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect NAP Box")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Connect a Network Access Point (NAP) box to ${olt.name} on PON Port $selectedPonPort. Its location will be pinned on the MAP.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Select PON Port
                Text(text = "Assign to PON Port", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (p in 1..olt.ponPortsCount) {
                        FilterChip(
                            selected = selectedPonPort == p,
                            onClick = { selectedPonPort = p },
                            label = { Text("Port $p") }
                        )
                    }
                }

                // Input field for NAP Name
                OutlinedTextField(
                    value = napName,
                    onValueChange = { napName = it },
                    label = { Text("NAP Box Name / Code *") },
                    placeholder = { Text("e.g. NAP-01-A") },
                    modifier = Modifier.fillMaxWidth().testTag("nap_name_input")
                )

                // Input field for Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("NAP Location / Landmark *") },
                    placeholder = { Text("e.g. Corner Rizal Ave & Bonifacio St, Pole #14") },
                    modifier = Modifier.fillMaxWidth().testTag("nap_location_input")
                )

                // Input fields for Coordinates to be pinned on the MAP
                Text(
                    text = "GPS Coordinates (Pinned on Map) *",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latitudeText,
                        onValueChange = { latitudeText = it },
                        label = { Text("Latitude *") },
                        modifier = Modifier.weight(1f).testTag("nap_latitude_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = longitudeText,
                        onValueChange = { longitudeText = it },
                        label = { Text("Longitude *") },
                        modifier = Modifier.weight(1f).testTag("nap_longitude_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                // Quick coordinate offsets presets
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            latitudeText = String.format(Locale.US, "%.4f", olt.latitude + 0.002)
                            longitudeText = String.format(Locale.US, "%.4f", olt.longitude + 0.002)
                        },
                        label = { Text("Near OLT (+200m)") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            latitudeText = String.format(Locale.US, "%.4f", olt.latitude + 0.005)
                            longitudeText = String.format(Locale.US, "%.4f", olt.longitude + 0.004)
                        },
                        label = { Text("Subdivision (+500m)") }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalPortsText,
                        onValueChange = { totalPortsText = it.filter { char -> char.isDigit() } },
                        label = { Text("Total Ports") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = splitterRatio,
                        onValueChange = { splitterRatio = it },
                        label = { Text("Splitter Ratio") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF00695C).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF00695C), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "This NAP box will be placed on the Map with an optical feeder line connecting it back to OLT ${olt.name} PON Port $selectedPonPort.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF004D40)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (napName.isNotBlank() && location.isNotBlank()) {
                        val lat = latitudeText.toDoubleOrNull() ?: olt.latitude
                        val lng = longitudeText.toDoubleOrNull() ?: olt.longitude
                        val ports = totalPortsText.toIntOrNull() ?: 8
                        val pwr = opticalPowerText.toDoubleOrNull() ?: -19.2
                        val nap = NapBox(
                            name = napName,
                            oltId = olt.id,
                            oltName = olt.name,
                            ponPortNumber = selectedPonPort,
                            location = location,
                            latitude = lat,
                            longitude = lng,
                            totalPorts = ports,
                            usedPorts = 0,
                            splitterRatio = splitterRatio,
                            status = "Active",
                            opticalPowerDbm = pwr
                        )
                        onSave(selectedPonPort, nap)
                    }
                },
                enabled = napName.isNotBlank() && location.isNotBlank(),
                modifier = Modifier.testTag("save_nap_button")
            ) {
                Text("Connect & Pin on Map")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
