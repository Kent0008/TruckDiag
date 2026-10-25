package com.truckdiag.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryAmber,
    onPrimary = BgDark,
    primaryContainer = CardDark,
    onPrimaryContainer = PrimaryAmberLight,
    secondary = AccentCyan,
    onSecondary = BgDark,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = StatusRed,
    onError = TextPrimary
)

@Composable
fun TruckDiagTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
