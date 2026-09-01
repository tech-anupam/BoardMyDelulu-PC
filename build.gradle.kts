import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.20"
    id("org.jetbrains.compose") version "1.8.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
}

group = "dev.boardmydelulu"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

    implementation("com.github.kwhat:jnativehook:2.2.2")

    implementation("org.jsoup:jsoup:1.18.3")

    implementation("javazoom:jlayer:1.0.1")
}

compose.desktop {
    application {
        mainClass = "MainKt"
        jvmArgs += listOf(
            "-Dskiko.renderApi=DIRECT3D"
        )
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "BoardMyDelulu"
            packageVersion = "1.0.0"
            description = "BoardMyDelulu - Meme Soundboard for PC"
            vendor = "Anupam Jha"
            windows {
                iconFile.set(project.file("logo.png"))
                menuGroup = "BoardMyDelulu"
                shortcut = true
                dirChooser = true
                perUserInstall = true
            }
        }
        buildTypes.release {
            proguard {
                isEnabled = false
            }
        }
    }
}

kotlin {
    jvmToolchain(17)
}
