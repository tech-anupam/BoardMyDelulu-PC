import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import ui.App
import ui.AppViewModel
import util.SingleInstanceLock
import java.awt.Dimension
import java.awt.Frame
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.SwingUtilities

fun main() {
    // ── Single-instance check ────────────────────────────────────────────────
    // If another instance is already running, send it a wakeup signal and exit.
    // The running instance will bring its window to front upon receiving the signal.
    if (!SingleInstanceLock.tryAcquire()) {
        println("BoardMyDelulu is already running. Signal sent to existing instance.")
        return
    }

    Logger.getLogger("okhttp3.OkHttpClient").level = Level.OFF
    Logger.getLogger("org.jetbrains.skiko").level = Level.OFF

    application {
        val viewModel = remember { AppViewModel() }
        val trayState = rememberTrayState()
        var isVisible by remember { mutableStateOf(true) }
        val windowState = rememberWindowState(
            size     = DpSize(1150.dp, 760.dp),
            position = WindowPosition(Alignment.Center)
        )

        // Load app icon
        val appIcon = remember {
            try { BitmapPainter(useResource("logo.png") { loadImageBitmap(it) }) }
            catch (_: Exception) { null }
        }

        // ── Tray icon ────────────────────────────────────────────────────────
        if (appIcon != null) {
            Tray(
                state   = trayState,
                icon    = appIcon,
                tooltip = "BoardMyDelulu - Meme Soundboard",
                onAction = {
                    isVisible = true
                    windowState.isMinimized = false
                },
                menu = {
                    Item("Open BoardMyDelulu") {
                        isVisible = true
                        windowState.isMinimized = false
                    }
                    Separator()
                    Item("Stop All Sounds (Esc)") { audio.SoundEngine.stop() }
                    Separator()
                    Item("Exit") {
                        SingleInstanceLock.release()
                        viewModel.cleanup()
                        exitApplication()
                    }
                }
            )
        }

        // ── Main window ──────────────────────────────────────────────────────
        Window(
            onCloseRequest = {
                SingleInstanceLock.release()
                viewModel.cleanup()
                exitApplication()
            },
            visible = isVisible,
            state   = windowState,
            title   = "BoardMyDelulu - The Ultimate Meme Soundboard",
            icon    = appIcon
        ) {
            window.minimumSize = Dimension(820, 580)

            // Register window reference for wakeup signals from secondary instance launches
            DisposableEffect(window) {
                val awtWindow = window
                fun bringToForeground() {
                    isVisible = true
                    windowState.isMinimized = false
                    if (awtWindow is Frame && awtWindow.extendedState == Frame.ICONIFIED) {
                        awtWindow.extendedState = Frame.NORMAL
                    }
                    try {
                        awtWindow.toFront()
                        awtWindow.requestFocus()
                        awtWindow.isAlwaysOnTop = true
                        awtWindow.isAlwaysOnTop = false
                    } catch (_: Exception) { }
                }

                SwingUtilities.invokeLater {
                    bringToForeground()
                }

                SingleInstanceLock.setWakeupListener {
                    SwingUtilities.invokeLater {
                        bringToForeground()
                    }
                }
                onDispose {
                    SingleInstanceLock.setWakeupListener { }
                }
            }

            App(viewModel)
        }
    }
}
