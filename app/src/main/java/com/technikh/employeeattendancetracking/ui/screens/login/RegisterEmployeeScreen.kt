package com.technikh.employeeattendancetracking.ui.screens.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.technikh.employeeattendancetracking.data.database.AppDatabase
import com.technikh.employeeattendancetracking.utils.SettingsManager // <--- Import this
import com.technikh.employeeattendancetracking.viewmodel.AttendanceViewModelV2

@Composable
fun RegisterEmployeeScreen(
    onRegistered: () -> Unit
) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val settingsManager = remember { SettingsManager(context) } // <--- Check Settings

    val viewModel: AttendanceViewModelV2 = viewModel(
        factory = AttendanceViewModelV2.Factory(
            database.attendanceDao(),
            database.workReasonDao(),
            database.employeeDao()
        )
    )

    BackHandler { onRegistered() }

    var name by remember { mutableStateOf("") }
    var id by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Check if we need to show the password field
    val isPasswordEnabled = settingsManager.isPasswordFeatureEnabled

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(32.dp)) {
            Text("Register New Employee", style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = id,
                onValueChange = { id = it },
                label = { Text("Employee ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // --- CONDITIONAL PASSWORD FIELD ---
            if (isPasswordEnabled) {
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Set User Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    val finalPassword = if (isPasswordEnabled) password else "1234" // Default if disabled

                    if (name.isNotBlank() && id.isNotBlank() && (finalPassword.isNotBlank())) {
                        viewModel.registerEmployee(name, id, finalPassword)
                        onRegistered()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Save Employee")
            }
        }

        FloatingActionButton(
            onClick = onRegistered,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }
    }
}