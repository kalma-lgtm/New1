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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.MetricCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.SimpleDonutChart
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusSuspended
import com.example.ui.viewmodel.IspViewModel
import com.example.ui.viewmodel.UiState
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: IspViewModel,
    onNavigate: (String) -> Unit,
    onOpenAddSubscriber: () -> Unit = {},
    onOpenGenerateBills: () -> Unit = {},
    onOpenRecordPayment: () -> Unit = {},
    onOpenAddExpense: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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

    val activeSubsList = subscribers.filter { !it.isArchived }
    val totalSubscribers = activeSubsList.size
    val activeSubscribers = activeSubsList.count { it.connectionStatus == "Active" }
    val suspendedSubscribers = activeSubsList.count { it.connectionStatus == "Suspended" }
    val disconnectedSubscribers = activeSubsList.count { it.connectionStatus == "Disconnected" }
    val archivedSubscribers = subscribers.count { it.isArchived }

    val totalBilling = bills.sumOf { it.totalAmountDue }
    val totalCollections = collections.sumOf { it.amount }
    val totalExpenses = expenses.sumOf { it.amount }
    val outstandingBalances = subscribers.sumOf { it.currentBalance }
    val netIncome = totalCollections - totalExpenses

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Business Overview (KPI Summary Cards)
        item {
            SectionHeader(title = "Key Business Metrics", subtitle = "Real-time database aggregated summary")
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Subscribers",
                    value = "$totalSubscribers",
                    subtext = "Registered ISP users",
                    icon = Icons.Default.Group,
                    iconColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Active Subscribers",
                    value = "$activeSubscribers",
                    subtext = "${if (totalSubscribers > 0) (activeSubscribers * 100 / totalSubscribers) else 0}% online",
                    icon = Icons.Default.CheckCircle,
                    iconColor = StatusActive,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Suspended",
                    value = "$suspendedSubscribers",
                    subtext = "Overdue / Pending",
                    icon = Icons.Default.HourglassTop,
                    iconColor = StatusSuspended,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Disconnected",
                    value = "$disconnectedSubscribers",
                    subtext = "Terminated lines",
                    icon = Icons.Default.Cancel,
                    iconColor = StatusDisconnected,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Billing",
                    value = "₱${String.format(Locale.US, "%,.0f", totalBilling)}",
                    subtext = "Current cycle billed",
                    icon = Icons.Default.Receipt,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Collections",
                    value = "₱${String.format(Locale.US, "%,.0f", totalCollections)}",
                    subtext = "Payments received",
                    icon = Icons.Default.AccountBalanceWallet,
                    iconColor = StatusActive,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Expenses",
                    value = "₱${String.format(Locale.US, "%,.0f", totalExpenses)}",
                    subtext = "Operational costs",
                    icon = Icons.Default.MoneyOff,
                    iconColor = Color(0xFFE11D48),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Net Income",
                    value = "₱${String.format(Locale.US, "%,.0f", netIncome)}",
                    subtext = if (netIncome >= 0) "Profitable margin" else "Deficit",
                    icon = Icons.Default.TrendingUp,
                    iconColor = if (netIncome >= 0) StatusActive else Color(0xFFE11D48),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            MetricCard(
                title = "Outstanding Subscriber Balances",
                value = "₱${String.format(Locale.US, "%,.2f", outstandingBalances)}",
                subtext = "Total receivables pending collection across subscribers",
                icon = Icons.Default.ReceiptLong,
                iconColor = StatusSuspended,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Section 2: Graphical Reports
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "Graphical Reports & Analytics",
                subtitle = "Monthly trends, operational costs, and connection health"
            )
        }

        // Active vs Disconnected ratio donut
        item {
            SimpleDonutChart(
                activeCount = activeSubscribers,
                suspendedCount = suspendedSubscribers,
                disconnectedCount = disconnectedSubscribers,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Collections vs Expenses Comparison Chart
        item {
            Text(
                text = "Collection vs Expenses Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            val chartLabels = listOf("Jul", "Aug", "Sep", "Oct", "Nov")
            val collectionsTrend = listOf(35000f, 42000f, 48000f, 52000f, maxOf(1000f, totalCollections.toFloat()))
            val expensesTrend = listOf(22000f, 26000f, 24000f, 31000f, maxOf(1000f, totalExpenses.toFloat()))

            SimpleBarChart(
                labels = chartLabels,
                values1 = collectionsTrend,
                values2 = expensesTrend,
                legend1 = "Collections",
                legend2 = "Expenses",
                color1 = MaterialTheme.colorScheme.primary,
                color2 = Color(0xFFE11D48),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // MikroTik Live Status Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
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
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(StatusActive)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MikroTik Gateway Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${routers.count { it.status == "Online" }}/${routers.size} Online",
                            style = MaterialTheme.typography.labelMedium,
                            color = StatusActive,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    routers.take(2).forEach { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "${r.name} (${r.ipAddress})", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "CPU: ${r.cpuLoad}% | Sessions: ${r.activeSessionsCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { onNavigate("mikrotik") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Router, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open MikroTik Console")
                    }
                }
            }
        }
    }
}
