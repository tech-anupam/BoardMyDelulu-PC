package data.repository

import data.api.GifItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object GifFavoritesRepository {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file: File by lazy {
        val dir = File(System.getProperty("user.home"), ".boardmydelulu")
        dir.mkdirs()
        File(dir, "gif_favorites.json")
    }

    fun getAll(): List<GifItem> {
        return try {
            if (!file.exists()) return emptyList()
            json.decodeFromString<List<GifItem>>(file.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(gifs: List<GifItem>) {
        try {
            file.writeText(json.encodeToString(gifs))
        } catch (_: Exception) { }
    }

    fun add(gif: GifItem) {
        val list = getAll().toMutableList()
        if (list.none { it.id == gif.id }) {
            list.add(0, gif)
            save(list)
        }
    }

    fun remove(gifId: String) {
        save(getAll().filter { it.id != gifId })
    }

    fun isFavorite(gifId: String): Boolean = getAll().any { it.id == gifId }
}
