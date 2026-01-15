package com.technikh.employeeattendancetracking.ui.screens.dashboard

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import coil.compose.AsyncImage
import com.technikh.employeeattendancetracking.data.database.AppDatabase
import com.technikh.employeeattendancetracking.data.database.entities.DayOfficeHours
import com.technikh.employeeattendancetracking.data.database.entities.AttendanceRecord
import com.technikh.employeeattendancetracking.utils.CsvUtils
import com.technikh.employeeattendancetracking.utils.SettingsManager
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
    val settingsManager = remember { SettingsManager(context) }

    BackHandler { onBack() }

    val viewModel: AttendanceViewModelV2 = viewModel(
        factory = AttendanceViewModelV2.Factory(
            database.attendanceDao(),
            database.workReasonDao(),
            database.employeeDao()
        )
    )

    LaunchedEffect(employeeId) { viewModel.loadDashboardData(employeeId) }

    // --- DATA STATES ---
    val currentDayRecords by viewModel.currentDayRecords.collectAsState(initial = emptyList())
    val currentMonthRecords by viewModel.currentMonthRecords.collectAsState(initial = emptyList())
    val monthlyReportState by viewModel.monthlyReport.collectAsState(initial = emptyList())

    // --- TEXT STATES ---
    val currentDateText by viewModel.currentDateText.collectAsState()
    val currentMonthText by viewModel.currentMonthText.collectAsState()
    val employeeName by viewModel.currentEmployeeName.collectAsState()

    // --- TAB STATE ---
    var selectedTab by remember { mutableIntStateOf(0) }

    // --- SELFIE PRIVACY STATES ---
    var areSelfiesVisible by remember { mutableStateOf(false) } // Default Hidden
    var showAuthDialog by remember { mutableStateOf(false) }
    var adminPasswordInput by remember { mutableStateOf("") }
    var isAuthError by remember { mutableStateOf(false) }

    // --- CALENDAR DIALOG ---
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, day: Int ->
            val newDate = Calendar.getInstance(); newDate.set(year, month, day)
            viewModel.setDate(newDate.timeInMillis)
        },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                actions = {
                    // --- 1. TOGGLE SELFIE BUTTON ---
                    IconButton(onClick = {
                        if (areSelfiesVisible) {
                            areSelfiesVisible = false // Hide immediately
                        } else {
                            // Show Password Dialog to Unlock
                            adminPasswordInput = ""
                            isAuthError = false
                            showAuthDialog = true
                        }
                    }) {
                        // FIXED: Replaced 'Visibility' with 'Face' (Show) and 'Lock' (Hide)
                        Icon(
                            imageVector = if (areSelfiesVisible) Icons.Default.Face else Icons.Default.Lock,
                            contentDescription = "Toggle Selfies",
                            tint = if (areSelfiesVisible) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    // --- 2. EXPORT CSV BUTTON ---
                    IconButton(onClick = {
                        if (currentMonthRecords.isNotEmpty()) {
                            CsvUtils.generateAndShareCsv(context, employeeName, currentMonthRecords)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export Month CSV")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                // --- NAVIGATION HEADER ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        if (selectedTab == 0) viewModel.incrementDay(-1) else viewModel.incrementMonth(-1)
                    }) { Icon(Icons.Default.KeyboardArrowLeft, "Prev") }

                    Text(
                        text = if (selectedTab == 0) currentDateText else currentMonthText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row {
                        IconButton(onClick = {
                            if (selectedTab == 0) viewModel.incrementDay(1) else viewModel.incrementMonth(1)
                        }) { Icon(Icons.Default.KeyboardArrowRight, "Next") }
                        IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.DateRange, "Select Date") }
                    }
                }

                // --- TABS ---
                TabRow(selectedTabIndex = selectedTab) {
                    listOf("Daily Report", "Monthly Chart").forEachIndexed { index, title ->
                        Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // --- VIEW CONTENT ---
                when (selectedTab) {
                    0 -> DailySingleDayView(currentDayRecords, areSelfiesVisible)
                    1 -> MonthlyReportViewV2(monthlyReportState)
                }
            }

            FloatingActionButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
        }

        // --- PASSWORD DIALOG ---
        if (showAuthDialog) {
            AlertDialog(
                onDismissRequest = { showAuthDialog = false },
                title = { Text("Admin Access Required") },
                text = {
                    Column {
                        Text("Enter Admin Password to view selfies:")
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = adminPasswordInput,
                            onValueChange = { adminPasswordInput = it; isAuthError = false },
                            visualTransformation = PasswordVisualTransformation(),
                            isError = isAuthError,
                            singleLine = true,
                            label = { Text("Password") }
                        )
                        if (isAuthError) {
                            Text("Incorrect Password", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (adminPasswordInput == settingsManager.adminPassword) {
                            areSelfiesVisible = true
                            showAuthDialog = false
                        } else {
                            isAuthError = true
                        }
                    }) { Text("Unlock") }
                },
                dismissButton = { Button(onClick = { showAuthDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

// --- UPDATED DAILY VIEW (Accepts showSelfies param) ---
@Composable
fun DailySingleDayView(records: List<AttendanceRecord>, showSelfies: Boolean) {
    if (records.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No records for this date.")
        }
        return
    }

    LazyColumn {
        items(records) { record ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        Text(
                            text = timeFormat.format(Date(record.employeeTimeMillis)),
                            color = if (record.punchType == "IN") Color(0xFF4CAF50) else Color(0xFFF44336),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (record.isManuallyEdited) {
                            Text(
                                text = "Recorded on ${dateFormat.format(Date(record.systemTimeMillis))} at ${timeFormat.format(Date(record.systemTimeMillis))}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "Self-corrected",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(record.punchType, fontWeight = FontWeight.SemiBold)

                        if (record.punchType == "OUT") {
                            val displayText = if (record.isOfficeWork && !record.workReason.isNullOrBlank())
                                "${record.reason}: ${record.workReason}"
                            else record.reason

                            if (!displayText.isNullOrBlank()) {
                                Text(text = displayText, fontStyle = FontStyle.Italic, fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                    }

                    // --- CONDITIONALLY SHOW SELFIE ---
                    if (showSelfies && record.selfiePath != null) {
                        Card(
                            modifier = Modifier.size(60.dp),
                            shape = MaterialTheme.shapes.small,
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            AsyncImage(model = File(record.selfiePath), contentDescription = "Selfie", modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }

        // Daily Total Calculation
        item {
            val totalHours = calculateDailyHoursLocalV2(records)
            if (totalHours > 0.0) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(
                        text = "Total Hours: ${"%.2f".format(totalHours)}",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyReportViewV2(monthlyData: List<DayOfficeHours>) {
    if (monthlyData.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("No chart data yet.")
        }
        return
    }
    val chartProducer = remember(monthlyData) {
        val entries = monthlyData.mapIndexed { index, dayData -> FloatEntry(x = index.toFloat(), y = dayData.officeHours.toFloat()) }
        ChartEntryModelProducer(entries)
    }
    Column {
        Text("Office Hours (Current Month)", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Chart(chart = columnChart(), chartModelProducer = chartProducer, modifier = Modifier.fillMaxWidth().height(250.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        LazyColumn {
            items(monthlyData) { dayData ->
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
    records.sortedBy { it.employeeTimeMillis }.forEach { record ->
        if (record.punchType == "IN") lastPunchIn = record.employeeTimeMillis
        else if (record.punchType == "OUT" && lastPunchIn != null) {
            totalHours += (record.employeeTimeMillis - lastPunchIn!!) / (1000.0 * 60 * 60)
            lastPunchIn = null
        }
    }
    return totalHours
}