package ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import audio.SoundEngine
import data.api.Sound
import ui.AppViewModel
import ui.component.SoundPad

private data class SfxCategory(val name: String, val query: String, val color: Color, val icon: ImageVector)

private val sfxCategories = listOf(
    SfxCategory("Memes", "meme", Color(0xFF6C5CE7), Icons.Filled.EmojiEmotions),
    SfxCategory("Music", "music beat", Color(0xFFFF6B9D), Icons.Filled.Headphones),
    SfxCategory("Funny", "funny laugh", Color(0xFFFFA502), Icons.Filled.Celebration),
    SfxCategory("Animals", "animal", Color(0xFF55E6C1), Icons.Filled.Pets),
    SfxCategory("Games", "game sound", Color(0xFF48DBFB), Icons.Filled.SportsEsports),
    SfxCategory("Movies", "movie", Color(0xFFA29BFE), Icons.Filled.Movie),
    SfxCategory("Horror", "horror scary", Color(0xFFFF7675), Icons.Filled.Nightlight),
    SfxCategory("Nature", "nature rain", Color(0xFF00D2D3), Icons.Filled.Park),
    SfxCategory("Anime", "anime", Color(0xFFF368E0), Icons.Filled.AutoAwesome),
    SfxCategory("Bollywood", "bollywood", Color(0xFFFF9F43), Icons.Filled.LiveTv),
    SfxCategory("SFX", "sound effect", Color(0xFF636E72), Icons.AutoMirrored.Filled.VolumeUp),
    SfxCategory("Alerts", "notification alert", Color(0xFFE17055), Icons.Filled.NotificationsActive)
)

private val quickTags = listOf(
    "vine boom", "bruh", "oof", "rizz", "emotional damage", "amogus",
    "npc", "gta", "cricket", "bollywood", "sigma", "fart", "meme",
    "sad", "laugh", "wow", "slay", "skibidi"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsState()
    val playingId by SoundEngine.playingId.collectAsState()
    val gridState = rememberLazyGridState()
    var rightClickSound by remember { mutableStateOf<Sound?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }

    val shouldLoadMore by remember {
        derivedStateOf {
            val total = gridState.layoutInfo.totalItemsCount
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && last >= total - 6
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.isSearching && state.searchResults.isNotEmpty()) {
            viewModel.loadMoreSearch()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Search",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            if (playingId != null) {
                FilledTonalButton(
                    onClick = { SoundEngine.stop() },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.Stop, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Stop (Esc)", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.updateSearch(it) },
            placeholder = { Text("Search sounds, memes, voices...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearch("") }) {
                        Icon(Icons.Filled.Close, "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(14.dp))

        when {
            state.isSearching && state.searchResults.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 2.5.dp)
                }
            }
            !state.hasSearched && state.searchQuery.isEmpty() -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "quick_tags_header") {
                        Text("Quick Search", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item(span = { GridItemSpan(maxLineSpan) }, key = "quick_tags") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            quickTags.forEach { term ->
                                AssistChip(onClick = { viewModel.updateSearch(term) }, label = { Text(term) }, shape = RoundedCornerShape(20.dp))
                            }
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }, key = "cat_header") {
                        Spacer(Modifier.height(16.dp))
                        Text("Browse by Category", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(sfxCategories, key = { "cat_${it.name}" }) { cat ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = cat.color),
                            modifier = Modifier.height(80.dp).clickable { viewModel.updateSearch(cat.query) }
                        ) {
                            Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(cat.icon, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text(cat.name, style = MaterialTheme.typography.labelSmall, color = Color.White, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
            state.hasSearched && state.searchResults.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No sounds found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(state.searchResults, key = { idx, s -> "search_${s.id}_$idx" }) { index, sound ->
                        SoundPad(
                            sound = sound,
                            index = index,
                            isPlaying = playingId == sound.id,
                            isFavorite = state.favoriteIds.contains(sound.id),
                            hotkeyName = state.hotkeys[sound.id],
                            onTap = { SoundEngine.play(sound.id, sound.mp3) },
                            onRightClick = {
                                rightClickSound = sound
                                showContextMenu = true
                            }
                        )
                    }
                    if (state.searchResults.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "load_more_search") {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                if (state.isSearching) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                else Button(onClick = { viewModel.loadMoreSearch() }, shape = RoundedCornerShape(14.dp)) { Text("Load More") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showContextMenu && rightClickSound != null) {
        val sound = rightClickSound!!
        val boundKey = state.hotkeys[sound.id]
        val isFav = state.favoriteIds.contains(sound.id)

        AlertDialog(
            onDismissRequest = { showContextMenu = false },
            title = { Text(sound.title, style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            SoundEngine.play(sound.id, sound.mp3)
                            showContextMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Play Sound", modifier = Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = {
                            viewModel.toggleFavorite(sound)
                            showContextMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isFav) "Remove from Favorites" else "Add to Favorites (Deck)", modifier = Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = {
                            viewModel.downloadSound(sound)
                            showContextMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Download, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save to Device", modifier = Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = {
                            showContextMenu = false
                            if (!isFav) viewModel.toggleFavorite(sound)
                            viewModel.startRecordingSoundKey(sound)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Keyboard, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (boundKey != null) "Change Keybind ($boundKey)" else "Set Custom Keybind", modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContextMenu = false }) { Text("Close") }
            }
        )
    }

    if (state.recordingSound != null) {
        val sound = state.recordingSound!!
        AlertDialog(
            onDismissRequest = { viewModel.cancelRecording() },
            title = { Text("Record Custom Keybind", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Press ANY key on your keyboard now...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Sound: \"${sound.title}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.cancelRecording() }) { Text("Cancel") }
            }
        )
    }
}
