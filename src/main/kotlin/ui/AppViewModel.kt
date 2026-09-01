package ui

import audio.AudioDeviceManager
import audio.AudioOutputDevice
import audio.SoundEngine
import data.api.Region
import data.api.Sound
import data.api.regions
import data.repository.DownloadRepository
import data.repository.FavoritesRepository
import data.repository.SoundRepository
import hotkey.DeckSettings
import hotkey.GlobalHotkeyManager
import hotkey.HotkeyBinding
import hotkey.HotkeyConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import util.Preferences
import util.WindowsStartupManager
import kotlin.random.Random

enum class HomeCategory(val label: String) {
    TRENDING("Trending"),
    RECENT("Recent"),
    BEST("Best of All Time")
}

enum class Screen {
    SOUNDS, SEARCH, DOWNLOADS, FAVORITES, SETTINGS
}

data class AppState(
    val currentScreen: Screen = Screen.SOUNDS,
    val homeCategory: HomeCategory = HomeCategory.TRENDING,
    val selectedRegion: Region = regions.first(),
    val homeSounds: List<Sound> = emptyList(),
    val isHomeLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val homePage: Int = 1,
    val searchQuery: String = "",
    val searchResults: List<Sound> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val searchPage: Int = 1,
    val favorites: List<Sound> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val downloads: List<Sound> = emptyList(),
    val hotkeys: Map<String, String> = emptyMap(), // soundId -> keyName
    val hotkeyBindings: List<HotkeyBinding> = emptyList(),
    val deckSettings: DeckSettings = DeckSettings(),
    val recordingSound: Sound? = null,
    val recordingDeckAction: String? = null, // "shuffle" or "cycle"
    val volume: Float = 1.0f,
    val availableOutputs: List<AudioOutputDevice> = emptyList(),
    val selectedOutputDevice: String = "Default Audio Device",
    val isAutoStartEnabled: Boolean = false,
    val isDarkTheme: Boolean = true
)

class AppViewModel {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var searchJob: Job? = null
    private var currentFavIndex = 0

    init {
        val prefs = Preferences.load()
        val region = regions.find { it.code == prefs.selectedRegion } ?: regions.first()
        val deck = HotkeyConfig.getDeckSettings()
        val outputs = AudioDeviceManager.getOutputDevices()
        val savedDevice = outputs.find { it.name == prefs.selectedOutputDevice } ?: outputs.first()
        val autoStart = WindowsStartupManager.isAutoStartEnabled()

        SoundEngine.setVolume(prefs.volume)
        SoundEngine.setOutputDevice(savedDevice.mixerInfo)

        _state.update {
            it.copy(
                isDarkTheme = prefs.isDarkTheme,
                selectedRegion = region,
                deckSettings = deck,
                volume = prefs.volume,
                availableOutputs = outputs,
                selectedOutputDevice = savedDevice.name,
                isAutoStartEnabled = autoStart
            )
        }

        loadFavorites()
        loadDownloads()
        loadHotkeys()
        loadHome()
        setupHotkeys()

        // Continuous Loop Listener
        SoundEngine.onPlaybackFinished = {
            if (_state.value.deckSettings.autoLoop) {
                scope.launch {
                    delay(300)
                    playNextFavorite()
                }
            }
        }
    }

    private fun setupHotkeys() {
        GlobalHotkeyManager.start(
            onKey = { keyCode ->
                val deck = _state.value.deckSettings
                when (keyCode) {
                    deck.shuffleKey -> {
                        playRandomFavorite()
                    }
                    deck.cycleKey -> {
                        playNextFavorite()
                    }
                    else -> {
                        val binding = HotkeyConfig.findByKeyCode(keyCode)
                        if (binding != null) {
                            SoundEngine.play(binding.soundId, binding.mp3Url)
                        }
                    }
                }
            },
            onPanic = { SoundEngine.stop() }
        )
    }

    fun toggleAutoStart() {
        val next = !_state.value.isAutoStartEnabled
        val success = WindowsStartupManager.setAutoStart(next)
        if (success || !next) {
            _state.update { it.copy(isAutoStartEnabled = next) }
        }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        SoundEngine.setVolume(clamped)
        _state.update { it.copy(volume = clamped) }
        Preferences.save(Preferences.load().copy(volume = clamped))
    }

    fun setOutputDevice(device: AudioOutputDevice) {
        SoundEngine.setOutputDevice(device.mixerInfo)
        _state.update { it.copy(selectedOutputDevice = device.name) }
        Preferences.save(Preferences.load().copy(selectedOutputDevice = device.name))
    }

    fun refreshAudioDevices() {
        val outputs = AudioDeviceManager.getOutputDevices()
        _state.update { it.copy(availableOutputs = outputs) }
    }

    fun playRandomFavorite() {
        val list = if (_state.value.favorites.isNotEmpty()) _state.value.favorites else _state.value.downloads
        if (list.isEmpty()) return
        val randomIndex = Random.nextInt(list.size)
        val sound = list[randomIndex]
        SoundEngine.play(sound.id, sound.mp3)
    }

    fun playNextFavorite() {
        val list = if (_state.value.favorites.isNotEmpty()) _state.value.favorites else _state.value.downloads
        if (list.isEmpty()) return
        currentFavIndex = (currentFavIndex + 1) % list.size
        val sound = list[currentFavIndex]
        SoundEngine.play(sound.id, sound.mp3)
    }

    fun startRecordingSoundKey(sound: Sound) {
        _state.update { it.copy(recordingSound = sound, recordingDeckAction = null) }
        GlobalHotkeyManager.captureNextKey { keyCode ->
            bindHotkey(sound, keyCode)
            _state.update { it.copy(recordingSound = null) }
        }
    }

    fun startRecordingDeckKey(action: String) { // "shuffle" or "cycle"
        _state.update { it.copy(recordingDeckAction = action, recordingSound = null) }
        GlobalHotkeyManager.captureNextKey { keyCode ->
            val current = _state.value.deckSettings
            val updated = when (action) {
                "shuffle" -> current.copy(shuffleKey = keyCode)
                "cycle" -> current.copy(cycleKey = keyCode)
                else -> current
            }
            HotkeyConfig.saveDeckSettings(updated)
            _state.update { it.copy(deckSettings = updated, recordingDeckAction = null) }
        }
    }

    fun cancelRecording() {
        GlobalHotkeyManager.cancelCapture()
        _state.update { it.copy(recordingSound = null, recordingDeckAction = null) }
    }

    fun toggleAutoLoop() {
        val current = _state.value.deckSettings
        val updated = current.copy(autoLoop = !current.autoLoop)
        HotkeyConfig.saveDeckSettings(updated)
        _state.update { it.copy(deckSettings = updated) }
    }

    fun loadHotkeys() {
        val bindings = HotkeyConfig.getBindings()
        val map = bindings.associate { it.soundId to HotkeyConfig.keyName(it.keyCode) }
        _state.update { it.copy(hotkeys = map, hotkeyBindings = bindings) }
    }

    fun bindHotkey(sound: Sound, keyCode: Int) {
        val binding = HotkeyBinding(
            keyCode = keyCode,
            soundId = sound.id,
            soundTitle = sound.title,
            mp3Url = sound.mp3
        )
        HotkeyConfig.addBinding(binding)
        loadHotkeys()
    }

    fun removeHotkey(keyCode: Int) {
        HotkeyConfig.removeBinding(keyCode)
        loadHotkeys()
    }

    fun removeHotkeyBySoundId(soundId: String) {
        HotkeyConfig.removeBindingBySoundId(soundId)
        loadHotkeys()
    }

    fun navigateTo(screen: Screen) {
        _state.update { it.copy(currentScreen = screen) }
    }

    fun loadHome() {
        _state.update { it.copy(isHomeLoading = true, homePage = 1) }
        scope.launch {
            val sounds = when (_state.value.homeCategory) {
                HomeCategory.TRENDING -> SoundRepository.getTrending(_state.value.selectedRegion.code)
                HomeCategory.RECENT -> SoundRepository.getRecent(_state.value.selectedRegion.code)
                HomeCategory.BEST -> SoundRepository.getBest(_state.value.selectedRegion.code)
            }
            _state.update { it.copy(homeSounds = sounds, isHomeLoading = false) }
        }
    }

    fun loadMoreHome() {
        if (_state.value.isLoadingMore) return
        _state.update { it.copy(isLoadingMore = true) }
        val nextPage = _state.value.homePage + 1
        scope.launch {
            val sounds = when (_state.value.homeCategory) {
                HomeCategory.TRENDING -> SoundRepository.getTrending(_state.value.selectedRegion.code, nextPage)
                HomeCategory.RECENT -> SoundRepository.getRecent(_state.value.selectedRegion.code, nextPage)
                HomeCategory.BEST -> SoundRepository.getBest(_state.value.selectedRegion.code, nextPage)
            }
            _state.update {
                it.copy(
                    homeSounds = it.homeSounds + sounds,
                    homePage = nextPage,
                    isLoadingMore = false
                )
            }
        }
    }

    fun switchCategory(cat: HomeCategory) {
        _state.update { it.copy(homeCategory = cat, homeSounds = emptyList()) }
        loadHome()
    }

    fun changeRegion(region: Region) {
        _state.update { it.copy(selectedRegion = region, homeSounds = emptyList()) }
        Preferences.save(Preferences.load().copy(selectedRegion = region.code))
        loadHome()
    }

    fun updateSearch(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList(), hasSearched = false, searchPage = 1) }
            return
        }
        searchJob = scope.launch {
            delay(400)
            _state.update { it.copy(isSearching = true, searchPage = 1) }
            val results = SoundRepository.search(query)
            _state.update { it.copy(searchResults = results, isSearching = false, hasSearched = true) }
        }
    }

    fun loadMoreSearch() {
        if (_state.value.isSearching || _state.value.searchQuery.isBlank()) return
        val nextPage = _state.value.searchPage + 1
        scope.launch {
            _state.update { it.copy(isSearching = true) }
            val results = SoundRepository.search(_state.value.searchQuery, nextPage)
            _state.update {
                it.copy(
                    searchResults = it.searchResults + results,
                    searchPage = nextPage,
                    isSearching = false
                )
            }
        }
    }

    fun toggleFavorite(sound: Sound) {
        if (FavoritesRepository.isFavorite(sound.id)) {
            FavoritesRepository.remove(sound.id)
            removeHotkeyBySoundId(sound.id)
        } else {
            FavoritesRepository.add(sound)
        }
        loadFavorites()
    }

    private fun loadFavorites() {
        val favs = FavoritesRepository.getAll()
        _state.update { it.copy(favorites = favs, favoriteIds = favs.map { s -> s.id }.toSet()) }
    }

    fun loadDownloads() {
        _state.update { it.copy(downloads = DownloadRepository.getAll()) }
    }

    fun downloadSound(sound: Sound) {
        scope.launch {
            DownloadRepository.download(sound)
            loadDownloads()
        }
    }

    fun deleteDownload(soundId: String) {
        DownloadRepository.delete(soundId)
        loadDownloads()
    }

    fun toggleTheme() {
        val newDark = !_state.value.isDarkTheme
        _state.update { it.copy(isDarkTheme = newDark) }
        Preferences.save(Preferences.load().copy(isDarkTheme = newDark))
    }

    fun cleanup() {
        GlobalHotkeyManager.stop()
        SoundEngine.stop()
        scope.cancel()
    }
}
