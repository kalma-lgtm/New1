package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "Fiber Optic Materials",
    val description: String = "",
    val amount: Double = 0.0,
    val transactionDate: String = "",
    val responsiblePerson: String = "",
    val paymentMethod: String = "Cash",
    val referenceNumber: String = "",
    val receiptUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
