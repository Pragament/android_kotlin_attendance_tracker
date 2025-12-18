package com.technikh.employeeattendancetracking.ui.screens.attendance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.technikh.employeeattendancetracking.viewmodel.AttendanceViewModel

@Composable
fun PunchOutReasonDialog(
    viewModel: AttendanceViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean, String?) -> Unit
) {
    var selectedReason by remember { mutableStateOf("Personal") }
    var isOfficeWork by remember { mutableStateOf(false) }
    var officeWorkReason by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Punch Out Reason") },
        text = {
            Column {
                // 1. Reason Selection Dropdown
                Text("Select Reason:", fontWeight = FontWeight.Bold)

                Column {
                    OutlinedTextField(
                        value = selectedReason,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Reason Type") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true },
                        enabled = false // Disable direct typing to force click
                    )

                    // Invisible clickable overlay to ensure the dropdown opens
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("Personal", "Office Work").forEach { reason ->
                            DropdownMenuItem(
                                text = { Text(reason) },
                                onClick = {
                                    selectedReason = reason
                                    isOfficeWork = (reason == "Office Work")
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Office Work Reason Input (Auto-complete)
                if (isOfficeWork) {
                    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
                    var showSuggestions by remember { mutableStateOf(false) }

                    Text("Office Work Details:")

                    // Search Logic: Triggers when user types 2+ characters
                    LaunchedEffect(officeWorkReason) {
                        if (officeWorkReason.length >= 2) {
                            suggestions = viewModel.searchWorkReasons(officeWorkReason)
                            showSuggestions = suggestions.isNotEmpty()
                        } else {
                            showSuggestions = false
                        }
                    }

                    OutlinedTextField(
                        value = officeWorkReason,
                        onValueChange = {
                            officeWorkReason = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Enter specific work (e.g. Client Meeting)") }
                    )

                    // Suggestions List Popup
                    if (showSuggestions) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column {
                                suggestions.forEach { suggestion ->
                                    Text(
                                        text = suggestion,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                officeWorkReason = suggestion
                                                showSuggestions = false
                                            }
                                            .padding(12.dp)
                                    )
                                    Divider()
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        selectedReason,
                        isOfficeWork,
                        if (isOfficeWork) officeWorkReason else null
                    )
                }
            ) {
                Text("Confirm Punch Out")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}