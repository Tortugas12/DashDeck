package com.dashdeck.app.theme

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModelsTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun allFiveUnifiedBuiltInPresetsArePresentAndOrdered() {
        val presetIds = BuiltInThemes.allPresets.map { it.presetId }
        assertEquals(
            listOf(
                ThemePresetId.FOLLOW_SYSTEM,
                ThemePresetId.DARK_SLATE,
                ThemePresetId.LIGHT,
                ThemePresetId.FROSTED_GLASS,
                ThemePresetId.AMOLED_BLACK,
            ),
            presetIds,
        )
    }

    @Test
    fun followSystemAndPresetPaletteValuesMatchSpec() {
        assertTrue(BuiltInThemes.FollowSystem.followSystemDarkMode)
        assertEquals(ThemePresetId.FOLLOW_SYSTEM, BuiltInThemes.FollowSystem.presetId)
        assertFalse(BuiltInThemes.AmoledBlack.followSystemDarkMode)
        assertEquals("#111418", BuiltInThemes.DarkSlate.backgroundHex)
        assertEquals("#181C22", BuiltInThemes.DarkSlate.surfaceHex)
        assertEquals(0.94f, BuiltInThemes.DarkSlate.backgroundAlpha, 0.001f)
        assertEquals("#F8FAFC", BuiltInThemes.Light.backgroundHex)
        assertEquals(0.72f, BuiltInThemes.FrostedGlass.backgroundAlpha, 0.001f)
        assertEquals("#000000", BuiltInThemes.AmoledBlack.backgroundHex)
    }

    @Test
    fun legacyPresetIdsDeserializeCleanlyToUnifiedPresets() {
        assertEquals(ThemePresetId.FOLLOW_SYSTEM, json.decodeFromString<ThemePresetId>("\"SYSTEM_AUTO\""))
        assertEquals(ThemePresetId.AMOLED_BLACK, json.decodeFromString<ThemePresetId>("\"MIDNIGHT_OLED\""))
        assertEquals(ThemePresetId.LIGHT, json.decodeFromString<ThemePresetId>("\"CLEAN_PAPER_LIGHT\""))
        assertEquals(ThemePresetId.DARK_SLATE, json.decodeFromString<ThemePresetId>("\"NORD_SLATE\""))
        assertEquals(ThemePresetId.FOLLOW_SYSTEM, json.decodeFromString<ThemePresetId>("\"UNKNOWN_FUTURE\""))
    }

    @Test
    fun validatedClampsOutOfRangeTokensDownToTwentyPercentAlpha() {
        val raw = ThemeTokens(
            presetId = ThemePresetId.CUSTOM,
            followSystemDarkMode = false,
            backgroundHex = "not-a-color",
            surfaceHex = "1e293b",
            accentHex = "#zzzzzz",
            backgroundAlpha = 0.05f,
            cornerRadiusDp = 99,
            contentSpacingDp = 1,
        )
        val validated = raw.validated()
        assertEquals("#111418", validated.backgroundHex)
        assertEquals("#1E293B", validated.surfaceHex)
        assertEquals("#6366F1", validated.accentHex)
        assertEquals(0.20f, validated.backgroundAlpha, 0.001f)
        assertEquals(32, validated.cornerRadiusDp)
        assertEquals(4, validated.contentSpacingDp)
    }

    @Test
    fun withAutoContrastTextDerivesReadableForegroundsFromSurfaceLuminance() {
        val lightCustom = ThemeTokens(
            presetId = ThemePresetId.CUSTOM,
            surfaceHex = "#FFFFFF",
        ).withAutoContrastText()
        assertFalse(lightCustom.isDark)
        assertEquals("#0F172A", lightCustom.textPrimaryHex)

        val darkCustom = ThemeTokens(
            presetId = ThemePresetId.CUSTOM,
            surfaceHex = "#121216",
        ).withAutoContrastText()
        assertTrue(darkCustom.isDark)
        assertEquals("#F8FAFC", darkCustom.textPrimaryHex)
    }

    @Test
    fun parseHexColorToArgbLongSupportsRgbAndArgb() {
        assertEquals(0xFF000000L, parseHexColorToArgbLong("#000000"))
        assertEquals(0xFF88C0D0L, parseHexColorToArgbLong("#88C0D0"))
        assertEquals(0x80FFFFFFL, parseHexColorToArgbLong("#80FFFFFF"))
        assertEquals(0xFF111318L, parseHexColorToArgbLong("invalid", fallback = 0xFF111318L))
    }
}
