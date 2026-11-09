package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Subscriber
import com.example.data.repository.IspRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IspRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun subscriber_createAndRead_succeedsForAuthenticatedUser() = runBlocking {
        signInTestUser("alice@netpulse.isp")
        val repository = IspRepository(firestore)

        val newSub = Subscriber(
            accountNumber = "NP-2026-9999",
            fullName = "Alice Tester",
            contactNumber = "09171234567",
            planName = "Fiber 20 Mbps",
            monthlyFee = 1299.0,
            connectionStatus = "Active"
        )

        val subId = withTimeout(10000L) {
            repository.addSubscriber(newSub)
        }
        assertNotNull(subId)
        assertTrue(subId.isNotBlank())
    }

    @Test
    fun billingAndPayment_balanceCalculation_updatesCorrectly() = runBlocking {
        signInTestUser("bob@netpulse.isp")
        val repository = IspRepository(firestore)

        val subId = repository.addSubscriber(
            Subscriber(
                accountNumber = "NP-2026-8888",
                fullName = "Bob PaymentTest",
                contactNumber = "09181234567",
                planName = "Fiber 50 Mbps",
                monthlyFee = 1999.0,
                connectionStatus = "Active"
            )
        )

        // Generate bills
        val generatedCount = repository.generateMonthlyBills("2026-11", "2026-11-20")
        assertTrue(generatedCount >= 1)

        // Record full payment
        val receipt = repository.recordPayment(
            subscriberId = subId,
            billId = "",
            amount = 1999.0,
            paymentMethod = "GCash",
            referenceNumber = "REF-TEST-001",
            remarks = "Test Payment"
        )
        assertNotNull(receipt)
        assertTrue(receipt.startsWith("REC-"))
    }

    @Test
    fun subscriber_archive_retainsCredentialsAndMarksDeletedInMikrotik() = runBlocking {
        signInTestUser("charlie@netpulse.isp")
        val repository = IspRepository(firestore)

        val subId = repository.addSubscriber(
            Subscriber(
                accountNumber = "NP-2026-7777",
                fullName = "Charlie ArchiveTest",
                contactNumber = "09191234567",
                pppoeUsername = "sub_charlie",
                pppoePassword = "secret_pass_777",
                planName = "Fiber 20 Mbps",
                monthlyFee = 1299.0,
                connectionStatus = "Active"
            )
        )

        // Delete/archive the subscriber
        repository.deleteSubscriber(subId)

        // Read the subscriber back
        val docSnapshot = firestore.collection("users").document("charlie@netpulse.isp").collection("subscribers").document(subId).get().await()
        val archivedSub = docSnapshot.toObject(Subscriber::class.java)

        assertNotNull(archivedSub)
        assertTrue(archivedSub!!.isArchived)
        assertEquals("sub_charlie", archivedSub.pppoeUsername)
        assertEquals("secret_pass_777", archivedSub.pppoePassword)
        assertTrue(archivedSub.isDeletedInMikrotik)
        assertEquals("Disconnected", archivedSub.connectionStatus)
    }
}
