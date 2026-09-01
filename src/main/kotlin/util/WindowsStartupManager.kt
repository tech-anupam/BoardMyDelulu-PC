package util

import java.io.File

object WindowsStartupManager {
    private const val REG_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private const val APP_NAME = "BoardMyDelulu"

    fun isAutoStartEnabled(): Boolean {
        return try {
            val process = ProcessBuilder("reg", "query", REG_KEY, "/v", APP_NAME)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.contains(APP_NAME)
        } catch (_: Exception) {
            false
        }
    }

    fun setAutoStart(enable: Boolean): Boolean {
        return try {
            if (enable) {
                val exePath = getExecutablePath()
                val process = ProcessBuilder(
                    "reg", "add", REG_KEY, "/v", APP_NAME, "/t", "REG_SZ", "/d", "\"$exePath\"", "/f"
                ).start()
                process.waitFor() == 0
            } else {
                val process = ProcessBuilder(
                    "reg", "delete", REG_KEY, "/v", APP_NAME, "/f"
                ).start()
                process.waitFor() == 0
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun getExecutablePath(): String {
        return try {
            val currentCommand = ProcessHandle.current().info().command().orElse("")
            if (currentCommand.isNotBlank() && currentCommand.endsWith(".exe", ignoreCase = true) && !currentCommand.contains("java", ignoreCase = true)) {
                currentCommand
            } else {
                val userDir = System.getProperty("user.dir")
                val localExe = File(userDir, "BoardMyDelulu.exe")
                if (localExe.exists()) localExe.absolutePath else File(userDir, "gradlew.bat").absolutePath
            }
        } catch (_: Exception) {
            File(System.getProperty("user.dir"), "BoardMyDelulu.exe").absolutePath
        }
    }
}
