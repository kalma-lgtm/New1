package com.example.data.model

enum class OperatorRole(val displayName: String) {
    ADMINISTRATOR("Administrator"),
    BILLING("Billing"),
    CASHER("Casher"),
    STAFF("Billing & Cashier Staff"),
    TECHNICIAN("Field Network Technician")
}

data class OperatorUser(
    val username: String,
    val fullName: String,
    val role: OperatorRole,
    val email: String = ""
)
