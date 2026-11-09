package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class SystemSettings(
    val ispName: String = "NetPulse Broadband Networks",
    val currencySymbol: String = "₱",
    val defaultDueDays: String = "15",
    val autoDisconnectEnabled: Boolean = true,
    val gracePeriodDays: String = "5",
    val supportEmail: String = "support@netpulse.isp",
    val supportHotline: String = "0917-555-NETP",
    val updatedAt: Long = System.currentTimeMillis()
)
