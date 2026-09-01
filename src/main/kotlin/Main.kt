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
import java.awt.Dimension
import java.util.logging.Level
import java.util.logging.Logger

fun main() {
    // Suppress verbose third-party loggers
    Logger.getLogger("okhttp3.OkHttpClient").level = Level.OFF
    Logger.getLogger("org.jetbrains.skiko").level = Level.OFF

    application {
        val viewModel = remember { AppViewModel() }
        val trayState = rememberTrayState()
        var isVisible by remember { mutableStateOf(true) }
        val windowState = rememberWindowState(
            size = DpSize(1150.dp, 760.dp),
            position = WindowPosition(Alignment.Center)
        )

        val appIcon = remember {
            try {
                BitmapPainter(useResource("logo.png") { loadImageBitmap(it) })
            } catch (_: Exception) {
                null
            }
        }

        if (appIcon != null) {
            Tray(
                state = trayState,
                icon = appIcon,
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
                    Item("Stop All Sounds (Esc)") {
                        audio.SoundEngine.stop()
                    }
                    Separator()
                    Item("Exit") {
                        viewModel.cleanup()
                        exitApplication()
                    }
                }
            )
        }

        Window(
            onCloseRequest = {
                viewModel.cleanup()
                exitApplication()
            },
            visible = isVisible,
            state = windowState,
            title = "BoardMyDelulu - The Ultimate Meme Soundboard",
            icon = appIcon
        ) {
            window.minimumSize = Dimension(820, 580)
            App(viewModel)
        }
    }
}
