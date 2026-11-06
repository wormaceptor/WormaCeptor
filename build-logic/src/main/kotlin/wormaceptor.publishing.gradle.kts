/**
 * Convention plugin for modules published to Maven Central.
 *
 * Each consuming module must set `POM_ARTIFACT_ID` and `POM_DESCRIPTION` in its
 * module-level `gradle.properties`. Root `gradle.properties` provides `GROUP`
 * and `VERSION_NAME`.
 *
 * Credentials (`mavenCentralUsername`, `mavenCentralPassword`, `signingInMemoryKey*`)
 * live in `~/.gradle/gradle.properties` locally and in CI env vars with the
 * `ORG_GRADLE_PROJECT_` prefix.
 *
 * Javadoc strategy: AGP 8.6.0's bundled Dokka uses ASM8, which crashes on
 * sealed classes compiled with JVM 17 (PermittedSubclasses attribute). We
 * disable AGP's javadoc generation and attach an empty javadoc.jar instead —
 * Maven Central validates the artifact exists but does not inspect contents.
 * Revisit when AGP bumps to a version with ASM9+ Dokka.
 */
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()

    pom {
        name.set(project.findProperty("POM_ARTIFACT_ID") as String? ?: project.name)
        description.set(
            project.findProperty("POM_DESCRIPTION") as String?
                ?: "WormaCeptor – advanced Android debugging toolkit",
        )
        url.set("https://github.com/wormaceptor/WormaCeptor")
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("azikar24")
                name.set("Abdulaziz Karam")
                url.set("https://github.com/AziKar24")
            }
        }
        scm {
            url.set("https://github.com/wormaceptor/WormaCeptor")
            connection.set("scm:git:git://github.com/wormaceptor/WormaCeptor.git")
            developerConnection.set("scm:git:ssh://git@github.com/wormaceptor/WormaCeptor.git")
        }
        issueManagement {
            system.set("GitHub Issues")
            url.set("https://github.com/wormaceptor/WormaCeptor/issues")
        }
    }
}

plugins.withId("com.android.library") {
    mavenPublishing {
        configure(
            AndroidSingleVariantLibrary(
                variant = "release",
                sourcesJar = true,
                publishJavadocJar = false,
            ),
        )
    }

    val emptyJavadocJar =
        tasks.register<Jar>("emptyJavadocJar") {
            archiveClassifier.set("javadoc")
        }

    afterEvaluate {
        extensions.configure<PublishingExtension> {
            publications.withType<MavenPublication>().configureEach {
                artifact(emptyJavadocJar)
            }
        }
    }
}

plugins.withId("org.jetbrains.kotlin.jvm") {
    mavenPublishing {
        configure(
            KotlinJvm(
                javadocJar = JavadocJar.Empty(),
                sourcesJar = true,
            ),
        )
    }
}
