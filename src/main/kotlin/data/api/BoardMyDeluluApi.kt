package data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object BoardMyDeluluApi {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private const val PRIMARY_URL = "https://boardmydelulu-api.vercel.app"
    private const val FALLBACK_URL = "https://myinstants-api.vercel.app"

    private fun resolveRegion(region: String): String =
        if (region.isBlank() || region.equals("global", ignoreCase = true) || region.equals("all", ignoreCase = true)) "us" else region.lowercase()

    suspend fun getTrending(region: String = "in", page: Int = 1): List<Sound> = withContext(Dispatchers.IO) {
        val r = resolveRegion(region)
        fetchFromAny("$PRIMARY_URL/trending/$r?page=$page", "$FALLBACK_URL/trending/$r?page=$page")
    }

    suspend fun getRecent(region: String = "in", page: Int = 1): List<Sound> = withContext(Dispatchers.IO) {
        fetchFromAny("$PRIMARY_URL/recent?page=$page", "$FALLBACK_URL/recent?page=$page")
    }

    suspend fun getBest(region: String = "in", page: Int = 1): List<Sound> = withContext(Dispatchers.IO) {
        val r = resolveRegion(region)
        fetchFromAny("$PRIMARY_URL/best/$r?page=$page", "$FALLBACK_URL/best/$r?page=$page")
    }

    suspend fun search(query: String, page: Int = 1): List<Sound> = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        fetchFromAny("$PRIMARY_URL/search/$encoded?page=$page", "$FALLBACK_URL/search/$encoded?page=$page")
    }

    private fun fetchFromAny(primaryUrl: String, fallbackUrl: String): List<Sound> {
        val first = fetch(primaryUrl)
        if (first.isNotEmpty()) return first
        return fetch(fallbackUrl)
    }

    private fun fetch(url: String): List<Sound> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                try {
                    val apiResponse = json.decodeFromString<ApiResponse>(body)
                    apiResponse.sounds
                } catch (_: Exception) {
                    try {
                        json.decodeFromString<List<Sound>>(body)
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
