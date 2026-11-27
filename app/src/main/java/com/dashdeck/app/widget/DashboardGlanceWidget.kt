package com.dashdeck.app.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.dashdeck.app.DashDeckApplication
import com.dashdeck.app.data.db.DashboardModuleType
import com.dashdeck.app.data.db.WidgetType
import com.dashdeck.app.data.repo.LauncherBoardState
import com.dashdeck.app.data.repo.MediaPlaybackState
import com.dashdeck.app.data.repo.NoteModel
import com.dashdeck.app.data.repo.NotificationSummaryState
import com.dashdeck.app.data.repo.WeatherSnapshot
import com.dashdeck.app.theme.BuiltInThemes
import com.dashdeck.app.theme.ThemePresetId
import com.dashdeck.app.theme.ThemeTokens
import com.dashdeck.app.theme.glanceContainerColorProvider
import com.dashdeck.app.theme.parseHexColorToArgbLong
import com.dashdeck.app.theme.toGlanceColorProviders
import com.dashdeck.app.widget.modules.CalendarGlanceModule
import com.dashdeck.app.widget.modules.ClockGlanceModule
import com.dashdeck.app.widget.modules.LauncherGlanceModule
import com.dashdeck.app.widget.modules.MediaGlanceModule
import com.dashdeck.app.widget.modules.NotesGlanceModule
import com.dashdeck.app.widget.modules.NotificationsGlanceModule
import com.dashdeck.app.widget.modules.WidgetCalendarSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

data class WidgetRenderSnapshot(
    val config: WidgetConfigModel,
    val calendarSnapshot: WidgetCalendarSnapshot = WidgetCalendarSnapshot(),
    val notificationState: NotificationSummaryState = NotificationSummaryState(),
    val mediaState: MediaPlaybackState = MediaPlaybackState(),
    val pinnedNotes: List<NoteModel> = emptyList(),
    val launcherBoard: LauncherBoardState = LauncherBoardState(emptyList(), emptyList()),
    val launcherIcons: Map<String, Bitmap> = emptyMap(),
    val weatherSnapshot: WeatherSnapshot? = null,
    val expandedFolderId: Long = -1L,
    val nowTime: LocalTime = LocalTime.now(),
    val today: LocalDate = LocalDate.now(),
    val nowElapsedMs: Long = SystemClock.elapsedRealtime(),
    val locale: java.util.Locale = java.util.Locale.getDefault(),
    val useLiveTextClock: Boolean = true,
    val isSystemNightMode: Boolean = false,
)

class DashboardGlanceWidget(
    private val defaultWidgetType: WidgetType = WidgetType.DASHBOARD,
) : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContainer = (context.applicationContext as DashDeckApplication).container
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val config = appContainer.widgetConfigRepository.getOrDefault(
            appWidgetId = appWidgetId,
            defaultType = defaultWidgetType,
        )
        val prefs = appContainer.preferencesRepository.getSnapshot()
        val today = LocalDate.now()
        val calMonth = appContainer.calendarRepository.loadMonthState(
            yearMonth = YearMonth.from(today),
            excludedCalendarIds = prefs.excludedCalendarIds,
        )
        val calSnapshot = WidgetCalendarSnapshot(
            hasPermission = calMonth.hasPermission,
            today = today,
            eventDatesInWeek = calMonth.eventsByDate.keys,
            monthEventDates = calMonth.eventsByDate.keys,
            todayEvents = calMonth.eventsByDate[today].orEmpty(),
        )

        val allPinnedNotes = appContainer.notesRepository.getPinnedNotesSnapshot()
        val filteredNotes = if (config.selectedNoteIds.isEmpty()) {
            allPinnedNotes
        } else {
            val selectedSet = config.selectedNoteIds.toSet()
            allPinnedNotes.filter { it.id in selectedSet }
        }

        val fullBoard = appContainer.appLauncherRepository.getBoardSnapshot()
        val filteredFolders = if (config.selectedFolderIds.isEmpty()) {
            fullBoard.folders
        } else {
            val selectedSet = config.selectedFolderIds.toSet()
            fullBoard.folders.filter { it.folder.folderId in selectedSet }
        }
        val board = fullBoard.copy(folders = filteredFolders)

        val icons = buildMap {
            board.topLevelItems.take(20).forEach { item ->
                appContainer.appLauncherRepository.getAppIconBitmap(item.packageName, item.activityName)?.let { bmp ->
                    put("${item.packageName}/${item.activityName}", bmp)
                }
            }
            board.folders.take(8).forEach { folder ->
                folder.items.take(8).forEach { item ->
                    appContainer.appLauncherRepository.getAppIconBitmap(item.packageName, item.activityName)?.let { bmp ->
                        put("${item.packageName}/${item.activityName}", bmp)
                    }
                }
            }
        }

        val cachedWeather = appContainer.weatherRepository.getCachedWeather()
        val nowMs = System.currentTimeMillis()
        if ((config.clockShowWeather || prefs.showWeather) &&
            (cachedWeather == null || cachedWeather.isStale(nowMs))
        ) {
            weatherRefreshScope.launch {
                val refreshed = appContainer.weatherRepository.getOrRefreshWeather(forceRefresh = false)
                if (refreshed != null && refreshed != cachedWeather) {
                    appContainer.requestWidgetRefreshAsync()
                }
            }
        }

        val isNightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        val baseSnapshot = WidgetRenderSnapshot(
            config = config,
            calendarSnapshot = calSnapshot,
            notificationState = appContainer.notificationSummaryRepository.state.value,
            mediaState = appContainer.mediaControlRepository.state.value,
            pinnedNotes = filteredNotes,
            launcherBoard = board,
            launcherIcons = icons,
            weatherSnapshot = cachedWeather,
            nowTime = LocalTime.now(),
            today = today,
            nowElapsedMs = SystemClock.elapsedRealtime(),
            useLiveTextClock = true,
            isSystemNightMode = isNightMode,
        )

        provideContent {
            val state = currentState<Preferences>()
            // Reading the nonce ensures every save/refresh triggers a fresh RemoteViews push
            @Suppress("UNUSED_VARIABLE")
            val refreshNonce = state[WidgetActionKeys.LAST_REFRESH_NONCE_PREF] ?: 0L
            val expandedFolderId = state[WidgetActionKeys.EXPANDED_FOLDER_ID_PREF] ?: -1L
            DashboardWidgetContent(
                snapshot = baseSnapshot.copy(expandedFolderId = expandedFolderId),
                size = LocalSize.current,
            )
        }
    }

    companion object {
        private val weatherRefreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

internal fun resolveClockOnSurfaceColorArgb(
    tokens: ThemeTokens,
    isSystemNightMode: Boolean,
): Int {
    val effective = if (tokens.presetId == ThemePresetId.FOLLOW_SYSTEM || tokens.followSystemDarkMode) {
        if (isSystemNightMode) BuiltInThemes.FollowSystemDarkFallback else BuiltInThemes.FollowSystem
    } else {
        tokens.validated()
    }
    return parseHexColorToArgbLong(effective.textPrimaryHex, fallback = 0xFFF1F5F9L).toInt()
}

@Composable
@GlanceComposable
fun DashboardWidgetContent(
    snapshot: WidgetRenderSnapshot,
    size: DpSize = DpSize(320.dp, 320.dp),
) {
    val config = snapshot.config
    val tokens = config.themeTokens.validated()
    val spacing = tokens.contentSpacingDp.coerceIn(6, 16).dp
    val moduleCornerRadius = (tokens.cornerRadiusDp - 4).coerceIn(6, 24)
    val clockColorArgb = resolveClockOnSurfaceColorArgb(tokens, snapshot.isSystemNightMode)
    val layoutRows = config.effectiveLayoutRows().filter { it.allSlots().isNotEmpty() }
    val canSplitHorizontally = size.width >= 320.dp
    val fullRowWidth = (size.width - 24.dp).coerceAtLeast(160.dp)
    val availableSplitWidth = (size.width - 24.dp - spacing).coerceAtLeast(160.dp)

    GlanceTheme(colors = tokens.toGlanceColorProviders()) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(tokens.cornerRadiusDp.dp)
                .background(tokens.glanceContainerColorProvider())
                .padding(12.dp),
        ) {
            if (config.showQuickSettingsGear) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "DashDeck",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        modifier = GlanceModifier
                            .defaultWeight()
                            .clickable(actionRunCallback<OpenMainAppActionCallback>()),
                    )
                    Text(
                        text = "⚙",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 14.sp,
                        ),
                        modifier = GlanceModifier
                            .cornerRadius(8.dp)
                            .clickable(
                                actionRunCallback<OpenWidgetSettingsActionCallback>(
                                    actionParametersOf(
                                        WidgetActionKeys.PARAM_APP_WIDGET_ID to config.appWidgetId,
                                    ),
                                ),
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Spacer(modifier = GlanceModifier.height(6.dp))
            }

            if (layoutRows.isEmpty()) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(
                            actionRunCallback<OpenWidgetSettingsActionCallback>(
                                actionParametersOf(
                                    WidgetActionKeys.PARAM_APP_WIDGET_ID to config.appWidgetId,
                                ),
                            ),
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "No modules enabled",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Text(
                        text = "Tap to customize this widget",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp,
                        ),
                    )
                }
                return@Column
            }

            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                itemsIndexed(layoutRows) { index, row ->
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        RenderWidgetRow(
                            row = row,
                            snapshot = snapshot,
                            canSplitHorizontally = canSplitHorizontally,
                            fullRowWidth = fullRowWidth,
                            availableSplitWidth = availableSplitWidth,
                            spacing = spacing,
                            cornerRadiusDp = moduleCornerRadius,
                            clockColorArgb = clockColorArgb,
                        )
                        if (index < layoutRows.lastIndex) {
                            Spacer(modifier = GlanceModifier.height(spacing))
                        }
                    }
                }
            }
        }
    }
}

@Composable
@GlanceComposable
private fun RenderWidgetRow(
    row: WidgetRowSpec,
    snapshot: WidgetRenderSnapshot,
    canSplitHorizontally: Boolean,
    fullRowWidth: Dp,
    availableSplitWidth: Dp,
    spacing: Dp,
    cornerRadiusDp: Int,
    clockColorArgb: Int,
) {
    if (!canSplitHorizontally || row.template == RowTemplate.FULL_WIDTH) {
        val slots = row.allSlots()
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            slots.forEachIndexed { idx, slot ->
                if (idx > 0) Spacer(modifier = GlanceModifier.height(spacing))
                RenderGlanceSlot(
                    slot = slot,
                    snapshot = snapshot,
                    cornerRadiusDp = cornerRadiusDp,
                    clockColorArgb = clockColorArgb,
                    slotWidthDp = fullRowWidth.value.toInt(),
                )
            }
        }
        return
    }

    val leftWidth = availableSplitWidth * row.splitRatio.leftFraction
    val rightWidth = (availableSplitWidth - leftWidth).coerceAtLeast(80.dp)

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = GlanceModifier.width(leftWidth)) {
            row.leftSlots.forEachIndexed { idx, slot ->
                if (idx > 0) Spacer(modifier = GlanceModifier.height(spacing))
                RenderGlanceSlot(
                    slot = slot,
                    snapshot = snapshot,
                    cornerRadiusDp = cornerRadiusDp,
                    clockColorArgb = clockColorArgb,
                    slotWidthDp = leftWidth.value.toInt(),
                )
            }
        }
        Spacer(modifier = GlanceModifier.width(spacing))
        Column(modifier = GlanceModifier.width(rightWidth)) {
            row.rightSlots.forEachIndexed { idx, slot ->
                if (idx > 0) Spacer(modifier = GlanceModifier.height(spacing))
                RenderGlanceSlot(
                    slot = slot,
                    snapshot = snapshot,
                    cornerRadiusDp = cornerRadiusDp,
                    clockColorArgb = clockColorArgb,
                    slotWidthDp = rightWidth.value.toInt(),
                )
            }
        }
    }
}

@Composable
@GlanceComposable
private fun RenderGlanceSlot(
    slot: WidgetSlotSpec,
    snapshot: WidgetRenderSnapshot,
    cornerRadiusDp: Int,
    clockColorArgb: Int,
    slotWidthDp: Int,
) {
    val targetH = slot.heightPreset.targetHeightDp
    when (slot.module) {
        DashboardModuleType.CLOCK -> ClockGlanceModule(
            use24HourClock = snapshot.config.use24HourClock,
            cornerRadiusDp = cornerRadiusDp,
            nowTime = snapshot.nowTime,
            today = snapshot.today,
            locale = snapshot.locale,
            sizeTier = slot.sizeTier,
            onSurfaceColorArgb = clockColorArgb,
            useLiveTextClock = snapshot.useLiveTextClock,
            clockStyle = snapshot.config.clockStyle,
            clockAlignment = snapshot.config.clockAlignment,
            showWeather = snapshot.config.clockShowWeather,
            weather = snapshot.weatherSnapshot,
            slotWidthDp = slotWidthDp,
            targetHeightDp = targetH,
        )

        DashboardModuleType.MEDIA -> MediaGlanceModule(
            state = snapshot.mediaState,
            cornerRadiusDp = cornerRadiusDp,
            sizeTier = slot.sizeTier,
            slotWidthDp = slotWidthDp,
            nowElapsedMs = snapshot.nowElapsedMs,
            targetHeightDp = targetH,
        )

        else -> {
            val containerMod = if (targetH != null) {
                GlanceModifier.fillMaxWidth().height(targetH.dp)
            } else {
                GlanceModifier.fillMaxWidth()
            }
            Box(modifier = containerMod) {
                when (slot.module) {
                    DashboardModuleType.CALENDAR -> CalendarGlanceModule(
                        snapshot = snapshot.calendarSnapshot,
                        cornerRadiusDp = cornerRadiusDp,
                        locale = snapshot.locale,
                        sizeTier = slot.sizeTier,
                    )

                    DashboardModuleType.NOTIFICATIONS -> NotificationsGlanceModule(
                        state = snapshot.notificationState,
                        cornerRadiusDp = cornerRadiusDp,
                        sizeTier = slot.sizeTier,
                    )

                    DashboardModuleType.NOTES -> NotesGlanceModule(
                        notes = snapshot.pinnedNotes,
                        cornerRadiusDp = cornerRadiusDp,
                        sizeTier = slot.sizeTier,
                    )

                    DashboardModuleType.LAUNCHER -> LauncherGlanceModule(
                        appWidgetId = snapshot.config.appWidgetId,
                        boardState = snapshot.launcherBoard,
                        folderStyle = snapshot.config.folderStyle,
                        expandedFolderId = snapshot.expandedFolderId,
                        iconBitmaps = snapshot.launcherIcons,
                        cornerRadiusDp = cornerRadiusDp,
                        sizeTier = slot.sizeTier,
                    )

                    DashboardModuleType.CLOCK, DashboardModuleType.MEDIA -> Unit
                }
            }
        }
    }
}
