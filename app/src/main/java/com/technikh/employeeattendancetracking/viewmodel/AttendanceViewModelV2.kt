package com.technikh.employeeattendancetracking.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Database Imports
import com.technikh.employeeattendancetracking.data.database.daos.AttendanceDao
import com.technikh.employeeattendancetracking.data.database.daos.WorkReasonDao
import com.technikh.employeeattendancetracking.data.database.entities.AttendanceRecord
import com.technikh.employeeattendancetracking.data.database.entities.DailyAttendance
import com.technikh.employeeattendancetracking.data.database.entities.DayOfficeHours
import com.technikh.employeeattendancetracking.data.database.entities.OfficeWorkReason

class AttendanceViewModelV2(
    private val attendanceDao: AttendanceDao,
    private val workReasonDao: WorkReasonDao
) : ViewModel() {

    // --- STATES ---
    var showPunchOutDialog by mutableStateOf(false)

    private val _isPunchedIn = MutableStateFlow(false)
    val isPunchedIn = _isPunchedIn.asStateFlow()

    private val _dailyReports = MutableStateFlow<List<DailyAttendance>>(emptyList())
    val dailyReports = _dailyReports.asStateFlow()

    private val _monthlyReport = MutableStateFlow<List<DayOfficeHours>>(emptyList())
    val monthlyReport = _monthlyReport.asStateFlow()

    // --- LOAD DATA ---
    fun loadDashboardData(employeeId: String) {
        viewModelScope.launch {
            // 1. Check Punch Status
            val lastRecord = attendanceDao.getAttendanceByEmployee(employeeId).firstOrNull()
            _isPunchedIn.value = lastRecord?.punchType == "IN"

            // 2. Load Daily Data
            val allRecords = attendanceDao.getAttendanceByEmployee(employeeId)
            val grouped = allRecords.groupBy { record ->
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(record.timestamp))
            }.map { (date, records) ->
                DailyAttendance(date, records)
            }
            _dailyReports.value = grouped

            // 3. Load Monthly Data
            loadMonthlyChartData(employeeId)
        }
    }

    private fun loadMonthlyChartData(employeeId: String) {
        viewModelScope.launch {
            val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            val report = attendanceDao.getMonthlyOfficeHours(employeeId, currentMonth)
            _monthlyReport.value = report
        }
    }

    // --- PUNCH LOGIC (UPDATED FOR SELFIE) ---

    // Updated to accept the selfie path
    fun onBiometricSuccess(employeeId: String, selfiePath: String?) {
        viewModelScope.launch {
            val lastRecord = attendanceDao.getAttendanceByEmployee(employeeId).firstOrNull()

            if (lastRecord?.punchType == "IN") {
                // If already in, show punch out dialog (Selfie not needed for OUT)
                showPunchOutDialog = true
            } else {
                // If punching IN, save the record with the photo path
                punchIn(employeeId, selfiePath)
            }
        }
    }

    private suspend fun punchIn(employeeId: String, selfiePath: String?) {
        val record = AttendanceRecord(
            employeeId = employeeId,
            punchType = "IN",
            timestamp = System.currentTimeMillis(),
            selfiePath = selfiePath // <--- SAVING THE PHOTO PATH HERE
        )
        attendanceDao.insert(record)
        _isPunchedIn.value = true
        loadDashboardData(employeeId) // Refresh UI
    }

    fun punchOut(employeeId: String, reason: String, isOfficeWork: Boolean, workReason: String?) {
        viewModelScope.launch {
            attendanceDao.insert(AttendanceRecord(
                employeeId = employeeId,
                punchType = "OUT",
                timestamp = System.currentTimeMillis(),
                reason = reason,
                isOfficeWork = isOfficeWork,
                workReason = workReason
            ))

            if (isOfficeWork && !workReason.isNullOrBlank()) {
                saveNewReason(workReason)
            }

            _isPunchedIn.value = false
            loadDashboardData(employeeId)
        }
    }

    private suspend fun saveNewReason(reasonText: String) {
        val existing = workReasonDao.searchReasons("%$reasonText%")
        if (existing.isEmpty()) {
            workReasonDao.insert(OfficeWorkReason(reason = reasonText, usageCount = 1))
        } else {
            workReasonDao.incrementUsage(reasonText, System.currentTimeMillis())
        }
    }

    // --- FACTORY ---
    class Factory(
        private val attendanceDao: AttendanceDao,
        private val workReasonDao: WorkReasonDao
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AttendanceViewModelV2::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AttendanceViewModelV2(attendanceDao, workReasonDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}