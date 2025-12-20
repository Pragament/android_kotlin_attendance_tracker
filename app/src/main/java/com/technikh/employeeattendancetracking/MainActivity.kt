package com.technikh.employeeattendancetracking

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import com.technikh.employeeattendancetracking.ui.screens.attendance.MainAttendanceScreen
import com.technikh.employeeattendancetracking.ui.screens.dashboard.ReportsDashboardV2
import com.technikh.employeeattendancetracking.ui.screens.login.LoginScreen
import com.technikh.employeeattendancetracking.ui.screens.login.RegisterEmployeeScreen
import com.technikh.employeeattendancetracking.ui.screens.settings.SettingsScreen
import com.technikh.employeeattendancetracking.ui.screens.reports.GlobalReportsScreen // <--- IMPORT THIS

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf("login") }
            var currentEmployeeId by remember { mutableStateOf("") }

            when (currentScreen) {
                "login" -> {
                    LoginScreen(
                        onLoginSuccess = { enteredId ->
                            currentEmployeeId = enteredId
                            currentScreen = "attendance"
                        },
                        onNavigateToRegister = { currentScreen = "register" },
                        onNavigateToSettings = { currentScreen = "settings" },
                        // --- NEW: CONNECT GLOBAL REPORTS ---
                        onNavigateToGlobalReports = { currentScreen = "global_reports" }
                    )
                }

                "register" -> {
                    RegisterEmployeeScreen(onRegistered = { currentScreen = "login" })
                }

                "attendance" -> {
                    MainAttendanceScreen(
                        employeeId = currentEmployeeId,
                        onNavigateToDashboard = { currentScreen = "reports" },
                        onNavigateHome = { currentScreen = "login" }
                    )
                }

                "reports" -> {
                    ReportsDashboardV2(
                        employeeId = currentEmployeeId,
                        onBack = { currentScreen = "attendance" }
                    )
                }

                "settings" -> {
                    SettingsScreen(onBack = { currentScreen = "login" })
                }

                // --- NEW SCREEN CASE ---
                "global_reports" -> {
                    GlobalReportsScreen(
                        onBack = { currentScreen = "login" }
                    )
                }
            }
        }
    }
}