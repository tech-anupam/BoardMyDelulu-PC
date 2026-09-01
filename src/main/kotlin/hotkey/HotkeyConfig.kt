package hotkey

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

@Serializable
data class HotkeyBinding(
    val keyCode: Int,
    val soundId: String,
    val soundTitle: String,
    val mp3Url: String
)

@Serializable
data class DeckSettings(
    val shuffleKey: Int = NativeKeyEvent.VC_F8,
    val cycleKey: Int = NativeKeyEvent.VC_F9,
    val autoLoop: Boolean = false
)

object HotkeyConfig {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val dir by lazy {
        val d = File(System.getProperty("user.home"), ".boardmydelulu")
        d.mkdirs()
        d
    }
    private val file: File by lazy { File(dir, "hotkeys.json") }
    private val deckFile: File by lazy { File(dir, "deck_settings.json") }

    fun getBindings(): List<HotkeyBinding> {
        return try {
            if (!file.exists()) return emptyList()
            json.decodeFromString<List<HotkeyBinding>>(file.readText())
        } catch (_: Exception) { emptyList() }
    }

    fun saveBindings(bindings: List<HotkeyBinding>) {
        try { file.writeText(json.encodeToString(bindings)) } catch (_: Exception) { }
    }

    fun addBinding(binding: HotkeyBinding) {
        val list = getBindings().toMutableList()
        list.removeAll { it.keyCode == binding.keyCode || it.soundId == binding.soundId }
        list.add(binding)
        saveBindings(list)
    }

    fun removeBinding(keyCode: Int) {
        saveBindings(getBindings().filter { it.keyCode != keyCode })
    }

    fun removeBindingBySoundId(soundId: String) {
        saveBindings(getBindings().filter { it.soundId != soundId })
    }

    fun findByKeyCode(keyCode: Int): HotkeyBinding? = getBindings().find { it.keyCode == keyCode }

    fun getDeckSettings(): DeckSettings {
        return try {
            if (!deckFile.exists()) return DeckSettings()
            json.decodeFromString<DeckSettings>(deckFile.readText())
        } catch (_: Exception) { DeckSettings() }
    }

    fun saveDeckSettings(settings: DeckSettings) {
        try { deckFile.writeText(json.encodeToString(settings)) } catch (_: Exception) { }
    }

    fun keyName(keyCode: Int): String = try {
        val text = NativeKeyEvent.getKeyText(keyCode)
        if (text.isNullOrBlank() || text.startsWith("Unknown")) "Key ($keyCode)" else text
    } catch (_: Exception) { "Key ($keyCode)" }
}
