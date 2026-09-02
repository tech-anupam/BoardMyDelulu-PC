package ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.api.GifFilter
import data.api.GifItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import util.AnimatedGif
import util.ImageLoader

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GifCard(
    gif: GifItem,
    isFavorite: Boolean,
    onToggleFavorite: (GifItem) -> Unit,
    onCopyImage: (GifItem) -> Unit,
    onCopyUrl: (GifItem) -> Unit,
    onSave: (GifItem) -> Unit,
    onShare: (GifItem) -> Unit,
    onRightClick: (GifItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val staticBitmap by produceState<ImageBitmap?>(null, gif.previewUrl) {
        value = ImageLoader.load(gif.previewUrl)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    var animatedGif by remember { mutableStateOf<AnimatedGif?>(null) }
    var currentFrameIndex by remember { mutableStateOf(0) }

    // ── Auto-play on hover ──────────────────────────────────────────────────
    LaunchedEffect(isHovered, gif.previewUrl, gif.fullUrl) {
        if (isHovered) {
            if (animatedGif == null) {
                animatedGif = ImageLoader.loadAnimation(gif.previewUrl)
                    ?: ImageLoader.loadAnimation(gif.fullUrl)
            }
            val anim = animatedGif
            if (anim != null && anim.frames.size > 1) {
                while (isActive) {
                    for (i in anim.frames.indices) {
                        currentFrameIndex = i
                        val delayTime = anim.delaysMs.getOrElse(i) { 100L }.coerceIn(40L, 500L)
                        delay(delayTime)
                    }
                }
            }
        } else {
            currentFrameIndex = 0
        }
    }

    val displayedBitmap = if (isHovered && animatedGif != null && animatedGif!!.frames.isNotEmpty()) {
        animatedGif!!.frames[currentFrameIndex % animatedGif!!.frames.size]
    } else {
        staticBitmap ?: animatedGif?.frames?.firstOrNull()
    }

    val actionAlpha by animateFloatAsState(
        targetValue = if (isHovered) 1f else 0f,
        label = "gif_action_alpha"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHovered) 8.dp else 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .hoverable(interactionSource)
            .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) { onRightClick(gif) }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── GIF / Sticker Frame ──────────────────────────────────────────
            if (displayedBitmap != null) {
                Image(
                    bitmap = displayedBitmap,
                    contentDescription = gif.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }

            // ── Type badge (top-left) ─────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(bottomEnd = 8.dp),
                color = if (gif.filter == GifFilter.STICKER) Color(0xCC6C5CE7) else Color(0xCCFF6B9D),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    if (gif.filter == GifFilter.STICKER) "STICKER" else "GIF",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // ── Favorite Heart Icon (top-right) ──────────────────────────────
            IconButton(
                onClick = { onToggleFavorite(gif) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                    tint = if (isFavorite) Color(0xFFFF4757) else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // ── Hover Action Bar (bottom) ─────────────────────────────────────
            Surface(
                color = Color(0xEE1E1E2E),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { alpha = actionAlpha }
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                ) {
                    // Copy GIF Image
                    IconButton(
                        onClick = { onCopyImage(gif) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "Copy GIF Image",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Copy Link
                    IconButton(
                        onClick = { onCopyUrl(gif) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Filled.Link,
                            contentDescription = "Copy Link",
                            tint = Color(0xFF48DBFB),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Save to Downloads
                    IconButton(
                        onClick = { onSave(gif) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = "Save GIF",
                            tint = Color(0xFF55E6C1),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Share / Browser
                    IconButton(
                        onClick = { onShare(gif) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in Browser",
                            tint = Color(0xFFFFA502),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
