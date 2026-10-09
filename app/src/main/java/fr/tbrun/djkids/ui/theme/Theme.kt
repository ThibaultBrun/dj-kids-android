package fr.tbrun.djkids.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DjColors = darkColorScheme(
    primary = Cyan,
    onPrimary = Bg,
    secondary = Pink,
    onSecondary = Bg,
    tertiary = Lime,
    background = Bg,
    onBackground = TextHi,
    surface = Surface1,
    onSurface = TextHi,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextLo,
)

@Composable
fun DJKidsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DjColors,
        typography = Typography(),
        content = content,
    )
}
