plugins {
    id("wormaceptor.android.library")
    id("wormaceptor.publishing")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.azikar24.wormaceptor.mcp.server"

    defaultConfig {
        buildConfigField("String", "VERSION_NAME", "\"${providers.gradleProperty("VERSION_NAME").get()}\"")
    }
    buildFeatures {
        buildConfig = true
    }
    // DTOs shared with :mcp:bridge, compiled into both modules (not a module or artifact of its own).
    sourceSets["main"].java.srcDir("../protocol/src/main/kotlin")
    packaging {
        resources {
            excludes += listOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
            )
        }
    }
}

dependencies {
    implementation(project(":core:engine"))
    implementation(project(":domain:entities"))
    implementation(project(":domain:contracts"))
    implementation(project(":features:preferences"))
    implementation(project(":features:database"))
    implementation(project(":features:filebrowser"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.koin.android)
    implementation(libs.kotlin.serialization)
    // replay_transaction; already on the host's debug classpath through core:engine.
    implementation(libs.okhttp)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.client.websockets)
}
