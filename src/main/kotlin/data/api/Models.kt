package data.api

import kotlinx.serialization.Serializable

@Serializable
data class Sound(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val mp3: String = ""
)

@Serializable
data class SoundDetail(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val mp3: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val views: Int = 0,
    val favorites: Int = 0,
    val uploader: Uploader = Uploader()
)

@Serializable
data class Uploader(
    val username: String = "",
    val profileUrl: String = ""
)

@Serializable
data class ApiResponse(
    val sounds: List<Sound> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1
)

data class Region(
    val code: String,
    val name: String
)

val regions = listOf(
    Region("global", "Global / All"),
    Region("in", "India"),
    Region("us", "United States"),
    Region("gb", "United Kingdom"),
    Region("br", "Brazil"),
    Region("de", "Germany"),
    Region("fr", "France"),
    Region("id", "Indonesia"),
    Region("mx", "Mexico"),
    Region("es", "Spain"),
    Region("it", "Italy"),
    Region("jp", "Japan"),
    Region("kr", "South Korea"),
    Region("au", "Australia"),
    Region("ca", "Canada"),
    Region("ru", "Russia")
)
