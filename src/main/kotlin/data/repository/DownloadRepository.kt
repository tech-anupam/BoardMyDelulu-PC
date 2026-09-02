package data.repository

import data.api.Sound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object DownloadRepository {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val downloadDir: File by lazy {
        val dir = File(System.getProperty("user.home"), "BoardMyDelulu")
        dir.mkdirs()
        dir
    }

    private val metaFile: File by lazy {
        val dir = File(System.getProperty("user.home"), ".boardmydelulu")
        dir.mkdirs()
        File(dir, "downloads.json")
    }

    fun getAll(): List<Sound> {
        return try {
            if (!metaFile.exists()) return emptyList()
            json.decodeFromString<List<Sound>>(metaFile.readText())
        } catch (_: Exception) { emptyList() }
    }

    private fun saveMeta(sounds: List<Sound>) {
        try { metaFile.writeText(json.encodeToString(sounds)) } catch (_: Exception) { }
    }

    suspend fun download(sound: Sound): Boolean = withContext(Dispatchers.IO) {
        try {
            val sanitized = sound.title.replace(Regex("[^a-zA-Z0-9 ]"), "").trim().ifBlank { "sound" }
            val destFile = File(downloadDir, "$sanitized.mp3")
            if (destFile.exists()) return@withContext true

            val request = Request.Builder()
                .url(sound.mp3)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
                }
            }

            val list = getAll().toMutableList()
            if (list.none { it.id == sound.id }) {
                list.add(0, sound)
                saveMeta(list)
            }
            true
        } catch (_: Exception) { false }
    }

    fun delete(soundId: String) {
        val sounds = getAll()
        val sound = sounds.find { it.id == soundId } ?: return
        val sanitized = sound.title.replace(Regex("[^a-zA-Z0-9 ]"), "").trim().ifBlank { "sound" }
        File(downloadDir, "$sanitized.mp3").delete()
        saveMeta(sounds.filter { it.id != soundId })
    }

    fun getLocalFile(sound: Sound): File? {
        val sanitized = sound.title.replace(Regex("[^a-zA-Z0-9 ]"), "").trim().ifBlank { "sound" }
        val file = File(downloadDir, "$sanitized.mp3")
        return if (file.exists()) file else null
    }

    fun openFolder() {
        try {
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                java.awt.Desktop.getDesktop().open(downloadDir)
            }
        } catch (_: Exception) { }
    }

    fun showInExplorer(file: File?) {
        try {
            if (file != null && file.exists()) {
                val os = System.getProperty("os.name", "").lowercase()
                if (os.contains("win")) {
                    ProcessBuilder("explorer.exe", "/select,", file.absolutePath).start()
                } else if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                    java.awt.Desktop.getDesktop().open(file.parentFile ?: downloadDir)
                }
            } else {
                openFolder()
            }
        } catch (_: Exception) {
            openFolder()
        }
    }
}
