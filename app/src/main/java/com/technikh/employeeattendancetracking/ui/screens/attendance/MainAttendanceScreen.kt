package com.technikh.employeeattendancetracking.ui.screens.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

import com.technikh.employeeattendancetracking.data.database.AppDatabase
import com.technikh.employeeattendancetracking.viewmodel.AttendanceViewModelV2
import com.technikh.employeeattendancetracking.utils.rememberBiometricPrompt
import com.technikh.employeeattendancetracking.utils.launchBiometric
import com.technikh.employeeattendancetracking.utils.takePhoto
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.window.Dialog

@Composable
fun MainAttendanceScreen(
    employeeId: String,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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

    val isPunchedIn by viewModel.isPunchedIn.collectAsState()
    var showPunchOutDialog by remember { mutableStateOf(false) }
    var isCapturingPhoto by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )
    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            bindToLifecycle(lifecycleOwner)
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA // Use Selfie Camera
        }
    }

    val biometricPrompt = rememberBiometricPrompt(
        onSuccess = {
            isCapturingPhoto = true
            Toast.makeText(context, "Verifying... Smile! 📸", Toast.LENGTH_SHORT).show()

            takePhoto(
                controller = cameraController,
                context = context,
                onPhotoTaken = { photoFile ->
                    viewModel.onBiometricSuccess(employeeId, photoFile.absolutePath)
                    isCapturingPhoto = false
                    Toast.makeText(context, "Punch In Successful!", Toast.LENGTH_SHORT).show()
                },
                onError = {
                    isCapturingPhoto = false
                    Toast.makeText(context, "Failed to capture photo", Toast.LENGTH_SHORT).show()
                    viewModel.onBiometricSuccess(employeeId, null)
                }
            )
        },
        onError = { _, errString ->
            Toast.makeText(context, "Auth Error: $errString", Toast.LENGTH_SHORT).show()
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission && !isPunchedIn) {
            AndroidView(
                factory = { ctx -> PreviewView(ctx).apply { controller = cameraController } },
                modifier = Modifier.size(1.dp).alpha(0f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome, $employeeId",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- PUNCH BUTTON ---
            Button(
                onClick = {
                    if (isPunchedIn) {
                        showPunchOutDialog = true
                    } else {
                        if (!hasCameraPermission) {
                            Toast.makeText(context, "Camera permission required for verification", Toast.LENGTH_LONG).show()
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            biometricPrompt?.let { prompt ->
                                launchBiometric(prompt)
                            } ?: run {
                                Toast.makeText(context, "Biometrics not setup on device", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPunchedIn) Color.Red else Color(0xFF4CAF50)
                ),
                enabled = !isCapturingPhoto, // Disable button while taking photo
                modifier = Modifier
                    .size(200.dp)
                    .padding(16.dp)
            ) {
                if (isCapturingPhoto) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Text(
                        text = if (isPunchedIn) "PUNCH OUT" else "PUNCH IN",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { onNavigateToDashboard() },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("View Monthly Reports")
            }
        }


        if (showPunchOutDialog) {
            PunchOutReasonDialog(
                onDismiss = { showPunchOutDialog = false },
                onConfirm = { reason, isOffice, workReason ->
                    viewModel.punchOut(employeeId, reason, isOffice, workReason)
                    showPunchOutDialog = false
                },
                initialIsOfficeWork = false
            )
        }
    }
}

@Composable
fun PunchOutReasonDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean, String?) -> Unit,
    initialIsOfficeWork: Boolean = false
) {
    var reason by remember { mutableStateOf("") }
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
                Text(
                    text = "Punch Out Details",
                    style = MaterialTheme.typography.titleLarge
                )

                Row(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .weight(1f)
                            .selectable(
                                selected = !isOfficeWork,
                                onClick = { isOfficeWork = false },
                                role = Role.RadioButton
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = !isOfficeWork, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Personal")
                    }


                    Row(
                        Modifier
                            .weight(1f)
                            .selectable(
                                selected = isOfficeWork,
                                onClick = { isOfficeWork = true },
                                role = Role.RadioButton
                            )
                            .padding(8.dp),
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
                        placeholder = { Text("e.g. Client Meeting, Coding") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason for leaving") },
                        placeholder = { Text("e.g. Lunch, Home") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val finalReason = if (isOfficeWork) "Office Work" else reason
                            val finalWorkReason = if (isOfficeWork) workReason else null
                            onConfirm(finalReason, isOfficeWork, finalWorkReason)
                        }
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}