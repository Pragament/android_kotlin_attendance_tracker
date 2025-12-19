package com.technikh.employeeattendancetracking.ui.screens.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAttendanceScreen(
    employeeId: String,
    onNavigateToDashboard: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val database = AppDatabase.getDatabase(context)


    BackHandler {
        onNavigateHome()
    }

    val viewModel: AttendanceViewModelV2 = viewModel(
        factory = AttendanceViewModelV2.Factory(
            database.attendanceDao(),
            database.workReasonDao(),
            database.employeeDao()
        )
    )

    LaunchedEffect(employeeId) {
        viewModel.loadDashboardData(employeeId)
    }

    val isPunchedIn by viewModel.isPunchedIn.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val employeeName = employees.find { it.employeeId == employeeId }?.name ?: employeeId

    var showPunchOutDialog by remember { mutableStateOf(false) }
    var tempSelfiePath by remember { mutableStateOf<String?>(null) }
    var isCapturingPhoto by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )
    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            bindToLifecycle(lifecycleOwner)
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
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
                    isCapturingPhoto = false
                    val path = photoFile.absolutePath
                    tempSelfiePath = path

                    if (isPunchedIn) {
                        showPunchOutDialog = true
                    } else {
                        viewModel.punchIn(employeeId, path)
                        Toast.makeText(context, "Punch In Successful!", Toast.LENGTH_SHORT).show()
                        onNavigateHome()
                    }
                },
                onError = {
                    isCapturingPhoto = false
                    Toast.makeText(context, "Photo failed", Toast.LENGTH_SHORT).show()
                }
            )
        },
        onError = { _, errString ->
            Toast.makeText(context, "Auth Error: $errString", Toast.LENGTH_SHORT).show()
        }
    )

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Hidden Camera Preview
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx -> PreviewView(ctx).apply { controller = cameraController } },
                    modifier = Modifier.size(1.dp).alpha(0f)
                )
            }


            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = employeeName, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(text = "ID: $employeeId", fontSize = 16.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = {
                        if (!hasCameraPermission) {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            biometricPrompt?.let { launchBiometric(it) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPunchedIn) Color(0xFFC62828) else Color(0xFF2E7D32)
                    ),
                    enabled = !isCapturingPhoto,
                    modifier = Modifier.size(220.dp).padding(16.dp)
                ) {
                    if (isCapturingPhoto) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        Text(
                            text = if (isPunchedIn) "PUNCH OUT" else "PUNCH IN",
                            fontSize = 24.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = { onNavigateToDashboard() },
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text("View My Reports")
                }
            }


            FloatingActionButton(
                onClick = onNavigateHome,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }


            if (showPunchOutDialog) {
                PunchOutReasonDialog(
                    onDismiss = { showPunchOutDialog = false },
                    onConfirm = { reason, isOffice, workReason ->
                        viewModel.punchOut(employeeId, reason, isOffice, workReason, tempSelfiePath)
                        showPunchOutDialog = false
                        Toast.makeText(context, "Punch Out Successful!", Toast.LENGTH_SHORT).show()
                        onNavigateHome()
                    }
                )
            }
        }
    }
}