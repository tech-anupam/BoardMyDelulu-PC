import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

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

// ── Generate logo.ico from logo.png for MSI/EXE installer icon ───────────────
// MSI/EXE packaging requires a proper .ico file; PNG is silently ignored.
// This task generates a multi-resolution ICO (16x16, 32x32, 48x48, 256x256)
// by embedding PNG streams inside the ICO container format.
val generateIco by tasks.registering {
    val pngInput  = project.file("logo.png")
    val icoOutput = project.file("logo.ico")
    inputs.file(pngInput)
    outputs.file(icoOutput)

    doLast {
        val sizes = listOf(16, 32, 48, 256)
        val source = ImageIO.read(pngInput)

        // Render each size as PNG bytes (PNG entries inside ICO = best quality)
        val pngDatas: List<ByteArray> = sizes.map { size ->
            val scaled = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
            val g = scaled.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY)
            g.drawImage(source, 0, 0, size, size, null)
            g.dispose()
            val baos = ByteArrayOutputStream()
            ImageIO.write(scaled, "PNG", baos)
            baos.toByteArray()
        }

        // ICO format uses little-endian. Write helper functions:
        val out = ByteArrayOutputStream()
        fun writeShortLE(v: Int) { out.write(v and 0xFF); out.write((v shr 8) and 0xFF) }
        fun writeIntLE(v: Int)   {
            out.write(v and 0xFF); out.write((v shr 8) and 0xFF)
            out.write((v shr 16) and 0xFF); out.write((v shr 24) and 0xFF)
        }

        // ICONDIR header
        writeShortLE(0)           // Reserved
        writeShortLE(1)           // Type: 1 = icon
        writeShortLE(sizes.size)  // Number of images

        // ICONDIRENTRY per image (16 bytes each)
        var dataOffset = 6 + sizes.size * 16
        for (i in sizes.indices) {
            val s = sizes[i]
            out.write(if (s >= 256) 0 else s)   // Width  (0 means 256)
            out.write(if (s >= 256) 0 else s)   // Height (0 means 256)
            out.write(0)                         // Color count (0 = true-color)
            out.write(0)                         // Reserved
            writeShortLE(1)                      // Planes
            writeShortLE(32)                     // Bits per pixel
            writeIntLE(pngDatas[i].size)         // Size of image data
            writeIntLE(dataOffset)               // Offset to image data
            dataOffset += pngDatas[i].size
        }

        // Image data
        for (data in pngDatas) out.write(data)

        icoOutput.writeBytes(out.toByteArray())
        println("✓ Generated logo.ico with sizes ${sizes} (${icoOutput.length()} bytes)")
    }
}

// Ensure ICO is generated before any packaging task
afterEvaluate {
    listOf("packageMsi", "packageExe", "packageReleaseMsi", "packageReleaseExe",
           "packageDmg", "packageDeb", "packageRpm", "createDistributable", "createReleaseDistributable")
        .forEach { name -> tasks.findByName(name)?.dependsOn(generateIco) }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        jvmArgs += listOf(
            "-Xmx200m",                      // Cap heap — JVM+Skia baseline is ~80-120 MB
            "-XX:+UseG1GC",                  // G1 GC: shorter, predictable pauses
            "-XX:MaxGCPauseMillis=50",        // Target <=50 ms GC pauses
            "-XX:+UseStringDeduplication"    // De-duplicate string literals (requires G1)
        )
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "BoardMyDelulu"
            packageVersion = "1.0.0"
            description = "BoardMyDelulu - Meme Soundboard for PC"
            vendor = "Anupam Jha"
            windows {
                iconFile.set(project.file("logo.ico"))   // Fixed: was logo.png (ignored by MSI)
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
