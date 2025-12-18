package com.technikh.employeeattendancetracking.data.database.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

// OfficeWorkReason.kt
@Entity(tableName = "office_work_reasons")
data class OfficeWorkReason(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val reason: String,
    val usageCount: Int = 0,
    val lastUsed: Long = System.currentTimeMillis()
)