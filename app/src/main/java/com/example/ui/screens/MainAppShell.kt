package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.OperatorRole
import com.example.data.model.OperatorUser
import com.example.ui.theme.StatusActive
import com.example.ui.viewmodel.IspViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch

enum class AppDestination(val route: String, val title: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.Dashboard),
    SUBSCRIBERS("subscribers", "Subscribers", Icons.Default.Group),
    INTERNET_PLANS("plans", "Internet Plans", Icons.Default.Speed),
    BILLING("billing", "Billing", Icons.Default.Receipt),
    COLLECTIONS("collections", "Collections", Icons.Default.AccountBalanceWallet),
    EXPENSES("expenses", "Expenses", Icons.Default.MoneyOff),
    MAP("map", "MAP", Icons.Default.LocationOn),
    MIKROTIK("mikrotik", "MikroTik Management", Icons.Default.Router),
    REPORTS("reports", "Reports", Icons.Default.Assessment),
    USER_MANAGEMENT("users", "User Management", Icons.Default.AdminPanelSettings),
    SYSTEM_SETTINGS("settings", "System Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(
    viewModel: IspViewModel,
    currentOperator: OperatorUser? = null,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val role = currentOperator?.role ?: OperatorRole.ADMINISTRATOR
    val isBillingOrCasher = role == OperatorRole.BILLING || role == OperatorRole.CASHER

    val allowedDestinations = remember(role) {
        if (isBillingOrCasher) {
            listOf(
                AppDestination.SUBSCRIBERS,
                AppDestination.BILLING,
                AppDestination.COLLECTIONS,
                AppDestination.EXPENSES
            )
        } else {
            AppDestination.values().toList()
        }
    }

    var currentDestination by remember {
        mutableStateOf(if (isBillingOrCasher) AppDestination.SUBSCRIBERS else AppDestination.DASHBOARD)
    }

    LaunchedEffect(allowedDestinations) {
        if (currentDestination !in allowedDestinations) {
            currentDestination = allowedDestinations.first()
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val userMessage by viewModel.userMessage.collectAsState()
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    // Modal forms state triggered from dashboard shortcuts
    var showAddSubscriberModal by remember { mutableStateOf(false) }
    var showGenerateBillsModal by remember { mutableStateOf(false) }
    var showRecordPaymentModal by remember { mutableStateOf(false) }
    var showAddExpenseModal by remember { mutableStateOf(false) }

    val routersState by viewModel.routersState.collectAsState()
    val routers = (routersState as? UiState.Success)?.data ?: emptyList()
    val onlineRoutersCount = routers.count { it.status == "Online" }

    val homeDestination = if (isBillingOrCasher) AppDestination.SUBSCRIBERS else AppDestination.DASHBOARD
    BackHandler(enabled = currentDestination != homeDestination) {
        currentDestination = homeDestination
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Router, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "NetPulse ISP",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Role: ${role.displayName} • ${currentOperator?.username ?: "Admin"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    allowedDestinations.forEach { destination ->
                        NavigationDrawerItem(
                            label = { Text(destination.title, fontWeight = FontWeight.Medium) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            selected = currentDestination == destination,
                            onClick = {
                                currentDestination = destination
                                coroutineScope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    NavigationDrawerItem(
                        label = { Text("LOGOUT", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                        icon = { Icon(Icons.Default.ExitToApp, contentDescription = "LOGOUT", tint = MaterialTheme.colorScheme.error) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            onSignOut()
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.error,
                            unselectedIconColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("drawer_logout_item"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentDestination.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Sidebar Navigation")
                        }
                    },
                    actions = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isBillingOrCasher) {
                                // MikroTik online indicator pill
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { currentDestination = AppDestination.MIKROTIK }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (onlineRoutersCount > 0) StatusActive else Color(0xFFDC2626))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "MikroTik: $onlineRoutersCount Online",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            IconButton(
                                onClick = onSignOut,
                                modifier = Modifier.testTag("top_bar_logout_button")
                            ) {
                                Icon(
                                    Icons.Default.ExitToApp,
                                    contentDescription = "LOGOUT",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentDestination) {
                    AppDestination.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { route ->
                            currentDestination = AppDestination.values().firstOrNull { it.route == route } ?: AppDestination.DASHBOARD
                        },
                        onOpenAddSubscriber = { showAddSubscriberModal = true },
                        onOpenGenerateBills = { showGenerateBillsModal = true },
                        onOpenRecordPayment = { showRecordPaymentModal = true },
                        onOpenAddExpense = { showAddExpenseModal = true }
                    )
                    AppDestination.SUBSCRIBERS -> SubscribersScreen(
                        viewModel = viewModel
                    )
                    AppDestination.INTERNET_PLANS -> InternetPlansScreen(
                        viewModel = viewModel
                    )
                    AppDestination.BILLING -> BillingScreen(
                        viewModel = viewModel,
                        onOpenRecordPayment = { subId, billId ->
                            currentDestination = AppDestination.COLLECTIONS
                        }
                    )
                    AppDestination.COLLECTIONS -> CollectionsScreen(
                        viewModel = viewModel
                    )
                    AppDestination.EXPENSES -> ExpensesScreen(
                        viewModel = viewModel
                    )
                    AppDestination.MAP -> SubscriberMapScreen(
                        viewModel = viewModel,
                        onNavigateToSubscriber = {
                            currentDestination = AppDestination.SUBSCRIBERS
                        }
                    )
                    AppDestination.MIKROTIK -> MikrotikScreen(
                        viewModel = viewModel
                    )
                    AppDestination.REPORTS -> ReportsScreen(
                        viewModel = viewModel
                    )
                    AppDestination.USER_MANAGEMENT -> UserManagementScreen(
                        viewModel = viewModel
                    )
                    AppDestination.SYSTEM_SETTINGS -> SystemSettingsScreen(
                        viewModel = viewModel,
                        onSignOut = onSignOut
                    )
                }

                // Global Modals triggered from quick actions
                if (showAddSubscriberModal) {
                    val plans = (viewModel.plansState.collectAsState().value as? UiState.Success)?.data ?: emptyList()
                    SubscriberFormDialog(
                        subscriber = null,
                        availablePlans = plans,
                        onDismiss = { showAddSubscriberModal = false },
                        onSave = { newSub ->
                            viewModel.addSubscriber(newSub) {
                                showAddSubscriberModal = false
                            }
                        }
                    )
                }

                if (showGenerateBillsModal) {
                    GenerateBillsDialog(
                        onDismiss = { showGenerateBillsModal = false },
                        onConfirm = { period, dueDate ->
                            viewModel.generateBills(period, dueDate) {
                                showGenerateBillsModal = false
                            }
                        }
                    )
                }

                if (showRecordPaymentModal) {
                    val subscribers = (viewModel.subscribersState.collectAsState().value as? UiState.Success)?.data ?: emptyList()
                    RecordPaymentDialog(
                        subscribers = subscribers,
                        initialSubscriberId = null,
                        initialBillId = null,
                        onDismiss = { showRecordPaymentModal = false },
                        onConfirm = { subId, billId, amount, method, ref, remarks ->
                            viewModel.recordPayment(subId, billId, amount, method, ref, remarks) {
                                showRecordPaymentModal = false
                            }
                        }
                    )
                }

                if (showAddExpenseModal) {
                    AddExpenseDialog(
                        onDismiss = { showAddExpenseModal = false },
                        onConfirm = { exp ->
                            viewModel.addExpense(exp) {
                                showAddExpenseModal = false
                            }
                        }
                    )
                }
            }
        }
    }
}
