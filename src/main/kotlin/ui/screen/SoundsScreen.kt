package ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import audio.SoundEngine
import data.api.Sound
import data.api.regions
import ui.AppViewModel
import ui.HomeCategory
import ui.component.SoundPad

@Composable
fun SoundsScreen(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsState()
    val playingId by SoundEngine.playingId.collectAsState()
    var showRegionMenu by remember { mutableStateOf(false) }
    var rightClickSound by remember { mutableStateOf<Sound?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val total = gridState.layoutInfo.totalItemsCount
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && last >= total - 6
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.isHomeLoading && !state.isLoadingMore && state.homeSounds.isNotEmpty()) {
            viewModel.loadMoreHome()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "BoardMyDelulu",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Volume Control
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.width(180.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (state.volume > 0f) viewModel.setVolume(0f, playFeedback = false) else viewModel.setVolume(1f, playFeedback = true)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (state.volume > 0f) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                            contentDescription = "Volume",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Slider(
                        value = state.volume,
                        onValueChange = { viewModel.setVolume(it) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${(state.volume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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

                IconButton(onClick = { viewModel.loadHome() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeCategory.entries.forEach { cat ->
                FilterChip(
                    selected = state.homeCategory == cat,
                    onClick = { viewModel.switchCategory(cat) },
                    label = { Text(cat.label) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            Spacer(Modifier.weight(1f))

            Box {
                OutlinedButton(
                    onClick = { showRegionMenu = true },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.Public, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${state.selectedRegion.name} (${state.selectedRegion.code.uppercase()})")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showRegionMenu, onDismissRequest = { showRegionMenu = false }) {
                    regions.forEach { region ->
                        DropdownMenuItem(
                            text = { Text("${region.name} (${region.code.uppercase()})") },
                            leadingIcon = { Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                viewModel.changeRegion(region)
                                showRegionMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (state.isHomeLoading && state.homeSounds.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.5.dp)
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 140.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(state.homeSounds, key = { idx, s -> "home_${s.id}_$idx" }) { index, sound ->
                    SoundPad(
                        sound = sound,
                        index = index,
                        isPlaying = playingId == sound.id,
                        isFavorite = state.favoriteIds.contains(sound.id),
                        hotkeyName = state.hotkeys[sound.id],
                        onTap = { SoundEngine.play(sound.id, sound.mp3) },
                        onRightClick = { rightClickSound = sound; showContextMenu = true }
                    )
                }

                if (state.homeSounds.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "load_more_home") {
                        Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            if (state.isLoadingMore) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Button(onClick = { viewModel.loadMoreHome() }, shape = RoundedCornerShape(14.dp)) {
                                    Text("Load More")
                                }
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
                    if (boundKey != null) {
                        TextButton(
                            onClick = {
                                viewModel.removeHotkeyBySoundId(sound.id)
                                showContextMenu = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Clear Keybind", color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                        }
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
