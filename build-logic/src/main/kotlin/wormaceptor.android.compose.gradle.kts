/** Convention plugin for Android library modules with Jetpack Compose. */
plugins {
    id("wormaceptor.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    buildFeatures {
        compose = true
    }
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    val bom = platform(libs.findLibrary("androidx-compose-bom").get())
    "implementation"(bom)
    "implementation"("androidx.compose.ui:ui")
    "implementation"("androidx.compose.ui:ui-tooling-preview")
    "implementation"("androidx.compose.material3:material3")
    "implementation"("androidx.compose.material:material-icons-extended")
    "debugImplementation"("androidx.compose.ui:ui-tooling")
}
