package com.technikh.employeeattendancetracking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.technikh.employeeattendancetracking.data.database.daos.AttendanceDao
import com.technikh.employeeattendancetracking.data.database.daos.WorkReasonDao
import com.technikh.employeeattendancetracking.data.database.daos.EmployeeDao
import com.technikh.employeeattendancetracking.data.database.entities.AttendanceRecord
import com.technikh.employeeattendancetracking.data.database.entities.DailyAttendance
import com.technikh.employeeattendancetracking.data.database.entities.DayOfficeHours
import com.technikh.employeeattendancetracking.data.database.entities.OfficeWorkReason
import com.technikh.employeeattendancetracking.data.database.entities.Employee

class AttendanceViewModelV2(
    private val attendanceDao: AttendanceDao,
    private val workReasonDao: WorkReasonDao,
    private val employeeDao: EmployeeDao
) : ViewModel() {

    private val _isPunchedIn = MutableStateFlow(false)
    val isPunchedIn = _isPunchedIn.asStateFlow()

    private val _dailyReports = MutableStateFlow<List<DailyAttendance>>(emptyList())
    val dailyReports = _dailyReports.asStateFlow()

    private val _monthlyReport = MutableStateFlow<List<DayOfficeHours>>(emptyList())
    val monthlyReport = _monthlyReport.asStateFlow()

    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees = _employees.asStateFlow()


    private val _currentEmployeeName = MutableStateFlow("")
    val currentEmployeeName = _currentEmployeeName.asStateFlow()


    private val _workReasonSuggestions = MutableStateFlow<List<String>>(emptyList())
    val workReasonSuggestions = _workReasonSuggestions.asStateFlow()

    init {
        viewModelScope.launch {
            employeeDao.getAllEmployees().collect { list -> _employees.value = list }
        }
    }

    fun registerEmployee(name: String, id: String) {
        viewModelScope.launch {
            val newEmployee = Employee(name = name, employeeId = id)
            employeeDao.insertEmployee(newEmployee)
        }
    }

    fun loadDashboardData(employeeId: String) {
        viewModelScope.launch {

            val emp = employeeDao.getEmployeeById(employeeId)
            _currentEmployeeName.value = emp?.name ?: "Unknown"


            val lastRecord = attendanceDao.getAttendanceByEmployee(employeeId).firstOrNull()
            _isPunchedIn.value = lastRecord?.punchType == "IN"


            val allRecords = attendanceDao.getAttendanceByEmployee(employeeId)
            val grouped = allRecords.groupBy { record ->
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(record.timestamp))
            }.map { (date, records) -> DailyAttendance(date, records) }
            _dailyReports.value = grouped

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

    fun searchReasons(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _workReasonSuggestions.value = emptyList()
            } else {
                val reasons = workReasonDao.searchReasons("%$query%")
                _workReasonSuggestions.value = reasons.map { it.reason }
            }
        }
    }

    fun punchIn(employeeId: String, selfiePath: String?) {
        viewModelScope.launch {
            val record = AttendanceRecord(
                employeeId = employeeId,
                punchType = "IN",
                timestamp = System.currentTimeMillis(),
                selfiePath = selfiePath
            )
            attendanceDao.insert(record)
            _isPunchedIn.value = true
            loadDashboardData(employeeId)
        }
    }

    fun punchOut(employeeId: String, reason: String, isOfficeWork: Boolean, workReason: String?, selfiePath: String?) {
        viewModelScope.launch {
            attendanceDao.insert(AttendanceRecord(
                employeeId = employeeId,
                punchType = "OUT",
                timestamp = System.currentTimeMillis(),
                reason = reason,
                isOfficeWork = isOfficeWork,
                workReason = workReason,
                selfiePath = selfiePath // Saving selfie for Out too
            ))

            if (isOfficeWork && !workReason.isNullOrBlank()) {
                saveNewReason(workReason)
            }

            _isPunchedIn.value = false
            loadDashboardData(employeeId)
        }
    }

    private suspend fun saveNewReason(reasonText: String) {
        val existing = workReasonDao.searchReasons(reasonText)
        if (existing.isEmpty()) {
            workReasonDao.insert(OfficeWorkReason(reason = reasonText, usageCount = 1))
        } else {
            workReasonDao.incrementUsage(reasonText, System.currentTimeMillis())
        }
    }

    class Factory(
        private val attendanceDao: AttendanceDao,
        private val workReasonDao: WorkReasonDao,
        private val employeeDao: EmployeeDao
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AttendanceViewModelV2::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AttendanceViewModelV2(attendanceDao, workReasonDao, employeeDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}