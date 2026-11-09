package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.SystemSettings
import com.example.data.repository.OperatorAuthManager
import com.example.ui.components.SectionHeader
import com.example.ui.viewmodel.IspViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch

@Composable
fun SystemSettingsScreen(
    viewModel: IspViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authManager = remember { OperatorAuthManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val settingsState by viewModel.settingsState.collectAsState()

    var ispName by remember { mutableStateOf("NetPulse Broadband Networks") }
    var currencySymbol by remember { mutableStateOf("₱") }
    var defaultDueDays by remember { mutableStateOf("15") }
    var autoDisconnectEnabled by remember { mutableStateOf(true) }
    var gracePeriodDays by remember { mutableStateOf("5") }

    var isSaving by remember { mutableStateOf(false) }

    // Update local state when remote settings load
    LaunchedEffect(settingsState) {
        if (settingsState is UiState.Success) {
            val settings = (settingsState as UiState.Success<SystemSettings>).data
            ispName = settings.ispName
            currencySymbol = settings.currencySymbol
            defaultDueDays = settings.defaultDueDays
            autoDisconnectEnabled = settings.autoDisconnectEnabled
            gracePeriodDays = settings.gracePeriodDays
        }
    }

    var showResetDialog by remember { mutableStateOf(false) }
    var resetAdminPassword by remember { mutableStateOf("") }
    var resetPasswordError by remember { mutableStateOf<String?>(null) }
    var isResetting by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(
                title = "System Settings & Configuration",
                subtitle = "ISP company profile, billing policies, and network automation rules"
            )

            // Company Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "ISP Organization Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = ispName,
                        onValueChange = { ispName = it },
                        label = { Text("ISP Brand / Company Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("isp_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = currencySymbol,
                        onValueChange = { currencySymbol = it },
                        label = { Text("Currency Symbol (e.g. ₱, $, €)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("currency_symbol_input"),
                        singleLine = true
                    )
                }
            }

            // Billing & Automation Rules Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Billing & MikroTik Disconnection Policy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = defaultDueDays,
                        onValueChange = { defaultDueDays = it },
                        label = { Text("Default Monthly Due Day (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("default_due_days_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = gracePeriodDays,
                        onValueChange = { gracePeriodDays = it },
                        label = { Text("Disconnection Grace Period (Days after Due Date)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("grace_period_input"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Auto-Disconnect Overdue Accounts", fontWeight = FontWeight.Bold)
                            Text(
                                text = "Automatically execute RouterOS PPPoE disable command when bills exceed grace period.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoDisconnectEnabled,
                            onCheckedChange = { autoDisconnectEnabled = it },
                            modifier = Modifier.testTag("auto_disconnect_switch")
                        )
                    }
                }
            }

            // Functional SAVE SETTINGS Button
            Button(
                onClick = {
                    isSaving = true
                    val updated = SystemSettings(
                        ispName = ispName.trim(),
                        currencySymbol = currencySymbol.trim(),
                        defaultDueDays = defaultDueDays.trim(),
                        gracePeriodDays = gracePeriodDays.trim(),
                        autoDisconnectEnabled = autoDisconnectEnabled
                    )
                    viewModel.saveSettings(updated) {
                        isSaving = false
                        scope.launch {
                            snackbarHostState.showSnackbar("System settings saved successfully and synchronized.")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_settings_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSaving) "Saving Settings..." else "SAVE SETTINGS",
                    fontWeight = FontWeight.Bold
                )
            }

            // System Reset Section (Irreversible Data Wipe with Admin Password Verification)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "System Reset & Operational Data Purge",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = "Allows an authorized administrator to reset and erase all subscribers, billing records, payment collections, recorded expenses, router configs, and OLT network topology. Application settings and administrator accounts are preserved. This action is irreversible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            resetAdminPassword = ""
                            resetPasswordError = null
                            showResetDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_system_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESET SYSTEM", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Account Session & LOGOUT Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Account Session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Signed in as ISP Administrator with operational control.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("logout_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LOGOUT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Explicit Confirmation Dialog for System Reset with Admin Password Verification
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isResetting) showResetDialog = false
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp)) },
            title = {
                Text(
                    text = "RESET SYSTEM — Irreversible Operation",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "CRITICAL WARNING: This will immediately erase all registered subscribers, billing invoices, collection transactions, expenses, MikroTik routers, and GPON OLTs from the database.\n\nTo prevent accidental erasure, you must enter your administrator password to authorize this reset.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = resetAdminPassword,
                        onValueChange = {
                            resetAdminPassword = it
                            resetPasswordError = null
                        },
                        label = { Text("Administrator Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = resetPasswordError != null,
                        supportingText = resetPasswordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_admin_password_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!authManager.verifyAdminPassword(resetAdminPassword)) {
                            resetPasswordError = "Incorrect administrator password. Verification failed."
                            return@Button
                        }
                        isResetting = true
                        viewModel.resetSystem {
                            isResetting = false
                            showResetDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("System reset complete. All operational data has been erased.")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isResetting && resetAdminPassword.isNotBlank(),
                    modifier = Modifier.testTag("confirm_system_reset_button")
                ) {
                    Text(if (isResetting) "Erasing System Data..." else "Confirm & Reset System")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    enabled = !isResetting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
