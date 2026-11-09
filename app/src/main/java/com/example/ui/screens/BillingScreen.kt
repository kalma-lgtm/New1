package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Bill
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodel.IspViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BillingScreen(
    viewModel: IspViewModel,
    onOpenRecordPayment: (String, String) -> Unit, // subscriberId, billId
    modifier: Modifier = Modifier
) {
    val billsState by viewModel.billsState.collectAsState()
    val bills = (billsState as? UiState.Success)?.data ?: emptyList()

    var isGenerateBillsDialogOpen by remember { mutableStateOf(false) }
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredBills = bills.filter { bill ->
        val matchesQuery = searchQuery.isBlank() ||
                bill.subscriberName.contains(searchQuery, ignoreCase = true) ||
                bill.accountNumber.contains(searchQuery, ignoreCase = true) ||
                bill.billingPeriod.contains(searchQuery, ignoreCase = true)

        val matchesStatus = selectedStatusFilter == "All" || bill.status == selectedStatusFilter

        matchesQuery && matchesStatus
    }

    val totalBilled = bills.sumOf { it.totalAmountDue }
    val totalPaid = bills.sumOf { it.amountPaid }
    val totalRemaining = bills.sumOf { it.remainingBalance }
    val overdueCount = bills.count { it.status == "Overdue" || (it.status == "Unpaid" && it.remainingBalance > 0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isGenerateBillsDialogOpen = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Generate Bills")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionHeader(
                title = "Billing Management",
                subtitle = "Automated monthly billing, balance computation & statements",
                actionButton = {
                    Button(onClick = { isGenerateBillsDialogOpen = true }) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Bills")
                    }
                }
            )

            // Billing Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BillingSummaryPill("Total Billed", "₱${String.format(Locale.US, "%,.0f", totalBilled)}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                BillingSummaryPill("Collected", "₱${String.format(Locale.US, "%,.0f", totalPaid)}", Color(0xFF16A34A), Modifier.weight(1f))
                BillingSummaryPill("Remaining", "₱${String.format(Locale.US, "%,.0f", totalRemaining)}", Color(0xFFE11D48), Modifier.weight(1f))
            }

            // Search & Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by subscriber name, account #, or period...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val statuses = listOf("All", "Unpaid", "Partially Paid", "Paid", "Overdue")
                items(statuses) { status ->
                    FilterChip(
                        selected = selectedStatusFilter == status,
                        onClick = { selectedStatusFilter = status },
                        label = { Text(status) }
                    )
                }
            }

            // Bills List
            if (filteredBills.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (bills.isEmpty()) "No billing records found. Tap 'Generate Bills' to run this month's billing." else "No bills match the selected filter.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredBills, key = { it.id }) { bill ->
                        BillCard(
                            bill = bill,
                            onPay = { onOpenRecordPayment(bill.subscriberId, bill.id) }
                        )
                    }
                }
            }
        }
    }

    if (isGenerateBillsDialogOpen) {
        GenerateBillsDialog(
            onDismiss = { isGenerateBillsDialogOpen = false },
            onConfirm = { period, dueDate ->
                viewModel.generateBills(period, dueDate) {
                    isGenerateBillsDialogOpen = false
                }
            }
        )
    }
}

@Composable
fun BillingSummaryPill(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun BillCard(bill: Bill, onPay: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Column {
                    Text(text = bill.subscriberName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "Account: ${bill.accountNumber} • Period: ${bill.billingPeriod}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusBadge(status = bill.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Breakdown box
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BillRow("Previous Balance", "₱${String.format(Locale.US, "%,.2f", bill.previousBalance)}")
                    BillRow("Monthly Subscription Fee", "₱${String.format(Locale.US, "%,.2f", bill.monthlyFee)}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    BillRow("Total Due", "₱${String.format(Locale.US, "%,.2f", bill.totalAmountDue)}", isBold = true)
                    BillRow("Amount Paid", "₱${String.format(Locale.US, "%,.2f", bill.amountPaid)}")
                    BillRow("Remaining Balance", "₱${String.format(Locale.US, "%,.2f", bill.remainingBalance)}", highlight = bill.remainingBalance > 0)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Due Date: ${if (bill.dueDate.isBlank()) "Standard Due" else bill.dueDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (bill.remainingBalance > 0) {
                    Button(onClick = onPay, shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record Payment")
                    }
                }
            }
        }
    }
}

@Composable
private fun BillRow(label: String, value: String, isBold: Boolean = false, highlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold || highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun GenerateBillsDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val currentPeriod = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    var period by remember { mutableStateOf(currentPeriod) }
    var dueDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-25", Locale.getDefault()).format(Date())) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Generate Monthly Subscriber Bills",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "This will generate statements for all active subscribers for the selected cycle. Remaining Balance = Previous Balance + Monthly Fee - Total Payments. Duplicate bills are automatically prevented.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = period,
                    onValueChange = { period = it },
                    label = { Text("Billing Period (YYYY-MM)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Payment Due Date") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(period, dueDate) },
                        enabled = period.isNotBlank() && dueDate.isNotBlank()
                    ) {
                        Text("Run Billing Cycle")
                    }
                }
            }
        }
    }
}
