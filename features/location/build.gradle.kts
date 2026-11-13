plugins {
    id("wormaceptor.android.feature")
    id("wormaceptor.publishing")
}

android {
    namespace = "com.azikar24.wormaceptor.feature.location"
}

dependencies {
    implementation(project(":common:presentation"))
    implementation(project(":core:engine"))
    implementation(project(":core:ui"))
    implementation(project(":domain:contracts"))
    implementation(project(":domain:entities"))

    implementation(libs.androidx.navigation.compose)

    // Google Play Services Location for getting real location
    implementation("com.google.android.gms:play-services-location:21.2.0")
    // Coroutines support for Play Services Tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")
    // OpenStreetMap for map visualization
    implementation(libs.osmdroid.android)
}
