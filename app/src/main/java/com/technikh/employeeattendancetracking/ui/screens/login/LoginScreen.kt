package com.technikh.employeeattendancetracking.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit
) {
    var employeeId by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Employee Login", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = employeeId,
            onValueChange = {
                employeeId = it
                isError = false
            },
            label = { Text("Enter Employee ID") },
            placeholder = { Text("e.g., EMP002") },
            isError = isError,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (isError) {
            Text("Please enter a valid ID", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (employeeId.isNotBlank()) {
                    onLoginSuccess(employeeId)
                } else {
                    isError = true
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Login")
        }
    }
}