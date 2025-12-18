package com.technikh.employeeattendancetracking.data.database.daos
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.technikh.employeeattendancetracking.data.database.entities.OfficeWorkReason

@Dao
interface WorkReasonDao {
    @Insert
    suspend fun insert(reason: OfficeWorkReason)

    @Query("SELECT * FROM office_work_reasons ORDER BY usageCount DESC, lastUsed DESC")
    suspend fun getAllReasons(): List<OfficeWorkReason>

    @Query("SELECT * FROM office_work_reasons WHERE reason LIKE :query ORDER BY usageCount DESC")
    suspend fun searchReasons(query: String): List<OfficeWorkReason>

    @Query("UPDATE office_work_reasons SET usageCount = usageCount + 1, lastUsed = :timestamp WHERE reason = :reason")
    suspend fun incrementUsage(reason: String, timestamp: Long)
}