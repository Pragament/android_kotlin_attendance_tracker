package com.technikh.employeeattendancetracking.ui.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.technikh.employeeattendancetracking.data.database.AppDatabase
import com.technikh.employeeattendancetracking.data.database.entities.AttendanceRecord
import com.technikh.employeeattendancetracking.data.database.entities.DailyAttendance
import com.technikh.employeeattendancetracking.data.database.entities.DayOfficeHours
import com.technikh.employeeattendancetracking.viewmodel.AttendanceViewModelV2
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsDashboardV2(
    employeeId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)

    // Using V2 ViewModel
    val viewModel: AttendanceViewModelV2 = viewModel(
        factory = AttendanceViewModelV2.Factory(
            database.attendanceDao(),
            database.workReasonDao()
        )
    )

    LaunchedEffect(employeeId) {
        viewModel.loadDashboardData(employeeId)
    }

    val dailyReportsState = viewModel.dailyReports.collectAsState(initial = emptyList())
    val monthlyReportState = viewModel.monthlyReport.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance Reports") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            var selectedTab by remember { mutableIntStateOf(0) }

            TabRow(selectedTabIndex = selectedTab) {
                listOf("Daily Report", "Monthly Chart").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> DailyReportViewV2(dailyReportsState.value)
                1 -> MonthlyReportViewV2(monthlyReportState.value)
            }
        }
    }
}



@Composable
fun DailyReportViewV2(reports: List<DailyAttendance>) {
    if (reports.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No daily records found.")
        }
        return
    }
    LazyColumn {
        items(reports) { daily ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = daily.date, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    daily.records.forEach { record ->
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                            Text(text = timeFormat.format(Date(record.timestamp)),
                                color = if (record.punchType == "IN") Color(0xFF4CAF50) else Color(0xFFF44336))
                            Text(record.punchType)
                            if (record.reason != null) Text(record.reason, fontStyle = FontStyle.Italic)
                        }
                    }
                    val totalHours = calculateDailyHoursLocalV2(daily.records)
                    if (totalHours > 0.0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Total: ${"%.2f".format(totalHours)} hours",
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyReportViewV2(monthlyData: List<DayOfficeHours>) {
    if (monthlyData.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No chart data. Punch OUT as 'Office Work' to see bars.")
        }
        return
    }

    val chartProducer = remember(monthlyData) {
        val entries = monthlyData.mapIndexed { index, dayData ->
            FloatEntry(x = index.toFloat(), y = dayData.officeHours.toFloat())
        }
        ChartEntryModelProducer(entries)
    }

    Column {
        Text("Office Hours (Current Month)", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))


        Chart(
            chart = columnChart(),
            chartModelProducer = chartProducer,
            modifier = Modifier.fillMaxWidth().height(250.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Divider()

        LazyColumn {
            items(monthlyData) { dayData ->
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text(dayData.day)
                    Text("${"%.2f".format(dayData.officeHours)} hrs", fontWeight = FontWeight.Bold)
                }
                Divider()
            }
        }
    }
}

fun calculateDailyHoursLocalV2(records: List<AttendanceRecord>): Double {
    var totalHours = 0.0
    var lastPunchIn: Long? = null
    records.sortedBy { it.timestamp }.forEach { record ->
        if (record.punchType == "IN") lastPunchIn = record.timestamp
        else if (record.punchType == "OUT" && lastPunchIn != null) {
            totalHours += (record.timestamp - lastPunchIn!!) / (1000.0 * 60 * 60)
            lastPunchIn = null
        }
    }
    return totalHours
}