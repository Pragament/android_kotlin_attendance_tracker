package com.technikh.employeeattendancetracking.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val employeeId: String,
    val punchType: String, // "IN" or "OUT"
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String? = null,
    val workReason: String? = null,
    val isOfficeWork: Boolean = false,
    val selfiePath: String? = null
)

// --- ADDED THESE HERE TO FIX "SYMBOL NOT FOUND" ERRORS ---
data class DailyAttendance(
    val date: String,
    val records: List<AttendanceRecord>
)

data class DayOfficeHours(
    val day: String,
    val officeHours: Double
)