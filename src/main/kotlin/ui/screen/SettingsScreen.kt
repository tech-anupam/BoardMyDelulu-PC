package ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hotkey.HotkeyConfig
import ui.AppViewModel
import java.awt.Desktop
import java.net.URI

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsState()
    var showDeviceMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp))

        // Developer Profile Card
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp).clip(CircleShape))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Anupam Jha", style = MaterialTheme.typography.titleMedium)
                        Text("Indie Developer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SocialChip("GitHub", Icons.Filled.Code) { openUrl("https://github.com/tech-anupam") }
                    SocialChip("Portfolio", Icons.Filled.Language) { openUrl("https://anupambuilds.store/about") }
                    SocialChip("Instagram", Icons.Filled.Share) { openUrl("https://instagram.com/tech.anupam") }
                    SocialChip("Email", Icons.Filled.Email) { openUrl("mailto:killerboy99126@gmail.com") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Support Card
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Support Development", style = MaterialTheme.typography.titleSmall)
                    Text("UPI: anupambuilds@fam", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Support indie development with any small contribution!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Audio & Sound Output Control", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                // Device Selector
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Headphones, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Output Audio Device", fontWeight = FontWeight.SemiBold)
                            Text(state.selectedOutputDevice, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.refreshAudioDevices() }) {
                            Icon(Icons.Filled.Refresh, "Refresh Devices", tint = MaterialTheme.colorScheme.primary)
                        }
                        Box {
                            OutlinedButton(
                                onClick = { showDeviceMenu = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Select Device")
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(expanded = showDeviceMenu, onDismissRequest = { showDeviceMenu = false }) {
                                state.availableOutputs.forEach { device ->
                                    DropdownMenuItem(
                                        text = { Text(device.name) },
                                        onClick = {
                                            viewModel.setOutputDevice(device)
                                            showDeviceMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                // Master Volume Slider
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (state.volume > 0f) viewModel.setVolume(0f, playFeedback = false) else viewModel.setVolume(1f, playFeedback = true) }) {
                        Icon(
                            if (state.volume > 0f) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                            contentDescription = "Volume",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Master Soundboard Volume", fontWeight = FontWeight.SemiBold)
                            Text("${(state.volume * 100).toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = state.volume,
                            onValueChange = { viewModel.setVolume(it) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Meme Deck & Global Hotkeys", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                // Panic Key
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Keyboard, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Panic Stop Key: ESCAPE", fontWeight = FontWeight.SemiBold)
                        Text("Press Escape anytime to immediately kill all playing sounds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                // Shuffle Favorites Hotkey
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Shuffle, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Shuffle Play Favorites", fontWeight = FontWeight.SemiBold)
                            Text("Plays a random favorite sound globally", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.startRecordingDeckKey("shuffle") },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(HotkeyConfig.keyName(state.deckSettings.shuffleKey))
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Cycle Next Favorite Hotkey
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.SkipNext, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Cycle Next Favorite", fontWeight = FontWeight.SemiBold)
                            Text("Plays next favorite sound one by one in order", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.startRecordingDeckKey("cycle") },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(HotkeyConfig.keyName(state.deckSettings.cycleKey))
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Auto-Loop Switch
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Repeat, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Auto-Loop Continuous Playlist", fontWeight = FontWeight.SemiBold)
                            Text("Automatically plays next favorite when previous sound finishes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(checked = state.deckSettings.autoLoop, onCheckedChange = { viewModel.toggleAutoLoop() })
                }

                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Text("Custom Sound Keybinds", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))

                if (state.hotkeyBindings.isEmpty()) {
                    Text(
                        "No custom sound keybinds set yet. Go to Favorites or Downloads and click 'Set Key' to bind any keyboard key.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.hotkeyBindings.forEach { binding ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            HotkeyConfig.keyName(binding.keyCode),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(binding.soundTitle, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                }
                                IconButton(
                                    onClick = { viewModel.removeHotkey(binding.keyCode) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Preferences", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))

        Card(shape = RoundedCornerShape(16.dp)) {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PowerSettingsNew, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Launch on Windows Startup")
                            Text("Start BoardMyDelulu in system tray when PC boots", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(checked = state.isAutoStartEnabled, onCheckedChange = { viewModel.toggleAutoStart() })
                }
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DarkMode, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Dark Theme")
                    }
                    Switch(checked = state.isDarkTheme, onCheckedChange = { viewModel.toggleTheme() })
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(
            "BoardMyDelulu Desktop v1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (state.recordingDeckAction != null) {
        val actionName = if (state.recordingDeckAction == "shuffle") "Shuffle Favorites" else "Cycle Next Favorite"
        AlertDialog(
            onDismissRequest = { viewModel.cancelRecording() },
            title = { Text("Record Keybind for $actionName", style = MaterialTheme.typography.titleMedium) },
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
                        "Pressing this key in any game will trigger $actionName.",
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

@Composable
private fun SocialChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun openUrl(url: String) {
    try { Desktop.getDesktop().browse(URI(url)) } catch (_: Exception) { }
}
