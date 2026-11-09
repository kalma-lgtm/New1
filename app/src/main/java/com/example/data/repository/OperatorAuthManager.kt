package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.OperatorRole
import com.example.data.model.OperatorUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class OperatorAuthManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("isp_operator_auth_prefs", Context.MODE_PRIVATE)

    private val _currentOperator = MutableStateFlow<OperatorUser?>(null)
    val currentOperator: StateFlow<OperatorUser?> = _currentOperator.asStateFlow()

    init {
        ensureDefaultAccounts()
        restoreSavedSession()
    }

    private fun ensureDefaultAccounts() {
        if (!prefs.contains("user_admin")) {
            saveUser("admin", "admin123", "Chief Administrator", OperatorRole.ADMINISTRATOR, "admin@netpulse.isp")
        }
        if (!prefs.contains("user_billing")) {
            saveUser("billing", "billing123", "Brenda Cruz (Billing)", OperatorRole.BILLING, "billing@netpulse.isp")
        }
        if (!prefs.contains("user_casher")) {
            saveUser("casher", "casher123", "Carla Reyes (Cashier)", OperatorRole.CASHER, "casher@netpulse.isp")
        }
        if (!prefs.contains("user_staff")) {
            saveUser("staff", "staff123", "Sarah Jenkins (Staff)", OperatorRole.STAFF, "sarah@netpulse.isp")
        }
        if (!prefs.contains("user_tech")) {
            saveUser("tech", "tech123", "Mark Ramos (OLT Engr)", OperatorRole.TECHNICIAN, "mark@netpulse.isp")
        }
    }

    private fun saveUser(
        username: String,
        pass: String,
        fullName: String,
        role: OperatorRole,
        email: String
    ) {
        val json = JSONObject().apply {
            put("username", username)
            put("password", pass)
            put("fullName", fullName)
            put("role", role.name)
            put("email", email)
        }
        prefs.edit().putString("user_${username.lowercase()}", json.toString()).apply()
    }

    private fun restoreSavedSession() {
        val savedUsername = prefs.getString("active_session_username", null)
        if (savedUsername != null) {
            val userJsonStr = prefs.getString("user_${savedUsername.lowercase()}", null)
            if (userJsonStr != null) {
                try {
                    val json = JSONObject(userJsonStr)
                    _currentOperator.value = OperatorUser(
                        username = json.getString("username"),
                        fullName = json.getString("fullName"),
                        role = OperatorRole.valueOf(json.getString("role")),
                        email = json.optString("email")
                    )
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    fun authenticate(username: String, pass: String): Result<OperatorUser> {
        val userJsonStr = prefs.getString("user_${username.trim().lowercase()}", null)
            ?: return Result.failure(IllegalArgumentException("Invalid username or password."))

        return try {
            val json = JSONObject(userJsonStr)
            val storedPass = json.getString("password")
            if (storedPass != pass.trim()) {
                Result.failure(IllegalArgumentException("Invalid username or password."))
            } else {
                val operator = OperatorUser(
                    username = json.getString("username"),
                    fullName = json.getString("fullName"),
                    role = OperatorRole.valueOf(json.getString("role")),
                    email = json.optString("email")
                )
                _currentOperator.value = operator
                prefs.edit().putString("active_session_username", operator.username).apply()
                Result.success(operator)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun changePassword(username: String, oldPass: String, newPass: String): Result<Unit> {
        val userJsonStr = prefs.getString("user_${username.trim().lowercase()}", null)
            ?: return Result.failure(IllegalArgumentException("Account not found."))

        return try {
            val json = JSONObject(userJsonStr)
            val storedPass = json.getString("password")
            if (storedPass != oldPass) {
                Result.failure(IllegalArgumentException("Current password does not match."))
            } else {
                json.put("password", newPass)
                prefs.edit().putString("user_${username.trim().lowercase()}", json.toString()).apply()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun resetPassword(username: String, newPass: String): Result<Unit> {
        val userJsonStr = prefs.getString("user_${username.trim().lowercase()}", null)
            ?: return Result.failure(IllegalArgumentException("Account '$username' not found."))

        return try {
            val json = JSONObject(userJsonStr)
            json.put("password", newPass)
            prefs.edit().putString("user_${username.trim().lowercase()}", json.toString()).apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun verifyAdminPassword(password: String): Boolean {
        val userJsonStr = prefs.getString("user_admin", null) ?: return password == "admin123"
        return try {
            val json = JSONObject(userJsonStr)
            json.getString("password") == password.trim()
        } catch (e: Exception) {
            password == "admin123"
        }
    }

    fun signOut() {
        _currentOperator.value = null
        prefs.edit().remove("active_session_username").apply()
    }

    companion object {
        @Volatile
        private var instance: OperatorAuthManager? = null

        fun getInstance(context: Context): OperatorAuthManager {
            return instance ?: synchronized(this) {
                instance ?: OperatorAuthManager(context).also { instance = it }
            }
        }
    }
}
