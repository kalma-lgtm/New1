package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Router(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val ipAddress: String = "",
    val apiPort: Int = 8728,
    val username: String = "admin",
    val password: String = "",
    val location: String = "Main Server Room",
    val model: String = "CCR1036-8G-2S+",
    val status: String = "Online", // "Online", "Offline", "Warning"
    val activeSessionsCount: Int = 0,
    val cpuLoad: Int = 12,
    val uptime: String = "45d 12h 30m",
    @ServerTimestamp val createdAt: Timestamp? = null
)
