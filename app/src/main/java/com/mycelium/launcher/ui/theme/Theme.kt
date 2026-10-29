package com.mycelium.launcher.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

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

private fun Typography.withFont(f: FontFamily?): Typography {
    if (f == null) return this
    return Typography(
        displayLarge = displayLarge.copy(fontFamily = f),
        displayMedium = displayMedium.copy(fontFamily = f),
        displaySmall = displaySmall.copy(fontFamily = f),
        headlineLarge = headlineLarge.copy(fontFamily = f),
        headlineMedium = headlineMedium.copy(fontFamily = f),
        headlineSmall = headlineSmall.copy(fontFamily = f),
        titleLarge = titleLarge.copy(fontFamily = f),
        titleMedium = titleMedium.copy(fontFamily = f),
        titleSmall = titleSmall.copy(fontFamily = f),
        bodyLarge = bodyLarge.copy(fontFamily = f),
        bodyMedium = bodyMedium.copy(fontFamily = f),
        bodySmall = bodySmall.copy(fontFamily = f),
        labelLarge = labelLarge.copy(fontFamily = f),
        labelMedium = labelMedium.copy(fontFamily = f),
        labelSmall = labelSmall.copy(fontFamily = f),
    )
}

@Composable
fun MyceliumTheme(accent: Color, font: FontFamily?, content: @Composable () -> Unit) {
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
    MaterialTheme(colorScheme = scheme, typography = Typography().withFont(font)) {
        // Plain Text() calls read LocalTextStyle, so set the font there too.
        CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = font)) {
            content()
        }
    }
}
