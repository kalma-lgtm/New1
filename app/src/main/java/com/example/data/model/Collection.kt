package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Collection(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subscriberId: String = "",
    val accountNumber: String = "",
    val subscriberName: String = "",
    val billId: String = "",
    val amount: Double = 0.0,
    val paymentMethod: String = "Cash", // "Cash", "E-Wallet", "Bank Transfer"
    val referenceNumber: String = "",
    val paymentDate: String = "",
    val receivedBy: String = "Admin",
    val receiptNumber: String = "",
    val remarks: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
