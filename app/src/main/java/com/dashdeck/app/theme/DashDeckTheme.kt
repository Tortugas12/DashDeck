package com.dashdeck.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders as GlanceMaterial3ColorProviders

val LocalDashDeckThemeTokens = staticCompositionLocalOf { BuiltInThemes.FollowSystem }

fun ThemeTokens.toComposeColorScheme(isSystemDark: Boolean): ColorScheme {
    val effective = if (presetId == ThemePresetId.FOLLOW_SYSTEM || followSystemDarkMode) {
        if (isSystemDark) BuiltInThemes.FollowSystemDarkFallback else BuiltInThemes.FollowSystem
    } else {
        validated()
    }

    val background = parseHexColor(effective.backgroundHex).copy(alpha = effective.backgroundAlpha)
    val surface = parseHexColor(effective.surfaceHex)
    val accent = parseHexColor(effective.accentHex)
    val textPrimary = parseHexColor(effective.textPrimaryHex)
    val textSecondary = parseHexColor(effective.textSecondaryHex)
    val border = parseHexColor(effective.borderHex)
    val onAccent = if (accent.luminanceApprox() > 0.55f) Color(0xFF0F172A) else Color.White

    return if (effective.isDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accent.copy(alpha = 0.22f),
            onPrimaryContainer = textPrimary,
            secondary = accent.copy(alpha = 0.85f),
            onSecondary = onAccent,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surface.copy(alpha = 0.88f),
            onSurfaceVariant = textSecondary,
            outline = border,
            outlineVariant = border.copy(alpha = 0.6f),
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accent.copy(alpha = 0.14f),
            onPrimaryContainer = textPrimary,
            secondary = accent.copy(alpha = 0.85f),
            onSecondary = onAccent,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surface.copy(alpha = 0.94f),
            onSurfaceVariant = textSecondary,
            outline = border,
            outlineVariant = border.copy(alpha = 0.6f),
        )
    }
}

@Composable
fun DashDeckTheme(
    tokens: ThemeTokens = BuiltInThemes.FollowSystem,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = tokens.toComposeColorScheme(isSystemDark = systemDark)

    val resolvedTokens = if (tokens.presetId == ThemePresetId.FOLLOW_SYSTEM || tokens.followSystemDarkMode) {
        if (systemDark) BuiltInThemes.FollowSystemDarkFallback else BuiltInThemes.FollowSystem
    } else {
        tokens.validated()
    }

    CompositionLocalProvider(LocalDashDeckThemeTokens provides resolvedTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

/**
 * Resolves a Glance [ColorProviders] for widget rendering.
 * When [ThemePresetId.FOLLOW_SYSTEM] is active, uses a Day/Night adaptive pair so widgets
 * automatically flip when Android or Googlebook OS switches Light/Dark mode.
 */
fun ThemeTokens.toGlanceColorProviders(): ColorProviders {
    return if (presetId == ThemePresetId.FOLLOW_SYSTEM || followSystemDarkMode) {
        GlanceMaterial3ColorProviders(
            light = BuiltInThemes.FollowSystem.toComposeColorScheme(isSystemDark = false),
            dark = BuiltInThemes.FollowSystemDarkFallback.toComposeColorScheme(isSystemDark = true),
        )
    } else {
        val scheme = toComposeColorScheme(isSystemDark = isDark)
        GlanceMaterial3ColorProviders(light = scheme, dark = scheme)
    }
}

/**
 * Helper for direct Day/Night border or translucent container colors in Glance widgets.
 */
fun ThemeTokens.glanceContainerColorProvider() =
    if (presetId == ThemePresetId.FOLLOW_SYSTEM || followSystemDarkMode) {
        ColorProvider(
            day = parseHexColor(BuiltInThemes.FollowSystem.backgroundHex).copy(alpha = backgroundAlpha),
            night = parseHexColor(BuiltInThemes.FollowSystemDarkFallback.backgroundHex).copy(alpha = backgroundAlpha),
        )
    } else {
        val color = parseHexColor(backgroundHex).copy(alpha = backgroundAlpha)
        ColorProvider(day = color, night = color)
    }

private fun Color.luminanceApprox(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
