package com.dashdeck.app.widget

import android.os.Build
import com.dashdeck.app.data.db.DashboardModuleType
import com.dashdeck.app.data.db.FolderStyle
import com.dashdeck.app.data.db.ModuleCardSize
import com.dashdeck.app.data.db.WidgetConfigDao
import com.dashdeck.app.data.db.WidgetConfigEntity
import com.dashdeck.app.data.db.WidgetType
import com.dashdeck.app.theme.BuiltInThemes
import com.dashdeck.app.theme.ThemeTokens
import kotlinx.serialization.json.Json

data class WidgetConfigModel(
    val appWidgetId: Int,
    val widgetType: WidgetType = WidgetType.DASHBOARD,
    val enabledModules: List<DashboardModuleType> = defaultModulesFor(widgetType),
    val themeTokens: ThemeTokens = BuiltInThemes.FollowSystem,
    val folderStyle: FolderStyle = FolderStyle.INLINE,
    val use24HourClock: Boolean = false,
    val showQuickSettingsGear: Boolean = defaultShowSettingsGear(),
    val selectedNoteIds: List<Long> = emptyList(),
    val selectedFolderIds: List<Long> = emptyList(),
    val moduleSizes: Map<DashboardModuleType, ModuleCardSize> = emptyMap(),
    val layoutRows: List<WidgetRowSpec> = legacyModulesToLayoutRows(enabledModules, moduleSizes),
    val clockStyle: ClockStylePreset = ClockStylePreset.CLASSIC,
    val clockAlignment: ClockHorizontalAlignment = ClockHorizontalAlignment.CENTER,
    val clockShowWeather: Boolean = false,
) {
    fun sizeFor(module: DashboardModuleType): ModuleCardSize = moduleSizes[module] ?: ModuleCardSize()

    fun effectiveLayoutRows(): List<WidgetRowSpec> =
        layoutRows.ifEmpty { legacyModulesToLayoutRows(enabledModules, moduleSizes) }

    companion object {
        fun defaultModulesFor(type: WidgetType): List<DashboardModuleType> = when (type) {
            WidgetType.DASHBOARD -> listOf(
                DashboardModuleType.CLOCK,
                DashboardModuleType.MEDIA,
                DashboardModuleType.CALENDAR,
                DashboardModuleType.NOTIFICATIONS,
                DashboardModuleType.NOTES,
                DashboardModuleType.LAUNCHER,
            )
            WidgetType.MODULAR -> listOf(
                DashboardModuleType.CLOCK,
                DashboardModuleType.MEDIA,
            )
        }

        fun defaultShowSettingsGear(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    }
}

class WidgetConfigRepository(
    private val dao: WidgetConfigDao,
    private val json: Json,
) {
    suspend fun getOrDefault(
        appWidgetId: Int,
        defaultType: WidgetType = WidgetType.DASHBOARD,
    ): WidgetConfigModel {
        val existing = dao.getConfig(appWidgetId) ?: return WidgetConfigModel(
            appWidgetId = appWidgetId,
            widgetType = defaultType,
            layoutRows = defaultLayoutRowsFor(defaultType),
        )
        return existing.toModel(json)
    }

    suspend fun save(model: WidgetConfigModel) {
        dao.upsertConfig(model.toEntity(json))
    }

    suspend fun updateThemeForAllWidgets(tokens: ThemeTokens) {
        dao.updateThemeForAllWidgets(json.encodeToString(tokens.validated()))
    }

    suspend fun delete(appWidgetId: Int) {
        dao.deleteConfig(appWidgetId)
    }
}

fun WidgetConfigEntity.toModel(json: Json): WidgetConfigModel {
    val parsedType = runCatching { WidgetType.valueOf(widgetType) }.getOrDefault(WidgetType.DASHBOARD)
    val parsedModules = runCatching {
        json.decodeFromString<List<DashboardModuleType>>(enabledModulesJson)
    }.getOrDefault(WidgetConfigModel.defaultModulesFor(parsedType))
    val parsedTokens = runCatching {
        json.decodeFromString<ThemeTokens>(themeTokensJson).validated()
    }.getOrDefault(BuiltInThemes.FollowSystem)
    val parsedFolderStyle = runCatching { FolderStyle.valueOf(folderStyle) }.getOrDefault(FolderStyle.INLINE)
    val parsedNoteIds = runCatching {
        json.decodeFromString<List<Long>>(selectedNoteIdsJson)
    }.getOrDefault(emptyList())
    val parsedFolderIds = runCatching {
        json.decodeFromString<List<Long>>(selectedFolderIdsJson)
    }.getOrDefault(emptyList())
    val parsedModuleSizes = runCatching {
        json.decodeFromString<Map<DashboardModuleType, ModuleCardSize>>(moduleSizesJson)
    }.getOrDefault(emptyMap())
    val parsedLayoutRows = if (layoutSpecJson.isNotBlank()) {
        runCatching {
            json.decodeFromString<List<WidgetRowSpec>>(layoutSpecJson)
        }.getOrElse { legacyModulesToLayoutRows(parsedModules, parsedModuleSizes) }
    } else {
        legacyModulesToLayoutRows(parsedModules, parsedModuleSizes)
    }
    val parsedClockStyle = runCatching { ClockStylePreset.valueOf(clockStyle) }
        .getOrDefault(ClockStylePreset.CLASSIC)
    val parsedClockAlignment = runCatching { ClockHorizontalAlignment.valueOf(clockAlignment) }
        .getOrDefault(ClockHorizontalAlignment.CENTER)

    val derivedModules = parsedLayoutRows
        .flatMap { it.allSlots() }
        .map { it.module }
        .ifEmpty { parsedModules }

    return WidgetConfigModel(
        appWidgetId = appWidgetId,
        widgetType = parsedType,
        enabledModules = derivedModules,
        themeTokens = parsedTokens,
        folderStyle = parsedFolderStyle,
        use24HourClock = use24HourClock,
        showQuickSettingsGear = showQuickSettingsGear,
        selectedNoteIds = parsedNoteIds,
        selectedFolderIds = parsedFolderIds,
        moduleSizes = parsedModuleSizes,
        layoutRows = parsedLayoutRows,
        clockStyle = parsedClockStyle,
        clockAlignment = parsedClockAlignment,
        clockShowWeather = showWeather,
    )
}

fun WidgetConfigModel.toEntity(json: Json): WidgetConfigEntity {
    val rowsToPersist = layoutRows
    val flatModules = rowsToPersist.flatMap { it.allSlots() }.map { it.module }
    return WidgetConfigEntity(
        appWidgetId = appWidgetId,
        widgetType = widgetType.name,
        enabledModulesJson = json.encodeToString(flatModules),
        themeTokensJson = json.encodeToString(themeTokens.validated()),
        folderStyle = folderStyle.name,
        use24HourClock = use24HourClock,
        showQuickSettingsGear = showQuickSettingsGear,
        selectedNoteIdsJson = json.encodeToString(selectedNoteIds),
        selectedFolderIdsJson = json.encodeToString(selectedFolderIds),
        privacyHideNotificationText = false,
        moduleSizesJson = json.encodeToString(moduleSizes),
        layoutSpecJson = json.encodeToString(rowsToPersist),
        clockStyle = clockStyle.name,
        clockAlignment = clockAlignment.name,
        showWeather = clockShowWeather,
    )
}
