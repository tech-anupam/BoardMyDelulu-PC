package ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import data.api.GifFilter
import data.api.GifItem
import ui.AppViewModel
import ui.component.GifCard

private data class GifCategory(
    val name: String,
    val query: String,
    val color: Color,
    val icon: ImageVector
)

private val gifCategories = listOf(
    GifCategory("Memes",     "meme",         Color(0xFF6C5CE7), Icons.Filled.EmojiEmotions),
    GifCategory("Reactions", "reaction",     Color(0xFFFF6B9D), Icons.Filled.ThumbUp),
    GifCategory("Anime",     "anime",        Color(0xFFF368E0), Icons.Filled.AutoAwesome),
    GifCategory("Gaming",    "gaming",       Color(0xFF48DBFB), Icons.Filled.SportsEsports),
    GifCategory("Bollywood", "bollywood",    Color(0xFFFF9F43), Icons.Filled.LiveTv),
    GifCategory("Trending",  "trending",     Color(0xFF2ECC71), Icons.AutoMirrored.Filled.TrendingUp),
    GifCategory("Funny",     "funny",        Color(0xFFFFA502), Icons.Filled.Celebration),
    GifCategory("Love",      "love heart",   Color(0xFFE17055), Icons.Filled.Favorite),
    GifCategory("Dance",     "dance",        Color(0xFFE84393), Icons.Filled.MusicNote),
    GifCategory("Fail",      "fail",         Color(0xFFFF7675), Icons.Filled.SentimentVeryDissatisfied),
    GifCategory("Cute",      "cute aww",     Color(0xFF55E6C1), Icons.Filled.Pets),
    GifCategory("Movies",    "movie cinema", Color(0xFFA29BFE), Icons.Filled.Movie)
)

private val gifQuickTags = listOf(
    "meme", "slay", "nope", "yikes", "vibes", "sigma",
    "rizz", "skibidi", "bruh", "cringe", "based", "gg",
    "lol", "omg", "sad", "happy", "anime", "bollywood"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GifsScreen(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsState()
    val gridState = rememberLazyGridState()
    var rightClickGif by remember { mutableStateOf<GifItem?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }

    // Auto-load trending on first open
    LaunchedEffect(Unit) { viewModel.initGifsScreen() }

    // Infinite scroll trigger
    val shouldLoadMore by remember {
        derivedStateOf {
            val total = gridState.layoutInfo.totalItemsCount
            val last  = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && last >= total - 6
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.isGifSearching && state.gifResults.isNotEmpty() && state.gifFilter != GifFilter.FAVORITES) {
            viewModel.loadMoreGifs()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.gifSaveStatus) {
        state.gifSaveStatus?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "GIFs & Stickers",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                if (state.gifFilter == GifFilter.FAVORITES) {
                    Text(
                        "${state.gifFavorites.size} Saved",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Filter chips: GIFs | Stickers | Favorites
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GifFilter.entries.forEach { filter ->
                    val label = if (filter == GifFilter.FAVORITES) {
                        "Favorites (${state.gifFavorites.size})"
                    } else {
                        filter.label
                    }

                    FilterChip(
                        selected = state.gifFilter == filter,
                        onClick = { viewModel.setGifFilter(filter) },
                        label = { Text(label) },
                        leadingIcon = {
                            if (filter == GifFilter.FAVORITES) {
                                Icon(Icons.Filled.Favorite, null, modifier = Modifier.size(16.dp))
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (filter == GifFilter.FAVORITES) Color(0xFFFF4757) else MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Search bar
            OutlinedTextField(
                value = state.gifQuery,
                onValueChange = { viewModel.updateGifSearch(it) },
                placeholder = {
                    Text(if (state.gifFilter == GifFilter.FAVORITES) "Search favorite GIFs..." else "Search GIFs, stickers, memes...")
                },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (state.gifQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateGifSearch("") }) {
                            Icon(Icons.Filled.Close, "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(10.dp))

            // Quick tags (only show when in GIFs or Stickers browse mode)
            if (state.gifFilter != GifFilter.FAVORITES) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    gifQuickTags.forEach { tag ->
                        AssistChip(
                            onClick = { viewModel.updateGifSearch(tag) },
                            label = { Text(tag) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Content area
            when {
                state.isGifSearching && state.gifResults.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 2.5.dp)
                    }
                }

                // Favorites empty state
                state.gifFilter == GifFilter.FAVORITES && state.gifResults.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Icon(
                                Icons.Filled.FavoriteBorder,
                                null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (state.gifQuery.isNotBlank()) "No favorite GIFs matching \"${state.gifQuery}\""
                                else "No favorite GIFs yet!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Hover any GIF or sticker and click the Heart icon to bookmark it here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                !state.isGifSearching && state.gifResults.isEmpty() && state.hasGifSearched -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.SearchOff,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No ${state.gifFilter.label} found for \"${state.gifQuery}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Categories browse
                !state.isGifSearching && state.gifResults.isEmpty() && !state.hasGifSearched && state.gifQuery.isEmpty() -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "cat_header") {
                            Text(
                                "Browse by Category",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(gifCategories, key = { "gifcat_${it.name}" }) { cat ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = cat.color),
                                modifier = Modifier.height(80.dp).clickable {
                                    viewModel.updateGifSearch(cat.query)
                                }
                            ) {
                                Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            cat.icon,
                                            null,
                                            tint = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            cat.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Results grid
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.gifQuery.isEmpty() && state.gifFilter != GifFilter.FAVORITES) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "trending_label") {
                                Text(
                                    "Trending ${state.gifFilter.label}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        itemsIndexed(state.gifResults, key = { idx, g -> "gif_${g.id}_$idx" }) { _, gif ->
                            val isFav = state.gifFavoriteIds.contains(gif.id)
                            GifCard(
                                gif = gif,
                                isFavorite = isFav,
                                onToggleFavorite = { viewModel.toggleGifFavorite(it) },
                                onCopyImage = { viewModel.copyGifImage(it) },
                                onCopyUrl = { viewModel.copyGifUrl(it) },
                                onSave = { viewModel.saveGif(it) },
                                onShare = { viewModel.shareGif(it) },
                                onRightClick = { rightClickGif = it; showContextMenu = true }
                            )
                        }

                        if (state.gifResults.isNotEmpty() && state.gifFilter != GifFilter.FAVORITES) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "gif_load_more") {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    if (state.isGifSearching) {
                                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                    } else {
                                        Button(
                                            onClick = { viewModel.loadMoreGifs() },
                                            shape = RoundedCornerShape(14.dp)
                                        ) { Text("Load More") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Right-click context menu dialog
    if (showContextMenu && rightClickGif != null) {
        val gif = rightClickGif!!
        val isFav = state.gifFavoriteIds.contains(gif.id)

        AlertDialog(
            onDismissRequest = { showContextMenu = false },
            title = {
                Text(
                    gif.title.take(60),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Copy GIF Image
                    TextButton(
                        onClick = { viewModel.copyGifImage(gif); showContextMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Copy GIF (Image for Discord/WhatsApp)", modifier = Modifier.weight(1f))
                    }

                    // Copy GIF Link
                    TextButton(
                        onClick = { viewModel.copyGifUrl(gif); showContextMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Link, null, modifier = Modifier.size(20.dp), tint = Color(0xFF48DBFB))
                        Spacer(Modifier.width(8.dp))
                        Text("Copy Link (URL)", modifier = Modifier.weight(1f))
                    }

                    // Toggle Favorite
                    TextButton(
                        onClick = { viewModel.toggleGifFavorite(gif); showContextMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFFFF4757)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isFav) "Remove from Favorites" else "Add to Favorites", modifier = Modifier.weight(1f))
                    }

                    // Save to Downloads
                    TextButton(
                        onClick = { viewModel.saveGif(gif); showContextMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Download, null, modifier = Modifier.size(20.dp), tint = Color(0xFF55E6C1))
                        Spacer(Modifier.width(8.dp))
                        Text("Save to Device", modifier = Modifier.weight(1f))
                    }

                    // Share / Browser
                    TextButton(
                        onClick = { viewModel.shareGif(gif); showContextMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(20.dp), tint = Color(0xFFFFA502))
                        Spacer(Modifier.width(8.dp))
                        Text("Open in Browser", modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContextMenu = false }) { Text("Close") }
            }
        )
    }
}
