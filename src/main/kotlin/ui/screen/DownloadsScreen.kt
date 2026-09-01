package ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import audio.SoundEngine
import data.repository.DownloadRepository
import hotkey.HotkeyConfig
import ui.AppViewModel
import java.awt.Desktop

@Composable
fun DownloadsScreen(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsState()
    val playingId by SoundEngine.playingId.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Downloads", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = {
                        try { Desktop.getDesktop().open(DownloadRepository.downloadDir) } catch (_: Exception) { }
                    },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.FolderOpen, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open Folder")
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { viewModel.loadDownloads() }) {
                    Icon(Icons.Filled.Refresh, "Refresh", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (state.downloads.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Download, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(12.dp))
                    Text("No downloaded sounds", style = MaterialTheme.typography.titleMedium)
                    Text("Right-click any sound pad to save it for offline and set keybinds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Text(
                "${state.downloads.size} saved",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.downloads, key = { "dl_${it.id}" }) { sound ->
                    val soundKey = "download_${sound.id}"
                    val localFile = DownloadRepository.getLocalFile(sound)
                    val boundKey = state.hotkeys[sound.id]

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (playingId == soundKey) 4.dp else 1.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (playingId == soundKey) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (localFile != null) SoundEngine.playLocal(soundKey, localFile.absolutePath)
                                    else SoundEngine.play(soundKey, sound.mp3)
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    if (playingId == soundKey) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    null, tint = if (playingId == soundKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(sound.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (localFile != null) {
                                        Text("${localFile.length() / 1024} KB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (boundKey != null) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary) {
                                            Text(boundKey, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                }
                            }

                            // Keybind Button
                            OutlinedButton(
                                onClick = { viewModel.startRecordingSoundKey(sound) },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Keyboard, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (boundKey != null) boundKey else "Set Key", style = MaterialTheme.typography.labelSmall)
                            }

                            Spacer(Modifier.width(8.dp))

                            IconButton(onClick = { viewModel.deleteDownload(sound.id) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
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
