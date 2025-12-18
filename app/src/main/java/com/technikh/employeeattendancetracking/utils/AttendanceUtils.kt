import com.technikh.employeeattendancetracking.data.database.entities.AttendanceRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
// AttendanceUtils.kt
object AttendanceUtils {

    fun calculateDailyHours(records: List<AttendanceRecord>): Double {
        var totalHours = 0.0
        var lastPunchIn: Long? = null

        records.sortedBy { it.timestamp }.forEach { record ->
            when (record.punchType) {
                "IN" -> lastPunchIn = record.timestamp
                "OUT" -> lastPunchIn?.let { punchIn ->
                    val duration = (record.timestamp - punchIn) / (1000.0 * 60 * 60)
                    if (record.isOfficeWork) {
                        totalHours += duration
                    }
                    lastPunchIn = null
                }
            }
        }

        return totalHours
    }

    fun groupRecordsByDate(records: List<AttendanceRecord>): List<DailyAttendance> {
        return records.groupBy {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp))
        }.map { (date, dayRecords) ->
            DailyAttendance(
                date = date,
                records = dayRecords.sortedByDescending { it.timestamp }
            )
        }.sortedByDescending { it.date }
    }
}

data class DailyAttendance(
    val date: String,
    val records: List<AttendanceRecord>
)