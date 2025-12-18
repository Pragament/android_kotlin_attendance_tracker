package com.technikh.employeeattendancetracking.utils

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Context.createImageFile(): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"
    val storageDir = filesDir
    return File.createTempFile(
        imageFileName,
        ".jpg",
        storageDir
    )
}

fun takePhoto(
    controller: LifecycleCameraController,
    context: Context,
    onPhotoTaken: (File) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val photoFile = context.createImageFile()

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    controller.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onPhotoTaken(photoFile)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("Camera", "Photo capture failed: ${exception.message}", exception)
                onError(exception)
            }
        }
    )
}