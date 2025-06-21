package neth.iecal.questphone.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

val Mindful = darkColorScheme(
    primary = Color(0xFF000000), // Black
    secondary = Color(0xFFE6E6FA), // Soft lavender for mindfulness/peace
    background = Color(0xFFFFFFFF), // White
    surface = Color(0xFFF0F0F0), // Light gray
    onPrimary = Color(0xFFFFFFFF), // White on black
    onSecondary = Color(0xFF000000), // Black on lavender
    onBackground = Color(0xFF000000), // Black on white
    onSurface = Color(0xFF000000) // Black on light gray
)
val PinkGirly = darkColorScheme(
    primary = Color(0xFFFF69B4), // Hot pink
    secondary = Color(0xFFFFC1CC), // Soft baby pink
    background = Color(0xFFFFF0F5), // Lightest pink (lavender blush)
    surface = Color(0xFFFFE4E1), // Misty rose
    onPrimary = Color(0xFFFFFFFF), // White on hot pink
    onSecondary = Color(0xFF000000), // Black on soft pink
    onBackground = Color(0xFF000000), // Black on lightest pink
    onSurface = Color(0xFF000000) // Black on misty rose
)

@Composable
fun LauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    colorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = customTypography,
        content = content
    )
}