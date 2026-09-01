package hotkey

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import java.util.logging.Level
import java.util.logging.Logger

typealias HotkeyCallback = (Int) -> Unit

object GlobalHotkeyManager : NativeKeyListener {
    private var callback: HotkeyCallback? = null
    private var panicKey: Int = NativeKeyEvent.VC_ESCAPE
    private var panicCallback: (() -> Unit)? = null
    private var captureCallback: ((Int) -> Unit)? = null
    private var isRegistered = false

    fun start(onKey: HotkeyCallback, onPanic: () -> Unit) {
        callback = onKey
        panicCallback = onPanic
        if (!isRegistered) {
            try {
                val logger = Logger.getLogger(GlobalScreen::class.java.`package`.name)
                logger.level = Level.OFF
                logger.useParentHandlers = false
                GlobalScreen.registerNativeHook()
                GlobalScreen.addNativeKeyListener(this)
                isRegistered = true
            } catch (_: Exception) { }
        }
    }

    fun captureNextKey(onCaptured: (Int) -> Unit) {
        captureCallback = onCaptured
    }

    fun cancelCapture() {
        captureCallback = null
    }

    fun stop() {
        try {
            if (isRegistered) {
                GlobalScreen.removeNativeKeyListener(this)
                GlobalScreen.unregisterNativeHook()
                isRegistered = false
            }
        } catch (_: Exception) { }
    }

    override fun nativeKeyPressed(e: NativeKeyEvent) {
        val capturer = captureCallback
        if (capturer != null) {
            captureCallback = null
            capturer(e.keyCode)
            return
        }

        if (e.keyCode == panicKey) {
            panicCallback?.invoke()
        } else {
            callback?.invoke(e.keyCode)
        }
    }

    override fun nativeKeyReleased(e: NativeKeyEvent) { }
    override fun nativeKeyTyped(e: NativeKeyEvent) { }
}
