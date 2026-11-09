/** Convention plugin for feature modules (Compose + Koin + lifecycle + collections + AutoService). */
plugins {
    id("wormaceptor.android.compose")
    id("com.google.devtools.ksp")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    "implementation"(libs.findLibrary("androidx-core-ktx").get())
    "implementation"(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
    "implementation"(libs.findLibrary("koin-compose").get())
    "implementation"(libs.findLibrary("kotlinx-collections-immutable").get())
    "ksp"(libs.findLibrary("auto-service-ksp").get())
    "compileOnly"(libs.findLibrary("auto-service-annotations").get())
}
