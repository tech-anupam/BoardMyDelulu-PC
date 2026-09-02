package ui

import audio.AudioDeviceManager
import audio.AudioOutputDevice
import audio.SoundEngine
import data.api.GifFilter
import data.api.GifItem
import data.api.Region
import data.api.Sound
import data.api.regions
import data.repository.DownloadRepository
import data.repository.FavoritesRepository
import data.repository.GifFavoritesRepository
import data.repository.GifRepository
import data.repository.SoundRepository
import data.scraper.GifScraper
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
    SOUNDS, SEARCH, GIFS, DOWNLOADS, FAVORITES, SETTINGS
}

data class AppState(
    // Core
    val currentScreen: Screen = Screen.SOUNDS,
    val isDarkTheme: Boolean = true,
    val isInitializing: Boolean = true,     // true while JNativeHook + audio devices init in background

    // Home / Sounds
    val homeCategory: HomeCategory = HomeCategory.TRENDING,
    val selectedRegion: Region = regions.first(),
    val homeSounds: List<Sound> = emptyList(),
    val isHomeLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val homePage: Int = 1,

    // Search
    val searchQuery: String = "",
    val searchResults: List<Sound> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val searchPage: Int = 1,

    // GIFs & Stickers
    val gifFilter: GifFilter = GifFilter.GIF,
    val gifQuery: String = "",
    val gifResults: List<GifItem> = emptyList(),
    val isGifSearching: Boolean = false,
    val hasGifSearched: Boolean = false,
    val gifOffset: Int = 0,
    val gifSaveStatus: String? = null,
    val gifFavorites: List<GifItem> = emptyList(),
    val gifFavoriteIds: Set<String> = emptySet(),

    // Favorites / Downloads
    val favorites: List<Sound> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val downloads: List<Sound> = emptyList(),

    // Hotkeys
    val hotkeys: Map<String, String> = emptyMap(),
    val hotkeyBindings: List<HotkeyBinding> = emptyList(),
    val deckSettings: DeckSettings = DeckSettings(),
    val recordingSound: Sound? = null,
    val recordingDeckAction: String? = null,

    // Audio
    val volume: Float = 1.0f,
    val availableOutputs: List<AudioOutputDevice> = emptyList(),
    val selectedOutputDevice: String = "Default Audio Device",

    // System
    val isAutoStartEnabled: Boolean = false
)

class AppViewModel {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var searchJob: Job? = null
    private var gifSearchJob: Job? = null
    private var currentFavIndex = 0

    init {
        // ── Phase 1: Fast synchronous init — completes in <10 ms, window shows immediately ──
        val prefs  = Preferences.load()
        val region = regions.find { it.code == prefs.selectedRegion } ?: regions.first()
        val deck   = HotkeyConfig.getDeckSettings()
        SoundEngine.setVolume(prefs.volume)

        _state.update {
            it.copy(
                isDarkTheme    = prefs.isDarkTheme,
                selectedRegion = region,
                deckSettings   = deck,
                volume         = prefs.volume
            )
        }

        // Local file reads (no network, no native hooks)
        loadFavorites()
        loadGifFavorites()
        loadDownloads()
        loadHotkeys()

        // ── Phase 2: Heavy async init — window is fully visible while this runs ──
        scope.launch(Dispatchers.IO) {
            val outputs     = AudioDeviceManager.getOutputDevices()
            val savedDevice = outputs.find { it.name == prefs.selectedOutputDevice } ?: outputs.first()
            SoundEngine.setOutputDevice(savedDevice.mixerInfo)
            val autoStart = WindowsStartupManager.isAutoStartEnabled()

            _state.update {
                it.copy(
                    availableOutputs     = outputs,
                    selectedOutputDevice = savedDevice.name,
                    isAutoStartEnabled   = autoStart
                )
            }

            // JNativeHook registration on background thread
            setupHotkeys()

            _state.update { it.copy(isInitializing = false) }
        }

        // Home feed (network, fully async)
        loadHome()

        // Auto-loop listener
        SoundEngine.onPlaybackFinished = {
            if (_state.value.deckSettings.autoLoop) {
                scope.launch { delay(300); playNextFavorite() }
            }
        }
    }

    private fun setupHotkeys() {
        GlobalHotkeyManager.start(
            onKey = { keyCode ->
                val deck = _state.value.deckSettings
                when (keyCode) {
                    deck.shuffleKey -> playRandomFavorite()
                    deck.cycleKey   -> playNextFavorite()
                    else -> {
                        val binding = HotkeyConfig.findByKeyCode(keyCode)
                        if (binding != null) SoundEngine.play(binding.soundId, binding.mp3Url)
                    }
                }
            },
            onPanic = { SoundEngine.stop() }
        )
    }

    // ── Audio ─────────────────────────────────────────────────────────────────

    private var volumePreviewJob: Job? = null

    fun setVolume(vol: Float, playFeedback: Boolean = true) {
        val v = vol.coerceIn(0f, 1f)
        SoundEngine.setVolume(v)
        _state.update { it.copy(volume = v) }
        Preferences.save(Preferences.load().copy(volume = v))

        if (playFeedback && v > 0f) {
            volumePreviewJob?.cancel()
            volumePreviewJob = scope.launch {
                delay(70)
                SoundEngine.playVolumeTest()
            }
        }
    }

    fun setOutputDevice(device: AudioOutputDevice) {
        SoundEngine.setOutputDevice(device.mixerInfo)
        _state.update { it.copy(selectedOutputDevice = device.name) }
        Preferences.save(Preferences.load().copy(selectedOutputDevice = device.name))
    }

    fun refreshAudioDevices() {
        scope.launch(Dispatchers.IO) {
            val outputs = AudioDeviceManager.getOutputDevices()
            _state.update { it.copy(availableOutputs = outputs) }
        }
    }

    // ── Deck ──────────────────────────────────────────────────────────────────

    fun playRandomFavorite() {
        val list = if (_state.value.favorites.isNotEmpty()) _state.value.favorites else _state.value.downloads
        if (list.isEmpty()) return
        val sound = list[Random.nextInt(list.size)]
        SoundEngine.play(sound.id, sound.mp3)
    }

    fun playNextFavorite() {
        val list = if (_state.value.favorites.isNotEmpty()) _state.value.favorites else _state.value.downloads
        if (list.isEmpty()) return
        currentFavIndex = (currentFavIndex + 1) % list.size
        val sound = list[currentFavIndex]
        SoundEngine.play(sound.id, sound.mp3)
    }

    fun toggleAutoLoop() {
        val updated = _state.value.deckSettings.copy(autoLoop = !_state.value.deckSettings.autoLoop)
        HotkeyConfig.saveDeckSettings(updated)
        _state.update { it.copy(deckSettings = updated) }
    }

    fun toggleAutoStart() {
        val next = !_state.value.isAutoStartEnabled
        val success = WindowsStartupManager.setAutoStart(next)
        if (success || !next) _state.update { it.copy(isAutoStartEnabled = next) }
    }

    // ── Hotkeys ───────────────────────────────────────────────────────────────

    fun loadHotkeys() {
        val bindings = HotkeyConfig.getBindings()
        _state.update {
            it.copy(
                hotkeys        = bindings.associate { b -> b.soundId to HotkeyConfig.keyName(b.keyCode) },
                hotkeyBindings = bindings
            )
        }
    }

    fun bindHotkey(sound: Sound, keyCode: Int) {
        HotkeyConfig.addBinding(HotkeyBinding(keyCode = keyCode, soundId = sound.id, soundTitle = sound.title, mp3Url = sound.mp3))
        loadHotkeys()
    }

    fun removeHotkey(keyCode: Int) { HotkeyConfig.removeBinding(keyCode); loadHotkeys() }

    fun removeHotkeyBySoundId(soundId: String) { HotkeyConfig.removeBindingBySoundId(soundId); loadHotkeys() }

    fun startRecordingSoundKey(sound: Sound) {
        _state.update { it.copy(recordingSound = sound, recordingDeckAction = null) }
        GlobalHotkeyManager.captureNextKey { keyCode ->
            bindHotkey(sound, keyCode)
            _state.update { it.copy(recordingSound = null) }
        }
    }

    fun startRecordingDeckKey(action: String) {
        _state.update { it.copy(recordingDeckAction = action, recordingSound = null) }
        GlobalHotkeyManager.captureNextKey { keyCode ->
            val current = _state.value.deckSettings
            val updated = when (action) {
                "shuffle" -> current.copy(shuffleKey = keyCode)
                "cycle"   -> current.copy(cycleKey   = keyCode)
                else      -> current
            }
            HotkeyConfig.saveDeckSettings(updated)
            _state.update { it.copy(deckSettings = updated, recordingDeckAction = null) }
        }
    }

    fun cancelRecording() {
        GlobalHotkeyManager.cancelCapture()
        _state.update { it.copy(recordingSound = null, recordingDeckAction = null) }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun navigateTo(screen: Screen) { _state.update { it.copy(currentScreen = screen) } }

    // ── Home / Sounds ─────────────────────────────────────────────────────────

    fun loadHome() {
        _state.update { it.copy(isHomeLoading = true, homePage = 1) }
        scope.launch {
            val sounds = when (_state.value.homeCategory) {
                HomeCategory.TRENDING -> SoundRepository.getTrending(_state.value.selectedRegion.code)
                HomeCategory.RECENT   -> SoundRepository.getRecent(_state.value.selectedRegion.code)
                HomeCategory.BEST     -> SoundRepository.getBest(_state.value.selectedRegion.code)
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
                HomeCategory.RECENT   -> SoundRepository.getRecent(_state.value.selectedRegion.code, nextPage)
                HomeCategory.BEST     -> SoundRepository.getBest(_state.value.selectedRegion.code, nextPage)
            }
            _state.update { it.copy(homeSounds = it.homeSounds + sounds, homePage = nextPage, isLoadingMore = false) }
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

    // ── Sound Search ──────────────────────────────────────────────────────────

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
            _state.update { it.copy(searchResults = it.searchResults + results, searchPage = nextPage, isSearching = false) }
        }
    }

    // ── GIFs & Stickers ───────────────────────────────────────────────────────

    fun initGifsScreen() {
        if (_state.value.gifResults.isEmpty() && !_state.value.isGifSearching) {
            if (_state.value.gifFilter == GifFilter.FAVORITES) {
                _state.update { it.copy(gifResults = it.gifFavorites) }
            } else {
                loadTrendingGifs()
            }
        }
    }

    private fun loadTrendingGifs() {
        if (_state.value.gifFilter == GifFilter.FAVORITES) {
            _state.update { it.copy(gifResults = it.gifFavorites, isGifSearching = false) }
            return
        }
        scope.launch {
            _state.update { it.copy(isGifSearching = true) }
            val results = GifScraper.trending(_state.value.gifFilter, 0)
            _state.update { it.copy(gifResults = results, isGifSearching = false, gifOffset = results.size) }
        }
    }

    fun updateGifSearch(query: String) {
        _state.update { it.copy(gifQuery = query) }
        gifSearchJob?.cancel()

        if (_state.value.gifFilter == GifFilter.FAVORITES) {
            val favs = _state.value.gifFavorites
            val filtered = if (query.isBlank()) favs else favs.filter { it.title.contains(query, ignoreCase = true) }
            _state.update { it.copy(gifResults = filtered, hasGifSearched = query.isNotBlank()) }
            return
        }

        if (query.isBlank()) {
            _state.update { it.copy(gifResults = emptyList(), hasGifSearched = false, gifOffset = 0) }
            loadTrendingGifs()
            return
        }

        gifSearchJob = scope.launch {
            delay(400)
            _state.update { it.copy(isGifSearching = true, gifOffset = 0) }
            val results = GifScraper.search(query, _state.value.gifFilter, 0)
            _state.update { it.copy(gifResults = results, isGifSearching = false, hasGifSearched = true, gifOffset = results.size) }
        }
    }

    fun loadMoreGifs() {
        val s = _state.value
        if (s.isGifSearching || s.gifFilter == GifFilter.FAVORITES) return
        val offset = s.gifOffset
        scope.launch {
            _state.update { it.copy(isGifSearching = true) }
            val results = if (s.gifQuery.isBlank()) GifScraper.trending(s.gifFilter, offset)
                          else GifScraper.search(s.gifQuery, s.gifFilter, offset)
            _state.update { it.copy(gifResults = it.gifResults + results, isGifSearching = false, gifOffset = offset + results.size) }
        }
    }

    fun setGifFilter(filter: GifFilter) {
        _state.update { it.copy(gifFilter = filter, gifResults = emptyList(), gifOffset = 0, hasGifSearched = false) }
        gifSearchJob?.cancel()

        if (filter == GifFilter.FAVORITES) {
            val favs = _state.value.gifFavorites
            val query = _state.value.gifQuery
            val filtered = if (query.isBlank()) favs else favs.filter { it.title.contains(query, ignoreCase = true) }
            _state.update { it.copy(gifResults = filtered, hasGifSearched = query.isNotBlank()) }
            return
        }

        val query = _state.value.gifQuery
        if (query.isBlank()) {
            loadTrendingGifs()
        } else {
            gifSearchJob = scope.launch {
                _state.update { it.copy(isGifSearching = true) }
                val results = GifScraper.search(query, filter, 0)
                _state.update { it.copy(gifResults = results, isGifSearching = false, hasGifSearched = true, gifOffset = results.size) }
            }
        }
    }

    fun toggleGifFavorite(gif: GifItem) {
        if (GifFavoritesRepository.isFavorite(gif.id)) {
            GifFavoritesRepository.remove(gif.id)
        } else {
            GifFavoritesRepository.add(gif)
        }
        loadGifFavorites()
        if (_state.value.gifFilter == GifFilter.FAVORITES) {
            val favs = GifFavoritesRepository.getAll()
            val query = _state.value.gifQuery
            val filtered = if (query.isBlank()) favs else favs.filter { it.title.contains(query, ignoreCase = true) }
            _state.update { it.copy(gifResults = filtered) }
        }
    }

    private fun loadGifFavorites() {
        val favs = GifFavoritesRepository.getAll()
        _state.update { it.copy(gifFavorites = favs, gifFavoriteIds = favs.map { g -> g.id }.toSet()) }
    }

    fun saveGif(gif: GifItem) {
        scope.launch {
            val ok = GifRepository.save(gif)
            _state.update { it.copy(gifSaveStatus = if (ok) "Saved to Downloads/BoardMyDelulu/GIFs/" else "Failed to save GIF") }
            delay(3000)
            _state.update { it.copy(gifSaveStatus = null) }
        }
    }

    fun copyGifImage(gif: GifItem) {
        scope.launch {
            val ok = GifRepository.copyGifImage(gif)
            _state.update {
                it.copy(gifSaveStatus = if (ok) "Copied GIF to clipboard (Ready to paste in Discord/WhatsApp)!" else "Failed to copy GIF")
            }
            delay(3000)
            _state.update { it.copy(gifSaveStatus = null) }
        }
    }

    fun copyGifUrl(gif: GifItem) {
        val ok = GifRepository.copyUrlToClipboard(gif.fullUrl)
        _state.update {
            it.copy(gifSaveStatus = if (ok) "Copied GIF link to clipboard!" else "Failed to copy link")
        }
        scope.launch {
            delay(3000)
            _state.update { it.copy(gifSaveStatus = null) }
        }
    }

    fun shareGif(gif: GifItem) = GifRepository.openInBrowser(gif.pageUrl)

    // ── Favorites / Downloads ─────────────────────────────────────────────────

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

    fun loadDownloads() { _state.update { it.copy(downloads = DownloadRepository.getAll()) } }

    fun downloadSound(sound: Sound) {
        scope.launch { DownloadRepository.download(sound); loadDownloads() }
    }

    fun deleteDownload(soundId: String) { DownloadRepository.delete(soundId); loadDownloads() }

    // ── Theme ─────────────────────────────────────────────────────────────────

    fun toggleTheme() {
        val newDark = !_state.value.isDarkTheme
        _state.update { it.copy(isDarkTheme = newDark) }
        Preferences.save(Preferences.load().copy(isDarkTheme = newDark))
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    fun cleanup() {
        scope.launch(Dispatchers.IO) { GlobalHotkeyManager.stop() }
        SoundEngine.stop()
        scope.cancel()
    }
}
