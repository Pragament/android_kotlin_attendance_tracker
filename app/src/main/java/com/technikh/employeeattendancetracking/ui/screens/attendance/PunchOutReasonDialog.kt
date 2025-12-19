package com.technikh.employeeattendancetracking.ui.screens.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun PunchOutReasonDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean, String?) -> Unit,
    initialIsOfficeWork: Boolean = false
) {
    var isOfficeWork by remember { mutableStateOf(initialIsOfficeWork) }
    var workReason by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Punch Out Details", style = MaterialTheme.typography.titleLarge)


                Row(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.weight(1f).selectable(
                            selected = !isOfficeWork,
                            onClick = { isOfficeWork = false },
                            role = Role.RadioButton
                        ).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = !isOfficeWork, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Personal")
                    }

                    Row(
                        Modifier.weight(1f).selectable(
                            selected = isOfficeWork,
                            onClick = { isOfficeWork = true },
                            role = Role.RadioButton
                        ).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isOfficeWork, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Office Work")
                    }
                }

                if (isOfficeWork) {
                    OutlinedTextField(
                        value = workReason,
                        onValueChange = { workReason = it },
                        label = { Text("What did you work on?") },
                        placeholder = { Text("e.g. Client Meeting") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "No explanation needed for personal time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            val finalReason = if (isOfficeWork) "Office Work" else "Personal"
                            val finalWorkReason = if (isOfficeWork) workReason else null
                            onConfirm(finalReason, isOfficeWork, finalWorkReason)
                        }
                    ) {
                        Text("Confirm & Logout")
                    }
                }
            }
        }
    }
}