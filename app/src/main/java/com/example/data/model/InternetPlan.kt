package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class InternetPlan(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val downloadSpeedMbps: Int = 25,
    val uploadSpeedMbps: Int = 25,
    val monthlyPrice: Double = 1299.0,
    val description: String = "",
    val status: String = "Active", // "Active" or "Inactive"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
