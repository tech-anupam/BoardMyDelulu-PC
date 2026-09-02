package data.scraper

import data.api.GifFilter
import data.api.GifItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@Serializable
private data class TenorResponse(
    val results: List<TenorResult> = emptyList(),
    val next: String = ""
)

@Serializable
private data class TenorResult(
    val id: String = "",
    val title: String = "",
    val content_description: String = "",
    val itemurl: String = "",
    val media_formats: TenorMediaFormats = TenorMediaFormats()
)

@Serializable
private data class TenorMediaFormats(
    val tinygif: TenorMedia = TenorMedia(),
    val nanogif: TenorMedia = TenorMedia(),
    val gif: TenorMedia = TenorMedia(),
    val mediumgif: TenorMedia = TenorMedia()
)

@Serializable
private data class TenorMedia(
    val url: String = ""
)

object GifScraper {
    private const val API_KEY = "AIzaSyCZt6SSh5VgVPzD9fhyzG1DprdPRhtoaR4"
    private const val CLIENT_KEY = "tenor_web"
    private const val BASE_URL = "https://tenor.googleapis.com/v2"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun search(query: String, filter: GifFilter, offset: Int = 0): List<GifItem> =
        withContext(Dispatchers.IO) {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val filterParam = if (filter == GifFilter.STICKER) "&searchfilter=sticker" else ""
            val posParam = if (offset > 0) "&pos=$offset" else ""
            val url = "$BASE_URL/search?q=$encoded&key=$API_KEY&client_key=$CLIENT_KEY&limit=30$filterParam$posParam"
            fetch(url, filter)
        }

    suspend fun trending(filter: GifFilter, offset: Int = 0): List<GifItem> =
        withContext(Dispatchers.IO) {
            val filterParam = if (filter == GifFilter.STICKER) "&searchfilter=sticker" else ""
            val posParam = if (offset > 0) "&pos=$offset" else ""
            val endpoint = if (filter == GifFilter.STICKER) "search?q=sticker" else "featured?"
            val url = "$BASE_URL/$endpoint&key=$API_KEY&client_key=$CLIENT_KEY&limit=30$filterParam$posParam"
            fetch(url, filter)
        }

    private fun fetch(url: String, filter: GifFilter): List<GifItem> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val parsed = json.decodeFromString<TenorResponse>(body)

                parsed.results.mapNotNull { item ->
                    val preview = item.media_formats.tinygif.url
                        .ifBlank { item.media_formats.nanogif.url }
                        .ifBlank { item.media_formats.gif.url }

                    val full = item.media_formats.gif.url
                        .ifBlank { item.media_formats.mediumgif.url }
                        .ifBlank { preview }

                    if (preview.isBlank() && full.isBlank()) return@mapNotNull null

                    val rawTitle = item.title.ifBlank { item.content_description }.ifBlank { "GIF" }
                    val cleanTitle = rawTitle.take(60).trim()

                    GifItem(
                        id = item.id.ifBlank { preview.hashCode().toString() },
                        title = cleanTitle,
                        previewUrl = preview,
                        fullUrl = full,
                        pageUrl = item.itemurl.ifBlank { "https://tenor.com" },
                        filter = filter
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
