package com.technikh.employeeattendancetracking.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// CORRECT IMPORTS FOR YOUR NEW FOLDER STRUCTURE
import com.technikh.employeeattendancetracking.data.database.daos.AttendanceDao
import com.technikh.employeeattendancetracking.data.database.daos.WorkReasonDao
import com.technikh.employeeattendancetracking.data.database.entities.AttendanceRecord
import com.technikh.employeeattendancetracking.data.database.entities.OfficeWorkReason
import com.technikh.employeeattendancetracking.data.database.entities.DailyAttendance

class AttendanceViewModel(
    private val attendanceDao: AttendanceDao,
    private val workReasonDao: WorkReasonDao
) : ViewModel() {

    // --- UI STATES ---
    var showPunchOutDialog by mutableStateOf(false)

    // Holds the "In/Out" status for the Button color
    private val _isPunchedIn = MutableStateFlow(false)
    val isPunchedIn = _isPunchedIn.asStateFlow()

    // Holds the Reports Data (Fixed: Now returns grouped DailyAttendance)
    private val _dailyReports = MutableStateFlow<List<DailyAttendance>>(emptyList())
    val dailyReports = _dailyReports.asStateFlow()

    // --- MAIN LOGIC ---

    // Called when the screen opens to load data
    fun loadEmployeeState(employeeId: String) {
        viewModelScope.launch {
            // 1. Check if user is currently punched in
            val lastRecord = attendanceDao.getAttendanceByEmployee(employeeId).firstOrNull()
            _isPunchedIn.value = lastRecord?.punchType == "IN"

            // 2. Load and Group the Reports Data
            attendanceDao.getDailyAttendance(employeeId).collect { records ->
                // This logic transforms the "Raw List" into "Grouped by Date"
                val groupedData = records.groupBy { record ->
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(record.systemTimeMillis))
                }.map { (date, dayRecords) ->
                    DailyAttendance(date, dayRecords)
                }
                _dailyReports.value = groupedData
            }
        }
    }

    fun onBiometricSuccess(employeeId: String) {
        viewModelScope.launch {
            // Double check state to avoid duplicate punches
            val lastRecord = attendanceDao.getAttendanceByEmployee(employeeId).firstOrNull()

            if (lastRecord?.punchType == "IN") {
                showPunchOutDialog = true
            } else {
                attendanceDao.insert(AttendanceRecord(
                    employeeId = employeeId,
                    punchType = "IN"
                ))
                _isPunchedIn.value = true
            }
        }
    }

    fun punchOut(employeeId: String, reason: String, isOfficeWork: Boolean, workReason: String?) {
        viewModelScope.launch {
            attendanceDao.insert(AttendanceRecord(
                employeeId = employeeId,
                punchType = "OUT",
                reason = reason,
                isOfficeWork = isOfficeWork,
                workReason = workReason
            ))
            _isPunchedIn.value = false // Switch button back to "PUNCH IN" (Green)

            if (isOfficeWork && !workReason.isNullOrBlank()) {
                saveNewReason(workReason)
            }
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

    suspend fun searchWorkReasons(query: String): List<String> {
        return withContext(Dispatchers.IO) {
            workReasonDao.searchReasons("%$query%").map { it.reason }
        }
    }


    class Factory(
        private val attendanceDao: AttendanceDao,
        private val workReasonDao: WorkReasonDao
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AttendanceViewModel(attendanceDao, workReasonDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}