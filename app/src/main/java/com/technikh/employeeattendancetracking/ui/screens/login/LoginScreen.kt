package com.technikh.employeeattendancetracking.ui.screens.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search // <--- Search Icon
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share // <--- Export Icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.technikh.employeeattendancetracking.data.database.AppDatabase
import com.technikh.employeeattendancetracking.utils.CsvUtils // <--- Import CsvUtils
import com.technikh.employeeattendancetracking.utils.SettingsManager
import com.technikh.employeeattendancetracking.viewmodel.AttendanceViewModelV2
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch // <--- For Export Coroutine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToGlobalReports: () -> Unit
) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val viewModel: AttendanceViewModelV2 = viewModel(
        factory = AttendanceViewModelV2.Factory(
            database.attendanceDao(),
            database.workReasonDao(),
            database.employeeDao()
        )
    )

    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope() // <--- Scope for database export

    val employees by viewModel.employees.collectAsState()
    val timeline by viewModel.todayTimeline.collectAsState()

    // --- SEARCH & FILTER LOGIC ---
    var searchQuery by remember { mutableStateOf("") }

    val filteredEmployees = remember(employees, searchQuery, settingsManager.maxHomeEmployees) {
        if (searchQuery.isBlank()) {
            // If no search, show limited list from Settings
            employees.take(settingsManager.maxHomeEmployees)
        } else {
            // If searching, show all matches
            employees.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.employeeId.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    var showAuthDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance Home") },
                actions = {
                    IconButton(onClick = onNavigateToGlobalReports) {
                        Icon(Icons.Default.List, contentDescription = "View Reports")
                    }

                    // --- GLOBAL EXPORT BUTTON ---
                    IconButton(onClick = {
                        scope.launch {
                            // 1. Fetch All Records
                            val allRecords = database.attendanceDao().getAllRecordsList()

                            if (allRecords.isNotEmpty()) {
                                // 2. Create Map (ID -> Name) for readable CSV
                                val empMap = employees.associate { it.employeeId to it.name }
                                // 3. Generate CSV
                                CsvUtils.generateAndShareCsv(context, "All_Employees_Report", allRecords, empMap)
                            }
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export All Reports")
                    }

                    // --- SETTINGS BUTTON ---
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAuthDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Employee")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // --- SEARCH BAR (New) ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Employee") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // --- EMPLOYEE LIST ---
            Text(
                "Select Employee",
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredEmployees.isEmpty()) {
                    item {
                        Text(
                            if(searchQuery.isBlank()) "No employees found. Click + to add." else "No matches found.",
                            color = Color.Gray,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                items(filteredEmployees) { employee ->
                    Card(
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onLoginSuccess(employee.employeeId) }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(employee.name, fontWeight = FontWeight.Bold)
                                Text("ID: ${employee.employeeId}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            Divider(thickness = 2.dp, color = Color.LightGray, modifier = Modifier.padding(vertical = 8.dp))

            // --- TODAY'S TIMELINE ---
            // Added a small "Export Today" button here too for convenience
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Today's Activity", style = MaterialTheme.typography.titleMedium)

                if (timeline.isNotEmpty()) {
                    IconButton(onClick = {
                        CsvUtils.generateAndShareCsv(context, "Today_Log", timeline)
                    }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Export Today", tint = Color.Gray)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (timeline.isEmpty()) {
                    item { Text("No punches today.", modifier = Modifier.padding(8.dp), color = Color.Gray) }
                }

                items(timeline) { record ->
                    val empName = employees.find { it.employeeId == record.employeeId }?.name ?: "Unknown"
                    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(record.timestamp))
                    val color = if (record.punchType == "IN") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = color),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(empName, fontWeight = FontWeight.Bold)
                                Text("ID: ${record.employeeId}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = record.punchType,
                                    fontWeight = FontWeight.Bold,
                                    color = if(record.punchType == "IN") Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                                Text(time, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        if (showAuthDialog) {
            AdminPassDialog(
                correctPassword = settingsManager.adminPassword,
                onDismiss = { showAuthDialog = false },
                onSuccess = { onNavigateToRegister() }
            )
        }
    }
}

@Composable
fun AdminPassDialog(correctPassword: String, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Admin Authorization") },
        text = {
            Column {
                OutlinedTextField(
                    value = input, onValueChange = { input = it; isError = false },
                    label = { Text("Admin Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = isError
                )
                if (isError) Text("Incorrect Password", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (input == correctPassword) {
                    onSuccess()
                    onDismiss()
                } else isError = true
            }) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}