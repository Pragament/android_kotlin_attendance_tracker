package com.technikh.employeeattendancetracking.repository

import android.util.Log
import com.technikh.employeeattendancetracking.data.database.daos.AttendanceDao
import com.technikh.employeeattendancetracking.data.database.daos.WorkReasonDao
import com.technikh.employeeattendancetracking.data.database.entities.AttendanceRecord
import com.technikh.employeeattendancetracking.data.database.entities.OfficeWorkReason
import com.technikh.employeeattendancetracking.data.database.entities.SupabaseAttendanceRecord
import com.technikh.employeeattendancetracking.data.database.entities.PunchOutUpdate
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AttendanceRepository(
    private val attendanceDao: AttendanceDao,
    private val workReasonDao: WorkReasonDao,
    var supabase: SupabaseClient?
) {

    fun getDailyAttendance(employeeId: String): Flow<List<AttendanceRecord>> {
        return attendanceDao.getDailyAttendance(employeeId)
    }

    suspend fun getAttendanceByEmployee(employeeId: String): List<AttendanceRecord> {
        return attendanceDao.getAttendanceByEmployee(employeeId)
    }

    suspend fun insertAttendance(record: AttendanceRecord) {
        Log.d("REPO", "Saving record locally: ${record.punchType}")

        // A. Save to Local Phone (Room)
        attendanceDao.insert(record)

        try {
            supabase?.from("attendance")?.insert(record)
            Log.d("REPO", "Synced to Laptop Success!")
        } catch (e: Exception) {
            Log.e("REPO", "Offline mode: Could not sync to laptop. Error: ${e.message}")
        }
    }
    suspend fun uploadImageToSupabase(filePath: String): String? {
        val client = supabase ?: return null
        
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.e("REPO", "Selfie file does not exist: $filePath")
                return null
            }
            
            val fileBytes = file.readBytes()
            val fileName = "selfie_${System.currentTimeMillis()}_${file.name}"
            
            // Upload to 'selfies' bucket
            val bucket = client.storage.from("selfies")
            bucket.upload(fileName, fileBytes, upsert = true)
            
            // Get public URL
            val publicUrl = bucket.publicUrl(fileName)
            Log.d("REPO", "Image uploaded successfully: $publicUrl")
            publicUrl
        } catch (e: Exception) {
            Log.e("REPO", "Failed to upload image: ${e.message}")
            null
        }
    }
    suspend fun syncAttendanceToSupabase(
        employeeId: String,
        punchType: String,
        timestamp: Long,
        selfiePath: String?
    ) {
        Log.d("REPO", "syncAttendanceToSupabase called - employeeId: $employeeId, punchType: $punchType, selfiePath: $selfiePath")
        
        val client = supabase ?: run {
            Log.w("REPO", "Supabase client is null, skipping sync")
            return
        }
        
        Log.d("REPO", "Supabase client is available, proceeding with sync...")

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
            val timeString = dateFormat.format(Date(timestamp))

            // Upload image if available
            Log.d("REPO", "selfiePath received: $selfiePath")
            val imageUrl = if (selfiePath != null) {
                Log.d("REPO", "Attempting to upload image from: $selfiePath")
                val url = uploadImageToSupabase(selfiePath)
                Log.d("REPO", "Image upload result: $url")
                url
            } else {
                Log.w("REPO", "No selfiePath provided - image_url will be null")
                null
            }

            if (punchType == "IN") {
                // Create new attendance record for punch in
                val record = SupabaseAttendanceRecord(
                    employeeId = employeeId,
                    punchInTime = timeString,
                    punchOutTime = null,
                    imageUrl = imageUrl,
                    isSynced = true
                )
                try {
                    client.from("attendance").insert(record)
                    Log.d("REPO", "PUNCH IN synced to Supabase for $employeeId")
                } catch (insertError: Exception) {
                    Log.e("REPO", "CRITICAL: PUNCH IN Insert Failed for $employeeId", insertError)
                    insertError.printStackTrace()
                }
            } else {
                Log.d("REPO", "Preparing PUNCH OUT update. ImageURL: $imageUrl")

                // Create serializable update record for punch_out_time
                val updateData = PunchOutUpdate(
                    punchOutTime = timeString,
                    punchOutImageUrl = imageUrl,
                    isSynced = true
                )
                
                try {
                    client.from("attendance").update(updateData) {
                        filter {
                            eq("employee_id", employeeId)
                            filter("punch_out_time", FilterOperator.IS, null)
                        }
                    }
                    Log.d("REPO", "PUNCH OUT synced to Supabase for $employeeId. Payload: $updateData")
                } catch (updateError: Exception) {
                    Log.e("REPO", "CRITICAL: PUNCH OUT Update Failed for $employeeId", updateError)
                    updateError.printStackTrace()
                }
            }
        } catch (e: Exception) {
            Log.e("REPO", "Failed to sync to Supabase (General Error): ${e.message}", e)
            e.printStackTrace()
        }
    }

    suspend fun searchReasons(query: String): List<OfficeWorkReason> {
        return workReasonDao.searchReasons(query)
    }

    suspend fun insertReason(reason: OfficeWorkReason) {
        workReasonDao.insert(reason)
    }

    suspend fun incrementReasonUsage(reason: String, timestamp: Long) {
        workReasonDao.incrementUsage(reason, timestamp)
    }
}
