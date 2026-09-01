package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ui.screen.*
import ui.theme.BoardMyDeluluTheme

private data class NavItem(val screen: Screen, val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem(Screen.SOUNDS, "Sounds", Icons.Filled.MusicNote),
    NavItem(Screen.SEARCH, "Search", Icons.Filled.Search),
    NavItem(Screen.DOWNLOADS, "Downloads", Icons.Filled.Download),
    NavItem(Screen.FAVORITES, "Favorites", Icons.Filled.Favorite),
    NavItem(Screen.SETTINGS, "Settings", Icons.Filled.Settings)
)

@Composable
fun App(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsState()

    BoardMyDeluluTheme(darkTheme = state.isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Spacer(Modifier.height(16.dp))
                    navItems.forEach { item ->
                        NavigationRailItem(
                            selected = state.currentScreen == item.screen,
                            onClick = { viewModel.navigateTo(item.screen) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (state.currentScreen) {
                        Screen.SOUNDS -> SoundsScreen(viewModel)
                        Screen.SEARCH -> SearchScreen(viewModel)
                        Screen.DOWNLOADS -> DownloadsScreen(viewModel)
                        Screen.FAVORITES -> FavoritesScreen(viewModel)
                        Screen.SETTINGS -> SettingsScreen(viewModel)
                    }
                }
            }
        }
    }
}
