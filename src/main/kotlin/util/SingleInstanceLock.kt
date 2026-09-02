package util

import java.net.BindException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

object SingleInstanceLock {
    private const val PORT = 61432
    private var serverSocket: ServerSocket? = null
    private val running = AtomicBoolean(false)

    @Volatile
    private var onWakeupCallback: (() -> Unit)? = null

    /**
     * Attempt to acquire the single-instance lock by binding a ServerSocket on [PORT].
     * - Returns `true`  -> this is the first instance; proceed normally.
     * - Returns `false` -> another instance is already running; a wakeup signal has
     *                     been sent to it. The caller should exit immediately.
     */
    fun tryAcquire(): Boolean {
        return try {
            serverSocket = ServerSocket(PORT)
            startListeningInternal()
            true
        } catch (_: BindException) {
            // Port already taken -> another instance is live. Ping it to restore its window.
            try {
                Socket("127.0.0.1", PORT).use { s ->
                    s.getOutputStream().write(1)
                    s.getOutputStream().flush()
                }
            } catch (_: Exception) { }
            false
        } catch (_: Exception) {
            true // Unexpected error — let this instance run
        }
    }

    private fun startListeningInternal() {
        val server = serverSocket ?: return
        if (!running.compareAndSet(false, true)) return
        thread(isDaemon = true, name = "SingleInstance-Listener") {
            while (running.get()) {
                try {
                    server.accept().use { socket ->
                        socket.getInputStream().read()
                        onWakeupCallback?.invoke()
                    }
                } catch (_: SocketException) {
                    break
                } catch (_: Exception) {
                    if (!running.get()) break
                    try { Thread.sleep(200) } catch (_: Exception) { }
                }
            }
        }
    }

    fun setWakeupListener(onWakeup: () -> Unit) {
        onWakeupCallback = onWakeup
    }

    fun release() {
        running.set(false)
        try { serverSocket?.close() } catch (_: Exception) { }
        serverSocket = null
    }
}
