package com.technikh.employeeattendancetracking.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.technikh.employeeattendancetracking.data.database.entities.Employee

@Dao
interface EmployeeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee)

    @Query("SELECT * FROM employees WHERE employeeId = :id")
    suspend fun getEmployeeById(id: String): Employee?
}