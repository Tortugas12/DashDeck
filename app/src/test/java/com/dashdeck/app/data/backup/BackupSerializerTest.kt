package com.dashdeck.app.data.backup

import com.dashdeck.app.theme.BuiltInThemes
import com.dashdeck.app.theme.ThemePresetId
import com.dashdeck.app.theme.ThemeTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializerTest {

    private val serializer = BackupSerializer()

    @Test
    fun roundTripPreservesThemesFoldersAndApps() {
        val original = DashDeckBackup(
            schemaVersion = DashDeckBackup.CURRENT_SCHEMA_VERSION,
            exportedAtEpochMs = 1_764_000_000_000L,
            globalTheme = BuiltInThemes.DarkSlate,
            customThemePresets = listOf(
                ThemeTokens(
                    presetId = ThemePresetId.CUSTOM,
                    followSystemDarkMode = false,
                    backgroundHex = "#101010",
                    surfaceHex = "#202020",
                    accentHex = "#FF6B6B",
                    backgroundAlpha = 0.85f,
                    cornerRadiusDp = 16,
                    contentSpacingDp = 10,
                ),
            ),
            folders = listOf(
                BackupFolder(
                    name = "Productivity",
                    iconTintHex = "#38BDF8",
                    sortOrder = 0,
                    items = listOf(
                        BackupLauncherItem(
                            packageName = "com.google.android.keep",
                            activityName = "com.google.android.keep.activities.BrowseActivity",
                            label = "Keep",
                            sortOrder = 0,
                        ),
                    ),
                ),
            ),
            topLevelApps = listOf(
                BackupLauncherItem(
                    packageName = "com.android.chrome",
                    activityName = "com.google.android.apps.chrome.Main",
                    label = "Chrome",
                    sortOrder = 0,
                ),
            ),
        )

        val encoded = serializer.encode(original)
        val decoded = serializer.decode(encoded).getOrThrow()

        assertEquals(original, decoded)
    }

    @Test
    fun decodeRejectsUnsupportedFutureSchemaVersion() {
        val futureJson = """
            {
              "schemaVersion": 99,
              "exportedAtEpochMs": 0
            }
        """.trimIndent()

        val result = serializer.decode(futureJson)
        assertTrue(result.isFailure)
    }

    @Test
    fun decodeMapsLegacyThemePresetIdsAndSanitizesBlankFolders() {
        val jsonWithLegacyTheme = """
            {
              "schemaVersion": 1,
              "unknownFutureField": "ignored",
              "globalTheme": {
                "presetId": "SYSTEM_AUTO",
                "followSystemDarkMode": true,
                "backgroundAlpha": 5.0
              },
              "customThemePresets": [
                { "presetId": "MIDNIGHT_OLED" },
                { "presetId": "NORD_SLATE" },
                { "presetId": "CLEAN_PAPER_LIGHT" }
              ],
              "folders": [
                { "name": "   ", "items": [] },
                { "name": "  Work  ", "iconTintHex": "bad-hex", "items": [
                  { "packageName": "", "activityName": "a", "label": "Invalid" },
                  { "packageName": "com.example.docs", "activityName": "Main", "label": "Docs" }
                ]}
              ]
            }
        """.trimIndent()

        val decoded = serializer.decode(jsonWithLegacyTheme).getOrThrow()
        assertEquals(ThemePresetId.FOLLOW_SYSTEM, decoded.globalTheme.presetId)
        assertEquals(1.0f, decoded.globalTheme.backgroundAlpha, 0.001f)
        assertEquals(
            listOf(ThemePresetId.AMOLED_BLACK, ThemePresetId.DARK_SLATE, ThemePresetId.LIGHT),
            decoded.customThemePresets.map { it.presetId },
        )
        assertEquals(1, decoded.folders.size)
        assertEquals("Work", decoded.folders.first().name)
        assertEquals("#6366F1", decoded.folders.first().iconTintHex)
        assertEquals(1, decoded.folders.first().items.size)
        assertEquals("com.example.docs", decoded.folders.first().items.first().packageName)
    }
}
