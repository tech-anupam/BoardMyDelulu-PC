package data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── App-level model ─────────────────────────────────────────────────────────

@Serializable
data class GifItem(
    val id: String = "",
    val title: String = "",
    val previewUrl: String = "",   // fixed_height_small thumbnail (smaller, for grid)
    val fullUrl: String = "",      // original.gif (for save)
    val pageUrl: String = "",      // giphy.com / tenor.com page URL (for Share)
    val filter: GifFilter = GifFilter.GIF
)

@Serializable
enum class GifFilter(val label: String) {
    GIF("GIFs"),
    STICKER("Stickers"),
    FAVORITES("Favorites")
}

// ── Giphy API response models ────────────────────────────────────────────────

@Serializable
data class GiphyResponse(
    val data: List<GiphyGif> = emptyList()
)

@Serializable
data class GiphyGif(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val images: GiphyImages = GiphyImages()
)

@Serializable
data class GiphyImages(
    @SerialName("fixed_height_small") val fixedHeightSmall: GiphyImageData = GiphyImageData(),
    @SerialName("fixed_height")       val fixedHeight: GiphyImageData = GiphyImageData(),
    val original: GiphyImageData = GiphyImageData()
)

@Serializable
data class GiphyImageData(
    val url: String = "",
    val width: String = "0",
    val height: String = "0"
)
