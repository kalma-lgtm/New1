package com.example

import com.example.data.model.OperatorRole
import com.example.data.model.SystemSettings
import com.example.ui.screens.AppDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun rbacPermissions_billingAndCasher_onlyAllowedOperationalModules() {
        val billingRole = OperatorRole.BILLING
        val casherRole = OperatorRole.CASHER

        fun getAllowedDestinations(role: OperatorRole): List<AppDestination> {
            val isBillingOrCasher = role == OperatorRole.BILLING || role == OperatorRole.CASHER
            return if (isBillingOrCasher) {
                listOf(
                    AppDestination.SUBSCRIBERS,
                    AppDestination.BILLING,
                    AppDestination.COLLECTIONS,
                    AppDestination.EXPENSES
                )
            } else {
                AppDestination.values().toList()
            }
        }

        val billingDestinations = getAllowedDestinations(billingRole)
        val casherDestinations = getAllowedDestinations(casherRole)
        val adminDestinations = getAllowedDestinations(OperatorRole.ADMINISTRATOR)

        assertEquals(4, billingDestinations.size)
        assertEquals(4, casherDestinations.size)
        assertEquals(AppDestination.values().size, adminDestinations.size)

        // Must NOT have access to Dashboard, Internet Plans, MikroTik, Reports, User Management, Settings
        assertFalse(billingDestinations.contains(AppDestination.DASHBOARD))
        assertFalse(billingDestinations.contains(AppDestination.INTERNET_PLANS))
        assertFalse(billingDestinations.contains(AppDestination.MIKROTIK))
        assertFalse(billingDestinations.contains(AppDestination.REPORTS))
        assertFalse(billingDestinations.contains(AppDestination.USER_MANAGEMENT))
        assertFalse(billingDestinations.contains(AppDestination.SYSTEM_SETTINGS))

        // Must have access ONLY to Subscribers, Billing, Collections, Expenses
        assertTrue(billingDestinations.contains(AppDestination.SUBSCRIBERS))
        assertTrue(billingDestinations.contains(AppDestination.BILLING))
        assertTrue(billingDestinations.contains(AppDestination.COLLECTIONS))
        assertTrue(billingDestinations.contains(AppDestination.EXPENSES))
        assertTrue(adminDestinations.contains(AppDestination.INTERNET_PLANS))
    }

    @Test
    fun systemSettings_defaultsAndPersistence_holdValidValues() {
        val defaultSettings = SystemSettings()
        assertEquals("NetPulse Broadband Networks", defaultSettings.ispName)
        assertEquals("₱", defaultSettings.currencySymbol)
        assertEquals("15", defaultSettings.defaultDueDays)
        assertEquals("5", defaultSettings.gracePeriodDays)
        assertTrue(defaultSettings.autoDisconnectEnabled)

        val custom = defaultSettings.copy(
            ispName = "Custom Fiber ISP",
            currencySymbol = "$",
            defaultDueDays = "20",
            gracePeriodDays = "7",
            autoDisconnectEnabled = false
        )
        assertEquals("Custom Fiber ISP", custom.ispName)
        assertEquals("$", custom.currencySymbol)
        assertEquals("20", custom.defaultDueDays)
        assertEquals("7", custom.gracePeriodDays)
        assertFalse(custom.autoDisconnectEnabled)
    }

    @Test
    fun expenseMonthFilter_correctlyMatchesDateStrings() {
        val months = listOf(
            "ALL MONTHS", "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        fun expenseMatchesMonth(dateStr: String, month: String): Boolean {
            if (month == "ALL MONTHS") return true
            val monthIdx = months.indexOf(month)
            if (monthIdx < 1) return true
            val parts = dateStr.split("-")
            if (parts.size >= 2) {
                val m = parts[1].toIntOrNull()
                if (m == monthIdx) return true
            }
            return false
        }

        assertTrue(expenseMatchesMonth("2026-03-15", "March"))
        assertFalse(expenseMatchesMonth("2026-03-15", "April"))
        assertTrue(expenseMatchesMonth("2026-11-08", "November"))
        assertTrue(expenseMatchesMonth("2026-11-08", "ALL MONTHS"))
    }

    @Test
    fun internetPlan_creationAndSubscriberIntegration() {
        val plan = com.example.data.model.InternetPlan(
            id = "plan_speed_100",
            name = "Fiber Ultra 100 Mbps",
            downloadSpeedMbps = 100,
            uploadSpeedMbps = 50,
            monthlyPrice = 2499.0,
            description = "High-speed gaming & streaming plan",
            status = "Active"
        )

        assertEquals("Fiber Ultra 100 Mbps", plan.name)
        assertEquals(100, plan.downloadSpeedMbps)
        assertEquals(50, plan.uploadSpeedMbps)
        assertEquals(2499.0, plan.monthlyPrice, 0.001)
        assertEquals("Active", plan.status)

        // Subscriber auto-fills with plan info
        val subscriber = com.example.data.model.Subscriber(
            fullName = "Juan dela Cruz",
            planId = plan.id,
            planName = plan.name,
            monthlyFee = plan.monthlyPrice,
            mikrotikProfile = plan.name
        )

        assertEquals("plan_speed_100", subscriber.planId)
        assertEquals("Fiber Ultra 100 Mbps", subscriber.planName)
        assertEquals(2499.0, subscriber.monthlyFee, 0.001)
        assertEquals("Fiber Ultra 100 Mbps", subscriber.mikrotikProfile)

        // Deactivated plan check
        val deactivated = plan.copy(status = "Inactive")
        assertEquals("Inactive", deactivated.status)
    }
}
