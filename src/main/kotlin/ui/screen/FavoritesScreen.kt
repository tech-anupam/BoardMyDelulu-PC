package ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import audio.SoundEngine
import data.api.Sound
import hotkey.HotkeyConfig
import ui.AppViewModel
import ui.component.SoundPad

@Composable
fun FavoritesScreen(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsState()
    val playingId by SoundEngine.playingId.collectAsState()
    var rightClickSound by remember { mutableStateOf<Sound?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }

    if (state.favorites.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Outlined.FavoriteBorder, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))
            Text("No favorites yet", style = MaterialTheme.typography.titleMedium)
            Text("Right-click any sound to add it to Favorites and assign keybinds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Favorites", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("(${state.favorites.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.playRandomFavorite() },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Shuffle, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Shuffle (${HotkeyConfig.keyName(state.deckSettings.shuffleKey)})", style = MaterialTheme.typography.labelSmall)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.playNextFavorite() },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.SkipNext, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Next (${HotkeyConfig.keyName(state.deckSettings.cycleKey)})", style = MaterialTheme.typography.labelSmall)
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
                            Text("Stop", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(state.favorites, key = { idx, s -> "fav_${s.id}_$idx" }) { index, sound ->
                    SoundPad(
                        sound = sound,
                        index = index,
                        isPlaying = playingId == sound.id,
                        isFavorite = true,
                        hotkeyName = state.hotkeys[sound.id],
                        onTap = { SoundEngine.play(sound.id, sound.mp3) },
                        onRightClick = {
                            rightClickSound = sound
                            showContextMenu = true
                        }
                    )
                }
            }
        }
    }

    if (showContextMenu && rightClickSound != null) {
        val sound = rightClickSound!!
        val boundKey = state.hotkeys[sound.id]
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
                            showContextMenu = false
                            viewModel.startRecordingSoundKey(sound)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Keyboard, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (boundKey != null) "Change Custom Keybind ($boundKey)" else "Set Custom Keybind", modifier = Modifier.weight(1f))
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
                            viewModel.toggleFavorite(sound)
                            showContextMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.FavoriteBorder, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Remove from Favorites", color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
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
