package util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.Node
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

data class AnimatedGif(
    val frames: List<ImageBitmap>,
    val delaysMs: List<Long>,
    val firstBufferedImage: BufferedImage? = null,
    val rawBytes: ByteArray? = null
)

object ImageLoader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // Capped static thumbnail cache (max 60 bitmaps)
    private val staticCache: LinkedHashMap<String, ImageBitmap> =
        object : LinkedHashMap<String, ImageBitmap>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?) = size > 60
        }

    // Capped animated GIF cache (max 10 active animated GIFs in memory)
    private val animCache: LinkedHashMap<String, AnimatedGif> =
        object : LinkedHashMap<String, AnimatedGif>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AnimatedGif>?) = size > 10
        }

    private val cacheLock = Any()

    suspend fun load(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        synchronized(cacheLock) {
            staticCache[url]?.let { return@withContext it }
            animCache[url]?.frames?.firstOrNull()?.let { return@withContext it }
        }

        try {
            val bytes = fetchBytes(url) ?: return@withContext null
            val buffered = ImageIO.read(ByteArrayInputStream(bytes)) ?: return@withContext null
            val scaled = downscaleIfNeeded(buffered, 240)
            val bitmap = scaled.toComposeImageBitmap()
            synchronized(cacheLock) { staticCache[url] = bitmap }
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    suspend fun loadAnimation(url: String): AnimatedGif? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        synchronized(cacheLock) { animCache[url] }?.let { return@withContext it }

        try {
            val bytes = fetchBytes(url) ?: return@withContext null
            val anim = decodeGif(bytes) ?: run {
                val buffered = ImageIO.read(ByteArrayInputStream(bytes)) ?: return@withContext null
                val scaled = downscaleIfNeeded(buffered, 240)
                val bitmap = scaled.toComposeImageBitmap()
                AnimatedGif(listOf(bitmap), listOf(100L), buffered, bytes)
            }

            synchronized(cacheLock) {
                animCache[url] = anim
                if (anim.frames.isNotEmpty()) {
                    staticCache[url] = anim.frames.first()
                }
            }
            anim
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        synchronized(cacheLock) { animCache[url]?.rawBytes }?.let { return@withContext it }
        fetchBytes(url)
    }

    private fun fetchBytes(url: String): ByteArray? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()
        return client.newCall(request).execute().use { it.body?.bytes() }
    }

    private fun decodeGif(bytes: ByteArray): AnimatedGif? {
        try {
            val stream = ImageIO.createImageInputStream(ByteArrayInputStream(bytes)) ?: return null
            val readers = ImageIO.getImageReadersByFormatName("gif")
            if (!readers.hasNext()) return null
            val reader = readers.next()
            reader.setInput(stream, false)
            val numFrames = reader.getNumImages(true)
            if (numFrames <= 0) return null

            // Limit preview animation to at most 24 frames for low memory & smooth playback
            val maxFrames = 24
            val step = (numFrames / maxFrames).coerceAtLeast(1)

            val frames = ArrayList<ImageBitmap>()
            val delays = ArrayList<Long>()
            var firstBufferedImage: BufferedImage? = null

            val firstImg = reader.read(0)
            firstBufferedImage = firstImg
            val origW = firstImg.width
            val origH = firstImg.height

            // Scale down preview frames to max 240px to conserve memory
            val maxDim = 240
            val scale = if (origW > maxDim || origH > maxDim) {
                maxDim.toDouble() / maxOf(origW, origH)
            } else 1.0
            val targetW = (origW * scale).toInt().coerceAtLeast(1)
            val targetH = (origH * scale).toInt().coerceAtLeast(1)

            val master = BufferedImage(origW, origH, BufferedImage.TYPE_INT_ARGB)
            val masterG = master.createGraphics()

            for (i in 0 until numFrames) {
                val frameImg = reader.read(i)
                var delayMs = 100L
                try {
                    val metadata = reader.getImageMetadata(i)
                    val tree = metadata.getAsTree("javax_imageio_gif_image_1.0")
                    if (tree is Node) {
                        val gce = findNode(tree, "GraphicControlExtension")
                        val delayStr = gce?.attributes?.getNamedItem("delayTime")?.nodeValue
                        if (!delayStr.isNullOrBlank()) {
                            val d = delayStr.toLongOrNull() ?: 10L
                            delayMs = (d * 10L).coerceIn(40L, 500L)
                        }
                    }
                } catch (_: Exception) { }

                masterG.drawImage(frameImg, 0, 0, null)

                if (i % step == 0 && frames.size < maxFrames) {
                    val scaled = BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB)
                    val sg = scaled.createGraphics()
                    sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                    sg.drawImage(master, 0, 0, targetW, targetH, null)
                    sg.dispose()

                    frames.add(scaled.toComposeImageBitmap())
                    delays.add(delayMs * step)
                }
            }
            masterG.dispose()

            return AnimatedGif(frames, delays, firstBufferedImage, bytes)
        } catch (_: Exception) {
            return null
        }
    }

    private fun downscaleIfNeeded(src: BufferedImage, maxDim: Int): BufferedImage {
        val w = src.width
        val h = src.height
        if (w <= maxDim && h <= maxDim) return src
        val scale = maxDim.toDouble() / maxOf(w, h)
        val tw = (w * scale).toInt().coerceAtLeast(1)
        val th = (h * scale).toInt().coerceAtLeast(1)
        val scaled = BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB)
        val g = scaled.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.drawImage(src, 0, 0, tw, th, null)
        g.dispose()
        return scaled
    }

    private fun findNode(root: Node, nodeName: String): Node? {
        if (root.nodeName.equals(nodeName, ignoreCase = true)) return root
        var child = root.firstChild
        while (child != null) {
            val found = findNode(child, nodeName)
            if (found != null) return found
            child = child.nextSibling
        }
        return null
    }
}
