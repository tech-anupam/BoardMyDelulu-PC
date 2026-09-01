package data.repository

import data.api.Sound
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

object FavoritesRepository {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file: File by lazy {
        val dir = File(System.getProperty("user.home"), ".boardmydelulu")
        dir.mkdirs()
        File(dir, "favorites.json")
    }

    fun getAll(): List<Sound> {
        return try {
            if (!file.exists()) return emptyList()
            json.decodeFromString<List<Sound>>(file.readText())
        } catch (_: Exception) { emptyList() }
    }

    fun save(sounds: List<Sound>) {
        try { file.writeText(json.encodeToString(sounds)) } catch (_: Exception) { }
    }

    fun add(sound: Sound) {
        val list = getAll().toMutableList()
        if (list.none { it.id == sound.id }) {
            list.add(0, sound)
            save(list)
        }
    }

    fun remove(soundId: String) {
        save(getAll().filter { it.id != soundId })
    }

    fun isFavorite(soundId: String): Boolean = getAll().any { it.id == soundId }
}
