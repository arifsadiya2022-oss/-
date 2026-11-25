package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ArifThemePreset(val label: String, val subtitle: String) {
    ELECTRIC_CYAN("Electric Cyan", "Signature ARIF cyber blue & neon purple"),
    CYBER_PURPLE("Cyber Violet", "Overdrive ultraviolet & plasma cyan"),
    CRIMSON_PULSE("Crimson Pulse", "Esports tactical crimson & electric blue")
}

@Immutable
data class ArifAccentColors(
    val neonPrimary: Color = ElectricBlue,
    val neonSecondary: Color = NeonPurple,
    val glassSurface: Color = ArifGlassOverlay,
    val glassBorder: Color = GlassStroke,
    val deepBackground: Color = ArifDeepBlack,
    val cardBackground: Color = ArifSurfaceNavy,
    val success: Color = NeonEmerald,
    val warning: Color = AmberTelemetry,
    val danger: Color = DangerRed
)

val LocalArifColors = staticCompositionLocalOf { ArifAccentColors() }

private val ArifShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ArifGameBoosterTheme(
    themePreset: ArifThemePreset = ArifThemePreset.ELECTRIC_CYAN,
    content: @Composable () -> Unit
) {
    val accentColors = when (themePreset) {
        ArifThemePreset.ELECTRIC_CYAN -> ArifAccentColors(
            neonPrimary = ElectricBlue,
            neonSecondary = NeonPurple,
            glassBorder = Color(0x4000E5FF)
        )
        ArifThemePreset.CYBER_PURPLE -> ArifAccentColors(
            neonPrimary = NeonPurpleBright,
            neonSecondary = ElectricBlue,
            glassBorder = Color(0x458B5CF6)
        )
        ArifThemePreset.CRIMSON_PULSE -> ArifAccentColors(
            neonPrimary = CrimsonPulse,
            neonSecondary = ElectricBlue,
            glassBorder = Color(0x45FF2E63)
        )
    }

    val colorScheme = darkColorScheme(
        primary = accentColors.neonPrimary,
        onPrimary = ArifDeepBlack,
        primaryContainer = accentColors.neonPrimary.copy(alpha = 0.16f),
        onPrimaryContainer = accentColors.neonPrimary,
        secondary = accentColors.neonSecondary,
        onSecondary = TextPrimary,
        secondaryContainer = accentColors.neonSecondary.copy(alpha = 0.18f),
        onSecondaryContainer = TextPrimary,
        tertiary = NeonEmerald,
        onTertiary = ArifDeepBlack,
        background = ArifDeepBlack,
        onBackground = TextPrimary,
        surface = ArifDarkNavy,
        onSurface = TextPrimary,
        surfaceVariant = ArifSurfaceNavy,
        onSurfaceVariant = TextSecondary,
        surfaceContainerLowest = ArifDeepBlack,
        surfaceContainerLow = ArifDarkNavy,
        surfaceContainer = ArifSurfaceNavy,
        surfaceContainerHigh = ArifSurfaceElevated,
        outline = accentColors.glassBorder,
        outlineVariant = GlassStrokeSubtle,
        error = DangerRed,
        onError = TextPrimary
    )

    CompositionLocalProvider(LocalArifColors provides accentColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ArifTypography,
            shapes = ArifShapes,
            content = content
        )
    }
}
