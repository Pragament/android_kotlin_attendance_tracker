package com.technikh.employeeattendancetracking.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.technikh.employeeattendancetracking.utils.SettingsManager

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }

    var isAuthenticated by remember { mutableStateOf(false) }
    var inputPass by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    if (!isAuthenticated) {

        Column(modifier = Modifier.padding(16.dp)) {
            Text("Admin Access Required", style = MaterialTheme.typography.headlineMedium)
            Text("Default Password: admin123", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = inputPass,
                onValueChange = { inputPass = it },
                label = { Text("Enter Admin Password") },
                visualTransformation = PasswordVisualTransformation(),
                isError = errorMsg.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
            if (errorMsg.isNotEmpty()) {
                Text(errorMsg, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                if (inputPass == settingsManager.adminPassword) {
                    isAuthenticated = true
                } else {
                    errorMsg = "Wrong Password"
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Login")
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    } else {

        var isPassEnabled by remember { mutableStateOf(settingsManager.isPasswordFeatureEnabled) }
        var isPreviewEnabled by remember { mutableStateOf(settingsManager.showCameraPreview) }
        var maxEmp by remember { mutableStateOf(settingsManager.maxHomeEmployees.toString()) }
        var newAdminPass by remember { mutableStateOf("") }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))

            // 1. Toggle Password Feature
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Require User Password for Punch")
                Switch(checked = isPassEnabled, onCheckedChange = {
                    isPassEnabled = it
                    settingsManager.isPasswordFeatureEnabled = it
                })
            }
            Divider(Modifier.padding(vertical = 8.dp))


            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Show Selfie Camera Preview")
                Switch(checked = isPreviewEnabled, onCheckedChange = {
                    isPreviewEnabled = it
                    settingsManager.showCameraPreview = it
                })
            }
            Divider(Modifier.padding(vertical = 8.dp))


            OutlinedTextField(
                value = maxEmp,
                onValueChange = {
                    maxEmp = it
                    it.toIntOrNull()?.let { num -> settingsManager.maxHomeEmployees = num }
                },
                label = { Text("Max Employees List on Home") },
                modifier = Modifier.fillMaxWidth()
            )
            Divider(Modifier.padding(vertical = 8.dp))


            OutlinedTextField(
                value = newAdminPass,
                onValueChange = { newAdminPass = it },
                label = { Text("Set New Admin Password") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                if(newAdminPass.isNotBlank()) {
                    settingsManager.adminPassword = newAdminPass
                    newAdminPass = ""
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Update Admin Password") }

            Spacer(Modifier.height(20.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Close Settings") }
        }
    }
}