package util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

@Serializable
data class AppPreferences(
    val isDarkTheme: Boolean = true,
    val selectedRegion: String = "global",
    val volume: Float = 1.0f,
    val selectedOutputDevice: String = "Default Audio Device"
)

object Preferences {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file: File by lazy {
        val dir = File(System.getProperty("user.home"), ".boardmydelulu")
        dir.mkdirs()
        File(dir, "preferences.json")
    }

    fun load(): AppPreferences {
        return try {
            if (!file.exists()) return AppPreferences()
            json.decodeFromString<AppPreferences>(file.readText())
        } catch (_: Exception) { AppPreferences() }
    }

    fun save(prefs: AppPreferences) {
        try { file.writeText(json.encodeToString(prefs)) } catch (_: Exception) { }
    }
}
