package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Bill(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subscriberId: String = "",
    val accountNumber: String = "",
    val subscriberName: String = "",
    val billingPeriod: String = "", // e.g. "2026-11"
    val previousBalance: Double = 0.0,
    val monthlyFee: Double = 0.0,
    val totalAmountDue: Double = 0.0,
    val amountPaid: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val status: String = "Unpaid", // "Unpaid", "Partially Paid", "Paid", "Overdue"
    val dueDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
