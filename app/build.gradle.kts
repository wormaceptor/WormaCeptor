import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    alias(libs.plugins.play.publisher)
    alias(libs.plugins.ksp)
}

play {
    val serviceAccountPath = providers.environmentVariable("PLAY_STORE_SERVICE_ACCOUNT_JSON").orNull
    if (!serviceAccountPath.isNullOrBlank()) {
        serviceAccountCredentials.set(file(serviceAccountPath))
    }
    track.set("internal")
    defaultToAppBundles.set(true)
    releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.DRAFT)
    enabled.set(serviceAccountPath != null)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile.exists()) {
            load(keystorePropertiesFile.inputStream())
        }
    }

android {
    namespace = "com.azikar24.wormaceptorapp"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        val versionNameValue = libs.versions.versionName.get()
        val (major, minor, patch) =
            versionNameValue.split("-")
                .first()
                .split(".")
                .map { it.toInt() }
        versionCode = major * 100 + minor * 10 + patch
        versionName = versionNameValue
        multiDexEnabled = true
    }

    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProperties["storeFile"] as String?
            if (storeFilePath != null) {
                val keyFile = File(storeFilePath)
                if (keyFile.exists()) {
                    storeFile = keyFile
                    storePassword = keystoreProperties["storePassword"] as String?
                    keyAlias = keystoreProperties["keyAlias"] as String?
                    keyPassword = keystoreProperties["keyPassword"] as String?
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig =
                if (signingConfigs.getByName("release").storeFile != null) {
                    signingConfigs.getByName("release")
                } else {
                    signingConfigs.getByName("debug")
                }
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        resources {
            excludes += listOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.androidx.multidex)

    implementation(project(":api:client"))
    implementation(project(":common:presentation"))
    implementation(project(":core:ui")) // Used in main source set
    implementation(project(":api:impl:persistence"))
//    debugImplementation(project(":api:impl:no-op"))
//    debugImplementation(project(":api:impl:imdb"))

    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.compose)

    // Security crypto for EncryptedSharedPreferences testing
    implementation(libs.androidx.security.crypto)

    // Preferences for osmdroid configuration
    implementation(libs.androidx.preference.ktx)

    // Preferences DataStore, seeded so the Preferences inspector has a DataStore file to show
    implementation(libs.androidx.datastore.preferences)

    // osmdroid for location map
    implementation(libs.osmdroid.android)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Room — used by the demo to populate the database inspector
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // MCP device server (debug only — auto-starts HTTP server for AI agent access)
    debugImplementation(project(":mcp:device-server"))
}
