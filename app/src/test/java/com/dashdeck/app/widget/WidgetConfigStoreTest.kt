package com.dashdeck.app.widget

import com.dashdeck.app.data.db.DashboardModuleType
import com.dashdeck.app.data.db.FolderStyle
import com.dashdeck.app.data.db.ModuleCardSize
import com.dashdeck.app.data.db.ModuleSizeTier
import com.dashdeck.app.data.db.WidgetConfigEntity
import com.dashdeck.app.data.db.WidgetType
import com.dashdeck.app.theme.BuiltInThemes
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetConfigStoreTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun roundTripPreservesLayoutRowsClockSettingsAndModuleSizes() {
        val rows = listOf(
            WidgetRowSpec(
                id = "r1",
                template = RowTemplate.LEFT_TALL_RIGHT_STACK,
                splitRatio = SplitWidthRatio.LEFT_60_40,
                leftSlots = listOf(
                    WidgetSlotSpec(DashboardModuleType.CALENDAR, ModuleSizeTier.LARGE, CardHeightPreset.TALL),
                ),
                rightSlots = listOf(
                    WidgetSlotSpec(DashboardModuleType.CLOCK, ModuleSizeTier.COMPACT, CardHeightPreset.COMPACT),
                    WidgetSlotSpec(DashboardModuleType.MEDIA, ModuleSizeTier.STANDARD, CardHeightPreset.AUTO),
                ),
            ),
        )
        val original = WidgetConfigModel(
            appWidgetId = 77,
            widgetType = WidgetType.MODULAR,
            enabledModules = listOf(DashboardModuleType.CALENDAR, DashboardModuleType.CLOCK, DashboardModuleType.MEDIA),
            themeTokens = BuiltInThemes.FrostedGlass,
            folderStyle = FolderStyle.POPUP,
            use24HourClock = true,
            showQuickSettingsGear = true,
            selectedNoteIds = listOf(1L, 5L),
            selectedFolderIds = listOf(3L),
            moduleSizes = mapOf(
                DashboardModuleType.CALENDAR to ModuleCardSize(tier = ModuleSizeTier.LARGE, fullWidth = false),
            ),
            layoutRows = rows,
            clockStyle = ClockStylePreset.EDITORIAL,
            clockAlignment = ClockHorizontalAlignment.LEFT,
            clockShowWeather = true,
        )

        val entity = original.toEntity(json)
        assertFalse(entity.privacyHideNotificationText)
        val restored = entity.toModel(json)

        assertEquals(original, restored)
        assertEquals(RowTemplate.LEFT_TALL_RIGHT_STACK, restored.layoutRows.single().template)
        assertEquals(SplitWidthRatio.LEFT_60_40, restored.layoutRows.single().splitRatio)
        assertEquals(ClockStylePreset.EDITORIAL, restored.clockStyle)
        assertEquals(ClockHorizontalAlignment.LEFT, restored.clockAlignment)
        assertTrue(restored.clockShowWeather)
    }

    @Test
    fun blankLayoutSpecMigratesLegacyEnabledModulesAndFullWidthSizes() {
        val legacyEntity = WidgetConfigEntity(
            appWidgetId = 55,
            widgetType = WidgetType.DASHBOARD.name,
            enabledModulesJson = json.encodeToString(
                listOf(DashboardModuleType.CLOCK, DashboardModuleType.NOTES, DashboardModuleType.MEDIA),
            ),
            themeTokensJson = json.encodeToString(BuiltInThemes.DarkSlate),
            moduleSizesJson = json.encodeToString(
                mapOf(
                    DashboardModuleType.CLOCK to ModuleCardSize(ModuleSizeTier.LARGE, fullWidth = true),
                ),
            ),
            layoutSpecJson = "",
        )

        val migrated = legacyEntity.toModel(json)
        assertEquals(2, migrated.layoutRows.size)
        assertEquals(RowTemplate.FULL_WIDTH, migrated.layoutRows[0].template)
        assertEquals(DashboardModuleType.CLOCK, migrated.layoutRows[0].leftSlots.single().module)
        assertEquals(ModuleSizeTier.LARGE, migrated.layoutRows[0].leftSlots.single().sizeTier)
        assertEquals(RowTemplate.SPLIT_TWO, migrated.layoutRows[1].template)
        assertEquals(DashboardModuleType.NOTES, migrated.layoutRows[1].leftSlots.single().module)
        assertEquals(DashboardModuleType.MEDIA, migrated.layoutRows[1].rightSlots.single().module)
    }

    @Test
    fun defaultsDifferBetweenFullDashboardAndModularWidgets() {
        val dashboardDefaults = WidgetConfigModel.defaultModulesFor(WidgetType.DASHBOARD)
        val modularDefaults = WidgetConfigModel.defaultModulesFor(WidgetType.MODULAR)

        assertEquals(6, dashboardDefaults.size)
        assertEquals(2, modularDefaults.size)
        assertTrue(DashboardModuleType.CLOCK in modularDefaults)
    }

    @Test
    fun malformedJsonFallsBackGracefullyWithoutCrashing() {
        val broken = WidgetConfigEntity(
            appWidgetId = 99,
            widgetType = "UNKNOWN_TYPE",
            enabledModulesJson = "{bad json",
            themeTokensJson = "not-json",
            folderStyle = "INVALID",
            selectedNoteIdsJson = "nope",
            selectedFolderIdsJson = "nope",
            moduleSizesJson = "{invalid-sizes",
            layoutSpecJson = "{invalid-layout",
            clockStyle = "UNKNOWN_STYLE",
            clockAlignment = "UNKNOWN_ALIGN",
        )

        val recovered = broken.toModel(json)
        assertEquals(99, recovered.appWidgetId)
        assertEquals(WidgetType.DASHBOARD, recovered.widgetType)
        assertEquals(6, recovered.enabledModules.size)
        assertEquals(BuiltInThemes.FollowSystem, recovered.themeTokens)
        assertEquals(FolderStyle.INLINE, recovered.folderStyle)
        assertEquals(ClockStylePreset.CLASSIC, recovered.clockStyle)
        assertEquals(ClockHorizontalAlignment.CENTER, recovered.clockAlignment)
        assertTrue(recovered.moduleSizes.isEmpty())
    }
}
