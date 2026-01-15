package com.technikh.employeeattendancetracking.utils

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var adminPassword: String
        get() = prefs.getString("admin_pass", "admin123") ?: "admin123"
        set(value) = prefs.edit().putString("admin_pass", value).apply()

    var isPasswordFeatureEnabled: Boolean
        get() = prefs.getBoolean("user_pass_enabled", false)
        set(value) = prefs.edit().putBoolean("user_pass_enabled", value).apply()

    var showCameraPreview: Boolean
        get() = prefs.getBoolean("show_camera_preview", false)
        set(value) = prefs.edit().putBoolean("show_camera_preview", value).apply()

    var maxHomeEmployees: Int
        get() = prefs.getInt("max_home_employees", 5)
        set(value) = prefs.edit().putInt("max_home_employees", value).apply()

    var allowSelfCorrection: Boolean
        get() = prefs.getBoolean("allow_self_correction", false)
        set(value) = prefs.edit().putBoolean("allow_self_correction", value).apply()
}