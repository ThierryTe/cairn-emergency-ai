package com.cairn.emergency.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val DarkColorScheme = darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)
private val LightColorScheme = lightColorScheme(primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40)
private val HighContrastColorScheme = lightColorScheme(
    primary = Color(0xFF39228D),
    onPrimary = Color.White,
    surface = Color.White,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    outline = Color(0xFF102B4E),
)

internal val LocalCairnHighContrast = staticCompositionLocalOf { false }

@Composable
fun CairnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    largeText: Boolean = false,
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        highContrast -> HighContrastColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val density = LocalDensity.current
    val appDensity = if (largeText) Density(density.density, density.fontScale * 1.2f) else density
    CompositionLocalProvider(LocalDensity provides appDensity, LocalCairnHighContrast provides highContrast) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}
