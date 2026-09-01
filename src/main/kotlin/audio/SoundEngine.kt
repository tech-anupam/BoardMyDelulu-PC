package audio

import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.SampleBuffer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URL
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.Mixer
import javax.sound.sampled.SourceDataLine

object SoundEngine {
    private val _playingId = MutableStateFlow<String?>(null)
    val playingId: StateFlow<String?> = _playingId
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var activeLine: SourceDataLine? = null
    private var activeBitstream: Bitstream? = null
    private var currentJob: Job? = null

    @Volatile
    var currentVolume: Float = 1.0f
        private set

    @Volatile
    var currentMixerInfo: Mixer.Info? = null
        private set

    var onPlaybackFinished: (() -> Unit)? = null
    private var isUserStopped = false

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
    }

    fun setOutputDevice(mixerInfo: Mixer.Info?) {
        currentMixerInfo = mixerInfo
    }

    fun play(soundId: String, mp3Url: String) {
        if (_playingId.value == soundId) {
            stop()
            return
        }
        stop()
        isUserStopped = false
        _playingId.value = soundId
        currentJob = scope.launch {
            try {
                val url = URL(mp3Url)
                val connection = url.openConnection()
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.connectTimeout = 6000
                connection.readTimeout = 12000
                val stream = BufferedInputStream(connection.getInputStream(), 65536)
                streamPcm(stream, soundId)
            } catch (e: Exception) {
                _playingId.value = null
            }
        }
    }

    fun playLocal(soundId: String, filePath: String) {
        if (_playingId.value == soundId) {
            stop()
            return
        }
        stop()
        isUserStopped = false
        _playingId.value = soundId
        currentJob = scope.launch {
            try {
                val file = File(filePath)
                val stream = BufferedInputStream(FileInputStream(file), 65536)
                streamPcm(stream, soundId)
            } catch (e: Exception) {
                _playingId.value = null
            }
        }
    }

    private fun streamPcm(inputStream: InputStream, soundId: String) {
        var line: SourceDataLine? = null
        var bitstream: Bitstream? = null
        try {
            bitstream = Bitstream(inputStream)
            activeBitstream = bitstream
            val decoder = Decoder()

            var header = bitstream.readFrame()
            while (header != null && _playingId.value == soundId && !isUserStopped) {
                val sampleBuffer = decoder.decodeFrame(header, bitstream) as SampleBuffer
                val channels = sampleBuffer.channelCount
                val sampleRate = sampleBuffer.sampleFrequency

                if (line == null) {
                    val format = AudioFormat(sampleRate.toFloat(), 16, channels, true, false)
                    val info = DataLine.Info(SourceDataLine::class.java, format)
                    val mixerInfo = currentMixerInfo
                    val l = if (mixerInfo != null) {
                        AudioSystem.getMixer(mixerInfo).getLine(info) as SourceDataLine
                    } else {
                        AudioSystem.getLine(info) as SourceDataLine
                    }
                    l.open(format, 32768)
                    l.start()
                    line = l
                    activeLine = l
                }

                val samples = sampleBuffer.buffer
                val length = sampleBuffer.bufferLength
                val vol = currentVolume
                val byteBuf = ByteArray(length * 2)

                var bIdx = 0
                for (i in 0 until length) {
                    val sample = samples[i]
                    val scaled = (sample * vol).toInt().coerceIn(-32768, 32767).toShort()
                    byteBuf[bIdx++] = (scaled.toInt() and 0xFF).toByte()
                    byteBuf[bIdx++] = ((scaled.toInt() shr 8) and 0xFF).toByte()
                }

                line.write(byteBuf, 0, byteBuf.size)
                bitstream.closeFrame()
                header = bitstream.readFrame()
            }

            if (!isUserStopped && _playingId.value == soundId) {
                line?.drain()
                _playingId.value = null
                onPlaybackFinished?.invoke()
            }
        } catch (_: Exception) {
        } finally {
            try { line?.stop(); line?.close() } catch (_: Exception) { }
            try { bitstream?.close() } catch (_: Exception) { }
            if (activeLine == line) activeLine = null
            if (activeBitstream == bitstream) activeBitstream = null
            if (_playingId.value == soundId) _playingId.value = null
        }
    }

    fun stop() {
        isUserStopped = true
        try {
            activeLine?.stop()
            activeLine?.close()
        } catch (_: Exception) { }
        try {
            activeBitstream?.close()
        } catch (_: Exception) { }
        currentJob?.cancel()
        activeLine = null
        activeBitstream = null
        currentJob = null
        _playingId.value = null
    }
}
