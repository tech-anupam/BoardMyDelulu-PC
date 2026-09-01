package ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.api.Sound
import ui.theme.padColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoundPad(
    sound: Sound,
    index: Int,
    isPlaying: Boolean,
    isFavorite: Boolean,
    hotkeyName: String? = null,
    onTap: () -> Unit,
    onRightClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = padColors[index % padColors.size]
    val topColor = theme.first
    val bottomColor = theme.second

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlaying) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) { onRightClick() }
            .clickable { onTap() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Hotkey Badge in Top Left
            if (!hotkeyName.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = hotkeyName,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // Favorite in Top Right
            if (isFavorite) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.TopEnd).size(14.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(elevation = if (isPlaying) 8.dp else 4.dp, shape = CircleShape)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(bottomColor, bottomColor.copy(alpha = 0.6f))))
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(bottom = if (isPlaying) 0.dp else 2.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(topColor, bottomColor)))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.GraphicEq else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = sound.title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
