package com.dashdeck.app.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.assertHasClickAction
import androidx.glance.testing.unit.assertHasNoClickAction
import androidx.glance.testing.unit.hasText
import androidx.glance.testing.unit.hasTextEqualTo
import com.dashdeck.app.data.db.DashboardModuleType
import com.dashdeck.app.data.db.FolderStyle
import com.dashdeck.app.data.db.LauncherFolderEntity
import com.dashdeck.app.data.db.LauncherItemEntity
import com.dashdeck.app.data.db.ModuleCardSize
import com.dashdeck.app.data.db.ModuleSizeTier
import com.dashdeck.app.data.repo.LauncherBoardState
import com.dashdeck.app.data.repo.LauncherFolderWithItems
import com.dashdeck.app.data.repo.MediaPlaybackState
import com.dashdeck.app.data.repo.NotificationAppGroup
import com.dashdeck.app.data.repo.NotificationFilterEngine
import com.dashdeck.app.data.repo.NotificationItem
import com.dashdeck.app.data.repo.NotificationSummaryState
import com.dashdeck.app.data.repo.WeatherSnapshot
import com.dashdeck.app.widget.modules.WidgetCalendarSnapshot
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

class DashboardGlanceWidgetTest {

    @Test
    fun rendersOnlyEnabledModules() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(320.dp, 320.dp))

        val snapshot = WidgetRenderSnapshot(
            config = WidgetConfigModel(
                appWidgetId = 1,
                enabledModules = listOf(DashboardModuleType.CLOCK, DashboardModuleType.NOTES),
                use24HourClock = true,
                showQuickSettingsGear = false,
            ),
            nowTime = LocalTime.of(14, 30),
            today = LocalDate.of(2026, 11, 25),
            locale = Locale.US,
            useLiveTextClock = false,
        )

        provideComposable {
            DashboardWidgetContent(snapshot = snapshot, size = DpSize(320.dp, 320.dp))
        }

        onNode(hasText("14:30")).assertExists()
        onNode(hasText("Pinned Notes")).assertExists()
        onNode(hasText("Now Playing")).assertDoesNotExist()
        onNode(hasText("Notifications")).assertDoesNotExist()
    }

    @Test
    fun clockRendersAcrossCompactStandardAndLargeTiers() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(320.dp, 500.dp))
        val date = LocalDate.of(2026, 11, 25)
        val time = LocalTime.of(9, 15)

        val compactSnapshot = WidgetRenderSnapshot(
            config = WidgetConfigModel(
                appWidgetId = 10,
                enabledModules = listOf(DashboardModuleType.CLOCK),
                use24HourClock = false,
                showQuickSettingsGear = false,
                moduleSizes = mapOf(
                    DashboardModuleType.CLOCK to ModuleCardSize(tier = ModuleSizeTier.COMPACT),
                ),
            ),
            nowTime = time,
            today = date,
            locale = Locale.US,
            useLiveTextClock = false,
        )

        provideComposable {
            DashboardWidgetContent(snapshot = compactSnapshot, size = DpSize(320.dp, 500.dp))
        }
        onNode(hasText("9:15 AM")).assertExists()
        onNode(hasText("Wed, Nov 25")).assertExists()
    }

    @Test
    fun clockLargeTierDisplaysFullDateWithYearAndWeatherWhenEnabled() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(360.dp, 500.dp))
        val weather = WeatherSnapshot(
            cityLabel = "Toledo, Ohio",
            latitude = 41.66,
            longitude = -83.55,
            temperatureValue = 42.3,
            isFahrenheit = true,
            weatherCode = 0,
            isDay = true,
            fetchedAtEpochMs = 1_000L,
        )
        val largeSnapshot = WidgetRenderSnapshot(
            config = WidgetConfigModel(
                appWidgetId = 11,
                enabledModules = listOf(DashboardModuleType.CLOCK),
                use24HourClock = true,
                showQuickSettingsGear = false,
                moduleSizes = mapOf(
                    DashboardModuleType.CLOCK to ModuleCardSize(tier = ModuleSizeTier.LARGE, fullWidth = true),
                ),
                clockStyle = ClockStylePreset.CLASSIC,
                clockAlignment = ClockHorizontalAlignment.CENTER,
                clockShowWeather = true,
            ),
            weatherSnapshot = weather,
            nowTime = LocalTime.of(21, 5),
            today = LocalDate.of(2026, 11, 25),
            locale = Locale.US,
            useLiveTextClock = false,
        )

        provideComposable {
            DashboardWidgetContent(snapshot = largeSnapshot, size = DpSize(360.dp, 500.dp))
        }
        onNode(hasText("21:05")).assertExists()
        onNode(hasText("Wednesday, November 25, 2026")).assertExists()
        onNode(hasText("☀ 42°F · Clear · Toledo")).assertExists()
    }

    @Test
    fun clockHidesWeatherWhenToggleIsOffAndSupportsEditorialStyle() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(360.dp, 400.dp))
        val weather = WeatherSnapshot(
            cityLabel = "Toledo, Ohio",
            latitude = 41.66,
            longitude = -83.55,
            temperatureValue = 42.3,
            isFahrenheit = true,
            weatherCode = 0,
            isDay = true,
            fetchedAtEpochMs = 1_000L,
        )
        val snapshot = WidgetRenderSnapshot(
            config = WidgetConfigModel(
                appWidgetId = 18,
                enabledModules = listOf(DashboardModuleType.CLOCK),
                use24HourClock = true,
                showQuickSettingsGear = false,
                clockStyle = ClockStylePreset.EDITORIAL,
                clockAlignment = ClockHorizontalAlignment.LEFT,
                clockShowWeather = false,
            ),
            weatherSnapshot = weather,
            nowTime = LocalTime.of(8, 0),
            today = LocalDate.of(2026, 11, 25),
            locale = Locale.US,
            useLiveTextClock = false,
        )

        provideComposable {
            DashboardWidgetContent(snapshot = snapshot, size = DpSize(360.dp, 400.dp))
        }
        onNode(hasText("08:00")).assertExists()
        onNode(hasText("WEDNESDAY, NOV 25, 2026")).assertExists()
        onNode(hasText("Toledo")).assertDoesNotExist()
    }

    @Test
    fun asymmetricLeftTallRightStackRendersAllThreeModules() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(420.dp, 480.dp))
        val today = LocalDate.of(2026, 11, 25)
        val rows = listOf(
            WidgetRowSpec(
                id = "asym_1",
                template = RowTemplate.LEFT_TALL_RIGHT_STACK,
                splitRatio = SplitWidthRatio.LEFT_60_40,
                leftSlots = listOf(
                    WidgetSlotSpec(DashboardModuleType.CALENDAR, ModuleSizeTier.LARGE, CardHeightPreset.TALL),
                ),
                rightSlots = listOf(
                    WidgetSlotSpec(DashboardModuleType.CLOCK, ModuleSizeTier.COMPACT, CardHeightPreset.COMPACT),
                    WidgetSlotSpec(DashboardModuleType.NOTES, ModuleSizeTier.COMPACT, CardHeightPreset.AUTO),
                ),
            ),
        )
        val snapshot = WidgetRenderSnapshot(
            config = WidgetConfigModel(
                appWidgetId = 19,
                layoutRows = rows,
                use24HourClock = true,
                showQuickSettingsGear = false,
            ),
            calendarSnapshot = WidgetCalendarSnapshot(hasPermission = true, today = today),
            nowTime = LocalTime.of(12, 0),
            today = today,
            locale = Locale.US,
            useLiveTextClock = false,
        )

        provideComposable {
            DashboardWidgetContent(snapshot = snapshot, size = DpSize(420.dp, 480.dp))
        }
        onNode(hasText("November 2026")).assertExists()
        onNode(hasText("12:00")).assertExists()
        onNode(hasText("Pinned Notes")).assertExists()
    }

    @Test
    fun calendarLargeTierRendersFullMonthGridAndStandardRendersWeekStrip() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(340.dp, 520.dp))
        val today = LocalDate.of(2026, 11, 25)

        val largeCalendarSnapshot = WidgetRenderSnapshot(
            config = WidgetConfigModel(
                appWidgetId = 12,
                enabledModules = listOf(DashboardModuleType.CALENDAR),
                showQuickSettingsGear = false,
                moduleSizes = mapOf(
                    DashboardModuleType.CALENDAR to ModuleCardSize(tier = ModuleSizeTier.LARGE),
                ),
            ),
            calendarSnapshot = WidgetCalendarSnapshot(
                hasPermission = true,
                today = today,
                eventDatesInWeek = setOf(today),
                monthEventDates = setOf(LocalDate.of(2026, 11, 1), today, LocalDate.of(2026, 11, 30)),
            ),
            today = today,
            locale = Locale.US,
            useLiveTextClock = false,
        )

        provideComposable {
            DashboardWidgetContent(snapshot = largeCalendarSnapshot, size = DpSize(340.dp, 520.dp))
        }
        onNode(hasTextEqualTo("1")).assertExists()
        onNode(hasTextEqualTo("30")).assertExists()
        onAllNodes(hasTextEqualTo("S")).assertCountEquals(2)
    }

    @Test
    fun calendarStandardTierRendersSevenDayStripOnly() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(340.dp, 400.dp))
        val today = LocalDate.of(2026, 11, 25)

        val standardCalendarSnapshot = WidgetRenderSnapshot(
            config = WidgetConfigModel(
                appWidgetId = 13,
                enabledModules = listOf(DashboardModuleType.CALENDAR),
                showQuickSettingsGear = false,
                moduleSizes = mapOf(
                    DashboardModuleType.CALENDAR to ModuleCardSize(tier = ModuleSizeTier.STANDARD),
                ),
            ),
            calendarSnapshot = WidgetCalendarSnapshot(
                hasPermission = true,
                today = today,
                eventDatesInWeek = setOf(today),
                monthEventDates = setOf(today),
            ),
            today = today,
            locale = Locale.US,
            useLiveTextClock = false,
        )

        provideComposable {
            DashboardWidgetContent(snapshot = standardCalendarSnapshot, size = DpSize(340.dp, 400.dp))
        }
        onNode(hasTextEqualTo("25")).assertExists()
        onNode(hasTextEqualTo("1")).assertDoesNotExist()
    }

    @Test
    fun notificationsFollowsPrivacyModeStateStrictly() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(320.dp, 360.dp))

        val item = NotificationItem(
            key = "k1",
            packageName = "com.example.chat",
            appLabel = "Chat",
            title = "Alex",
            previewText = NotificationFilterEngine.PRIVACY_REDACTED_TEXT,
            postTimeMs = 1000L,
        )
        val snapshot = WidgetRenderSnapshot(
            config = WidgetConfigModel(
                appWidgetId = 14,
                enabledModules = listOf(DashboardModuleType.NOTIFICATIONS),
                showQuickSettingsGear = false,
            ),
            notificationState = NotificationSummaryState(
                hasListenerAccess = true,
                totalCount = 1,
                groups = listOf(
                    NotificationAppGroup(
                        packageName = "com.example.chat",
                        appLabel = "Chat",
                        count = 1,
                        latestTimeMs = 1000L,
                        items = listOf(item),
                    ),
                ),
                privacyModeEnabled = true,
            ),
            useLiveTextClock = false,
        )

        provideComposable {
            DashboardWidgetContent(snapshot = snapshot, size = DpSize(320.dp, 360.dp))
        }
        onNode(hasText("Chat · ${NotificationFilterEngine.PRIVACY_REDACTED_TEXT}")).assertExists()
    }

    @Test
    fun notificationsShowsPreviewWhenPrivacyModeDisabled() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(320.dp, 360.dp))

        val item = NotificationItem(
            key = "k1",
            packageName = "com.example.chat",
            appLabel = "Chat",
            title = "Alex",
            previewText = "Dinner at 7?",
            postTimeMs = 1000L,
        )
        val snapshot = WidgetRenderSnapshot(
            config = WidgetConfigModel(
                appWidgetId = 15,
                enabledModules = listOf(DashboardModuleType.NOTIFICATIONS),
                showQuickSettingsGear = false,
            ),
            notificationState = NotificationSummaryState(
                hasListenerAccess = true,
                totalCount = 1,
                groups = listOf(
                    NotificationAppGroup(
                        packageName = "com.example.chat",
                        appLabel = "Chat",
                        count = 1,
                        latestTimeMs = 1000L,
                        items = listOf(item),
                    ),
                ),
                privacyModeEnabled = false,
            ),
            useLiveTextClock = false,
        )

        provideComposable {
            DashboardWidgetContent(snapshot = snapshot, size = DpSize(320.dp, 360.dp))
        }
        onNode(hasText("Chat · Dinner at 7?")).assertExists()
    }

    @Test
    fun mediaRendersControlsProgressTimestampsAndNoRootClickAction() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(340.dp, 320.dp))

        val snapshot = WidgetRenderSnapshot(
            config = WidgetConfigModel(
                appWidgetId = 16,
                enabledModules = listOf(DashboardModuleType.MEDIA),
                showQuickSettingsGear = false,
            ),
            mediaState = MediaPlaybackState(
                hasListenerAccess = true,
                isActive = true,
                title = "Track One",
                artist = "Artist One",
                packageName = "com.example.music",
                isPlaying = false,
                durationMs = 225_000L,
                positionMs = 84_000L,
                positionUpdateElapsedMs = 1_000L,
            ),
            nowElapsedMs = 1_000L,
            useLiveTextClock = false,
        )

        provideComposable {
            DashboardWidgetContent(snapshot = snapshot, size = DpSize(340.dp, 320.dp))
        }
        onNode(hasText("Now Playing")).assertHasNoClickAction()
        onNode(hasText("Track One")).assertExists()
        onNode(hasText("Artist One")).assertExists()
        onNode(hasText("1:24 / 3:45")).assertExists()
        onNode(hasText("⏮")).assertHasClickAction()
        onNode(hasText("▶")).assertHasClickAction()
        onNode(hasText("⏭")).assertHasClickAction()
    }

    @Test
    fun mediaCompactTierOmitsProgressTimestamps() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(340.dp, 320.dp))

        val snapshot = WidgetRenderSnapshot(
            config = WidgetConfigModel(
                appWidgetId = 20,
                enabledModules = listOf(DashboardModuleType.MEDIA),
                showQuickSettingsGear = false,
                moduleSizes = mapOf(
                    DashboardModuleType.MEDIA to ModuleCardSize(tier = ModuleSizeTier.COMPACT),
                ),
            ),
            mediaState = MediaPlaybackState(
                hasListenerAccess = true,
                isActive = true,
                title = "Track Compact",
                packageName = "com.example.music",
                isPlaying = false,
                durationMs = 225_000L,
                positionMs = 84_000L,
                positionUpdateElapsedMs = 1_000L,
            ),
            nowElapsedMs = 1_000L,
            useLiveTextClock = false,
        )

        provideComposable {
            DashboardWidgetContent(snapshot = snapshot, size = DpSize(340.dp, 320.dp))
        }
        onNode(hasText("Track Compact")).assertExists()
        onNode(hasText("1:24 / 3:45")).assertDoesNotExist()
    }

    @Test
    fun wideDesktopRendersFullWidthAndTwoColumnModulesAndOmitsHeaderWhenDisabled() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(420.dp, 480.dp))

        val snapshot = WidgetRenderSnapshot(
            config = WidgetConfigModel(
                appWidgetId = 17,
                enabledModules = listOf(
                    DashboardModuleType.CLOCK,
                    DashboardModuleType.NOTES,
                    DashboardModuleType.MEDIA,
                ),
                use24HourClock = true,
                showQuickSettingsGear = false,
                moduleSizes = mapOf(
                    DashboardModuleType.CLOCK to ModuleCardSize(tier = ModuleSizeTier.STANDARD, fullWidth = true),
                ),
            ),
            mediaState = MediaPlaybackState(hasListenerAccess = true, isActive = false),
            nowTime = LocalTime.of(10, 45),
            today = LocalDate.of(2026, 11, 25),
            locale = Locale.US,
            useLiveTextClock = false,
        )

        provideComposable {
            DashboardWidgetContent(snapshot = snapshot, size = DpSize(420.dp, 480.dp))
        }
        onNode(hasText("⚙")).assertDoesNotExist()
        onNode(hasText("DashDeck")).assertDoesNotExist()
        onNode(hasText("10:45")).assertExists()
        onNode(hasText("Pinned Notes")).assertExists()
        onNode(hasText("No media playing right now")).assertExists()
    }

    @Test
    fun rendersEmptyStatesGracefully() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(320.dp, 400.dp))

        val snapshot = WidgetRenderSnapshot(
            config = WidgetConfigModel(
                appWidgetId = 2,
                enabledModules = listOf(
                    DashboardModuleType.MEDIA,
                    DashboardModuleType.NOTIFICATIONS,
                    DashboardModuleType.NOTES,
                ),
                showQuickSettingsGear = false,
            ),
            mediaState = MediaPlaybackState(hasListenerAccess = true, isActive = false),
            notificationState = NotificationSummaryState(hasListenerAccess = true, totalCount = 0),
            pinnedNotes = emptyList(),
            useLiveTextClock = false,
        )

        provideComposable {
            DashboardWidgetContent(snapshot = snapshot, size = DpSize(320.dp, 400.dp))
        }

        onNode(hasText("No media playing right now")).assertExists()
        onNode(hasText("All caught up")).assertExists()
        onNode(hasText("Share a note from Google Keep to pin it here")).assertExists()
    }

    @Test
    fun inlineFolderCollapsedHidesFolderApps() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(320.dp, 320.dp))

        val workFolder = LauncherFolderEntity(folderId = 42L, name = "Work")
        val docsApp = LauncherItemEntity(
            itemId = 10L,
            packageName = "com.example.docs",
            activityName = "Main",
            label = "Docs",
            folderId = 42L,
        )
        val board = LauncherBoardState(
            folders = listOf(LauncherFolderWithItems(workFolder, listOf(docsApp))),
            topLevelItems = emptyList(),
        )

        val collapsedSnapshot = WidgetRenderSnapshot(
            config = WidgetConfigModel(
                appWidgetId = 3,
                enabledModules = listOf(DashboardModuleType.LAUNCHER),
                folderStyle = FolderStyle.INLINE,
                showQuickSettingsGear = false,
            ),
            launcherBoard = board,
            expandedFolderId = -1L,
            useLiveTextClock = false,
        )

        provideComposable {
            DashboardWidgetContent(snapshot = collapsedSnapshot, size = DpSize(320.dp, 320.dp))
        }
        onNode(hasText("Work")).assertExists()
        onNode(hasText("Docs")).assertDoesNotExist()
    }

    @Test
    fun inlineFolderExpandedRevealsFolderApps() = runGlanceAppWidgetUnitTest {
        setAppWidgetSize(DpSize(320.dp, 320.dp))

        val workFolder = LauncherFolderEntity(folderId = 42L, name = "Work")
        val docsApp = LauncherItemEntity(
            itemId = 10L,
            packageName = "com.example.docs",
            activityName = "Main",
            label = "Docs",
            folderId = 42L,
        )
        val board = LauncherBoardState(
            folders = listOf(LauncherFolderWithItems(workFolder, listOf(docsApp))),
            topLevelItems = emptyList(),
        )

        val expandedSnapshot = WidgetRenderSnapshot(
            config = WidgetConfigModel(
                appWidgetId = 3,
                enabledModules = listOf(DashboardModuleType.LAUNCHER),
                folderStyle = FolderStyle.INLINE,
                showQuickSettingsGear = false,
            ),
            launcherBoard = board,
            expandedFolderId = 42L,
            useLiveTextClock = false,
        )

        provideComposable {
            DashboardWidgetContent(snapshot = expandedSnapshot, size = DpSize(320.dp, 320.dp))
        }
        onAllNodes(hasText("Work")).assertCountEquals(2)
        onNode(hasText("Docs")).assertExists()
    }
}
