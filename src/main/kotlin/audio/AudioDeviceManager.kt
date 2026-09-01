package audio

import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Mixer
import javax.sound.sampled.SourceDataLine

data class AudioOutputDevice(
    val name: String,
    val description: String,
    val mixerInfo: Mixer.Info?
)

object AudioDeviceManager {
    fun getOutputDevices(): List<AudioOutputDevice> {
        val list = mutableListOf<AudioOutputDevice>()
        list.add(AudioOutputDevice("Default Audio Device", "Primary Sound Driver", null))

        try {
            val mixers = AudioSystem.getMixerInfo()
            for (info in mixers) {
                try {
                    val mixer = AudioSystem.getMixer(info)
                    val lineInfo = mixer.sourceLineInfo
                    if (lineInfo.isNotEmpty()) {
                        val isPlayback = lineInfo.any {
                            it.lineClass == SourceDataLine::class.java || it.toString().contains("SourceDataLine")
                        }
                        if (isPlayback) {
                            list.add(AudioOutputDevice(info.name, info.description, info))
                        }
                    }
                } catch (_: Exception) { }
            }
        } catch (_: Exception) { }

        return list.distinctBy { it.name }
    }

    fun findMixerByName(name: String): Mixer.Info? {
        if (name == "Default Audio Device" || name.isBlank()) return null
        return getOutputDevices().find { it.name.equals(name, ignoreCase = true) }?.mixerInfo
    }
}
