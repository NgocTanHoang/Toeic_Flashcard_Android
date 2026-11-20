package com.toeicflashcard.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.toeicflashcard.domain.model.DarkTone
import com.toeicflashcard.domain.model.ThemeMode
import com.toeicflashcard.domain.model.UserPreferences

@Immutable
data class Sm2ColorPair(
    val container: Color,
    val content: Color,
    val border: Color = content.copy(alpha = 0.35f)
)

@Immutable
data class Sm2RatingColors(
    val again: Sm2ColorPair,
    val hard: Sm2ColorPair,
    val good: Sm2ColorPair,
    val easy: Sm2ColorPair
)

@Immutable
data class LiquidGlassColors(
    val isDark: Boolean,
    val cardAlpha: Float,
    val hazeBackground: Color,
    val surfaceFill: Color,
    val elevatedGlassFill: Color,
    val dialogSheetFill: Color,
    val sheetScrim: Color,
    val borderStart: Color,
    val borderMid: Color,
    val borderEnd: Color,
    val sheenStart: Color,
    val sheenEnd: Color,
    val divider: Color,
    val hairlineTrack: Color,
    val defaultBorderBrush: Brush,
    val sheenBrush: Brush?,
    val hairlineProgressBrush: Brush
)

private val LightSm2Colors = Sm2RatingColors(
    again = Sm2ColorPair(Sm2AgainLightBg, Sm2AgainLightFg, Sm2AgainLightBorder),
    hard = Sm2ColorPair(Sm2HardLightBg, Sm2HardLightFg, Sm2HardLightBorder),
    good = Sm2ColorPair(Sm2GoodLightBg, Sm2GoodLightFg, Sm2GoodLightBorder),
    easy = Sm2ColorPair(Sm2EasyLightBg, Sm2EasyLightFg, Sm2EasyLightBorder)
)

private fun buildSm2PairOverBase(base: Color, hue: Color): Sm2ColorPair = Sm2ColorPair(
    container = lerp(base, hue, 0.22f).copy(alpha = 0.90f),
    content = hue,
    border = hue.copy(alpha = 0.45f)
)

private fun buildDarkSm2Colors(darkTone: DarkTone): Sm2RatingColors {
    val base = Color(darkTone.elevatedSurfaceHex)
    val (againHue, hardHue, goodHue, easyHue) = when (darkTone) {
        DarkTone.GRUVBOX_DARK -> listOf(
            Color(0xFFFB4934),
            Color(0xFFFABD2F),
            Color(0xFF83A598),
            Color(0xFFB8BB26)
        )
        DarkTone.CATPPUCCIN_MOCHA,
        DarkTone.TOKYO_NIGHT,
        DarkTone.NORD_ARCTIC,
        DarkTone.ROSE_PINE -> listOf(
            Color(0xFFF38BA8),
            Color(0xFFFAB387),
            Color(0xFF89B4FA),
            Color(0xFFA6E3A1)
        )
        DarkTone.OLED_OBSIDIAN,
        DarkTone.GITHUB_DARK -> listOf(
            Color(0xFFF87171),
            Color(0xFFFBBF24),
            Color(0xFF60A5FA),
            Color(0xFF34D399)
        )
    }
    return Sm2RatingColors(
        again = buildSm2PairOverBase(base, againHue),
        hard = buildSm2PairOverBase(base, hardHue),
        good = buildSm2PairOverBase(base, goodHue),
        easy = buildSm2PairOverBase(base, easyHue)
    )
}

private fun buildLiquidGlassColors(
    isDark: Boolean,
    darkTone: DarkTone,
    glassOpacity: Float
): LiquidGlassColors {
    val alpha = glassOpacity.coerceIn(
        UserPreferences.MIN_GLASS_OPACITY,
        UserPreferences.MAX_GLASS_OPACITY
    )
    return if (isDark) {
        val bg = Color(darkTone.backgroundHex)
        val glass = Color(darkTone.glassSurfaceHex)
        val elevated = Color(darkTone.elevatedSurfaceHex)
        val accent = Color(darkTone.accentHex)
        val primaryText = Color(darkTone.primaryTextHex)
        val borderTint = Color(darkTone.borderTintHex)

        val (borderStartAlpha, borderMidAlpha, borderEndAlpha) = when (darkTone) {
            DarkTone.GITHUB_DARK -> Triple(0.95f, 0.72f, 0.48f)
            DarkTone.OLED_OBSIDIAN -> Triple(0.18f, 0.08f, 0.03f)
            DarkTone.CATPPUCCIN_MOCHA,
            DarkTone.TOKYO_NIGHT,
            DarkTone.NORD_ARCTIC,
            DarkTone.ROSE_PINE,
            DarkTone.GRUVBOX_DARK -> Triple(0.28f, 0.13f, 0.05f)
        }
        val borderStart = borderTint.copy(alpha = borderStartAlpha)
        val borderMid = borderTint.copy(alpha = borderMidAlpha)
        val borderEnd = borderTint.copy(alpha = borderEndAlpha)

        LiquidGlassColors(
            isDark = true,
            cardAlpha = alpha,
            hazeBackground = bg,
            surfaceFill = glass.copy(alpha = alpha),
            elevatedGlassFill = glass.copy(alpha = (alpha + 0.04f).coerceAtMost(0.96f)),
            dialogSheetFill = elevated.copy(alpha = (alpha + 0.14f).coerceIn(0.86f, 0.98f)),
            sheetScrim = Color(0xBD000000),
            borderStart = borderStart,
            borderMid = borderMid,
            borderEnd = borderEnd,
            sheenStart = Color.Transparent,
            sheenEnd = Color.Transparent,
            divider = primaryText.copy(alpha = 0.10f),
            hairlineTrack = primaryText.copy(alpha = 0.11f),
            defaultBorderBrush = Brush.linearGradient(
                0.0f to borderStart,
                0.48f to borderMid,
                1.0f to borderEnd,
                start = Offset.Zero,
                end = Offset.Infinite
            ),
            sheenBrush = null,
            hairlineProgressBrush = Brush.horizontalGradient(
                colors = listOf(accent, accent.copy(alpha = 0.60f))
            )
        )
    } else {
        val borderStart = Color.White.copy(alpha = 0.92f)
        val borderMid = Color.White.copy(alpha = 0.38f)
        val borderEnd = Color(0xFF93B4E8).copy(alpha = 0.24f)
        val sheenStart = Color.White.copy(alpha = 0.36f)
        val sheenEnd = Color.Transparent
        LiquidGlassColors(
            isDark = false,
            cardAlpha = alpha,
            hazeBackground = LightBackground,
            surfaceFill = LightSurface.copy(alpha = alpha),
            elevatedGlassFill = Color.White.copy(alpha = (alpha + 0.06f).coerceAtMost(0.96f)),
            dialogSheetFill = Color(0xFFF3F8FF).copy(alpha = (alpha + 0.16f).coerceIn(0.84f, 0.98f)),
            sheetScrim = Color(0x520F172A),
            borderStart = borderStart,
            borderMid = borderMid,
            borderEnd = borderEnd,
            sheenStart = sheenStart,
            sheenEnd = sheenEnd,
            divider = Color.White.copy(alpha = 0.70f),
            hairlineTrack = Color(0xFF0F172A).copy(alpha = 0.08f),
            defaultBorderBrush = Brush.linearGradient(
                0.0f to borderStart,
                0.48f to borderMid,
                1.0f to borderEnd,
                start = Offset.Zero,
                end = Offset.Infinite
            ),
            sheenBrush = Brush.linearGradient(
                colors = listOf(sheenStart, sheenEnd),
                start = Offset.Zero,
                end = Offset.Infinite
            ),
            hairlineProgressBrush = Brush.horizontalGradient(
                colors = listOf(LightPrimary, LightSecondary)
            )
        )
    }
}

val LocalGlassOpacity = compositionLocalOf { UserPreferences.DEFAULT_GLASS_OPACITY_LIGHT }
val LocalDarkTone = staticCompositionLocalOf { DarkTone.OLED_OBSIDIAN }
val LocalSm2RatingColors = staticCompositionLocalOf { LightSm2Colors }
val LocalLiquidGlassColors = compositionLocalOf {
    buildLiquidGlassColors(
        isDark = false,
        darkTone = DarkTone.OLED_OBSIDIAN,
        glassOpacity = UserPreferences.DEFAULT_GLASS_OPACITY_LIGHT
    )
}

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

private fun buildDarkColorScheme(darkTone: DarkTone) = darkColorScheme(
    primary = Color(darkTone.accentHex),
    onPrimary = Color(darkTone.onAccentHex),
    primaryContainer = Color(darkTone.elevatedSurfaceHex),
    onPrimaryContainer = Color(darkTone.primaryTextHex),
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = Color(darkTone.backgroundHex),
    onBackground = Color(darkTone.primaryTextHex),
    surface = Color(darkTone.glassSurfaceHex),
    onSurface = Color(darkTone.primaryTextHex),
    surfaceVariant = Color(darkTone.elevatedSurfaceHex),
    onSurfaceVariant = Color(darkTone.secondaryTextHex),
    outline = Color(darkTone.borderTintHex).copy(
        alpha = if (darkTone == DarkTone.GITHUB_DARK) 0.90f else 0.30f
    )
)

@Composable
fun ToeicFlashcardTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTone: DarkTone = DarkTone.OLED_OBSIDIAN,
    glassOpacity: Float? = null,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val resolvedGlassOpacity = (glassOpacity ?: UserPreferences.defaultGlassOpacity(isDark))
        .coerceIn(UserPreferences.MIN_GLASS_OPACITY, UserPreferences.MAX_GLASS_OPACITY)

    val colorScheme = remember(isDark, darkTone) {
        if (isDark) buildDarkColorScheme(darkTone) else LightColorScheme
    }
    val sm2Colors = remember(isDark, darkTone) {
        if (isDark) buildDarkSm2Colors(darkTone) else LightSm2Colors
    }
    val glassColors = remember(isDark, darkTone, resolvedGlassOpacity) {
        buildLiquidGlassColors(
            isDark = isDark,
            darkTone = darkTone,
            glassOpacity = resolvedGlassOpacity
        )
    }

    CompositionLocalProvider(
        LocalGlassOpacity provides resolvedGlassOpacity,
        LocalDarkTone provides darkTone,
        LocalSm2RatingColors provides sm2Colors,
        LocalLiquidGlassColors provides glassColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ToeicTypography,
            content = content
        )
    }
}
