package com.mycelium.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Forest-floor palette
val Bark = Color(0xFF2B2118)
val Soil = Color(0xFF3B2F24)
val Cream = Color(0xFFEDE3CF)
val Moss = Color(0xFF6B8E4E)
val Fern = Color(0xFF4F7942)
val Lichen = Color(0xFFA7B88A)
val Chanterelle = Color(0xFFD98E32)
val Rust = Color(0xFFA0522D)
val Amanita = Color(0xFFC0392B)
val Spore = Color(0xFFC8B68E)
val Glowcap = Color(0xFF7FD1AE)

@Composable
fun MyceliumTheme(accent: Color, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Bark,
        secondary = Lichen,
        onSecondary = Bark,
        background = Bark,
        onBackground = Cream,
        surface = Soil,
        onSurface = Cream,
        surfaceVariant = Soil,
        onSurfaceVariant = Cream.copy(alpha = 0.7f),
        outline = accent.copy(alpha = 0.6f),
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
