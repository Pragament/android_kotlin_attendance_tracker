package com.technikh.employeeattendancetracking.data.database.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

// Employee.kt
@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val employeeId: String,
    val fingerprintRegistered: Boolean = false,
    val registrationDate: Long = System.currentTimeMillis()
)