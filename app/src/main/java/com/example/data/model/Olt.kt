package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class NapBox(
    val id: String = "",
    val name: String = "",
    val oltId: String = "",
    val oltName: String = "",
    val ponPortNumber: Int = 1,
    val location: String = "",
    val latitude: Double = 14.5995,
    val longitude: Double = 120.9842,
    val totalPorts: Int = 8,
    val usedPorts: Int = 0,
    val splitterRatio: String = "1:8 PLC",
    val status: String = "Active", // "Active", "Full", "Maintenance"
    val opticalPowerDbm: Double = -19.2
)

@IgnoreExtraProperties
data class Olt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val model: String = "GPON 8-Port OLT",
    val ipAddress: String = "192.168.10.1",
    val ponPortsCount: Int = 8,
    val location: String = "Central Hub Data Center",
    val latitude: Double = 14.5995,
    val longitude: Double = 120.9842,
    val status: String = "Online", // "Online", "Degraded", "Offline"
    val connectedNaps: List<NapBox> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)
