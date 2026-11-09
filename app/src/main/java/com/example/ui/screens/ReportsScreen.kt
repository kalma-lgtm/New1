package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodel.IspViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val REPORT_TYPES = listOf(
    "Subscriber Masterlist",
    "Active Subscribers",
    "Suspended Subscribers",
    "Disconnected Subscribers",
    "Monthly Billing",
    "Daily Collections",
    "Monthly Collections",
    "Outstanding Balances",
    "Monthly Expenses",
    "Net Income Summary",
    "MikroTik Connection Status"
)

@Composable
fun ReportsScreen(
    viewModel: IspViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val subscribersState by viewModel.subscribersState.collectAsState()
    val billsState by viewModel.billsState.collectAsState()
    val collectionsState by viewModel.collectionsState.collectAsState()
    val expensesState by viewModel.expensesState.collectAsState()
    val routersState by viewModel.routersState.collectAsState()

    val subscribers = (subscribersState as? UiState.Success)?.data ?: emptyList()
    val bills = (billsState as? UiState.Success)?.data ?: emptyList()
    val collections = (collectionsState as? UiState.Success)?.data ?: emptyList()
    val expenses = (expensesState as? UiState.Success)?.data ?: emptyList()
    val routers = (routersState as? UiState.Success)?.data ?: emptyList()

    var selectedReport by remember { mutableStateOf(REPORT_TYPES[0]) }

    fun exportReportAsCsv() {
        val sb = StringBuilder()
        when (selectedReport) {
            "Subscriber Masterlist", "Active Subscribers", "Suspended Subscribers", "Disconnected Subscribers" -> {
                val list = when (selectedReport) {
                    "Active Subscribers" -> subscribers.filter { it.connectionStatus == "Active" }
                    "Suspended Subscribers" -> subscribers.filter { it.connectionStatus == "Suspended" }
                    "Disconnected Subscribers" -> subscribers.filter { it.connectionStatus == "Disconnected" }
                    else -> subscribers
                }
                sb.append("Account Number,Full Name,Contact Number,Plan,Monthly Fee,Status,NAP Box,Balance\n")
                list.forEach {
                    sb.append("\"${it.accountNumber}\",\"${it.fullName}\",\"${it.contactNumber}\",\"${it.planName}\",${it.monthlyFee},\"${it.connectionStatus}\",\"${it.napBox}\",${it.currentBalance}\n")
                }
            }
            "Monthly Billing" -> {
                sb.append("Bill Period,Account Number,Subscriber,Total Due,Amount Paid,Remaining,Status\n")
                bills.forEach {
                    sb.append("\"${it.billingPeriod}\",\"${it.accountNumber}\",\"${it.subscriberName}\",${it.totalAmountDue},${it.amountPaid},${it.remainingBalance},\"${it.status}\"\n")
                }
            }
            "Daily Collections", "Monthly Collections" -> {
                sb.append("Receipt Number,Date,Account Number,Subscriber,Amount,Method,Reference\n")
                collections.forEach {
                    sb.append("\"${it.receiptNumber}\",\"${it.paymentDate}\",\"${it.accountNumber}\",\"${it.subscriberName}\",${it.amount},\"${it.paymentMethod}\",\"${it.referenceNumber}\"\n")
                }
            }
            "Outstanding Balances" -> {
                sb.append("Account Number,Subscriber,Contact,Monthly Fee,Outstanding Balance\n")
                subscribers.filter { it.currentBalance > 0 }.forEach {
                    sb.append("\"${it.accountNumber}\",\"${it.fullName}\",\"${it.contactNumber}\",${it.monthlyFee},${it.currentBalance}\n")
                }
            }
            "Monthly Expenses" -> {
                sb.append("Date,Category,Description,Amount,Method,Responsible Person\n")
                expenses.forEach {
                    sb.append("\"${it.transactionDate}\",\"${it.category}\",\"${it.description}\",${it.amount},\"${it.paymentMethod}\",\"${it.responsiblePerson}\"\n")
                }
            }
            else -> {
                val collTotal = collections.sumOf { it.amount }
                val expTotal = expenses.sumOf { it.amount }
                sb.append("Net Income Summary\n")
                sb.append("Total Collections,${collTotal}\n")
                sb.append("Total Expenses,${expTotal}\n")
                sb.append("Net Income,${collTotal - expTotal}\n")
            }
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ISP Report CSV", sb.toString())
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$selectedReport exported to clipboard (CSV format)!", Toast.LENGTH_SHORT).show()
    }

    Scaffold(modifier = modifier.fillMaxSize()) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionHeader(
                title = "Financial & Operational Reports",
                subtitle = "Generate official statements, masterlists, and export to CSV/PDF",
                actionButton = {
                    Button(onClick = { exportReportAsCsv() }) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export CSV")
                    }
                }
            )

            // Report Types Horizontal Selector
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(REPORT_TYPES) { report ->
                    FilterChip(
                        selected = selectedReport == report,
                        onClick = { selectedReport = report },
                        label = { Text(report) }
                    )
                }
            }

            // Report Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = selectedReport, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(text = "Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                    OutlinedButton(onClick = { exportReportAsCsv() }) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                }
            }

            // Report Data Table View
            Box(modifier = Modifier.weight(1f)) {
                when (selectedReport) {
                    "Subscriber Masterlist", "Active Subscribers", "Suspended Subscribers", "Disconnected Subscribers" -> {
                        val list = when (selectedReport) {
                            "Active Subscribers" -> subscribers.filter { it.connectionStatus == "Active" }
                            "Suspended Subscribers" -> subscribers.filter { it.connectionStatus == "Suspended" }
                            "Disconnected Subscribers" -> subscribers.filter { it.connectionStatus == "Disconnected" }
                            else -> subscribers
                        }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(list) { s ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = s.fullName, fontWeight = FontWeight.Bold)
                                            Text(text = "${s.accountNumber} • ${s.planName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            StatusBadge(status = s.connectionStatus)
                                            Text(text = "₱${String.format(Locale.US, "%,.2f", s.monthlyFee)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Monthly Billing" -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(bills) { b ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = b.subscriberName, fontWeight = FontWeight.Bold)
                                            Text(text = "${b.accountNumber} • Cycle ${b.billingPeriod}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(text = "₱${String.format(Locale.US, "%,.2f", b.totalAmountDue)}", fontWeight = FontWeight.Bold)
                                            StatusBadge(status = b.status)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Daily Collections", "Monthly Collections" -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(collections) { c ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = c.receiptNumber, fontWeight = FontWeight.Bold)
                                            Text(text = "${c.subscriberName} • ${c.paymentDate}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text(text = "₱${String.format(Locale.US, "%,.2f", c.amount)}", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                    }
                                }
                            }
                        }
                    }
                    "Outstanding Balances" -> {
                        val withBalance = subscribers.filter { it.currentBalance > 0 }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(withBalance) { s ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = s.fullName, fontWeight = FontWeight.Bold)
                                            Text(text = "${s.accountNumber} • ${s.contactNumber}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text(text = "₱${String.format(Locale.US, "%,.2f", s.currentBalance)}", fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                                    }
                                }
                            }
                        }
                    }
                    "Monthly Expenses" -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(expenses) { e ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = e.category, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text(text = "${e.description} • ${e.transactionDate}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text(text = "₱${String.format(Locale.US, "%,.2f", e.amount)}", fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        val collTotal = collections.sumOf { it.amount }
                        val expTotal = expenses.sumOf { it.amount }
                        val net = collTotal - expTotal
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Gross Collections", style = MaterialTheme.typography.bodyMedium)
                                        Text("₱${String.format(Locale.US, "%,.2f", collTotal)}", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Operational Expenses", style = MaterialTheme.typography.bodyMedium)
                                        Text("₱${String.format(Locale.US, "%,.2f", expTotal)}", fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                                    }
                                    HorizontalDivider()
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Net Operating Income", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("₱${String.format(Locale.US, "%,.2f", net)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (net >= 0) Color(0xFF16A34A) else Color(0xFFE11D48))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
