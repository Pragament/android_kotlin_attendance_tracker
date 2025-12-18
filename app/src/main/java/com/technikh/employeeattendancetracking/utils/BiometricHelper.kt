package com.technikh.employeeattendancetracking.utils

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
fun rememberBiometricPrompt(
    onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
    onError: (Int, CharSequence) -> Unit
): BiometricPrompt? {
    val context = LocalContext.current

    // Ensure we are inside an Activity that supports Biometrics
    val fragmentActivity = context as? FragmentActivity ?: return null

    // Create the executor
    val executor = remember { ContextCompat.getMainExecutor(context) }

    // Create the callback
    val callback = remember {
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess(result)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errorCode, errString)
            }
        }
    }

    // Initialize the prompt
    return remember { BiometricPrompt(fragmentActivity, executor, callback) }
}

fun launchBiometric(
    prompt: BiometricPrompt,
    title: String = "Authentication Required",
    subtitle: String = "Verify your identity to punch in"
) {
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()

    prompt.authenticate(promptInfo)
}