package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ActivityLog
import com.example.data.model.NapBox
import com.example.data.model.Olt
import com.example.data.model.Router
import com.example.data.model.SpeedProfile
import com.example.data.model.Subscriber
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusSuspended
import com.example.ui.viewmodel.IspViewModel
import com.example.ui.viewmodel.UiState
import java.util.Locale

@Composable
fun MikrotikScreen(
    viewModel: IspViewModel,
    modifier: Modifier = Modifier
) {
    val routersState by viewModel.routersState.collectAsState()
    val profilesState by viewModel.profilesState.collectAsState()
    val subscribersState by viewModel.subscribersState.collectAsState()
    val logsState by viewModel.logsState.collectAsState()
    val oltsState by viewModel.oltsState.collectAsState()

    val routers = (routersState as? UiState.Success)?.data ?: emptyList()
    val profiles = (profilesState as? UiState.Success)?.data ?: emptyList()
    val subscribers = (subscribersState as? UiState.Success)?.data ?: emptyList()
    val logs = (logsState as? UiState.Success)?.data ?: emptyList()
    val olts = (oltsState as? UiState.Success)?.data ?: emptyList()

    val activeSubs = remember(subscribers) { subscribers.filter { !it.isArchived } }

    var selectedTab by remember { mutableIntStateOf(0) }
    var isAddRouterDialogOpen by remember { mutableStateOf(false) }
    var isAddOltDialogOpen by remember { mutableStateOf(false) }
    var isAddProfileDialogOpen by remember { mutableStateOf(false) }
    var oltForAddingNap by remember { mutableStateOf<Pair<Olt, Int>?>(null) }

    val clipboardManager = LocalClipboardManager.current
    var isSyncingPppoe by remember { mutableStateOf(false) }
    var showPppoeSyncedDialog by remember { mutableStateOf(false) }
    var syncedSubscribers by remember { mutableStateOf<List<Subscriber>>(emptyList()) }

    var subscriberForManualToggle by remember { mutableStateOf<Subscriber?>(null) }

    val onTriggerRefreshAndSyncPppoe: () -> Unit = {
        if (!isSyncingPppoe) {
            isSyncingPppoe = true
            viewModel.syncPppoeSecretsToSubscribers { updatedSubs ->
                isSyncingPppoe = false
                syncedSubscribers = updatedSubs
                showPppoeSyncedDialog = true
            }
        }
    }

    var routerToDelete by remember { mutableStateOf<Router?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionHeader(
                title = "MikroTik Router & OLT Management",
                subtitle = "RouterOS API, OLT & PON ports, connected NAPs, PPPoE sessions & queues",
                actionButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = onTriggerRefreshAndSyncPppoe,
                            modifier = Modifier
                                .testTag("refresh_sync_pppoe_button")
                                .size(40.dp)
                        ) {
                            if (isSyncingPppoe) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh and copy all available PPPoE usernames and passwords to subscribers"
                                )
                            }
                        }
                        if (selectedTab == 1) {
                            Button(
                                onClick = { isAddOltDialogOpen = true },
                                modifier = Modifier.testTag("add_olt_top_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add OLT")
                            }
                        } else {
                            Button(
                                onClick = { isAddRouterDialogOpen = true },
                                modifier = Modifier.testTag("add_router_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Router")
                            }
                        }
                    }
                }
            )

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Routers (${routers.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("OLT & PON (${olts.size})") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("PPPoE Sessions (${activeSubs.count { it.connectionStatus == "Active" }})") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Speed Profiles (${profiles.size})") })
                Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Activity Log (${logs.size})") })
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> RoutersTab(
                        routers = routers,
                        onTestRouter = { viewModel.testRouter(it) },
                        onDeleteRouter = { routerToDelete = it }
                    )
                    1 -> OltManagementTab(
                        olts = olts,
                        onOpenAddOlt = { isAddOltDialogOpen = true },
                        onOpenAddNap = { oltId, port ->
                            val olt = olts.firstOrNull { it.id == oltId }
                            if (olt != null) {
                                oltForAddingNap = Pair(olt, port)
                            }
                        },
                        onDeleteOlt = { viewModel.deleteOlt(it) },
                        onDeleteNap = { oltId, napId -> viewModel.deleteNapFromOlt(oltId, napId) }
                    )
                    2 -> PppoeSessionsTab(
                        subscribers = activeSubs,
                        onToggle = { subscriberForManualToggle = it }
                    )
                    3 -> SpeedProfilesTab(
                        profiles = profiles,
                        isRefreshing = isSyncingPppoe,
                        onRefreshAndCopyProfiles = onTriggerRefreshAndSyncPppoe,
                        onOpenAddProfile = { isAddProfileDialogOpen = true },
                        onCopySingleProfile = { prof ->
                            val cmd = "/ppp profile add name=\"${prof.name}\" rate-limit=${prof.rateLimit} comment=\"NetPulse ISP Profile\""
                            clipboardManager.setText(AnnotatedString(cmd))
                        }
                    )
                    4 -> ActivityLogsTab(logs = logs)
                }
            }
        }
    }

    if (isAddRouterDialogOpen) {
        AddRouterDialog(
            onDismiss = { isAddRouterDialogOpen = false },
            onSave = { router ->
                viewModel.addRouter(router) {
                    isAddRouterDialogOpen = false
                }
            }
        )
    }

    if (isAddProfileDialogOpen) {
        AddProfileDialog(
            onDismiss = { isAddProfileDialogOpen = false },
            onSave = { profile ->
                viewModel.addSpeedProfile(profile) {
                    isAddProfileDialogOpen = false
                }
            }
        )
    }

    if (isAddOltDialogOpen) {
        AddOltDialog(
            onDismiss = { isAddOltDialogOpen = false },
            onSave = { olt ->
                viewModel.addOlt(olt) {
                    isAddOltDialogOpen = false
                }
            }
        )
    }

    oltForAddingNap?.let { (olt, port) ->
        AddNapDialog(
            olt = olt,
            preselectedPonPort = port,
            onDismiss = { oltForAddingNap = null },
            onSave = { ponPort, nap ->
                viewModel.addNapToOlt(olt.id, ponPort, nap) {
                    oltForAddingNap = null
                }
            }
        )
    }

    // Confirmation for Destructive/Manual Router Operation
    subscriberForManualToggle?.let { sub ->
        val willSuspend = sub.connectionStatus == "Active"
        AlertDialog(
            onDismissRequest = { subscriberForManualToggle = null },
            title = { Text(if (willSuspend) "Suspend PPPoE Connection?" else "Reactivate PPPoE Connection?") },
            text = {
                Text(
                    if (willSuspend)
                        "This will execute RouterOS command '/ppp secret set [find name=\"${sub.pppoeUsername}\"] disabled=yes' and disconnect active session. Require administrator confirmation."
                    else
                        "This will execute RouterOS command '/ppp secret set [find name=\"${sub.pppoeUsername}\"] disabled=no' and restore internet access for ${sub.fullName}."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newStatus = if (willSuspend) "Suspended" else "Active"
                        viewModel.toggleSubscriberStatus(sub.id, newStatus, "Administrator manual RouterOS override")
                        subscriberForManualToggle = null
                    },
                    colors = if (willSuspend) androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = StatusSuspended) else androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = StatusActive)
                ) {
                    Text(if (willSuspend) "Confirm Suspension" else "Confirm Activation")
                }
            },
            dismissButton = {
                TextButton(onClick = { subscriberForManualToggle = null }) { Text("Cancel") }
            }
        )
    }

    if (showPppoeSyncedDialog) {
        PppoeSyncSuccessDialog(
            subscribers = syncedSubscribers,
            onDismiss = { showPppoeSyncedDialog = false }
        )
    }

    if (routerToDelete != null) {
        val router = routerToDelete!!
        AlertDialog(
            onDismissRequest = { routerToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Router") },
            text = {
                Text("Are you sure you want to delete router \"${router.name}\" (${router.ipAddress})? This will remove the router from system monitoring and disconnect active API sync. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = router.id
                        routerToDelete = null
                        viewModel.deleteRouter(id)
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_router_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { routerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RoutersTab(
    routers: List<Router>,
    onTestRouter: (Router) -> Unit,
    onDeleteRouter: (Router) -> Unit
) {
    if (routers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No MikroTik routers configured yet. Add your core/distribution router.")
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(routers, key = { it.id }) { router ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                    Icon(Icons.Default.Router, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = router.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(text = "${router.model} • ${router.location}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            StatusBadge(status = router.status)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "IP Address", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "${router.ipAddress}:${router.apiPort}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(text = "CPU Load", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "${router.cpuLoad}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (router.cpuLoad > 80) StatusDisconnected else StatusActive)
                            }
                            Column {
                                Text(text = "Active PPPoE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "${router.activeSessionsCount} users", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Uptime: ${router.uptime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onDeleteRouter(router) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.testTag("delete_router_${router.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete Router")
                                }

                                Button(
                                    onClick = { onTestRouter(router) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test Connection")
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
fun PppoeSessionsTab(
    subscribers: List<Subscriber>,
    onToggle: (Subscriber) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(subscribers, key = { it.id }) { sub ->
            val isActive = sub.connectionStatus == "Active"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = sub.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "User: ${sub.pppoeUsername} | IP: 10.10.20.${sub.billingDueDate + 10}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Profile: ${sub.planName} | Uptime: 14d 02h 45m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        StatusBadge(status = sub.connectionStatus)
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = { onToggle(sub) }) {
                            Text(if (isActive) "Disconnect" else "Reactivate", color = if (isActive) StatusSuspended else StatusActive)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedProfilesTab(
    profiles: List<SpeedProfile>,
    isRefreshing: Boolean,
    onRefreshAndCopyProfiles: () -> Unit,
    onOpenAddProfile: () -> Unit,
    onCopySingleProfile: (SpeedProfile) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Bandwidth Rate Limit Profiles",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${profiles.size} profiles configured in RouterOS",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onRefreshAndCopyProfiles,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("tab_refresh_copy_profiles_button")
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh and copy all available profiles"
                        )
                    }
                }
                Button(
                    onClick = onOpenAddProfile,
                    modifier = Modifier.testTag("new_profile_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Profile")
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(profiles, key = { it.id }) { prof ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = prof.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (prof.isDefault) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "DEFAULT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Rate Limit: ${prof.rateLimit} (↓${prof.downloadSpeedMbps}M / ↑${prof.uploadSpeedMbps}M)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "₱${String.format(Locale.US, "%,.2f", prof.price)}",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { onCopySingleProfile(prof) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("copy_profile_${prof.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy RouterOS command for ${prof.name}",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PppoeSyncSuccessDialog(
    subscribers: List<Subscriber>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "PPPoE Credentials Copied to Subscribers",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Queried MikroTik RouterOS (/ppp secret print). Copied and synchronized all available PPPoE usernames and passwords directly to ${subscribers.size} subscribers in the system.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "System Subscribers Synchronized:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                subscribers.forEach { sub ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = sub.fullName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = sub.accountNumber,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "User: ${sub.pppoeUsername}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Pass: ${sub.pppoePassword}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Profile: ${sub.mikrotikProfile}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun ActivityLogsTab(logs: List<ActivityLog>) {
    if (logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No activity logs recorded yet.")
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs, key = { it.id }) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = log.action, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            StatusBadge(status = log.status)
                        }
                        Text(text = log.details, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Target: ${log.targetSubscriber} • Router: ${log.routerName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddRouterDialog(onDismiss: () -> Unit, onSave: (Router) -> Unit) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8728") }
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Core Data Center") }
    var model by remember { mutableStateOf("CCR1036-8G-2S+") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Register MikroTik Router", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Router Name *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("IP Address (Public/VPN) *") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("API Port") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model") }, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("API Username *") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("API Password") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Physical Location") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val portNum = port.toIntOrNull() ?: 8728
                            onSave(Router(name = name.trim(), ipAddress = ip.trim(), apiPort = portNum, username = username.trim(), password = password.trim(), location = location.trim(), model = model.trim()))
                        },
                        enabled = name.isNotBlank() && ip.isNotBlank()
                    ) {
                        Text("Connect Router")
                    }
                }
            }
        }
    }
}

@Composable
fun AddProfileDialog(onDismiss: () -> Unit, onSave: (SpeedProfile) -> Unit) {
    var name by remember { mutableStateOf("") }
    var dl by remember { mutableStateOf("30") }
    var ul by remember { mutableStateOf("30") }
    var price by remember { mutableStateOf("1599.0") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Create Speed Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Profile Name *") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = dl, onValueChange = { dl = it }, label = { Text("Download (Mbps)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = ul, onValueChange = { ul = it }, label = { Text("Upload (Mbps)") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Monthly Price (₱)") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val dlNum = dl.toIntOrNull() ?: 30
                            val ulNum = ul.toIntOrNull() ?: 30
                            val priceNum = price.toDoubleOrNull() ?: 1599.0
                            onSave(SpeedProfile(name = name.trim(), downloadSpeedMbps = dlNum, uploadSpeedMbps = ulNum, rateLimit = "${ulNum}M/${dlNum}M", price = priceNum))
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save Profile")
                    }
                }
            }
        }
    }
}
