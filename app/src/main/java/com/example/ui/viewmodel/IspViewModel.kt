package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ActivityLog
import com.example.data.model.Bill
import com.example.data.model.Collection
import com.example.data.model.Expense
import com.example.data.model.InternetPlan
import com.example.data.model.NapBox
import com.example.data.model.Olt
import com.example.data.model.Router
import com.example.data.model.SpeedProfile
import com.example.data.model.Subscriber
import com.example.data.model.SystemSettings
import com.example.data.repository.IspRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<out T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

data class DashboardMetrics(
    val totalSubscribers: Int = 0,
    val activeSubscribers: Int = 0,
    val disconnectedSubscribers: Int = 0,
    val suspendedSubscribers: Int = 0,
    val totalMonthlyBilling: Double = 0.0,
    val totalMonthlyCollections: Double = 0.0,
    val totalMonthlyExpenses: Double = 0.0,
    val outstandingBalances: Double = 0.0,
    val netIncome: Double = 0.0
)

class IspViewModel(private val repository: IspRepository) : ViewModel() {

    val subscribersState: StateFlow<UiState<List<Subscriber>>> = repository.observeSubscribers()
        .catch { error ->
            val root = generateSequence(error) { it.cause }.firstOrNull { it is FirebaseFirestoreException } ?: error
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .combine(MutableStateFlow(Unit)) { list, _ -> UiState.Success(list) as UiState<List<Subscriber>> }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val billsState: StateFlow<UiState<List<Bill>>> = repository.observeBills()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .combine(MutableStateFlow(Unit)) { list, _ -> UiState.Success(list) as UiState<List<Bill>> }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val collectionsState: StateFlow<UiState<List<Collection>>> = repository.observeCollections()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .combine(MutableStateFlow(Unit)) { list, _ -> UiState.Success(list) as UiState<List<Collection>> }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val expensesState: StateFlow<UiState<List<Expense>>> = repository.observeExpenses()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .combine(MutableStateFlow(Unit)) { list, _ -> UiState.Success(list) as UiState<List<Expense>> }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val routersState: StateFlow<UiState<List<Router>>> = repository.observeRouters()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .combine(MutableStateFlow(Unit)) { list, _ -> UiState.Success(list) as UiState<List<Router>> }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val profilesState: StateFlow<UiState<List<SpeedProfile>>> = repository.observeProfiles()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .combine(MutableStateFlow(Unit)) { list, _ -> UiState.Success(list) as UiState<List<SpeedProfile>> }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val logsState: StateFlow<UiState<List<ActivityLog>>> = repository.observeActivityLogs()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .combine(MutableStateFlow(Unit)) { list, _ -> UiState.Success(list) as UiState<List<ActivityLog>> }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val oltsState: StateFlow<UiState<List<Olt>>> = repository.observeOlts()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .combine(MutableStateFlow(Unit)) { list, _ -> UiState.Success(list) as UiState<List<Olt>> }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val settingsState: StateFlow<UiState<SystemSettings>> = repository.observeSettings()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(SystemSettings())
        }
        .combine(MutableStateFlow(Unit)) { settings, _ -> UiState.Success(settings) as UiState<SystemSettings> }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val plansState: StateFlow<UiState<List<InternetPlan>>> = repository.observePlans()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .combine(MutableStateFlow(Unit)) { list, _ -> UiState.Success(list) as UiState<List<InternetPlan>> }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    // User message / snackbar
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    init {
        viewModelScope.launch {
            try {
                repository.seedSampleDataIfNeeded()
            } catch (e: Exception) {
                // Ignore seed error
            }
        }
    }

    // Subscriber Operations
    fun addSubscriber(subscriber: Subscriber, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.addSubscriber(subscriber)
                _userMessage.value = "Subscriber ${subscriber.fullName} registered successfully!"
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = "Failed to add subscriber: ${e.message}"
            }
        }
    }

    fun updateSubscriber(subscriber: Subscriber, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.updateSubscriber(subscriber)
                _userMessage.value = "Subscriber updated."
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = "Failed to update subscriber: ${e.message}"
            }
        }
    }

    fun toggleSubscriberStatus(subscriberId: String, newStatus: String, reason: String = "") {
        viewModelScope.launch {
            try {
                repository.updateSubscriberStatus(subscriberId, newStatus, reason)
                _userMessage.value = "Subscriber connection set to $newStatus."
            } catch (e: Exception) {
                _userMessage.value = "Error updating status: ${e.message}"
            }
        }
    }

    fun deleteSubscriber(subscriberId: String) {
        viewModelScope.launch {
            try {
                repository.deleteSubscriber(subscriberId)
                _userMessage.value = "Subscriber moved to ARCHIVED. Credentials retained in system; deleted in MikroTik PPPoE."
            } catch (e: Exception) {
                _userMessage.value = "Failed to archive subscriber: ${e.message}"
            }
        }
    }

    fun restoreSubscriber(subscriberId: String) {
        viewModelScope.launch {
            try {
                repository.restoreSubscriber(subscriberId)
                _userMessage.value = "Subscriber restored to active list and PPPoE reprovisioned."
            } catch (e: Exception) {
                _userMessage.value = "Failed to restore subscriber: ${e.message}"
            }
        }
    }

    fun permanentlyDeleteSubscriber(subscriberId: String) {
        viewModelScope.launch {
            try {
                repository.permanentlyDeleteSubscriber(subscriberId)
                _userMessage.value = "Subscriber permanently removed from system database."
            } catch (e: Exception) {
                _userMessage.value = "Failed to purge subscriber: ${e.message}"
            }
        }
    }

    // Billing Operations
    fun generateBills(period: String, dueDate: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val count = repository.generateMonthlyBills(period, dueDate)
                _userMessage.value = "Generated $count monthly bills for period $period."
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = "Billing generation error: ${e.message}"
            }
        }
    }

    // Collection Operations
    fun recordPayment(
        subscriberId: String,
        billId: String,
        amount: Double,
        paymentMethod: String,
        referenceNumber: String,
        remarks: String,
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val receipt = repository.recordPayment(
                    subscriberId = subscriberId,
                    billId = billId,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    referenceNumber = referenceNumber,
                    remarks = remarks
                )
                _userMessage.value = "Payment recorded! Receipt: $receipt"
                onSuccess(receipt)
            } catch (e: Exception) {
                _userMessage.value = "Failed to record payment: ${e.message}"
            }
        }
    }

    // Expense Operations
    fun addExpense(expense: Expense, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.addExpense(expense)
                _userMessage.value = "Expense of ₱${String.format(Locale.US, "%.2f", expense.amount)} logged."
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = "Failed to add expense: ${e.message}"
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            try {
                repository.deleteExpense(expenseId)
                _userMessage.value = "Expense deleted."
            } catch (e: Exception) {
                _userMessage.value = "Failed to delete expense: ${e.message}"
            }
        }
    }

    // Router Operations
    fun addRouter(router: Router, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.addRouter(router)
                _userMessage.value = "MikroTik router '${router.name}' registered."
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = "Failed to add router: ${e.message}"
            }
        }
    }

    fun testRouter(router: Router) {
        viewModelScope.launch {
            try {
                repository.testRouterConnection(router)
                _userMessage.value = "Router '${router.name}' connection test successful!"
            } catch (e: Exception) {
                _userMessage.value = "Router test failed: ${e.message}"
            }
        }
    }

    fun deleteRouter(routerId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteRouter(routerId)
                _userMessage.value = "MikroTik router configuration deleted."
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = "Failed to delete router: ${e.message}"
            }
        }
    }

    fun saveSettings(settings: SystemSettings, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.saveSettings(settings)
                _userMessage.value = "System settings saved successfully to database!"
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = "Failed to save settings: ${e.message}"
            }
        }
    }

    fun resetSystem(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.resetSystem()
                _userMessage.value = "System operational records have been completely reset."
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = "System reset error: ${e.message}"
            }
        }
    }

    // OLT & PON Operations
    fun addOlt(olt: Olt, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.addOlt(olt)
                _userMessage.value = "OLT '${olt.name}' registered with ${olt.ponPortsCount} PON ports."
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = "Failed to add OLT: ${e.message}"
            }
        }
    }

    fun updateOlt(olt: Olt, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.updateOlt(olt)
                _userMessage.value = "OLT '${olt.name}' updated."
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = "Failed to update OLT: ${e.message}"
            }
        }
    }

    fun deleteOlt(oltId: String) {
        viewModelScope.launch {
            try {
                repository.deleteOlt(oltId)
                _userMessage.value = "OLT deleted."
            } catch (e: Exception) {
                _userMessage.value = "Failed to delete OLT: ${e.message}"
            }
        }
    }

    fun addNapToOlt(oltId: String, ponPortNumber: Int, napBox: NapBox, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.addNapToOlt(oltId, ponPortNumber, napBox)
                _userMessage.value = "NAP '${napBox.name}' added to PON Port $ponPortNumber and pinned on Map!"
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = "Failed to add NAP: ${e.message}"
            }
        }
    }

    fun deleteNapFromOlt(oltId: String, napId: String) {
        viewModelScope.launch {
            try {
                repository.deleteNapFromOlt(oltId, napId)
                _userMessage.value = "NAP Box removed."
            } catch (e: Exception) {
                _userMessage.value = "Failed to delete NAP: ${e.message}"
            }
        }
    }

    fun addSpeedProfile(profile: SpeedProfile, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.addProfile(profile)
                _userMessage.value = "Speed profile '${profile.name}' created."
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = "Failed to create profile: ${e.message}"
            }
        }
    }

    fun syncPppoeSecretsToSubscribers(
        onSuccess: (updatedSubscribers: List<Subscriber>) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val updatedList = repository.syncPppoeSecretsToSubscribers()
                _userMessage.value = "Copied ${updatedList.size} PPPoE usernames and passwords to system subscribers!"
                onSuccess(updatedList)
            } catch (e: Exception) {
                _userMessage.value = "Failed to sync PPPoE credentials: ${e.message}"
            }
        }
    }

    fun refreshAndCopyProfiles(
        onResult: (formattedText: String, count: Int) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val profiles = repository.refreshRouterProfiles()
                val sb = StringBuilder()
                sb.append("=== MIKROTIK PPPOE SPEED PROFILES ===\n")
                sb.append("Total Profiles Available: ${profiles.size}\n")
                sb.append("Source: MikroTik Core Router API (/ppp profile print)\n\n")

                profiles.forEachIndexed { index, prof ->
                    sb.append("[${index + 1}] ${prof.name}${if (prof.isDefault) " [DEFAULT]" else ""}\n")
                    sb.append("    Rate Limit : ${prof.rateLimit} (Down: ${prof.downloadSpeedMbps} Mbps / Up: ${prof.uploadSpeedMbps} Mbps)\n")
                    sb.append("    Price      : ₱${String.format(Locale.US, "%,.2f", prof.price)}\n")
                    sb.append("    CLI Command: /ppp profile add name=\"${prof.name}\" rate-limit=${prof.rateLimit}\n\n")
                }

                sb.append("=== ROUTEROS CLI BATCH SCRIPT ===\n")
                profiles.forEach { prof ->
                    sb.append("/ppp profile add name=\"${prof.name}\" rate-limit=${prof.rateLimit} comment=\"NetPulse ISP Profile\"\n")
                }

                val exportText = sb.toString()
                _userMessage.value = "Refreshed & copied ${profiles.size} available profiles to clipboard!"
                onResult(exportText, profiles.size)
            } catch (e: Exception) {
                _userMessage.value = "Failed to refresh profiles: ${e.message}"
            }
        }
    }

    fun addPlan(plan: InternetPlan, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.addPlan(plan)
                _userMessage.value = "Internet plan '${plan.name}' added successfully."
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = "Failed to add plan: ${e.message}"
            }
        }
    }

    fun updatePlan(plan: InternetPlan, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.updatePlan(plan)
                _userMessage.value = "Internet plan '${plan.name}' updated."
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = "Failed to update plan: ${e.message}"
            }
        }
    }

    fun togglePlanStatus(planId: String) {
        viewModelScope.launch {
            try {
                repository.togglePlanStatus(planId)
            } catch (e: Exception) {
                _userMessage.value = "Failed to toggle plan status: ${e.message}"
            }
        }
    }

    fun deletePlan(planId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deletePlan(planId)
                _userMessage.value = "Internet plan deleted."
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = "Failed to delete plan: ${e.message}"
            }
        }
    }

    fun showNotification(message: String) {
        _userMessage.value = message
    }

    fun seedDemoData() {
        viewModelScope.launch {
            try {
                repository.seedSampleDataIfNeeded()
                _userMessage.value = "Sample ISP data loaded successfully!"
            } catch (e: Exception) {
                _userMessage.value = "Seeding error: ${e.message}"
            }
        }
    }
}

class IspViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return IspViewModel(IspRepository(context)) as T
    }
}
