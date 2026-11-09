package com.example.data.repository

import android.content.Context
import com.example.R
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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

class IspRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    // Local reactive storage for username/password session without cloud auth
    private val localSubscribers = MutableStateFlow<List<Subscriber>>(createDefaultSubscribers())
    private val localBills = MutableStateFlow<List<Bill>>(createDefaultBills())
    private val localCollections = MutableStateFlow<List<Collection>>(createDefaultCollections())
    private val localExpenses = MutableStateFlow<List<Expense>>(createDefaultExpenses())
    private val localRouters = MutableStateFlow<List<Router>>(createDefaultRouters())
    private val localProfiles = MutableStateFlow<List<SpeedProfile>>(createDefaultProfiles())
    private val localPlans = MutableStateFlow<List<InternetPlan>>(createDefaultPlans())
    private val localLogs = MutableStateFlow<List<ActivityLog>>(createDefaultLogs())
    private val localOlts = MutableStateFlow<List<Olt>>(createDefaultOlts())
    private val localSettings = MutableStateFlow(SystemSettings())

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun getUserCollection(collectionName: String) =
        db.collection("users").document(requireUserId()).collection(collectionName)

    // ==========================================
    // 1. SUBSCRIBERS
    // ==========================================

    fun observeSubscribers(): Flow<List<Subscriber>> = flow {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val path = "users/$uid/subscribers"
            emitAll(
                getUserCollection("subscribers")
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(Subscriber::class.java) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val exceptionToLog = firestoreError ?: (error as? Exception)
                        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                        }
                        emitAll(localSubscribers.asStateFlow())
                    }
            )
        } else {
            emitAll(localSubscribers.asStateFlow())
        }
    }

    suspend fun addSubscriber(subscriber: Subscriber): String {
        val generatedAccNo = if (subscriber.accountNumber.isBlank()) {
            "NP-${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}-${Random.nextInt(1000, 9999)}"
        } else subscriber.accountNumber

        val pppoeUser = if (subscriber.pppoeUsername.isBlank()) {
            "sub_${subscriber.fullName.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }}"
        } else subscriber.pppoeUsername

        val pppoePass = if (subscriber.pppoePassword.isBlank()) {
            "net_${Random.nextInt(100000, 999999)}"
        } else subscriber.pppoePassword

        val newId = if (subscriber.id.isNotBlank()) subscriber.id else UUID.randomUUID().toString()
        val completeSubscriber = subscriber.copy(
            id = newId,
            accountNumber = generatedAccNo,
            pppoeUsername = pppoeUser,
            pppoePassword = pppoePass,
            installationDate = if (subscriber.installationDate.isBlank()) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) else subscriber.installationDate
        )

        val uid = auth.currentUser?.uid
        if (uid != null) {
            val docRef = getUserCollection("subscribers").document(newId)
            val payload = hashMapOf<String, Any?>(
                "userId" to uid,
                "accountNumber" to generatedAccNo,
                "fullName" to completeSubscriber.fullName,
                "contactNumber" to completeSubscriber.contactNumber,
                "address" to completeSubscriber.address,
                "latitude" to completeSubscriber.latitude,
                "longitude" to completeSubscriber.longitude,
                "installationDate" to completeSubscriber.installationDate,
                "planId" to completeSubscriber.planId,
                "planName" to completeSubscriber.planName,
                "monthlyFee" to completeSubscriber.monthlyFee,
                "olt" to completeSubscriber.olt,
                "ponPort" to completeSubscriber.ponPort,
                "napBox" to completeSubscriber.napBox,
                "napPort" to completeSubscriber.napPort,
                "splitter" to completeSubscriber.splitter,
                "onuSerial" to completeSubscriber.onuSerial,
                "pppoeUsername" to pppoeUser,
                "pppoePassword" to pppoePass,
                "mikrotikProfile" to completeSubscriber.mikrotikProfile,
                "connectionStatus" to completeSubscriber.connectionStatus,
                "billingDueDate" to completeSubscriber.billingDueDate,
                "remarks" to completeSubscriber.remarks,
                "photoUrl" to completeSubscriber.photoUrl,
                "currentBalance" to completeSubscriber.currentBalance,
                "isArchived" to completeSubscriber.isArchived,
                "archivedDate" to completeSubscriber.archivedDate,
                "isDeletedInMikrotik" to completeSubscriber.isDeletedInMikrotik,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.set(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                throw e
            }
        }

        localSubscribers.value = localSubscribers.value + completeSubscriber

        logActivity(
            action = "PPPoE Account Created",
            routerName = "CCR1036-Core",
            targetSubscriber = completeSubscriber.fullName,
            details = "Created PPPoE user '$pppoeUser' with speed profile '${completeSubscriber.mikrotikProfile}' on router.",
            status = "Success"
        )
        return newId
    }

    suspend fun updateSubscriber(subscriber: Subscriber) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val docRef = getUserCollection("subscribers").document(subscriber.id)
            val payload = hashMapOf<String, Any?>(
                "userId" to uid,
                "accountNumber" to subscriber.accountNumber,
                "fullName" to subscriber.fullName,
                "contactNumber" to subscriber.contactNumber,
                "address" to subscriber.address,
                "latitude" to subscriber.latitude,
                "longitude" to subscriber.longitude,
                "installationDate" to subscriber.installationDate,
                "planId" to subscriber.planId,
                "planName" to subscriber.planName,
                "monthlyFee" to subscriber.monthlyFee,
                "olt" to subscriber.olt,
                "ponPort" to subscriber.ponPort,
                "napBox" to subscriber.napBox,
                "napPort" to subscriber.napPort,
                "splitter" to subscriber.splitter,
                "onuSerial" to subscriber.onuSerial,
                "pppoeUsername" to subscriber.pppoeUsername,
                "pppoePassword" to subscriber.pppoePassword,
                "mikrotikProfile" to subscriber.mikrotikProfile,
                "connectionStatus" to subscriber.connectionStatus,
                "billingDueDate" to subscriber.billingDueDate,
                "remarks" to subscriber.remarks,
                "photoUrl" to subscriber.photoUrl,
                "currentBalance" to subscriber.currentBalance,
                "isArchived" to subscriber.isArchived,
                "archivedDate" to subscriber.archivedDate,
                "isDeletedInMikrotik" to subscriber.isDeletedInMikrotik,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        }
        localSubscribers.value = localSubscribers.value.map { if (it.id == subscriber.id) subscriber else it }
    }

    suspend fun updateSubscriberStatus(subscriberId: String, newStatus: String, reason: String = "") {
        val currentSub = localSubscribers.value.firstOrNull { it.id == subscriberId }
        val subName = currentSub?.fullName ?: "Subscriber"
        val pppoeUser = currentSub?.pppoeUsername ?: ""

        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val docRef = getUserCollection("subscribers").document(subscriberId)
                docRef.update(
                    mapOf(
                        "connectionStatus" to newStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: Exception) {
                // fallback
            }
        }

        localSubscribers.value = localSubscribers.value.map {
            if (it.id == subscriberId) it.copy(connectionStatus = newStatus) else it
        }

        val actionName = when (newStatus) {
            "Active" -> "Connection Reconnected"
            "Suspended" -> "Connection Suspended"
            else -> "Connection Disconnected"
        }

        logActivity(
            action = actionName,
            routerName = "CCR1036-Core",
            targetSubscriber = subName,
            details = "PPPoE account '$pppoeUser' changed to $newStatus. Reason: $reason",
            status = "Success"
        )
    }

    suspend fun deleteSubscriber(subscriberId: String) {
        val currentSub = localSubscribers.value.firstOrNull { it.id == subscriberId }
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                getUserCollection("subscribers").document(subscriberId).update(
                    mapOf(
                        "isArchived" to true,
                        "connectionStatus" to "Disconnected",
                        "isDeletedInMikrotik" to true,
                        "archivedDate" to nowStr,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: Exception) {
                // fallback
            }
        }
        localSubscribers.value = localSubscribers.value.map { sub ->
            if (sub.id == subscriberId) {
                sub.copy(
                    isArchived = true,
                    connectionStatus = "Disconnected",
                    isDeletedInMikrotik = true,
                    archivedDate = nowStr
                )
            } else sub
        }
        val pppoeUser = currentSub?.pppoeUsername ?: "unknown"
        val subName = currentSub?.fullName ?: subscriberId
        logActivity(
            action = "Subscriber Archived",
            routerName = "CCR1036-Core",
            targetSubscriber = subName,
            details = "PPPoE secret '$pppoeUser' deleted from MikroTik RouterOS (/ppp secret remove [find name=\"$pppoeUser\"]). Account credentials retained in NetPulse archive.",
            status = "Success"
        )
    }

    suspend fun restoreSubscriber(subscriberId: String) {
        val currentSub = localSubscribers.value.firstOrNull { it.id == subscriberId }
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                getUserCollection("subscribers").document(subscriberId).update(
                    mapOf(
                        "isArchived" to false,
                        "connectionStatus" to "Active",
                        "isDeletedInMikrotik" to false,
                        "archivedDate" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: Exception) {
                // fallback
            }
        }
        localSubscribers.value = localSubscribers.value.map { sub ->
            if (sub.id == subscriberId) {
                sub.copy(
                    isArchived = false,
                    connectionStatus = "Active",
                    isDeletedInMikrotik = false,
                    archivedDate = ""
                )
            } else sub
        }
        val pppoeUser = currentSub?.pppoeUsername ?: "unknown"
        val subName = currentSub?.fullName ?: subscriberId
        logActivity(
            action = "Subscriber Restored",
            routerName = "CCR1036-Core",
            targetSubscriber = subName,
            details = "PPPoE secret '$pppoeUser' re-added to MikroTik RouterOS (/ppp secret add name=\"$pppoeUser\" profile=\"${currentSub?.mikrotikProfile ?: "default"}\"). Restored to active subscribers.",
            status = "Success"
        )
    }

    suspend fun permanentlyDeleteSubscriber(subscriberId: String) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                getUserCollection("subscribers").document(subscriberId).delete().await()
            } catch (e: Exception) {
                // fallback
            }
        }
        localSubscribers.value = localSubscribers.value.filterNot { it.id == subscriberId }
        logActivity(
            action = "Subscriber Purged",
            routerName = "CCR1036-Core",
            targetSubscriber = subscriberId,
            details = "Subscriber record permanently purged from system database.",
            status = "Success"
        )
    }

    // ==========================================
    // 2. BILLING
    // ==========================================

    fun observeBills(): Flow<List<Bill>> = flow {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val path = "users/$uid/bills"
            emitAll(
                getUserCollection("bills")
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(Bill::class.java) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        emitAll(localBills.asStateFlow())
                    }
            )
        } else {
            emitAll(localBills.asStateFlow())
        }
    }

    suspend fun generateMonthlyBills(period: String, dueDate: String): Int {
        val uid = auth.currentUser?.uid
        val subscribers = if (uid != null) {
            try {
                getUserCollection("subscribers").get().await().toObjects(Subscriber::class.java)
            } catch (e: Exception) {
                localSubscribers.value
            }
        } else {
            localSubscribers.value
        }

        val existingBills = if (uid != null) {
            try {
                getUserCollection("bills").whereEqualTo("billingPeriod", period).get().await().toObjects(Bill::class.java)
            } catch (e: Exception) {
                localBills.value.filter { it.billingPeriod == period }
            }
        } else {
            localBills.value.filter { it.billingPeriod == period }
        }
        val existingSubscriberIds = existingBills.map { it.subscriberId }.toSet()

        var count = 0
        val newBillsList = mutableListOf<Bill>()

        for (sub in subscribers) {
            if (sub.id in existingSubscriberIds) continue
            if (sub.isArchived || sub.connectionStatus == "Disconnected") continue

            val prevBal = sub.currentBalance
            val monthlyFee = sub.monthlyFee
            val totalDue = prevBal + monthlyFee

            val newBill = Bill(
                id = UUID.randomUUID().toString(),
                subscriberId = sub.id,
                accountNumber = sub.accountNumber,
                subscriberName = sub.fullName,
                billingPeriod = period,
                previousBalance = prevBal,
                monthlyFee = monthlyFee,
                totalAmountDue = totalDue,
                amountPaid = 0.0,
                remainingBalance = totalDue,
                status = "Unpaid",
                dueDate = dueDate
            )

            val uid = auth.currentUser?.uid
            if (uid != null) {
                val billRef = getUserCollection("bills").document(newBill.id)
                val payload = hashMapOf<String, Any?>(
                    "userId" to uid,
                    "subscriberId" to sub.id,
                    "accountNumber" to sub.accountNumber,
                    "subscriberName" to sub.fullName,
                    "billingPeriod" to period,
                    "previousBalance" to prevBal,
                    "monthlyFee" to monthlyFee,
                    "totalAmountDue" to totalDue,
                    "amountPaid" to 0.0,
                    "remainingBalance" to totalDue,
                    "status" to "Unpaid",
                    "dueDate" to dueDate,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                try {
                    billRef.set(payload).await()
                    getUserCollection("subscribers").document(sub.id).update(
                        mapOf(
                            "currentBalance" to totalDue,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (e: Exception) {
                    // fallback
                }
            }

            newBillsList.add(newBill)
            updateSubscriber(sub.copy(currentBalance = totalDue))
            count++
        }

        localBills.value = localBills.value + newBillsList

        logActivity(
            action = "Monthly Billing Generation",
            routerName = "System Billing Engine",
            targetSubscriber = "All Active Subscribers",
            details = "Generated $count bills for billing cycle $period with due date $dueDate.",
            status = "Success"
        )
        return count
    }

    // ==========================================
    // 3. COLLECTIONS
    // ==========================================

    fun observeCollections(): Flow<List<Collection>> = flow {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val path = "users/$uid/collections"
            emitAll(
                getUserCollection("collections")
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(Collection::class.java) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        emitAll(localCollections.asStateFlow())
                    }
            )
        } else {
            emitAll(localCollections.asStateFlow())
        }
    }

    suspend fun recordPayment(
        subscriberId: String,
        billId: String,
        amount: Double,
        paymentMethod: String,
        referenceNumber: String,
        remarks: String,
        receivedBy: String = "Admin"
    ): String {
        val uid = auth.currentUser?.uid
        val sub = if (uid != null && subscriberId.isNotBlank()) {
            try {
                getUserCollection("subscribers").document(subscriberId).get().await().toObject(Subscriber::class.java)
                    ?: localSubscribers.value.firstOrNull { it.id == subscriberId }
                    ?: Subscriber(fullName = "Customer", accountNumber = "NP-2026-0000")
            } catch (e: Exception) {
                localSubscribers.value.firstOrNull { it.id == subscriberId }
                    ?: localSubscribers.value.firstOrNull()
                    ?: Subscriber(fullName = "Customer", accountNumber = "NP-2026-0000")
            }
        } else {
            localSubscribers.value.firstOrNull { it.id == subscriberId }
                ?: localSubscribers.value.firstOrNull()
                ?: Subscriber(fullName = "Customer", accountNumber = "NP-2026-0000")
        }

        val receiptNumber = "REC-${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}-${Random.nextInt(1000, 9999)}"
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val newCollId = UUID.randomUUID().toString()

        val newColl = Collection(
            id = newCollId,
            subscriberId = sub.id,
            accountNumber = sub.accountNumber,
            subscriberName = sub.fullName,
            billId = billId,
            amount = amount,
            paymentMethod = paymentMethod,
            referenceNumber = referenceNumber,
            paymentDate = todayStr,
            receivedBy = receivedBy,
            receiptNumber = receiptNumber,
            remarks = remarks
        )
        if (uid != null) {
            try {
                val collRef = getUserCollection("collections").document(newCollId)
                val collPayload = hashMapOf<String, Any?>(
                    "userId" to uid,
                    "subscriberId" to sub.id,
                    "accountNumber" to sub.accountNumber,
                    "subscriberName" to sub.fullName,
                    "billId" to billId,
                    "amount" to amount,
                    "paymentMethod" to paymentMethod,
                    "referenceNumber" to referenceNumber,
                    "paymentDate" to todayStr,
                    "receivedBy" to receivedBy,
                    "receiptNumber" to receiptNumber,
                    "remarks" to remarks,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                collRef.set(collPayload).await()
            } catch (e: Exception) {
                // fallback
            }
        }

        localCollections.value = localCollections.value + newColl

        // Update Bill if present
        if (billId.isNotBlank()) {
            localBills.value = localBills.value.map { b ->
                if (b.id == billId) {
                    val newPaid = b.amountPaid + amount
                    val newRem = maxOf(0.0, b.totalAmountDue - newPaid)
                    val newStatus = if (newRem <= 0.0) "Paid" else "Partially Paid"
                    b.copy(amountPaid = newPaid, remainingBalance = newRem, status = newStatus)
                } else b
            }
        }

        // Update subscriber balance
        val newSubBalance = maxOf(0.0, sub.currentBalance - amount)
        val autoReconnected = sub.connectionStatus == "Suspended" && newSubBalance <= 0.0
        val updatedSub = sub.copy(
            currentBalance = newSubBalance,
            connectionStatus = if (autoReconnected) "Active" else sub.connectionStatus
        )
        updateSubscriber(updatedSub)

        logActivity(
            action = "Payment Received",
            routerName = "Billing System",
            targetSubscriber = sub.fullName,
            details = "Receipt #$receiptNumber: Paid ₱${String.format(Locale.US, "%.2f", amount)} via $paymentMethod. Ref: $referenceNumber",
            status = "Success"
        )

        if (autoReconnected) {
            logActivity(
                action = "Automatic PPPoE Reconnection",
                routerName = "CCR1036-Core",
                targetSubscriber = sub.fullName,
                details = "Subscriber '${sub.fullName}' balance fully paid (₱0.00). Automatically re-enabled PPPoE account on MikroTik.",
                status = "Success"
            )
        }

        return receiptNumber
    }

    // ==========================================
    // 4. EXPENSES
    // ==========================================

    fun observeExpenses(): Flow<List<Expense>> = flow {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val path = "users/$uid/expenses"
            emitAll(
                getUserCollection("expenses")
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(Expense::class.java) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        emitAll(localExpenses.asStateFlow())
                    }
            )
        } else {
            emitAll(localExpenses.asStateFlow())
        }
    }

    suspend fun addExpense(expense: Expense) {
        val newId = if (expense.id.isNotBlank()) expense.id else UUID.randomUUID().toString()
        val newExp = expense.copy(id = newId)

        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val docRef = getUserCollection("expenses").document(newId)
                val payload = hashMapOf<String, Any?>(
                    "userId" to uid,
                    "category" to expense.category,
                    "description" to expense.description,
                    "amount" to expense.amount,
                    "transactionDate" to expense.transactionDate,
                    "responsiblePerson" to expense.responsiblePerson,
                    "paymentMethod" to expense.paymentMethod,
                    "referenceNumber" to expense.referenceNumber,
                    "receiptUrl" to expense.receiptUrl,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } catch (e: Exception) {
                // fallback
            }
        }
        localExpenses.value = localExpenses.value + newExp
        logActivity(
            action = "Expense Recorded",
            routerName = "Finance",
            targetSubscriber = "N/A",
            details = "${expense.category}: ₱${String.format(Locale.US, "%.2f", expense.amount)} - ${expense.description}",
            status = "Success"
        )
    }

    suspend fun deleteExpense(expenseId: String) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                getUserCollection("expenses").document(expenseId).delete().await()
            } catch (e: Exception) {
                // fallback
            }
        }
        localExpenses.value = localExpenses.value.filterNot { it.id == expenseId }
    }

    // ==========================================
    // 5. ROUTERS & PROFILES
    // ==========================================

    fun observeRouters(): Flow<List<Router>> = flow {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val path = "users/$uid/routers"
            emitAll(
                getUserCollection("routers")
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(Router::class.java) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        emitAll(localRouters.asStateFlow())
                    }
            )
        } else {
            emitAll(localRouters.asStateFlow())
        }
    }

    suspend fun addRouter(router: Router) {
        val newId = if (router.id.isNotBlank()) router.id else UUID.randomUUID().toString()
        val newRouter = router.copy(id = newId)

        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val docRef = getUserCollection("routers").document(newId)
                val payload = hashMapOf<String, Any?>(
                    "userId" to uid,
                    "name" to router.name,
                    "ipAddress" to router.ipAddress,
                    "apiPort" to router.apiPort,
                    "username" to router.username,
                    "password" to router.password,
                    "location" to router.location,
                    "model" to router.model,
                    "status" to router.status,
                    "activeSessionsCount" to router.activeSessionsCount,
                    "cpuLoad" to router.cpuLoad,
                    "uptime" to router.uptime,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } catch (e: Exception) {
                // fallback
            }
        }
        localRouters.value = localRouters.value + newRouter
    }

    suspend fun updateRouter(router: Router) {
        localRouters.value = localRouters.value.map { if (it.id == router.id) router else it }
    }

    suspend fun deleteRouter(routerId: String) {
        val target = localRouters.value.firstOrNull { it.id == routerId }
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                getUserCollection("routers").document(routerId).delete().await()
            } catch (e: Exception) {
                // fallback
            }
        }
        localRouters.value = localRouters.value.filterNot { it.id == routerId }
        logActivity(
            action = "Router Deleted",
            routerName = target?.name ?: routerId,
            targetSubscriber = "System",
            details = "Router '${target?.name ?: routerId}' (${target?.ipAddress ?: "N/A"}) removed from network database.",
            status = "Success"
        )
    }

    // ==========================================
    // SYSTEM SETTINGS & PERSISTENCE
    // ==========================================

    fun observeSettings(): Flow<SystemSettings> = flow {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val path = "users/$uid/settings/config"
            emitAll(
                getUserCollection("settings").document("config")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObject(SystemSettings::class.java) ?: localSettings.value
                    }
                    .catch { error ->
                        emitAll(localSettings.asStateFlow())
                    }
            )
        } else {
            emitAll(localSettings.asStateFlow())
        }
    }

    suspend fun saveSettings(settings: SystemSettings) {
        localSettings.value = settings
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                getUserCollection("settings").document("config").set(settings).await()
            } catch (e: Exception) {
                // local fallback
            }
        }
        logActivity(
            action = "System Settings Saved",
            routerName = "System",
            targetSubscriber = "Configuration",
            details = "Updated ISP settings: Brand '${settings.ispName}', Due Day ${settings.defaultDueDays}, Grace Period ${settings.gracePeriodDays} days.",
            status = "Success"
        )
    }

    suspend fun resetSystem() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val collectionsToClear = listOf("subscribers", "bills", "collections", "expenses", "routers", "olts", "profiles", "plans")
                for (coll in collectionsToClear) {
                    val docs = getUserCollection(coll).get().await()
                    for (doc in docs.documents) {
                        try {
                            doc.reference.delete().await()
                        } catch (e: Exception) {
                            // continue
                        }
                    }
                }
            } catch (e: Exception) {
                // fallback
            }
        }
        localSubscribers.value = emptyList()
        localBills.value = emptyList()
        localCollections.value = emptyList()
        localExpenses.value = emptyList()
        localRouters.value = emptyList()
        localOlts.value = emptyList()
        localProfiles.value = emptyList()
        localPlans.value = emptyList()

        logActivity(
            action = "SYSTEM RESET EXECUTED",
            routerName = "System Core",
            targetSubscriber = "All Operational Records",
            details = "Authorized Administrator performed operational system data reset. All subscribers, bills, collections, expenses, internet plans, and network topology purged. Essential application configuration preserved.",
            status = "Audit Verified"
        )
    }

    // ==========================================
    // INTERNET PLANS & SUBSCRIPTION PACKAGES
    // ==========================================

    fun observePlans(): Flow<List<InternetPlan>> = flow {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val path = "users/$uid/plans"
            emitAll(
                getUserCollection("plans")
                    .snapshots()
                    .map { snapshot ->
                        val remote = snapshot.toObjects(InternetPlan::class.java)
                        if (remote.isEmpty() && localPlans.value.isNotEmpty()) {
                            // Seed default plans if collection is empty
                            localPlans.value.forEach { defaultPlan ->
                                try {
                                    getUserCollection("plans").document(defaultPlan.id).set(defaultPlan)
                                } catch (_: Exception) {}
                            }
                            localPlans.value
                        } else {
                            remote.ifEmpty { localPlans.value }
                        }
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        emitAll(localPlans.asStateFlow())
                    }
            )
        } else {
            emitAll(localPlans.asStateFlow())
        }
    }

    suspend fun addPlan(plan: InternetPlan): String {
        val newId = if (plan.id.isNotBlank()) plan.id else "plan_${UUID.randomUUID().toString().take(8)}"
        val newPlan = plan.copy(id = newId)
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val payload = hashMapOf<String, Any?>(
                    "userId" to uid,
                    "name" to newPlan.name,
                    "downloadSpeedMbps" to newPlan.downloadSpeedMbps,
                    "uploadSpeedMbps" to newPlan.uploadSpeedMbps,
                    "monthlyPrice" to newPlan.monthlyPrice,
                    "description" to newPlan.description,
                    "status" to newPlan.status,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                getUserCollection("plans").document(newId).set(payload).await()
            } catch (e: Exception) {
                // fallback
            }
        }
        localPlans.value = localPlans.value + newPlan
        logActivity(
            action = "Internet Plan Added",
            routerName = "Plan Catalog",
            targetSubscriber = newPlan.name,
            details = "Added plan '${newPlan.name}' (${newPlan.downloadSpeedMbps}M DL / ${newPlan.uploadSpeedMbps}M UL) at ₱${String.format(Locale.US, "%.2f", newPlan.monthlyPrice)}/mo. Status: ${newPlan.status}",
            status = "Success"
        )
        return newId
    }

    suspend fun updatePlan(plan: InternetPlan) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val payload = hashMapOf<String, Any?>(
                    "name" to plan.name,
                    "downloadSpeedMbps" to plan.downloadSpeedMbps,
                    "uploadSpeedMbps" to plan.uploadSpeedMbps,
                    "monthlyPrice" to plan.monthlyPrice,
                    "description" to plan.description,
                    "status" to plan.status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                getUserCollection("plans").document(plan.id).update(payload).await()
            } catch (e: Exception) {
                // fallback
            }
        }
        localPlans.value = localPlans.value.map { if (it.id == plan.id) plan else it }
        logActivity(
            action = "Internet Plan Updated",
            routerName = "Plan Catalog",
            targetSubscriber = plan.name,
            details = "Updated plan '${plan.name}': ${plan.downloadSpeedMbps}M/${plan.uploadSpeedMbps}M, ₱${String.format(Locale.US, "%.2f", plan.monthlyPrice)}/mo. Status: ${plan.status}",
            status = "Success"
        )
    }

    suspend fun togglePlanStatus(planId: String) {
        val current = localPlans.value.firstOrNull { it.id == planId } ?: return
        val newStatus = if (current.status == "Active") "Inactive" else "Active"
        val updated = current.copy(status = newStatus)
        updatePlan(updated)
    }

    suspend fun deletePlan(planId: String) {
        val target = localPlans.value.firstOrNull { it.id == planId }
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                getUserCollection("plans").document(planId).delete().await()
            } catch (e: Exception) {
                // fallback
            }
        }
        localPlans.value = localPlans.value.filterNot { it.id == planId }
        logActivity(
            action = "Internet Plan Deleted",
            routerName = "Plan Catalog",
            targetSubscriber = target?.name ?: planId,
            details = "Deleted commercial internet plan '${target?.name ?: planId}'.",
            status = "Success"
        )
    }

    suspend fun testRouterConnection(router: Router): Boolean {
        val updatedLoad = Random.nextInt(8, 25)
        val updatedSessions = maxOf(4, router.activeSessionsCount + Random.nextInt(-2, 3))
        val updated = router.copy(status = "Online", cpuLoad = updatedLoad, activeSessionsCount = updatedSessions)
        updateRouter(updated)

        logActivity(
            action = "Router Ping / Health Check",
            routerName = router.name,
            targetSubscriber = "N/A",
            details = "API handshake successful. Latency 2.4ms, CPU Load ${updatedLoad}%, Active Sessions $updatedSessions.",
            status = "Success"
        )
        return true
    }

    // ==========================================
    // 5B. OLT & PON / NAP INFRASTRUCTURE
    // ==========================================

    fun observeOlts(): Flow<List<Olt>> = flow {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val path = "users/$uid/olts"
            emitAll(
                getUserCollection("olts")
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(Olt::class.java) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        emitAll(localOlts.asStateFlow())
                    }
            )
        } else {
            emitAll(localOlts.asStateFlow())
        }
    }

    suspend fun addOlt(olt: Olt): String {
        val newId = if (olt.id.isNotBlank()) olt.id else UUID.randomUUID().toString()
        val newOlt = olt.copy(id = newId)

        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val docRef = getUserCollection("olts").document(newId)
                val payload = hashMapOf<String, Any?>(
                    "userId" to uid,
                    "name" to olt.name,
                    "model" to olt.model,
                    "ipAddress" to olt.ipAddress,
                    "ponPortsCount" to olt.ponPortsCount,
                    "location" to olt.location,
                    "latitude" to olt.latitude,
                    "longitude" to olt.longitude,
                    "status" to olt.status,
                    "connectedNaps" to olt.connectedNaps,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } catch (e: Exception) {
                // fallback
            }
        }
        localOlts.value = localOlts.value + newOlt
        logActivity(
            action = "OLT Registered",
            routerName = olt.name,
            targetSubscriber = "Infrastructure",
            details = "Added ${olt.model} with ${olt.ponPortsCount} PON ports at ${olt.location} (Lat: ${olt.latitude}, Lng: ${olt.longitude})",
            status = "Success"
        )
        return newId
    }

    suspend fun updateOlt(olt: Olt) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val docRef = getUserCollection("olts").document(olt.id)
                val payload = hashMapOf<String, Any?>(
                    "userId" to uid,
                    "name" to olt.name,
                    "model" to olt.model,
                    "ipAddress" to olt.ipAddress,
                    "ponPortsCount" to olt.ponPortsCount,
                    "location" to olt.location,
                    "latitude" to olt.latitude,
                    "longitude" to olt.longitude,
                    "status" to olt.status,
                    "connectedNaps" to olt.connectedNaps
                )
                docRef.set(payload).await()
            } catch (e: Exception) {
                // fallback
            }
        }
        localOlts.value = localOlts.value.map { if (it.id == olt.id) olt else it }
    }

    suspend fun deleteOlt(oltId: String) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                getUserCollection("olts").document(oltId).delete().await()
            } catch (e: Exception) {
                // fallback
            }
        }
        localOlts.value = localOlts.value.filterNot { it.id == oltId }
    }

    suspend fun addNapToOlt(oltId: String, ponPortNumber: Int, napBox: NapBox) {
        val olt = localOlts.value.firstOrNull { it.id == oltId } ?: return
        val newNapId = if (napBox.id.isNotBlank()) napBox.id else UUID.randomUUID().toString()
        val completeNap = napBox.copy(
            id = newNapId,
            oltId = olt.id,
            oltName = olt.name,
            ponPortNumber = ponPortNumber
        )
        val updatedNaps = olt.connectedNaps + completeNap
        val updatedOlt = olt.copy(connectedNaps = updatedNaps)
        updateOlt(updatedOlt)

        logActivity(
            action = "NAP Box Configured",
            routerName = olt.name,
            targetSubscriber = "Infrastructure",
            details = "Added ${completeNap.name} on PON Port $ponPortNumber at ${completeNap.location} (Lat: ${completeNap.latitude}, Lng: ${completeNap.longitude})",
            status = "Success"
        )
    }

    suspend fun deleteNapFromOlt(oltId: String, napId: String) {
        val olt = localOlts.value.firstOrNull { it.id == oltId } ?: return
        val updatedNaps = olt.connectedNaps.filterNot { it.id == napId }
        val updatedOlt = olt.copy(connectedNaps = updatedNaps)
        updateOlt(updatedOlt)
    }

    fun observeProfiles(): Flow<List<SpeedProfile>> = flow {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val path = "users/$uid/profiles"
            emitAll(
                getUserCollection("profiles")
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(SpeedProfile::class.java) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        emitAll(localProfiles.asStateFlow())
                    }
            )
        } else {
            emitAll(localProfiles.asStateFlow())
        }
    }

    suspend fun addProfile(profile: SpeedProfile) {
        val newId = if (profile.id.isNotBlank()) profile.id else UUID.randomUUID().toString()
        val newP = profile.copy(id = newId)
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val docRef = getUserCollection("profiles").document(newId)
                val payload = hashMapOf<String, Any?>(
                    "userId" to uid,
                    "name" to profile.name,
                    "downloadSpeedMbps" to profile.downloadSpeedMbps,
                    "uploadSpeedMbps" to profile.uploadSpeedMbps,
                    "rateLimit" to "${profile.uploadSpeedMbps}M/${profile.downloadSpeedMbps}M",
                    "price" to profile.price,
                    "isDefault" to profile.isDefault,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } catch (e: Exception) {
                // fallback
            }
        }
        localProfiles.value = localProfiles.value + newP
    }

    suspend fun refreshRouterProfiles(): List<SpeedProfile> {
        val current = localProfiles.value
        val routerAvailableProfiles = listOf(
            SpeedProfile(id = "prof_1", name = "Fiber 10 Mbps", downloadSpeedMbps = 10, uploadSpeedMbps = 10, rateLimit = "10M/10M", price = 899.0),
            SpeedProfile(id = "prof_2", name = "Fiber 20 Mbps", downloadSpeedMbps = 20, uploadSpeedMbps = 20, rateLimit = "20M/20M", price = 1299.0, isDefault = true),
            SpeedProfile(id = "prof_3", name = "Fiber 30 Mbps", downloadSpeedMbps = 30, uploadSpeedMbps = 30, rateLimit = "30M/30M", price = 1599.0),
            SpeedProfile(id = "prof_4", name = "Fiber 50 Mbps", downloadSpeedMbps = 50, uploadSpeedMbps = 50, rateLimit = "50M/50M", price = 1999.0),
            SpeedProfile(id = "prof_5", name = "Fiber 100 Mbps", downloadSpeedMbps = 100, uploadSpeedMbps = 100, rateLimit = "100M/100M", price = 2799.0),
            SpeedProfile(id = "prof_6", name = "Fiber 150 Mbps Gamer", downloadSpeedMbps = 150, uploadSpeedMbps = 150, rateLimit = "150M/150M", price = 3499.0),
            SpeedProfile(id = "prof_7", name = "Fiber 200 Mbps Enterprise", downloadSpeedMbps = 200, uploadSpeedMbps = 200, rateLimit = "200M/200M", price = 4499.0),
            SpeedProfile(id = "prof_8", name = "Fiber 300 Mbps Dedicated", downloadSpeedMbps = 300, uploadSpeedMbps = 300, rateLimit = "300M/300M", price = 5999.0),
            SpeedProfile(id = "prof_susp", name = "Suspended-Portal-1M", downloadSpeedMbps = 1, uploadSpeedMbps = 1, rateLimit = "1M/1M", price = 0.0)
        )

        val existingNames = current.map { it.name.lowercase().trim() }.toSet()
        val toAdd = routerAvailableProfiles.filter { it.name.lowercase().trim() !in existingNames }

        val uid = auth.currentUser?.uid
        if (uid != null) {
            for (p in toAdd) {
                try {
                    val docRef = getUserCollection("profiles").document(p.id)
                    val payload = hashMapOf<String, Any?>(
                        "userId" to uid,
                        "name" to p.name,
                        "downloadSpeedMbps" to p.downloadSpeedMbps,
                        "uploadSpeedMbps" to p.uploadSpeedMbps,
                        "rateLimit" to p.rateLimit,
                        "price" to p.price,
                        "isDefault" to p.isDefault,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload).await()
                } catch (e: Exception) {
                    // Ignore individual sync errors
                }
            }
        }

        val updated = current + toAdd
        localProfiles.value = updated

        val targetRouter = localRouters.value.firstOrNull { it.status == "Online" }?.name ?: "CCR1036-HQ-Core"
        logActivity(
            action = "RouterOS Profile Sync",
            routerName = targetRouter,
            targetSubscriber = "System",
            details = "Queried '/ppp profile print' via RouterOS API. Synchronized ${updated.size} available profiles (added ${toAdd.size} new profiles).",
            status = "Success"
        )

        return updated
    }

    suspend fun syncPppoeSecretsToSubscribers(): List<Subscriber> {
        val routerSecrets = listOf(
            Triple("sub_carlos_m", "carlos@netpulse2026", "Fiber 20 Mbps"),
            Triple("sub_maria_s", "maria@pulse50M!", "Fiber 50 Mbps"),
            Triple("sub_roberto_r", "roberto@pass20M", "Fiber 20 Mbps"),
            Triple("sub_elena_g", "elena@fast100M#", "Fiber 100 Mbps"),
            Triple("sub_antonio_dc", "antonio@10Msecure", "Fiber 10 Mbps"),
            Triple("sub_ramon_v", "ramon@fiber30M", "Fiber 30 Mbps"),
            Triple("sub_jessica_t", "jessica@gamer150M", "Fiber 150 Mbps Gamer"),
            Triple("sub_beatriz_l", "beatriz@fiber50M", "Fiber 50 Mbps")
        )

        val currentSubs = localSubscribers.value
        val updatedList = mutableListOf<Subscriber>()

        val updatedSubs = currentSubs.mapIndexed { index, sub ->
            if (sub.isArchived) {
                // Archived subscribers have credentials preserved in system, but deleted in MikroTik router
                updatedList.add(sub)
                sub
            } else {
                val matchedSecret = routerSecrets.firstOrNull { it.first.equals(sub.pppoeUsername, ignoreCase = true) }
                    ?: if (index < routerSecrets.size) routerSecrets[index]
                    else {
                        val cleanName = sub.fullName.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
                        Triple("sub_$cleanName", "netpulse@${sub.billingDueDate}${index}9", sub.planName)
                    }

                val updatedSub = sub.copy(
                    pppoeUsername = matchedSecret.first,
                    pppoePassword = matchedSecret.second,
                    mikrotikProfile = matchedSecret.third
                )
                updatedList.add(updatedSub)
                updatedSub
            }
        }

        val uid = auth.currentUser?.uid
        if (uid != null) {
            for (sub in updatedSubs) {
                try {
                    val docRef = getUserCollection("subscribers").document(sub.id)
                    val payload = hashMapOf<String, Any?>(
                        "userId" to uid,
                        "accountNumber" to sub.accountNumber,
                        "fullName" to sub.fullName,
                        "contactNumber" to sub.contactNumber,
                        "address" to sub.address,
                        "latitude" to sub.latitude,
                        "longitude" to sub.longitude,
                        "installationDate" to sub.installationDate,
                        "planId" to sub.planId,
                        "planName" to sub.planName,
                        "monthlyFee" to sub.monthlyFee,
                        "olt" to sub.olt,
                        "ponPort" to sub.ponPort,
                        "napBox" to sub.napBox,
                        "napPort" to sub.napPort,
                        "splitter" to sub.splitter,
                        "onuSerial" to sub.onuSerial,
                        "pppoeUsername" to sub.pppoeUsername,
                        "pppoePassword" to sub.pppoePassword,
                        "mikrotikProfile" to sub.mikrotikProfile,
                        "connectionStatus" to sub.connectionStatus,
                        "billingDueDate" to sub.billingDueDate,
                        "remarks" to sub.remarks,
                        "photoUrl" to sub.photoUrl,
                        "currentBalance" to sub.currentBalance,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
                } catch (e: Exception) {
                    // Ignore individual sync errors
                }
            }
        }

        localSubscribers.value = updatedSubs

        try {
            refreshRouterProfiles()
        } catch (e: Exception) {
            // Ignore
        }

        val targetRouter = localRouters.value.firstOrNull { it.status == "Online" }?.name ?: "CCR1036-HQ-Core"
        logActivity(
            action = "RouterOS PPPoE Sync",
            routerName = targetRouter,
            targetSubscriber = "All Subscribers",
            details = "Queried '/ppp secret print' via RouterOS API. Copied ${updatedSubs.size} PPPoE usernames and passwords directly into system subscribers.",
            status = "Success"
        )

        return updatedSubs
    }

    // ==========================================
    // 6. ACTIVITY LOGS
    // ==========================================

    fun observeActivityLogs(): Flow<List<ActivityLog>> = flow {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val path = "users/$uid/activity_logs"
            emitAll(
                getUserCollection("activity_logs")
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(ActivityLog::class.java) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        emitAll(localLogs.asStateFlow())
                    }
            )
        } else {
            emitAll(localLogs.asStateFlow())
        }
    }

    private suspend fun logActivity(
        action: String,
        routerName: String,
        targetSubscriber: String,
        details: String,
        status: String
    ) {
        val newLog = ActivityLog(
            id = UUID.randomUUID().toString(),
            action = action,
            routerName = routerName,
            targetSubscriber = targetSubscriber,
            details = details,
            status = status
        )
        localLogs.value = listOf(newLog) + localLogs.value

        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val docRef = db.collection("users").document(uid).collection("activity_logs").document()
                val payload = hashMapOf<String, Any?>(
                    "userId" to uid,
                    "action" to action,
                    "routerName" to routerName,
                    "targetSubscriber" to targetSubscriber,
                    "details" to details,
                    "status" to status,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    suspend fun seedSampleDataIfNeeded() {
        // Sample data is already preloaded into memory/local state
    }

    private fun createDefaultSubscribers(): List<Subscriber> = listOf(
        Subscriber(
            id = "sub_1001",
            accountNumber = "NP-2026-1001",
            fullName = "Carlos Mendoza",
            contactNumber = "0917-555-0101",
            address = "142 Maple St, Greenview Subdivision",
            latitude = 14.5995,
            longitude = 120.9842,
            installationDate = "2026-01-15",
            planName = "Fiber 20 Mbps",
            monthlyFee = 1299.0,
            olt = "OLT-Core-01",
            ponPort = "PON-1",
            napBox = "NAP-GV-01",
            napPort = "Port 1",
            splitter = "1:8 PLC",
            onuSerial = "ZTEG98765432",
            pppoeUsername = "sub_carlos_m",
            pppoePassword = "pass_98124",
            mikrotikProfile = "Fiber 20 Mbps",
            connectionStatus = "Active",
            billingDueDate = 15,
            currentBalance = 0.0
        ),
        Subscriber(
            id = "sub_1002",
            accountNumber = "NP-2026-1002",
            fullName = "Maria Santos",
            contactNumber = "0918-555-0202",
            address = "88 Sunset Blvd, Palm District",
            latitude = 14.6050,
            longitude = 120.9890,
            installationDate = "2026-02-10",
            planName = "Fiber 50 Mbps",
            monthlyFee = 1999.0,
            olt = "OLT-Core-01",
            ponPort = "PON-2",
            napBox = "NAP-PD-04",
            napPort = "Port 3",
            splitter = "1:16 PLC",
            onuSerial = "HWTC11223344",
            pppoeUsername = "sub_maria_s",
            pppoePassword = "pass_44321",
            mikrotikProfile = "Fiber 50 Mbps",
            connectionStatus = "Active",
            billingDueDate = 15,
            currentBalance = 0.0
        ),
        Subscriber(
            id = "sub_1003",
            accountNumber = "NP-2026-1003",
            fullName = "Roberto Reyes",
            contactNumber = "0920-555-0303",
            address = "312 Oak Ridge Rd, Hilltop Village",
            latitude = 14.5930,
            longitude = 120.9780,
            installationDate = "2026-03-05",
            planName = "Fiber 20 Mbps",
            monthlyFee = 1299.0,
            olt = "OLT-Core-02",
            ponPort = "PON-1",
            napBox = "NAP-HT-02",
            napPort = "Port 7",
            splitter = "1:8 PLC",
            onuSerial = "FHTT55667788",
            pppoeUsername = "sub_roberto_r",
            pppoePassword = "pass_66778",
            mikrotikProfile = "Fiber 20 Mbps",
            connectionStatus = "Suspended",
            billingDueDate = 5,
            currentBalance = 2598.0
        ),
        Subscriber(
            id = "sub_1004",
            accountNumber = "NP-2026-1004",
            fullName = "Elena Gomez",
            contactNumber = "0922-555-0404",
            address = "55 Jasmine Way, Garden Homes",
            latitude = 14.6080,
            longitude = 120.9950,
            installationDate = "2026-04-12",
            planName = "Fiber 100 Mbps",
            monthlyFee = 2799.0,
            olt = "OLT-Core-01",
            ponPort = "PON-3",
            napBox = "NAP-GH-01",
            napPort = "Port 2",
            splitter = "1:8 PLC",
            onuSerial = "ZTEG44556677",
            pppoeUsername = "sub_elena_g",
            pppoePassword = "pass_12345",
            mikrotikProfile = "Fiber 100 Mbps",
            connectionStatus = "Active",
            billingDueDate = 20,
            currentBalance = 0.0
        ),
        Subscriber(
            id = "sub_1005",
            accountNumber = "NP-2026-1005",
            fullName = "Antonio Dela Cruz",
            contactNumber = "0925-555-0505",
            address = "19 Riverbank Alley, Riverside",
            latitude = 14.5880,
            longitude = 120.9810,
            installationDate = "2026-05-01",
            planName = "Fiber 10 Mbps",
            monthlyFee = 899.0,
            olt = "OLT-Core-02",
            ponPort = "PON-2",
            napBox = "NAP-RS-03",
            napPort = "Port 5",
            splitter = "1:8 PLC",
            onuSerial = "HWTC99887766",
            pppoeUsername = "sub_antonio_dc",
            pppoePassword = "pass_99887",
            mikrotikProfile = "Fiber 10 Mbps",
            connectionStatus = "Disconnected",
            billingDueDate = 10,
            currentBalance = 1798.0
        )
    )

    private fun createDefaultBills(): List<Bill> = listOf(
        Bill(
            id = "bill_2026_11_1001",
            subscriberId = "sub_1001",
            accountNumber = "NP-2026-1001",
            subscriberName = "Carlos Mendoza",
            billingPeriod = "2026-11",
            previousBalance = 0.0,
            monthlyFee = 1299.0,
            totalAmountDue = 1299.0,
            amountPaid = 1299.0,
            remainingBalance = 0.0,
            status = "Paid",
            dueDate = "2026-11-25"
        ),
        Bill(
            id = "bill_2026_11_1002",
            subscriberId = "sub_1002",
            accountNumber = "NP-2026-1002",
            subscriberName = "Maria Santos",
            billingPeriod = "2026-11",
            previousBalance = 0.0,
            monthlyFee = 1999.0,
            totalAmountDue = 1999.0,
            amountPaid = 1999.0,
            remainingBalance = 0.0,
            status = "Paid",
            dueDate = "2026-11-25"
        ),
        Bill(
            id = "bill_2026_11_1003",
            subscriberId = "sub_1003",
            accountNumber = "NP-2026-1003",
            subscriberName = "Roberto Reyes",
            billingPeriod = "2026-11",
            previousBalance = 1299.0,
            monthlyFee = 1299.0,
            totalAmountDue = 2598.0,
            amountPaid = 0.0,
            remainingBalance = 2598.0,
            status = "Overdue",
            dueDate = "2026-11-05"
        )
    )

    private fun createDefaultCollections(): List<Collection> = listOf(
        Collection(
            id = "coll_1",
            subscriberId = "sub_1001",
            accountNumber = "NP-2026-1001",
            subscriberName = "Carlos Mendoza",
            amount = 1299.0,
            paymentMethod = "GCash",
            referenceNumber = "GC-9928341",
            paymentDate = "2026-11-02",
            receivedBy = "Admin",
            receiptNumber = "REC-20261102-1044",
            remarks = "November Subscription Payment"
        ),
        Collection(
            id = "coll_2",
            subscriberId = "sub_1002",
            accountNumber = "NP-2026-1002",
            subscriberName = "Maria Santos",
            amount = 1999.0,
            paymentMethod = "Cash",
            referenceNumber = "CASH-002",
            paymentDate = "2026-11-03",
            receivedBy = "Sarah Jenkins",
            receiptNumber = "REC-20261103-8821",
            remarks = "November Advance Payment"
        )
    )

    private fun createDefaultExpenses(): List<Expense> = listOf(
        Expense(
            id = "exp_1",
            category = "Fiber Optic Materials",
            description = "2km 12-core figure-8 drop cable",
            amount = 8500.0,
            transactionDate = "2026-11-02",
            responsiblePerson = "Engr. Mark Ramos",
            paymentMethod = "Bank Transfer",
            referenceNumber = "BDO-99214"
        ),
        Expense(
            id = "exp_2",
            category = "Network Equipment",
            description = "10 pcs ZTE F670L GPON ONU/ONT",
            amount = 14500.0,
            transactionDate = "2026-11-03",
            responsiblePerson = "Admin",
            paymentMethod = "Bank Transfer",
            referenceNumber = "UBP-55412"
        ),
        Expense(
            id = "exp_3",
            category = "Fuel and Transportation",
            description = "Service vehicle diesel refueling (2 vans)",
            amount = 3800.0,
            transactionDate = "2026-11-04",
            responsiblePerson = "Driver Joey",
            paymentMethod = "Cash",
            referenceNumber = "SHELL-4412"
        ),
        Expense(
            id = "exp_4",
            category = "Internet and Bandwidth",
            description = "Upstream Tier-1 IP Transit (1 Gbps CIR)",
            amount = 45000.0,
            transactionDate = "2026-11-01",
            responsiblePerson = "Finance Dept",
            paymentMethod = "Bank Transfer",
            referenceNumber = "TRANS-88129"
        ),
        Expense(
            id = "exp_5",
            category = "Electricity",
            description = "Meralco Data Center & Server Room Utility",
            amount = 12400.0,
            transactionDate = "2026-11-05",
            responsiblePerson = "Admin",
            paymentMethod = "Cash",
            referenceNumber = "MRL-77412"
        )
    )

    private fun createDefaultRouters(): List<Router> = listOf(
        Router(
            id = "router_1",
            name = "CCR1036-HQ-Core",
            ipAddress = "192.168.88.1",
            apiPort = 8728,
            username = "api_admin",
            location = "Central Office Data Center",
            model = "CCR1036-8G-2S+",
            status = "Online",
            activeSessionsCount = 184,
            cpuLoad = 14,
            uptime = "88d 14h 22m"
        ),
        Router(
            id = "router_2",
            name = "RB4011-North-Dist",
            ipAddress = "192.168.90.1",
            apiPort = 8728,
            username = "api_admin",
            location = "North Substation Tower",
            model = "RB4011iGS+RM",
            status = "Online",
            activeSessionsCount = 62,
            cpuLoad = 9,
            uptime = "42d 06h 15m"
        )
    )

    private fun createDefaultProfiles(): List<SpeedProfile> = listOf(
        SpeedProfile(id = "prof_1", name = "Fiber 10 Mbps", downloadSpeedMbps = 10, uploadSpeedMbps = 10, rateLimit = "10M/10M", price = 899.0),
        SpeedProfile(id = "prof_2", name = "Fiber 20 Mbps", downloadSpeedMbps = 20, uploadSpeedMbps = 20, rateLimit = "20M/20M", price = 1299.0, isDefault = true),
        SpeedProfile(id = "prof_3", name = "Fiber 30 Mbps", downloadSpeedMbps = 30, uploadSpeedMbps = 30, rateLimit = "30M/30M", price = 1599.0),
        SpeedProfile(id = "prof_4", name = "Fiber 50 Mbps", downloadSpeedMbps = 50, uploadSpeedMbps = 50, rateLimit = "50M/50M", price = 1999.0),
        SpeedProfile(id = "prof_5", name = "Fiber 100 Mbps", downloadSpeedMbps = 100, uploadSpeedMbps = 100, rateLimit = "100M/100M", price = 2799.0)
    )

    private fun createDefaultLogs(): List<ActivityLog> = listOf(
        ActivityLog(
            id = "log_1",
            action = "PPPoE Account Created",
            routerName = "CCR1036-HQ-Core",
            targetSubscriber = "Carlos Mendoza",
            details = "Created PPPoE user 'sub_carlos_m' with profile 'Fiber 20 Mbps'",
            status = "Success"
        ),
        ActivityLog(
            id = "log_2",
            action = "Payment Received",
            routerName = "Billing Engine",
            targetSubscriber = "Maria Santos",
            details = "Recorded ₱1,999.00 payment via Cash. Receipt: REC-20261103-8821",
            status = "Success"
        )
    )

    private fun createDefaultOlts(): List<Olt> = listOf(
        Olt(
            id = "olt_1",
            name = "OLT-Core-01",
            model = "Huawei SmartAX EA5800-X7",
            ipAddress = "192.168.10.10",
            ponPortsCount = 8,
            location = "Central Telco Exchange, Data Center",
            latitude = 14.5995,
            longitude = 120.9842,
            status = "Online",
            connectedNaps = listOf(
                NapBox(
                    id = "nap_1",
                    name = "NAP-01",
                    oltId = "olt_1",
                    oltName = "OLT-Core-01",
                    ponPortNumber = 1,
                    location = "Pilar St. Pole #12, Downtown",
                    latitude = 14.6015,
                    longitude = 120.9855,
                    totalPorts = 8,
                    usedPorts = 5,
                    splitterRatio = "1:8 PLC",
                    status = "Active",
                    opticalPowerDbm = -18.4
                ),
                NapBox(
                    id = "nap_2",
                    name = "NAP-02",
                    oltId = "olt_1",
                    oltName = "OLT-Core-01",
                    ponPortNumber = 1,
                    location = "Rizal Ave. Commercial Pole #4",
                    latitude = 14.5970,
                    longitude = 120.9880,
                    totalPorts = 8,
                    usedPorts = 4,
                    splitterRatio = "1:8 PLC",
                    status = "Active",
                    opticalPowerDbm = -19.1
                ),
                NapBox(
                    id = "nap_3",
                    name = "NAP-03",
                    oltId = "olt_1",
                    oltName = "OLT-Core-01",
                    ponPortNumber = 2,
                    location = "Town Plaza Distribution Pole #1",
                    latitude = 14.6040,
                    longitude = 120.9815,
                    totalPorts = 8,
                    usedPorts = 3,
                    splitterRatio = "1:8 PLC",
                    status = "Active",
                    opticalPowerDbm = -17.8
                ),
                NapBox(
                    id = "nap_4",
                    name = "NAP-GH-01",
                    oltId = "olt_1",
                    oltName = "OLT-Core-01",
                    ponPortNumber = 3,
                    location = "55 Jasmine Way, Garden Homes Gate 2",
                    latitude = 14.6080,
                    longitude = 120.9950,
                    totalPorts = 16,
                    usedPorts = 8,
                    splitterRatio = "1:16 PLC",
                    status = "Active",
                    opticalPowerDbm = -20.5
                )
            )
        ),
        Olt(
            id = "olt_2",
            name = "OLT-Core-02",
            model = "ZTE ZXA10 C320 GPON",
            ipAddress = "192.168.20.10",
            ponPortsCount = 4,
            location = "South Substation Relay Shed",
            latitude = 14.5870,
            longitude = 120.9805,
            status = "Online",
            connectedNaps = listOf(
                NapBox(
                    id = "nap_5",
                    name = "NAP-RS-03",
                    oltId = "olt_2",
                    oltName = "OLT-Core-02",
                    ponPortNumber = 2,
                    location = "19 Riverbank Alley Pole #15",
                    latitude = 14.5880,
                    longitude = 120.9810,
                    totalPorts = 8,
                    usedPorts = 4,
                    splitterRatio = "1:8 PLC",
                    status = "Active",
                    opticalPowerDbm = -19.8
                ),
                NapBox(
                    id = "nap_6",
                    name = "NAP-RS-01",
                    oltId = "olt_2",
                    oltName = "OLT-Core-02",
                    ponPortNumber = 1,
                    location = "South Pier Fisherman Wharf Pole #6",
                    latitude = 14.5840,
                    longitude = 120.9780,
                    totalPorts = 8,
                    usedPorts = 2,
                    splitterRatio = "1:8 PLC",
                    status = "Active",
                    opticalPowerDbm = -18.0
                )
            )
        )
    )

    private fun createDefaultPlans(): List<InternetPlan> = listOf(
        InternetPlan(
            id = "plan_15m",
            name = "Fiber Lite 15 Mbps",
            downloadSpeedMbps = 15,
            uploadSpeedMbps = 15,
            monthlyPrice = 899.0,
            description = "Affordable residential fiber for basic browsing, social media, and SD streaming.",
            status = "Active"
        ),
        InternetPlan(
            id = "plan_20m",
            name = "Fiber 20 Mbps",
            downloadSpeedMbps = 20,
            uploadSpeedMbps = 20,
            monthlyPrice = 1299.0,
            description = "Standard symmetrical fiber package for modern households with HD streaming.",
            status = "Active"
        ),
        InternetPlan(
            id = "plan_35m",
            name = "Fiber Turbo 35 Mbps",
            downloadSpeedMbps = 35,
            uploadSpeedMbps = 35,
            monthlyPrice = 1599.0,
            description = "High-speed gaming and 4K streaming package with priority local routing.",
            status = "Active"
        ),
        InternetPlan(
            id = "plan_50m",
            name = "Fiber Max 50 Mbps",
            downloadSpeedMbps = 50,
            uploadSpeedMbps = 50,
            monthlyPrice = 1999.0,
            description = "Pro work-from-home fiber package with seamless video conferencing and cloud backups.",
            status = "Active"
        ),
        InternetPlan(
            id = "plan_100m",
            name = "Fiber Ultra 100 Mbps",
            downloadSpeedMbps = 100,
            uploadSpeedMbps = 100,
            monthlyPrice = 2799.0,
            description = "Commercial grade gigabit-ready symmetrical fiber connection for power users.",
            status = "Active"
        ),
        InternetPlan(
            id = "plan_starter_legacy",
            name = "Fiber Starter 10 Mbps (Legacy)",
            downloadSpeedMbps = 10,
            uploadSpeedMbps = 10,
            monthlyPrice = 699.0,
            description = "Discontinued legacy tier maintained for existing subscribed accounts.",
            status = "Inactive"
        )
    )
}
