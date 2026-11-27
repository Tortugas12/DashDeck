package com.dashdeck.app.data.backup

import com.dashdeck.app.theme.BuiltInThemes
import com.dashdeck.app.theme.ThemeTokens
import com.dashdeck.app.theme.normalizeHexColor
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupFolder(
    val name: String,
    val iconTintHex: String = "#6366F1",
    val sortOrder: Int = 0,
    val items: List<BackupLauncherItem> = emptyList(),
)

@Serializable
data class BackupLauncherItem(
    val packageName: String,
    val activityName: String,
    val label: String,
    val sortOrder: Int = 0,
)

@Serializable
data class DashDeckBackup(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtEpochMs: Long = 0L,
    val globalTheme: ThemeTokens = BuiltInThemes.FollowSystem,
    val customThemePresets: List<ThemeTokens> = emptyList(),
    val folders: List<BackupFolder> = emptyList(),
    val topLevelApps: List<BackupLauncherItem> = emptyList(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val MAX_SUPPORTED_SCHEMA_VERSION = 1
    }
}

class BackupSerializer(
    private val json: Json = defaultJson,
) {
    fun encode(backup: DashDeckBackup): String {
        val sanitized = sanitize(backup)
        return json.encodeToString(sanitized)
    }

    fun decode(rawJson: String): Result<DashDeckBackup> = runCatching {
        require(rawJson.isNotBlank()) { "Backup JSON cannot be empty." }
        val parsed = json.decodeFromString<DashDeckBackup>(rawJson)
        require(parsed.schemaVersion in 1..DashDeckBackup.MAX_SUPPORTED_SCHEMA_VERSION) {
            "Unsupported backup schema version: ${parsed.schemaVersion}"
        }
        sanitize(parsed)
    }

    private fun sanitize(backup: DashDeckBackup): DashDeckBackup {
        return backup.copy(
            globalTheme = backup.globalTheme.validated(),
            customThemePresets = backup.customThemePresets.map { it.validated() },
            folders = backup.folders
                .filter { it.name.isNotBlank() }
                .mapIndexed { idx, folder ->
                    folder.copy(
                        name = folder.name.trim(),
                        iconTintHex = normalizeHexColor(folder.iconTintHex, "#6366F1"),
                        sortOrder = if (folder.sortOrder >= 0) folder.sortOrder else idx,
                        items = folder.items.filter { it.packageName.isNotBlank() },
                    )
                },
            topLevelApps = backup.topLevelApps.filter { it.packageName.isNotBlank() },
        )
    }

    companion object {
        val defaultJson: Json = Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}
