package data.repository

import data.api.GifItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import util.ImageLoader
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.net.URI

object GifRepository {
    private val saveDir: File by lazy {
        File(System.getProperty("user.home"), "Downloads/BoardMyDelulu/GIFs").also { it.mkdirs() }
    }

    private val tempGifDir: File by lazy {
        File(System.getProperty("user.home"), ".boardmydelulu/cache/gifs").also { it.mkdirs() }
    }

    /**
     * Download and save the GIF to ~/Downloads/BoardMyDelulu/GIFs/. Returns true on success.
     */
    suspend fun save(gif: GifItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val bytes = ImageLoader.getBytes(gif.fullUrl)
                ?: ImageLoader.getBytes(gif.previewUrl)
                ?: return@withContext false

            val safeName = gif.title
                .replace(Regex("[^a-zA-Z0-9 _-]"), "")
                .trim()
                .take(50)
                .ifBlank { gif.id }

            var file = File(saveDir, "$safeName.gif")
            var count = 1
            while (file.exists()) {
                file = File(saveDir, "$safeName ($count).gif")
                count++
            }
            file.writeBytes(bytes)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Copy GIF as an actual animated GIF file to the system clipboard.
     * By providing DataFlavor.javaFileListFlavor (and not static DIB imageFlavor),
     * chat apps like WhatsApp, Discord, Telegram, and Slack recognize the file
     * as an animated GIF instead of converting it to a static single-frame photo.
     */
    suspend fun copyGifImage(gif: GifItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val bytes = ImageLoader.getBytes(gif.fullUrl)
                ?: ImageLoader.getBytes(gif.previewUrl)
                ?: return@withContext false

            val safeId = gif.id.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "clip_${System.currentTimeMillis()}" }
            val tempFile = File(tempGifDir, "$safeId.gif")
            tempFile.writeBytes(bytes)

            // File-based transferable ensures WhatsApp/Discord/Telegram send as animated GIF
            val transferable = GifFileTransferable(tempFile)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(transferable, null)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Copy the GIF URL / link to the system clipboard.
     */
    fun copyUrlToClipboard(url: String): Boolean {
        return try {
            val sel = StringSelection(url)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(sel, sel)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Open the GIF / Tenor / Giphy page in the default browser.
     */
    fun openInBrowser(pageUrl: String) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(pageUrl))
            }
        } catch (_: Exception) { }
    }
}

/**
 * Transfers the GIF as a file on the clipboard so chat clients (WhatsApp, Discord, Telegram, Slack)
 * send it as an animated GIF rather than stripping the animation frames into a static bitmap.
 */
class GifFileTransferable(private val file: File) : Transferable {
    private val flavors = arrayOf(DataFlavor.javaFileListFlavor)

    override fun getTransferDataFlavors(): Array<DataFlavor> = flavors

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean =
        flavor.equals(DataFlavor.javaFileListFlavor)

    override fun getTransferData(flavor: DataFlavor): Any {
        if (flavor.equals(DataFlavor.javaFileListFlavor)) {
            return listOf(file)
        }
        throw UnsupportedFlavorException(flavor)
    }
}
