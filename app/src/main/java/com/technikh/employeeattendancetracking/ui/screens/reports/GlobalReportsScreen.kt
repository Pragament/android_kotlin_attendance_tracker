package com.technikh.employeeattendancetracking.ui.screens.reports

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.technikh.employeeattendancetracking.data.database.AppDatabase
import com.technikh.employeeattendancetracking.data.database.entities.AttendanceRecord
import com.technikh.employeeattendancetracking.utils.CsvUtils
import com.technikh.employeeattendancetracking.viewmodel.AttendanceViewModelV2
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalReportsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val viewModel: AttendanceViewModelV2 = viewModel(
        factory = AttendanceViewModelV2.Factory(database.attendanceDao(), database.workReasonDao(), database.employeeDao())
    )

    // Load ALL data when screen opens
    LaunchedEffect(Unit) { viewModel.loadGlobalReportData() }

    BackHandler { onBack() }

    // States
    val employees by viewModel.employees.collectAsState()
    val selectedIds by viewModel.selectedEmployeeIds.collectAsState()
    val globalDailyReports by viewModel.globalDailyReports.collectAsState()
    val globalMonthlySummary by viewModel.globalMonthlySummary.collectAsState()

    val currentDateText by viewModel.currentDateText.collectAsState()
    val currentMonthText by viewModel.currentMonthText.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0=Daily, 1=Monthly
    var showFilterDialog by remember { mutableStateOf(false) }

    // Map for easy ID -> Name lookup
    val empMap = remember(employees) { employees.associate { it.employeeId to it.name } }

    // Calendar
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            val newDate = Calendar.getInstance().apply { set(year, month, day) }
            viewModel.setDate(newDate.timeInMillis)
        },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manager Reports") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // --- EXPORT FILTERED CSV ---
                    IconButton(onClick = {
                        if (selectedTab == 0) {
                            // Export Daily List
                            if (globalDailyReports.isNotEmpty()) {
                                CsvUtils.generateAndShareCsv(context, "Filtered_Daily_Report", globalDailyReports, empMap)
                            }
                        } else {
                            // Export Monthly Summary (Fallback to daily data for CSV detail)
                            if (globalDailyReports.isNotEmpty()) {
                                CsvUtils.generateAndShareCsv(context, "Filtered_Report", globalDailyReports, empMap)
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Share, "Export Filtered CSV")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {

            // --- 1. FILTERS & NAVIGATION ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filter Button
                Button(
                    onClick = { showFilterDialog = true },
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    // FIXED ICON: Use 'List' instead of 'FilterList' to avoid import errors
                    Icon(Icons.Filled.List, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Filter Staff (${selectedIds.size})")
                }
            }

            // Date Navigation
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { if (selectedTab == 0) viewModel.incrementDay(-1) else viewModel.incrementMonth(-1) }) { Icon(Icons.Filled.KeyboardArrowLeft, null) }
                Text(
                    text = if (selectedTab == 0) currentDateText else currentMonthText,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = { if (selectedTab == 0) viewModel.incrementDay(1) else viewModel.incrementMonth(1) }) { Icon(Icons.Filled.KeyboardArrowRight, null) }
                    IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Filled.DateRange, null) }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- 2. TABS ---
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Daily Log") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Monthly Summary") })
            }

            Spacer(Modifier.height(8.dp))

            // --- 3. CONTENT ---
            if (selectedTab == 0) {
                // DAILY VIEW
                if (globalDailyReports.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No records for selected criteria.") }
                } else {
                    LazyColumn {
                        items(globalDailyReports) { record ->
                            GlobalLogItem(record, empMap[record.employeeId] ?: record.employeeId)
                        }
                    }
                }
            } else {
                // MONTHLY VIEW
                if (globalMonthlySummary.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No data for selected criteria.") }
                } else {
                    LazyColumn {
                        // Header
                        item {
                            Row(Modifier.fillMaxWidth().padding(8.dp).background(Color.LightGray)) {
                                Text("Employee", Modifier.weight(1f).padding(8.dp), fontWeight = FontWeight.Bold)
                                Text("Total Hours", Modifier.padding(8.dp), fontWeight = FontWeight.Bold)
                            }
                        }
                        // Summary Items
                        items(globalMonthlySummary) { (empId, hours) ->
                            Row(Modifier.fillMaxWidth().padding(8.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(empMap[empId] ?: empId, fontWeight = FontWeight.Bold)
                                    Text(empId, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Text("${"%.2f".format(hours)} hrs", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Divider()
                        }
                    }
                }
            }
        }

        // --- FILTER DIALOG ---
        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Select Employees") },
                text = {
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        item {
                            Row(
                                Modifier.fillMaxWidth().clickable { viewModel.selectAllEmployees(employees.map { it.employeeId }) }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Select All", fontWeight = FontWeight.Bold)
                            }
                            Divider()
                        }
                        items(employees) { emp ->
                            val isSelected = selectedIds.contains(emp.employeeId)
                            Row(
                                Modifier.fillMaxWidth().clickable { viewModel.toggleEmployeeSelection(emp.employeeId) }.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = isSelected, onCheckedChange = { viewModel.toggleEmployeeSelection(emp.employeeId) })
                                Column {
                                    Text(emp.name)
                                    Text(emp.employeeId, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                },
                confirmButton = { Button(onClick = { showFilterDialog = false }) { Text("Done") } }
            )
        }
    }
}

@Composable
fun GlobalLogItem(record: AttendanceRecord, empName: String) {
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(record.timestamp))
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(empName, fontWeight = FontWeight.Bold)
                Row {
                    Text(record.punchType, color = if(record.punchType == "IN") Color(0xFF2E7D32) else Color(0xFFC62828), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(time)
                }
            }
            if (record.punchType == "OUT" && record.reason != null) {
                Text(record.reason, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
            }
        }
    }
}