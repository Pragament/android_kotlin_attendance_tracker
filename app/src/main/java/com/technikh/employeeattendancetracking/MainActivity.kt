package com.technikh.employeeattendancetracking

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import com.technikh.employeeattendancetracking.ui.screens.attendance.MainAttendanceScreen
import com.technikh.employeeattendancetracking.ui.screens.login.LoginScreen
import com.technikh.employeeattendancetracking.ui.screens.reports.ReportsDashboardV2

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
                        }
                    )
                }
                "attendance" -> {
                    MainAttendanceScreen(
                        employeeId = currentEmployeeId,
                        onNavigateToDashboard = {
                            currentScreen = "reports"
                        }
                    )
                }
                "reports" -> {
                    ReportsDashboardV2(
                        employeeId = currentEmployeeId,
                        onBack = {
                            currentScreen = "attendance"
                        }
                    )
                }
            }
        }
    }
}