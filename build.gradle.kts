plugins {
    alias(libs.plugins.android.application) apply false

    // --- DELETE THIS LINE IF IT EXISTS: ---
    // alias(libs.plugins.kotlin.android) apply false

    // --- KEEP THESE LINES (This forces the version we need): ---
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.24" apply false

    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}