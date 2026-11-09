package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class SpeedProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val downloadSpeedMbps: Int = 20,
    val uploadSpeedMbps: Int = 20,
    val rateLimit: String = "20M/20M",
    val price: Double = 1299.0,
    val isDefault: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)
