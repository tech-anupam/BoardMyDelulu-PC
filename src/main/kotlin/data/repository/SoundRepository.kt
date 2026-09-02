package data.repository

import data.api.BoardMyDeluluApi
import data.api.Sound
import data.scraper.LocalScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Collections

object SoundRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val cacheDir by lazy {
        val dir = File(System.getProperty("user.home"), ".boardmydelulu/cache")
        dir.mkdirs()
        dir
    }

    private const val CACHE_TTL_MS = 10 * 60 * 1000L // 10 minutes TTL
    private const val MAX_CACHE_SIZE = 20             // LRU cap — prevents unbounded growth

    private data class CacheEntry(
        val sounds: List<Sound>,
        val timestamp: Long = System.currentTimeMillis()
    )

    // Thread-safe LRU cache: evicts least-recently-used entry when size > MAX_CACHE_SIZE
    private val memoryCache: MutableMap<String, CacheEntry> = Collections.synchronizedMap(
        object : LinkedHashMap<String, CacheEntry>(32, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?) =
                size > MAX_CACHE_SIZE
        }
    )

    private fun getFromCache(key: String): List<Sound>? {
        // Check Memory Cache
        val mem = memoryCache[key]
        if (mem != null && (System.currentTimeMillis() - mem.timestamp) < CACHE_TTL_MS) {
            return mem.sounds
        }

        // Check Disk Cache
        try {
            val safeKey = key.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(cacheDir, "$safeKey.json")
            if (file.exists()) {
                val sounds = json.decodeFromString<List<Sound>>(file.readText())
                if (sounds.isNotEmpty()) {
                    memoryCache[key] = CacheEntry(sounds, file.lastModified())
                    return sounds
                }
            }
        } catch (_: Exception) { }
        return null
    }

    private fun saveToCache(key: String, sounds: List<Sound>) {
        if (sounds.isEmpty()) return
        memoryCache[key] = CacheEntry(sounds)
        try {
            val safeKey = key.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(cacheDir, "$safeKey.json")
            file.writeText(json.encodeToString(sounds))
        } catch (_: Exception) { }
    }

    suspend fun getTrending(region: String, page: Int = 1): List<Sound> = withContext(Dispatchers.IO) {
        val key = "trending_${region.lowercase()}_$page"
        val cached = getFromCache(key)
        if (cached != null && cached.isNotEmpty()) return@withContext cached

        val fromApi = try { BoardMyDeluluApi.getTrending(region, page) } catch (_: Exception) { emptyList() }
        if (fromApi.isNotEmpty()) {
            saveToCache(key, fromApi)
            return@withContext fromApi
        }

        val fromScraper = try { LocalScraper.getTrending(region, page) } catch (_: Exception) { emptyList() }
        if (fromScraper.isNotEmpty()) {
            saveToCache(key, fromScraper)
            return@withContext fromScraper
        }

        cached ?: emptyList()
    }

    suspend fun getRecent(region: String, page: Int = 1): List<Sound> = withContext(Dispatchers.IO) {
        val key = "recent_${region.lowercase()}_$page"
        val cached = getFromCache(key)
        if (cached != null && cached.isNotEmpty()) return@withContext cached

        val fromApi = try { BoardMyDeluluApi.getRecent(region, page) } catch (_: Exception) { emptyList() }
        if (fromApi.isNotEmpty()) {
            saveToCache(key, fromApi)
            return@withContext fromApi
        }

        val fromScraper = try { LocalScraper.getRecent(page) } catch (_: Exception) { emptyList() }
        if (fromScraper.isNotEmpty()) {
            saveToCache(key, fromScraper)
            return@withContext fromScraper
        }

        cached ?: emptyList()
    }

    suspend fun getBest(region: String, page: Int = 1): List<Sound> = withContext(Dispatchers.IO) {
        val key = "best_${region.lowercase()}_$page"
        val cached = getFromCache(key)
        if (cached != null && cached.isNotEmpty()) return@withContext cached

        val fromApi = try { BoardMyDeluluApi.getBest(region, page) } catch (_: Exception) { emptyList() }
        if (fromApi.isNotEmpty()) {
            saveToCache(key, fromApi)
            return@withContext fromApi
        }

        val fromScraper = try { LocalScraper.getBest(region, page) } catch (_: Exception) { emptyList() }
        if (fromScraper.isNotEmpty()) {
            saveToCache(key, fromScraper)
            return@withContext fromScraper
        }

        cached ?: emptyList()
    }

    suspend fun search(query: String, page: Int = 1): List<Sound> = withContext(Dispatchers.IO) {
        val key = "search_${query.lowercase().trim()}_$page"
        val cached = getFromCache(key)
        if (cached != null && cached.isNotEmpty()) return@withContext cached

        val fromApi = try { BoardMyDeluluApi.search(query, page) } catch (_: Exception) { emptyList() }
        if (fromApi.isNotEmpty()) {
            saveToCache(key, fromApi)
            return@withContext fromApi
        }

        val fromScraper = try { LocalScraper.search(query, page) } catch (_: Exception) { emptyList() }
        if (fromScraper.isNotEmpty()) {
            saveToCache(key, fromScraper)
            return@withContext fromScraper
        }

        cached ?: emptyList()
    }
}
