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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.Expense
import com.example.ui.components.SectionHeader
import com.example.ui.components.SimpleBarChart
import com.example.ui.viewmodel.IspViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val EXPENSE_MONTHS = listOf(
    "ALL MONTHS",
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December"
)

val EXPENSE_CATEGORIES = listOf(
    "Fiber Optic Materials",
    "Network Equipment",
    "Fuel and Transportation",
    "Employee Salaries",
    "Food and Meals",
    "Electricity",
    "Internet and Bandwidth",
    "Maintenance and Repairs",
    "Office Supplies",
    "Other Expenses"
)

@Composable
fun ExpensesScreen(
    viewModel: IspViewModel,
    modifier: Modifier = Modifier
) {
    val expensesState by viewModel.expensesState.collectAsState()
    val expenses = (expensesState as? UiState.Success)?.data ?: emptyList()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedMonth by remember { mutableStateOf("ALL MONTHS") }
    var isMonthDropdownExpanded by remember { mutableStateOf(false) }
    var isAddExpenseDialogOpen by remember { mutableStateOf(false) }

    fun expenseMatchesMonth(dateStr: String, month: String): Boolean {
        if (month == "ALL MONTHS") return true
        val monthIdx = EXPENSE_MONTHS.indexOf(month)
        if (monthIdx < 1) return true
        val parts = dateStr.split("-")
        if (parts.size >= 2) {
            val m = parts[1].toIntOrNull()
            if (m == monthIdx) return true
        }
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d = sdf.parse(dateStr)
            if (d != null) {
                val cal = java.util.Calendar.getInstance()
                cal.time = d
                (cal.get(java.util.Calendar.MONTH) + 1) == monthIdx
            } else false
        } catch (_: Exception) {
            false
        }
    }

    val filteredExpenses = expenses.filter { exp ->
        val matchesQuery = searchQuery.isBlank() ||
                exp.description.contains(searchQuery, ignoreCase = true) ||
                exp.category.contains(searchQuery, ignoreCase = true) ||
                exp.responsiblePerson.contains(searchQuery, ignoreCase = true)

        val matchesCategory = selectedCategoryFilter == "All" || exp.category == selectedCategoryFilter
        val matchesMonth = expenseMatchesMonth(exp.transactionDate, selectedMonth)

        matchesQuery && matchesCategory && matchesMonth
    }

    val totalExpenses = expenses.sumOf { it.amount }
    val filteredMonthTotal = filteredExpenses.sumOf { it.amount }

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
                title = "Expense Management",
                subtitle = "ISP operational costs, equipment & maintenance tracking"
            )

            // Monthly breakdown KPI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE11D48).copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (selectedMonth == "ALL MONTHS") "Filtered Expenses" else "$selectedMonth Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₱${String.format(Locale.US, "%,.2f", filteredMonthTotal)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE11D48)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("All-Time Recorded Expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₱${String.format(Locale.US, "%,.2f", totalExpenses)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Month Filter Dropdown (All 12 Months + ALL MONTHS)
            @OptIn(ExperimentalMaterial3Api::class)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = isMonthDropdownExpanded,
                    onExpandedChange = { isMonthDropdownExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedMonth,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filter by Month (Jan - Dec)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMonthDropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("expenses_month_filter_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isMonthDropdownExpanded,
                        onDismissRequest = { isMonthDropdownExpanded = false }
                    ) {
                        EXPENSE_MONTHS.forEach { month ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = month,
                                        fontWeight = if (month == selectedMonth) FontWeight.Bold else FontWeight.Normal,
                                        color = if (month == selectedMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    selectedMonth = month
                                    isMonthDropdownExpanded = false
                                },
                                modifier = Modifier.testTag("month_option_$month")
                            )
                        }
                    }
                }

                if (selectedMonth != "ALL MONTHS") {
                    OutlinedButton(
                        onClick = { selectedMonth = "ALL MONTHS" },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("All")
                    }
                }
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search description, category, person responsible...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category Filter chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == "All",
                        onClick = { selectedCategoryFilter = "All" },
                        label = { Text("All Categories") }
                    )
                }
                items(EXPENSE_CATEGORIES) { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = { selectedCategoryFilter = if (selectedCategoryFilter == cat) "All" else cat },
                        label = { Text(cat) }
                    )
                }
            }

            // Expenses List
            if (filteredExpenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (expenses.isEmpty()) "No expense transactions recorded yet." else "No expenses matching filter.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredExpenses, key = { it.id }) { exp ->
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
                                    Text(text = exp.category, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(text = exp.description, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "${exp.transactionDate} • ${exp.paymentMethod} • By: ${exp.responsiblePerson}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (exp.referenceNumber.isNotBlank()) {
                                        Text(text = "Ref: ${exp.referenceNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₱${String.format(Locale.US, "%,.2f", exp.amount)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFFE11D48)
                                    )
                                    IconButton(onClick = { viewModel.deleteExpense(exp.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isAddExpenseDialogOpen) {
        AddExpenseDialog(
            onDismiss = { isAddExpenseDialogOpen = false },
            onConfirm = { expense ->
                viewModel.addExpense(expense) {
                    isAddExpenseDialogOpen = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (Expense) -> Unit
) {
    var category by remember { mutableStateOf(EXPENSE_CATEGORIES[0]) }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var transactionDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var responsiblePerson by remember { mutableStateOf("Admin") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var referenceNumber by remember { mutableStateOf("") }
    var receiptUrl by remember { mutableStateOf("") }

    var expandedCat by remember { mutableStateOf(false) }

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
                Text(
                    text = "Record Operational Expense",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCat,
                    onExpandedChange = { expandedCat = !expandedCat },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        label = { Text("Expense Category *") }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCat,
                        onDismissRequest = { expandedCat = false }
                    ) {
                        EXPENSE_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    expandedCat = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Item Details *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₱) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = transactionDate,
                        onValueChange = { transactionDate = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = { paymentMethod = it },
                        label = { Text("Payment Method") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = responsiblePerson,
                    onValueChange = { responsiblePerson = it },
                    label = { Text("Person Responsible") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = { referenceNumber = it },
                    label = { Text("Invoice / Receipt / Voucher Ref #") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (description.isNotBlank() && amt > 0) {
                                onConfirm(
                                    Expense(
                                        category = category,
                                        description = description.trim(),
                                        amount = amt,
                                        transactionDate = transactionDate.trim(),
                                        responsiblePerson = responsiblePerson.trim(),
                                        paymentMethod = paymentMethod.trim(),
                                        referenceNumber = referenceNumber.trim(),
                                        receiptUrl = receiptUrl.trim()
                                    )
                                )
                            }
                        },
                        enabled = description.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("Save Expense")
                    }
                }
            }
        }
    }
}
