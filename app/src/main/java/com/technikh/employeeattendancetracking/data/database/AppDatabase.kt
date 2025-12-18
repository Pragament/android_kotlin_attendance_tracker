package com.technikh.employeeattendancetracking.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.technikh.employeeattendancetracking.data.database.converters.Converters
import com.technikh.employeeattendancetracking.data.database.daos.AttendanceDao
import com.technikh.employeeattendancetracking.data.database.daos.EmployeeDao
import com.technikh.employeeattendancetracking.data.database.daos.WorkReasonDao
import com.technikh.employeeattendancetracking.data.database.entities.AttendanceRecord
import com.technikh.employeeattendancetracking.data.database.entities.Employee
import com.technikh.employeeattendancetracking.data.database.entities.OfficeWorkReason


@Database(
    entities = [Employee::class, AttendanceRecord::class, OfficeWorkReason::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun workReasonDao(): WorkReasonDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendance_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}