package ai.hassan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF176B5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F0E1),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF765A00),
    secondaryContainer = Color(0xFFFFE08A),
    tertiary = Color(0xFF455D92),
    background = Color(0xFFF7FAF7),
    surface = Color(0xFFF7FAF7),
    surfaceVariant = Color(0xFFE2EAE6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF99D4C5),
    primaryContainer = Color(0xFF005143),
    secondary = Color(0xFFE9C34F),
    secondaryContainer = Color(0xFF584300),
    tertiary = Color(0xFFB5C7FF),
)

@Composable
fun HassanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = if (darkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
