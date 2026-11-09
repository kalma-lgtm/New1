package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Subscriber(
    @DocumentId val id: String = "",
    val userId: String = "",
    val accountNumber: String = "",
    val fullName: String = "",
    val contactNumber: String = "",
    val address: String = "",
    val latitude: Double = 14.5995, // Default Metro coordinates
    val longitude: Double = 120.9842,
    val installationDate: String = "",
    val planId: String = "plan_20m",
    val planName: String = "Fiber 20 Mbps",
    val monthlyFee: Double = 1299.0,
    val olt: String = "OLT-Core-01",
    val ponPort: String = "PON-1",
    val napBox: String = "NAP-01",
    val napPort: String = "Port 1",
    val splitter: String = "1:8 PLC",
    val onuSerial: String = "",
    val pppoeUsername: String = "",
    val pppoePassword: String = "",
    val mikrotikProfile: String = "profile_20m",
    val connectionStatus: String = "Active", // "Active", "Suspended", "Disconnected"
    val billingDueDate: Int = 15,
    val remarks: String = "",
    val photoUrl: String = "",
    val currentBalance: Double = 0.0,
    val isArchived: Boolean = false,
    val archivedDate: String = "",
    val isDeletedInMikrotik: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
