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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.InternetPlan
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusDisconnected
import com.example.ui.viewmodel.IspViewModel
import com.example.ui.viewmodel.UiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternetPlansScreen(
    viewModel: IspViewModel,
    modifier: Modifier = Modifier
) {
    val plansState by viewModel.plansState.collectAsState()
    val plans = (plansState as? UiState.Success)?.data ?: emptyList()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") }

    var isAddPlanDialogOpen by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<InternetPlan?>(null) }
    var planToDelete by remember { mutableStateOf<InternetPlan?>(null) }

    val filteredPlans = plans.filter { plan ->
        val matchesQuery = searchQuery.isBlank() ||
                plan.name.contains(searchQuery, ignoreCase = true) ||
                plan.description.contains(searchQuery, ignoreCase = true) ||
                plan.downloadSpeedMbps.toString().contains(searchQuery) ||
                plan.uploadSpeedMbps.toString().contains(searchQuery)

        val matchesStatus = selectedStatusFilter == "All" || plan.status == selectedStatusFilter

        matchesQuery && matchesStatus
    }

    val totalPlans = plans.size
    val activePlansCount = plans.count { it.status == "Active" }
    val maxSpeed = plans.maxOfOrNull { it.downloadSpeedMbps } ?: 0
    val avgPrice = if (plans.isNotEmpty()) plans.map { it.monthlyPrice }.average() else 0.0

    Scaffold(modifier = modifier.fillMaxSize()) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionHeader(
                title = "Internet Plans & Packages",
                subtitle = "Commercial bandwidth tiers, speed limits, and subscriber subscription pricing",
                actionButton = {
                    Button(
                        onClick = { isAddPlanDialogOpen = true },
                        modifier = Modifier.testTag("add_plan_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ADD INTERNET PLAN")
                    }
                }
            )

            // KPI Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Available Packages", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("$activePlansCount / $totalPlans Active", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Max Bandwidth", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$maxSpeed Mbps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Average Fee", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₱${String.format(Locale.US, "%,.0f", avgPrice)}/mo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search Bar & Filter Chips
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_plans_input"),
                placeholder = { Text("Search by plan name, speed (Mbps), or description...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Search")
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val statusOptions = listOf("All", "Active", "Inactive")
                items(statusOptions) { st ->
                    FilterChip(
                        selected = selectedStatusFilter == st,
                        onClick = { selectedStatusFilter = st },
                        label = { Text(st) },
                        modifier = Modifier.testTag("plan_filter_$st")
                    )
                }
            }

            // Plans Organized Table / List
            if (filteredPlans.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (plans.isEmpty()) "No internet plans created yet. Click 'ADD INTERNET PLAN' to start." else "No plans matching current filter.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredPlans, key = { it.id }) { plan ->
                        PlanCard(
                            plan = plan,
                            onEdit = { editingPlan = plan },
                            onToggleStatus = { viewModel.togglePlanStatus(plan.id) },
                            onDelete = { planToDelete = plan }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (isAddPlanDialogOpen) {
        PlanFormDialog(
            plan = null,
            onDismiss = { isAddPlanDialogOpen = false },
            onSave = { newPlan ->
                viewModel.addPlan(newPlan) {
                    isAddPlanDialogOpen = false
                }
            }
        )
    }

    if (editingPlan != null) {
        PlanFormDialog(
            plan = editingPlan,
            onDismiss = { editingPlan = null },
            onSave = { updatedPlan ->
                viewModel.updatePlan(updatedPlan) {
                    editingPlan = null
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (planToDelete != null) {
        val target = planToDelete!!
        AlertDialog(
            onDismissRequest = { planToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Internet Plan") },
            text = {
                Text(
                    text = "Are you sure you want to delete the plan \"${target.name}\"?\n\nExisting subscribers currently on this plan will keep their billing fee and service intact, but this package will be removed from the catalog for new registrations."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = target.id
                        planToDelete = null
                        viewModel.deletePlan(id) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_plan_button")
                ) {
                    Text("Delete Plan")
                }
            },
            dismissButton = {
                TextButton(onClick = { planToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PlanCard(
    plan: InternetPlan,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val isActive = plan.status == "Active"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plan_card_${plan.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = plan.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Package Code: ${plan.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusBadge(status = plan.status)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Organized Table Info Row: Speed, Monthly Fee, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Bandwidth Speed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${plan.downloadSpeedMbps} Mbps",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Upload, contentDescription = "Upload", tint = Color(0xFF00897B), modifier = Modifier.size(16.dp))
                        Text(
                            text = "${plan.uploadSpeedMbps} Mbps",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Monthly Subscription", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "₱${String.format(Locale.US, "%,.2f", plan.monthlyPrice)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (plan.description.isNotBlank()) {
                Text(
                    text = plan.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Action Buttons: Edit, Toggle Active/Inactive, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Toggle status button
                OutlinedButton(
                    onClick = onToggleStatus,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isActive) MaterialTheme.colorScheme.error else StatusActive
                    ),
                    modifier = Modifier.testTag("toggle_plan_status_${plan.id}")
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.ToggleOff else Icons.Default.ToggleOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isActive) "Deactivate" else "Activate")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("edit_plan_${plan.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_plan_${plan.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Plan", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanFormDialog(
    plan: InternetPlan?,
    onDismiss: () -> Unit,
    onSave: (InternetPlan) -> Unit
) {
    val isEditing = plan != null

    var name by remember { mutableStateOf(plan?.name ?: "") }
    var downloadSpeed by remember { mutableStateOf(plan?.downloadSpeedMbps?.toString() ?: "25") }
    var uploadSpeed by remember { mutableStateOf(plan?.uploadSpeedMbps?.toString() ?: "25") }
    var monthlyPrice by remember { mutableStateOf(plan?.monthlyPrice?.toString() ?: "1299.0") }
    var description by remember { mutableStateOf(plan?.description ?: "") }
    var status by remember { mutableStateOf(plan?.status ?: "Active") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isEditing) "Edit Internet Plan" else "Add New Internet Plan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Plan Name * (e.g. Fiber Max 50 Mbps)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_form_name_input")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = downloadSpeed,
                        onValueChange = {
                            downloadSpeed = it
                            errorMessage = null
                        },
                        label = { Text("Download (Mbps) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("plan_form_download_input")
                    )

                    OutlinedTextField(
                        value = uploadSpeed,
                        onValueChange = {
                            uploadSpeed = it
                            errorMessage = null
                        },
                        label = { Text("Upload (Mbps) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("plan_form_upload_input")
                    )
                }

                OutlinedTextField(
                    value = monthlyPrice,
                    onValueChange = {
                        monthlyPrice = it
                        errorMessage = null
                    },
                    label = { Text("Monthly Price (PHP) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_form_price_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Plan Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                // Plan Status Selector (Active / Inactive)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Plan Status", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (status == "Active") "Active in commercial plan catalog" else "Inactive / Hidden from new subscriptions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = status == "Active",
                        onCheckedChange = { status = if (it) "Active" else "Inactive" },
                        modifier = Modifier.testTag("plan_form_status_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            val dl = downloadSpeed.toIntOrNull() ?: 0
                            val ul = uploadSpeed.toIntOrNull() ?: 0
                            val price = monthlyPrice.toDoubleOrNull() ?: 0.0

                            if (trimmedName.isBlank()) {
                                errorMessage = "Plan Name cannot be empty."
                                return@Button
                            }
                            if (dl <= 0 || ul <= 0) {
                                errorMessage = "Download and Upload speeds must be greater than 0 Mbps."
                                return@Button
                            }
                            if (price < 0) {
                                errorMessage = "Monthly Price must be 0 or greater."
                                return@Button
                            }

                            val newOrUpdated = (plan ?: InternetPlan()).copy(
                                name = trimmedName,
                                downloadSpeedMbps = dl,
                                uploadSpeedMbps = ul,
                                monthlyPrice = price,
                                description = description.trim(),
                                status = status
                            )
                            onSave(newOrUpdated)
                        },
                        modifier = Modifier.testTag("save_plan_button")
                    ) {
                        Text(if (isEditing) "Save Changes" else "Add Plan")
                    }
                }
            }
        }
    }
}
