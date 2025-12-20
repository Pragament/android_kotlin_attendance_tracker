package com.technikh.employeeattendancetracking.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.technikh.employeeattendancetracking.data.database.entities.AttendanceRecord
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object CsvUtils {

    fun generateAndShareCsv(
        context: Context,
        fileNamePrefix: String,
        records: List<AttendanceRecord>,
        employeeMap: Map<String, String>? = null // Optional: ID -> Name map for Global Export
    ) {
        try {
            val fileName = "${fileNamePrefix}_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            // 1. Dynamic Header
            if (employeeMap != null) {
                writer.append("Employee Name,Employee ID,Date,Time,Type,Reason,Is Office Work,Work Reason\n")
            } else {
                writer.append("Date,Time,Type,Reason,Is Office Work,Work Reason\n")
            }

            // 2. Write Data
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            records.forEach { record ->
                val date = dateFormat.format(Date(record.timestamp))
                val time = timeFormat.format(Date(record.timestamp))

                val cleanReason = record.reason?.replace(",", " ") ?: ""
                val cleanWorkReason = record.workReason?.replace(",", " ") ?: ""

                // Write Employee Name/ID if this is a global report
                if (employeeMap != null) {
                    val name = employeeMap[record.employeeId] ?: "Unknown"
                    writer.append("$name,${record.employeeId},")
                }

                writer.append("$date,$time,${record.punchType},$cleanReason,${record.isOfficeWork},$cleanWorkReason\n")
            }

            writer.flush()
            writer.close()

            // 3. Share
            shareFile(context, file)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareFile(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Attendance Report")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Report via..."))
    }
}