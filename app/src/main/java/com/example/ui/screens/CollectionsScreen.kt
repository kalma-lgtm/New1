package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Collection
import com.example.data.model.Subscriber
import com.example.ui.components.PrintableReceiptDialog
import com.example.ui.components.SectionHeader
import com.example.ui.theme.StatusActive
import com.example.ui.viewmodel.IspViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CollectionsScreen(
    viewModel: IspViewModel,
    initialSubscriberId: String? = null,
    initialBillId: String? = null,
    modifier: Modifier = Modifier
) {
    val collectionsState by viewModel.collectionsState.collectAsState()
    val subscribersState by viewModel.subscribersState.collectAsState()

    val collections = (collectionsState as? UiState.Success)?.data ?: emptyList()
    val subscribers = (subscribersState as? UiState.Success)?.data ?: emptyList()

    var searchQuery by remember { mutableStateOf("") }
    var isRecordPaymentDialogOpen by remember { mutableStateOf(initialSubscriberId != null) }
    var selectedCollectionForReceipt by remember { mutableStateOf<Collection?>(null) }

    val filteredCollections = collections.filter { coll ->
        searchQuery.isBlank() ||
                coll.subscriberName.contains(searchQuery, ignoreCase = true) ||
                coll.accountNumber.contains(searchQuery, ignoreCase = true) ||
                coll.receiptNumber.contains(searchQuery, ignoreCase = true) ||
                coll.referenceNumber.contains(searchQuery, ignoreCase = true)
    }

    val totalCollected = collections.sumOf { it.amount }
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayCollected = collections.filter { it.paymentDate == todayStr }.sumOf { it.amount }

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
                title = "Collections & Cashier",
                subtitle = "Payment transaction recording, receipts & daily audit"
            )

            // Metrics row
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
                        Text("Today's Collections", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("₱${String.format(Locale.US, "%,.2f", todayCollected)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusActive.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("All-Time Collections", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₱${String.format(Locale.US, "%,.2f", totalCollected)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StatusActive)
                    }
                }
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by receipt #, subscriber, account #...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Collections History List
            if (filteredCollections.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (collections.isEmpty()) "No payment collections recorded yet." else "No records matching query.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCollections, key = { it.id }) { coll ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCollectionForReceipt = coll },
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
                                    Text(text = coll.subscriberName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${coll.receiptNumber} • ${coll.accountNumber} • ${coll.paymentMethod}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Date: ${coll.paymentDate} • Received by: ${coll.receivedBy}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₱${String.format(Locale.US, "%,.2f", coll.amount)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = StatusActive
                                    )
                                    IconButton(onClick = { selectedCollectionForReceipt = coll }) {
                                        Icon(Icons.Default.Print, contentDescription = "View / Print Receipt", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isRecordPaymentDialogOpen) {
        RecordPaymentDialog(
            subscribers = subscribers,
            initialSubscriberId = initialSubscriberId,
            initialBillId = initialBillId,
            onDismiss = { isRecordPaymentDialogOpen = false },
            onConfirm = { subId, billId, amount, method, refNo, remarks ->
                viewModel.recordPayment(
                    subscriberId = subId,
                    billId = billId,
                    amount = amount,
                    paymentMethod = method,
                    referenceNumber = refNo,
                    remarks = remarks
                ) { receiptNo ->
                    isRecordPaymentDialogOpen = false
                    // Find created collection or display receipt
                    val sub = subscribers.firstOrNull { it.id == subId }
                    selectedCollectionForReceipt = Collection(
                        receiptNumber = receiptNo,
                        subscriberName = sub?.fullName ?: "Subscriber",
                        accountNumber = sub?.accountNumber ?: "",
                        amount = amount,
                        paymentMethod = method,
                        referenceNumber = refNo,
                        paymentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        receivedBy = "Admin"
                    )
                }
            }
        )
    }

    selectedCollectionForReceipt?.let { coll ->
        PrintableReceiptDialog(
            receiptNumber = coll.receiptNumber,
            date = coll.paymentDate,
            subscriberName = coll.subscriberName,
            accountNumber = coll.accountNumber,
            amount = coll.amount,
            paymentMethod = coll.paymentMethod,
            referenceNumber = coll.referenceNumber,
            receivedBy = coll.receivedBy,
            onDismiss = { selectedCollectionForReceipt = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
    subscribers: List<Subscriber>,
    initialSubscriberId: String?,
    initialBillId: String?,
    onDismiss: () -> Unit,
    onConfirm: (subId: String, billId: String, amount: Double, method: String, refNo: String, remarks: String) -> Unit
) {
    var selectedSubscriber by remember {
        mutableStateOf(subscribers.firstOrNull { it.id == initialSubscriberId } ?: subscribers.firstOrNull())
    }
    var expandedDropdown by remember { mutableStateOf(false) }

    var amount by remember {
        mutableStateOf(selectedSubscriber?.currentBalance?.let { if (it > 0) it.toString() else selectedSubscriber?.monthlyFee?.toString() } ?: "1299.0")
    }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var referenceNumber by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("Monthly subscription fee") }

    val methods = listOf("Cash", "GCash", "Maya", "PayPal", "Bank Transfer (BDO)", "Bank Transfer (BPI)")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Record Payment Collection",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Subscriber selection
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedSubscriber?.let { "${it.fullName} (${it.accountNumber})" } ?: "Select Subscriber",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        label = { Text("Subscriber Account") }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        subscribers.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text("${sub.fullName} (${sub.accountNumber}) - Due: ₱${sub.currentBalance}") },
                                onClick = {
                                    selectedSubscriber = sub
                                    if (sub.currentBalance > 0) {
                                        amount = sub.currentBalance.toString()
                                    } else {
                                        amount = sub.monthlyFee.toString()
                                    }
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                selectedSubscriber?.let { sub ->
                    if (sub.currentBalance > 0) {
                        Text(
                            text = "Outstanding Balance Due: ₱${String.format(Locale.US, "%,.2f", sub.currentBalance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE11D48),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Payment Amount (₱) *") },
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
                )

                // Payment Method
                var methodExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = methodExpanded,
                    onExpandedChange = { methodExpanded = !methodExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        label = { Text("Payment Method") }
                    )
                    ExposedDropdownMenu(
                        expanded = methodExpanded,
                        onDismissRequest = { methodExpanded = false }
                    ) {
                        methods.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = {
                                    paymentMethod = m
                                    methodExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = { referenceNumber = it },
                    label = { Text("Reference Number (OR / E-Wallet Ref)") },
                    placeholder = { Text("e.g. GC-123456789") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            val sub = selectedSubscriber
                            if (sub != null && amt > 0) {
                                onConfirm(sub.id, initialBillId ?: "", amt, paymentMethod, referenceNumber.trim(), remarks.trim())
                            }
                        },
                        enabled = selectedSubscriber != null && (amount.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("Record & Generate Receipt")
                    }
                }
            }
        }
    }
}
