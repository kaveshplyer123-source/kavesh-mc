package com.kavesh.mc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgObsidian = Color(0xFF121412)
val CardSurface = Color(0xFF1A1D1A)
val CardSurfaceElevated = Color(0xFF212521)
val BorderSubtle = Color(0xFF2A2E2A)
val EmeraldPrimary = Color(0xFF38D373)
val EmeraldBright = Color(0xFF4ADE80)
val EmeraldDarkBg = Color(0xFF153120)
val TextPrimary = Color(0xFFFFFFFF)
val TextMuted = Color(0xFF9CA3AF)
val WarnAmber = Color(0xFFF59E0B)
val WarnBg = Color(0xFF2B2212)
val ErrorRed = Color(0xFFEF4444)
val ErrorBg = Color(0xFF2D1616)

private val KaveshDarkScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF0A1A0F),
    background = BgObsidian,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurfaceElevated,
    onSurfaceVariant = TextMuted,
    outline = BorderSubtle,
    error = ErrorRed
)

@Composable
fun KaveshMCTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KaveshDarkScheme,
        content = content
    )
}
