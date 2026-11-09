package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ActivityLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val action: String = "",
    val routerName: String = "",
    val targetSubscriber: String = "",
    val details: String = "",
    val status: String = "Success", // "Success", "Failed"
    @ServerTimestamp val createdAt: Timestamp? = null
)
