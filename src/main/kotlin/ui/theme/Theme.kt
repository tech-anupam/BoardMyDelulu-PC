package ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp

val PermanentMarkerFamily = try {
    FontFamily(Font(resource = "font/permanent_marker.ttf", weight = FontWeight.Normal))
} catch (_: Exception) {
    FontFamily.Default
}

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    secondary = PrimaryPink,
    tertiary = Cyan500,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onPrimary = OnPrimary,
    onBackground = OnPrimary,
    onSurface = OnPrimary,
    onSurfaceVariant = OnPrimary.copy(alpha = 0.7f)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    secondary = PrimaryPink,
    tertiary = Cyan500,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightCard,
    onPrimary = OnPrimary,
    onBackground = DarkBackground,
    onSurface = DarkBackground,
    onSurfaceVariant = DarkBackground.copy(alpha = 0.6f)
)

val BoardTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = PermanentMarkerFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PermanentMarkerFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PermanentMarkerFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp
    )
)

@Composable
fun BoardMyDeluluTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = BoardTypography,
        content = content
    )
}
