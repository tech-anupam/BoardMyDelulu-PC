package data.scraper

import data.api.Sound
import data.api.SoundDetail
import data.api.Uploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object LocalScraper {
    private const val BASE = "https://www.myinstants.com"
    private const val UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

    private suspend fun fetchAndParse(url: String): List<Sound> = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url).userAgent(UA).timeout(10000).followRedirects(true).get()
            val sounds = mutableListOf<Sound>()
            doc.select("div.instant").forEach { el ->
                val link = el.selectFirst("a.instant-link") ?: return@forEach
                val title = link.text().trim()
                val href = link.attr("href")
                val soundUrl = BASE + href
                val id = href.removePrefix("/en/instant/").trimEnd('/')
                val btn = el.selectFirst("button.small-button")
                val onclick = btn?.attr("onclick") ?: ""
                val match = Regex("play\\('(.*?)'").find(onclick)
                if (match != null) {
                    val mp3Path = match.groupValues[1]
                    val mp3Url = if (mp3Path.startsWith("http")) mp3Path else BASE + mp3Path
                    sounds.add(Sound(id = id, title = title, url = soundUrl, mp3 = mp3Url))
                }
            }
            sounds
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun resolveRegion(region: String): String =
        if (region.isBlank() || region.equals("global", ignoreCase = true) || region.equals("all", ignoreCase = true)) "us" else region.lowercase()

    suspend fun getTrending(region: String, page: Int = 1): List<Sound> {
        val r = resolveRegion(region)
        val pageParam = if (page > 1) "?page=$page" else ""
        return fetchAndParse("$BASE/en/index/$r/$pageParam")
    }

    suspend fun search(query: String, page: Int = 1): List<Sound> {
        val pageParam = if (page > 1) "&page=$page" else ""
        return fetchAndParse("$BASE/en/search/?name=${java.net.URLEncoder.encode(query, "UTF-8")}$pageParam")
    }

    suspend fun getRecent(page: Int = 1): List<Sound> {
        val pageParam = if (page > 1) "?page=$page" else ""
        return fetchAndParse("$BASE/en/recent/$pageParam")
    }

    suspend fun getBest(region: String, page: Int = 1): List<Sound> {
        val r = resolveRegion(region)
        val pageParam = if (page > 1) "?page=$page" else ""
        return fetchAndParse("$BASE/en/best_of_all_time/$r/$pageParam")
    }
}
