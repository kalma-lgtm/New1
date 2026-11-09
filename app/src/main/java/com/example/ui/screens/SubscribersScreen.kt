package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.model.InternetPlan
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Bill
import com.example.data.model.Collection
import com.example.data.model.Subscriber
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusSuspended
import com.example.ui.viewmodel.IspViewModel
import com.example.ui.viewmodel.UiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscribersScreen(
    viewModel: IspViewModel,
    onNavigateToMap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val subscribersState by viewModel.subscribersState.collectAsState()
    val billsState by viewModel.billsState.collectAsState()
    val collectionsState by viewModel.collectionsState.collectAsState()

    val subscribers = (subscribersState as? UiState.Success)?.data ?: emptyList()
    val allBills = (billsState as? UiState.Success)?.data ?: emptyList()
    val allCollections = (collectionsState as? UiState.Success)?.data ?: emptyList()
    val plansState by viewModel.plansState.collectAsState()
    val availablePlans = (plansState as? UiState.Success)?.data ?: emptyList()

    var selectedScreenTab by remember { mutableIntStateOf(0) } // 0: Active Subscribers, 1: Archived Clients
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var selectedPlanFilter by remember { mutableStateOf("All") }

    var selectedSubscriberForDetail by remember { mutableStateOf<Subscriber?>(null) }
    var editingSubscriber by remember { mutableStateOf<Subscriber?>(null) }
    var isAddSubscriberDialogOpen by remember { mutableStateOf(false) }
    var subscriberToDelete by remember { mutableStateOf<Subscriber?>(null) }
    var subscriberToRestore by remember { mutableStateOf<Subscriber?>(null) }
    var subscriberToPurge by remember { mutableStateOf<Subscriber?>(null) }

    val activeSubscribers = remember(subscribers) { subscribers.filter { !it.isArchived } }
    val archivedSubscribers = remember(subscribers) { subscribers.filter { it.isArchived } }

    val filteredActiveSubscribers = activeSubscribers.filter { sub ->
        val matchesQuery = searchQuery.isBlank() ||
                sub.fullName.contains(searchQuery, ignoreCase = true) ||
                sub.accountNumber.contains(searchQuery, ignoreCase = true) ||
                sub.contactNumber.contains(searchQuery, ignoreCase = true) ||
                sub.pppoeUsername.contains(searchQuery, ignoreCase = true)

        val matchesStatus = selectedStatusFilter == "All" || sub.connectionStatus == selectedStatusFilter
        val matchesPlan = selectedPlanFilter == "All" || sub.planName == selectedPlanFilter

        matchesQuery && matchesStatus && matchesPlan
    }

    val filteredArchivedSubscribers = archivedSubscribers.filter { sub ->
        searchQuery.isBlank() ||
                sub.fullName.contains(searchQuery, ignoreCase = true) ||
                sub.accountNumber.contains(searchQuery, ignoreCase = true) ||
                sub.contactNumber.contains(searchQuery, ignoreCase = true) ||
                sub.pppoeUsername.contains(searchQuery, ignoreCase = true) ||
                sub.address.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (selectedScreenTab == 0) {
                FloatingActionButton(
                    onClick = { isAddSubscriberDialogOpen = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_subscriber_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Subscriber")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                title = if (selectedScreenTab == 0) "Subscriber Management" else "Archived Subscribers",
                subtitle = if (selectedScreenTab == 0)
                    "${activeSubscribers.size} active clients (${filteredActiveSubscribers.size} shown)"
                else
                    "${archivedSubscribers.size} archived/deleted clients (credentials retained in system)",
                actionButton = onNavigateToMap?.let { navigateAction ->
                    {
                        OutlinedButton(onClick = navigateAction) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View on Map")
                        }
                    }
                }
            )

            // Primary Tabs: Active Subscribers vs Archived
            TabRow(
                selectedTabIndex = selectedScreenTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("subscribers_main_tabs")
            ) {
                Tab(
                    selected = selectedScreenTab == 0,
                    onClick = { selectedScreenTab = 0 },
                    text = {
                        Text(
                            text = "Subscribers (${activeSubscribers.size})",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("subscribers_active_tab")
                )
                Tab(
                    selected = selectedScreenTab == 1,
                    onClick = { selectedScreenTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ARCHIVED (${archivedSubscribers.size})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    modifier = Modifier.testTag("subscribers_archived_tab")
                )
            }

            // Search Bar (Applicable to current tab)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subscriber_search_input"),
                placeholder = {
                    Text(
                        if (selectedScreenTab == 0)
                            "Search by name, account #, phone, or PPPoE username..."
                        else
                            "Search archived by name, account #, PPPoE username..."
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (selectedScreenTab == 0) {
                // Filter Chips for Active Subscribers
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val statusOptions = listOf("All", "Active", "Suspended", "Disconnected")
                    items(statusOptions) { status ->
                        FilterChip(
                            selected = selectedStatusFilter == status,
                            onClick = { selectedStatusFilter = status },
                            label = { Text(status) }
                        )
                    }

                    val planOptions = listOf("Fiber 10 Mbps", "Fiber 20 Mbps", "Fiber 30 Mbps", "Fiber 50 Mbps", "Fiber 100 Mbps")
                    items(planOptions) { plan ->
                        FilterChip(
                            selected = selectedPlanFilter == plan,
                            onClick = { selectedPlanFilter = if (selectedPlanFilter == plan) "All" else plan },
                            label = { Text(plan) }
                        )
                    }
                }

                // Active Subscribers List
                if (filteredActiveSubscribers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (activeSubscribers.isEmpty()) "No active subscribers yet. Tap + to register." else "No subscribers match your search filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredActiveSubscribers, key = { it.id }) { sub ->
                            SubscriberCard(
                                subscriber = sub,
                                onViewDetail = { selectedSubscriberForDetail = sub },
                                onEdit = { editingSubscriber = sub },
                                onToggleStatus = { newStatus ->
                                    viewModel.toggleSubscriberStatus(sub.id, newStatus, "Admin manual status switch")
                                },
                                onDelete = { subscriberToDelete = sub }
                            )
                        }
                    }
                }
            } else {
                // ARCHIVED TAB CONTENT
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Archive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Deleted Client Archive",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "PPPoE username & password remain in system database. PPPoE accounts have been deleted from MikroTik RouterOS.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (filteredArchivedSubscribers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Archive,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = if (archivedSubscribers.isEmpty())
                                    "No archived clients yet."
                                else
                                    "No archived clients match your search query.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Deleted subscribers will appear here with preserved username and password.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredArchivedSubscribers, key = { it.id }) { sub ->
                            ArchivedSubscriberCard(
                                subscriber = sub,
                                onViewDetail = { selectedSubscriberForDetail = sub },
                                onRestore = { subscriberToRestore = sub },
                                onPermanentDelete = { subscriberToPurge = sub }
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail BottomSheet
    selectedSubscriberForDetail?.let { sub ->
        SubscriberDetailBottomSheet(
            subscriber = sub,
            bills = allBills.filter { it.subscriberId == sub.id || it.accountNumber == sub.accountNumber },
            collections = allCollections.filter { it.subscriberId == sub.id || it.accountNumber == sub.accountNumber },
            onDismiss = { selectedSubscriberForDetail = null },
            onEdit = {
                selectedSubscriberForDetail = null
                editingSubscriber = sub
            }
        )
    }

    // Add / Edit Dialog
    if (isAddSubscriberDialogOpen || editingSubscriber != null) {
        SubscriberFormDialog(
            subscriber = editingSubscriber,
            availablePlans = availablePlans,
            onDismiss = {
                isAddSubscriberDialogOpen = false
                editingSubscriber = null
            },
            onSave = { updatedSub ->
                if (editingSubscriber != null) {
                    viewModel.updateSubscriber(updatedSub) {
                        editingSubscriber = null
                    }
                } else {
                    viewModel.addSubscriber(updatedSub) {
                        isAddSubscriberDialogOpen = false
                    }
                }
            }
        )
    }

    // Delete / Move to Archive Confirmation Dialog
    subscriberToDelete?.let { sub ->
        AlertDialog(
            onDismissRequest = { subscriberToDelete = null },
            icon = { Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete & Archive Client") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to delete '${sub.fullName}' (${sub.accountNumber})?")
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "• Username (${sub.pppoeUsername}) & password will REMAIN safely in the system archive.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• PPPoE secret will be DELETED from MikroTik RouterOS (/ppp secret remove).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubscriber(sub.id)
                        subscriberToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Client")
                }
            },
            dismissButton = {
                TextButton(onClick = { subscriberToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Subscriber Dialog
    subscriberToRestore?.let { sub ->
        AlertDialog(
            onDismissRequest = { subscriberToRestore = null },
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Restore Subscriber") },
            text = {
                Text("Restore '${sub.fullName}' (${sub.accountNumber}) to active subscribers? Their PPPoE account '${sub.pppoeUsername}' with saved password '${sub.pppoePassword}' will be reprovisioned to MikroTik RouterOS.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreSubscriber(sub.id)
                        subscriberToRestore = null
                    }
                ) {
                    Text("Restore to Active")
                }
            },
            dismissButton = {
                TextButton(onClick = { subscriberToRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Permanent Purge Dialog
    subscriberToPurge?.let { sub ->
        AlertDialog(
            onDismissRequest = { subscriberToPurge = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Permanently Purge Record") },
            text = {
                Text("Warning: This will permanently delete '${sub.fullName}' (${sub.accountNumber}) from the database. Their saved PPPoE credentials and history will be completely wiped. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.permanentlyDeleteSubscriber(sub.id)
                        subscriberToPurge = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Permanently Purge")
                }
            },
            dismissButton = {
                TextButton(onClick = { subscriberToPurge = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SubscriberCard(
    subscriber: Subscriber,
    onViewDetail: () -> Unit,
    onEdit: () -> Unit,
    onToggleStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetail() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subscriber.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${subscriber.accountNumber} • ${subscriber.contactNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(status = subscriber.connectionStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = subscriber.planName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                Text(
                    text = "₱${String.format(Locale.US, "%,.2f", subscriber.monthlyFee)}/mo",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "PPPoE: ${subscriber.pppoeUsername} | NAP: ${subscriber.napBox} (${subscriber.napPort})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (subscriber.currentBalance > 0) {
                Text(
                    text = "Balance Due: ₱${String.format(Locale.US, "%,.2f", subscriber.currentBalance)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE11D48),
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick status change button
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (subscriber.connectionStatus != "Active") {
                        TextButton(onClick = { onToggleStatus("Active") }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusActive)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Activate", color = StatusActive, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (subscriber.connectionStatus == "Active") {
                        TextButton(onClick = { onToggleStatus("Suspended") }) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusSuspended)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Suspend", color = StatusSuspended, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = onViewDetail, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Visibility, contentDescription = "Details", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun ArchivedSubscriberCard(
    subscriber: Subscriber,
    onViewDetail: () -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetail() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subscriber.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${subscriber.accountNumber} • ${subscriber.contactNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "ARCHIVED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Preserved Credentials Box (System retains credentials, deleted in MikroTik)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Preserved System Credentials",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = "Deleted in MikroTik",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Username: ${subscriber.pppoeUsername}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(subscriber.pppoeUsername))
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Username", modifier = Modifier.size(14.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showPassword) "Password: ${subscriber.pppoePassword}" else "Password: ••••••••",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal
                        )
                        Row {
                            IconButton(
                                onClick = { showPassword = !showPassword },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Password",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(subscriber.pppoePassword))
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Plan: ${subscriber.planName} (₱${String.format(Locale.US, "%,.2f", subscriber.monthlyFee)}) | NAP: ${subscriber.napBox} (${subscriber.napPort})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (subscriber.archivedDate.isNotBlank()) {
                Text(
                    text = "Archived on: ${subscriber.archivedDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onPermanentDelete,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Purge", style = MaterialTheme.typography.labelSmall)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onViewDetail,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Details", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = onRestore,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore Client", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriberDetailBottomSheet(
    subscriber: Subscriber,
    bills: List<Bill>,
    collections: List<Collection>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = subscriber.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Account: ${subscriber.accountNumber}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusBadge(status = if (subscriber.isArchived) "Archived" else subscriber.connectionStatus)
            }

            Spacer(modifier = Modifier.height(12.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Profile & Network") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Bills (${bills.size})") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Payments (${collections.size})") })
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.height(340.dp)) {
                when (selectedTab) {
                    0 -> ProfileAndNetworkTab(subscriber)
                    1 -> BillsHistoryTab(bills)
                    2 -> PaymentsHistoryTab(collections)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit Subscriber Information")
            }
        }
    }
}

@Composable
fun ProfileAndNetworkTab(sub: Subscriber) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailItem("Complete Address", sub.address)
        DetailItem("Contact Number", sub.contactNumber)
        DetailItem("GPS Coordinates", "${sub.latitude}, ${sub.longitude}")
        DetailItem("Installation Date", sub.installationDate)
        DetailItem("Plan & Fee", "${sub.planName} (₱${String.format(Locale.US, "%,.2f", sub.monthlyFee)}/mo)")
        DetailItem("Billing Due Date", "Every ${sub.billingDueDate}th of the month")

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text(text = "Optical Line Terminal (OLT) & Network", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        DetailItem("Assigned OLT", sub.olt)
        DetailItem("PON Port", sub.ponPort)
        DetailItem("Main NAP Box & Port", "${sub.napBox} - ${sub.napPort}")
        DetailItem("Splitter Ratio", sub.splitter)
        DetailItem("ONU/ONT Serial Number", sub.onuSerial)

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text(text = "MikroTik PPPoE Credentials", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        DetailItem("PPPoE Username", sub.pppoeUsername)
        DetailItem("PPPoE Password", sub.pppoePassword)
        DetailItem("Speed Profile", sub.mikrotikProfile)
        DetailItem(
            "MikroTik Status",
            if (sub.isArchived) "DELETED in MikroTik RouterOS (Retained in system)" else "ACTIVE in MikroTik RouterOS"
        )
        if (sub.isArchived && sub.archivedDate.isNotBlank()) {
            DetailItem("Archived Date", sub.archivedDate)
        }
        if (sub.remarks.isNotBlank()) {
            DetailItem("Account Remarks", sub.remarks)
        }
    }
}

@Composable
fun BillsHistoryTab(bills: List<Bill>) {
    if (bills.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No billing records generated for this subscriber.")
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(bills) { bill ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Cycle ${bill.billingPeriod}", fontWeight = FontWeight.Bold)
                            Text(text = "Due: ${bill.dueDate}", style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "₱${String.format(Locale.US, "%,.2f", bill.totalAmountDue)}", fontWeight = FontWeight.Bold)
                            StatusBadge(status = bill.status)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentsHistoryTab(collections: List<Collection>) {
    if (collections.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No collection records yet.")
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(collections) { coll ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = coll.receiptNumber, fontWeight = FontWeight.Bold)
                            Text(text = "${coll.paymentDate} • ${coll.paymentMethod}", style = MaterialTheme.typography.bodySmall)
                            if (coll.referenceNumber.isNotBlank()) {
                                Text(text = "Ref: ${coll.referenceNumber}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(
                            text = "₱${String.format(Locale.US, "%,.2f", coll.amount)}",
                            fontWeight = FontWeight.Bold,
                            color = StatusActive
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = if (value.isBlank()) "—" else value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriberFormDialog(
    subscriber: Subscriber?,
    availablePlans: List<InternetPlan> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Subscriber) -> Unit
) {
    var fullName by remember { mutableStateOf(subscriber?.fullName ?: "") }
    var contactNumber by remember { mutableStateOf(subscriber?.contactNumber ?: "") }
    var address by remember { mutableStateOf(subscriber?.address ?: "") }
    var latitude by remember { mutableStateOf(subscriber?.latitude?.toString() ?: "14.5995") }
    var longitude by remember { mutableStateOf(subscriber?.longitude?.toString() ?: "120.9842") }

    // Selected plan ID & details
    var selectedPlanId by remember {
        mutableStateOf(
            subscriber?.planId?.takeIf { it.isNotBlank() }
                ?: availablePlans.firstOrNull { it.name.equals(subscriber?.planName, ignoreCase = true) }?.id
                ?: availablePlans.firstOrNull { it.status == "Active" }?.id
                ?: "plan_20m"
        )
    }
    var planName by remember {
        mutableStateOf(
            subscriber?.planName?.takeIf { it.isNotBlank() }
                ?: availablePlans.firstOrNull { it.id == selectedPlanId }?.name
                ?: "Fiber 20 Mbps"
        )
    }
    var monthlyFee by remember {
        mutableStateOf(
            subscriber?.monthlyFee?.toString()
                ?: availablePlans.firstOrNull { it.id == selectedPlanId }?.monthlyPrice?.toString()
                ?: "1299.0"
        )
    }
    var isPlanDropdownExpanded by remember { mutableStateOf(false) }

    var olt by remember { mutableStateOf(subscriber?.olt ?: "OLT-Core-01") }
    var ponPort by remember { mutableStateOf(subscriber?.ponPort ?: "PON-1") }
    var napBox by remember { mutableStateOf(subscriber?.napBox ?: "NAP-01") }
    var napPort by remember { mutableStateOf(subscriber?.napPort ?: "Port 1") }
    var splitter by remember { mutableStateOf(subscriber?.splitter ?: "1:8 PLC") }
    var onuSerial by remember { mutableStateOf(subscriber?.onuSerial ?: "") }
    var pppoeUsername by remember { mutableStateOf(subscriber?.pppoeUsername ?: "") }
    var pppoePassword by remember { mutableStateOf(subscriber?.pppoePassword ?: "") }
    var billingDueDate by remember { mutableStateOf(subscriber?.billingDueDate?.toString() ?: "15") }
    var connectionStatus by remember { mutableStateOf(subscriber?.connectionStatus ?: "Active") }
    var remarks by remember { mutableStateOf(subscriber?.remarks ?: "") }

    val isEditing = subscriber != null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isEditing) "Edit Subscriber Profile" else "Register New Subscriber",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("sub_form_fullname")
                )

                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { contactNumber = it },
                    label = { Text("Contact Number *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Complete Address") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Internet Plan Selection with Auto-populated Speed and Price
                ExposedDropdownMenuBox(
                    expanded = isPlanDropdownExpanded,
                    onExpandedChange = { isPlanDropdownExpanded = !isPlanDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = planName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Internet Plan *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPlanDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isPlanDropdownExpanded,
                        onDismissRequest = { isPlanDropdownExpanded = false }
                    ) {
                        val activePlans = availablePlans.filter { it.status == "Active" }.ifEmpty { availablePlans }
                        if (activePlans.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Fiber 20 Mbps (₱1,299/mo)") },
                                onClick = {
                                    selectedPlanId = "plan_20m"
                                    planName = "Fiber 20 Mbps"
                                    monthlyFee = "1299.0"
                                    isPlanDropdownExpanded = false
                                }
                            )
                        } else {
                            activePlans.forEach { plan ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(plan.name, fontWeight = FontWeight.Bold)
                                            Text(
                                                "↓ ${plan.downloadSpeedMbps}M / ↑ ${plan.uploadSpeedMbps}M • ₱${String.format(Locale.US, "%,.2f", plan.monthlyPrice)}/mo",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedPlanId = plan.id
                                        planName = plan.name
                                        monthlyFee = plan.monthlyPrice.toString()
                                        isPlanDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = monthlyFee,
                    onValueChange = { monthlyFee = it },
                    label = { Text("Monthly Fee (PHP) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = olt,
                        onValueChange = { olt = it },
                        label = { Text("Assigned OLT") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ponPort,
                        onValueChange = { ponPort = it },
                        label = { Text("PON Port") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = napBox,
                        onValueChange = { napBox = it },
                        label = { Text("NAP Box") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = napPort,
                        onValueChange = { napPort = it },
                        label = { Text("NAP Port") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = onuSerial,
                    onValueChange = { onuSerial = it },
                    label = { Text("ONU/ONT Serial Number") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pppoeUsername,
                        onValueChange = { pppoeUsername = it },
                        label = { Text("PPPoE Username") },
                        placeholder = { Text("Auto-generated") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pppoePassword,
                        onValueChange = { pppoePassword = it },
                        label = { Text("PPPoE Password") },
                        placeholder = { Text("Auto-generated") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = billingDueDate,
                    onValueChange = { billingDueDate = it },
                    label = { Text("Billing Due Date (Day of Month)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Account Remarks") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (fullName.isNotBlank() && contactNumber.isNotBlank()) {
                                val latNum = latitude.toDoubleOrNull() ?: 14.5995
                                val lngNum = longitude.toDoubleOrNull() ?: 120.9842
                                val feeNum = monthlyFee.toDoubleOrNull() ?: 1299.0
                                val dueNum = billingDueDate.toIntOrNull() ?: 15

                                val newSub = (subscriber ?: Subscriber()).copy(
                                    fullName = fullName.trim(),
                                    contactNumber = contactNumber.trim(),
                                    address = address.trim(),
                                    latitude = latNum,
                                    longitude = lngNum,
                                    planId = selectedPlanId,
                                    planName = planName.trim(),
                                    monthlyFee = feeNum,
                                    olt = olt.trim(),
                                    ponPort = ponPort.trim(),
                                    napBox = napBox.trim(),
                                    napPort = napPort.trim(),
                                    splitter = splitter.trim(),
                                    onuSerial = onuSerial.trim(),
                                    pppoeUsername = pppoeUsername.trim(),
                                    pppoePassword = pppoePassword.trim(),
                                    mikrotikProfile = planName.trim(),
                                    billingDueDate = dueNum,
                                    connectionStatus = connectionStatus,
                                    remarks = remarks.trim()
                                )
                                onSave(newSub)
                            }
                        },
                        enabled = fullName.isNotBlank() && contactNumber.isNotBlank()
                    ) {
                        Text(if (isEditing) "Save Changes" else "Register Subscriber")
                    }
                }
            }
        }
    }
}
