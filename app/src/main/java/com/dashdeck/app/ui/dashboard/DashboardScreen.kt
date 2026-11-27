package com.dashdeck.app.ui.dashboard

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dashdeck.app.data.db.ChecklistItem
import com.dashdeck.app.data.db.LauncherFolderEntity
import com.dashdeck.app.data.prefs.AppPreferences
import com.dashdeck.app.data.repo.CalendarEvent
import com.dashdeck.app.data.repo.CalendarMonthState
import com.dashdeck.app.data.repo.InstalledApp
import com.dashdeck.app.data.repo.LauncherBoardState
import com.dashdeck.app.data.repo.MediaPlaybackState
import com.dashdeck.app.data.repo.NoteModel
import com.dashdeck.app.data.repo.NotificationSummaryState
import com.dashdeck.app.theme.LocalDashDeckThemeTokens
import com.dashdeck.app.ui.onboarding.PermissionStatusBanner
import java.time.LocalDate
import java.time.YearMonth

enum class DashboardColumns {
    ONE,
    TWO,
    THREE,
}

@Composable
fun DashboardScreen(
    columns: DashboardColumns,
    preferences: AppPreferences,
    calendarState: CalendarMonthState,
    selectedDate: LocalDate,
    notificationState: NotificationSummaryState,
    mediaState: MediaPlaybackState,
    notes: List<NoteModel>,
    launcherBoard: LauncherBoardState,
    installedApps: List<InstalledApp>,
    iconLoader: (String, String?) -> Bitmap?,
    onOpenQuickSearch: () -> Unit,
    onOpenPermissionsChecklist: () -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onToggle24HourClock: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onChangeMonth: (YearMonth) -> Unit,
    onOpenCalendarEvent: (CalendarEvent) -> Unit,
    onUpdateExcludedCalendars: (Set<Long>) -> Unit,
    onTogglePrivacyMode: (Boolean) -> Unit,
    onUpdateNotificationFilters: (Boolean, Set<String>, Set<String>) -> Unit,
    onMediaPrev: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit,
    onToggleNoteChecklist: (Long, String) -> Unit,
    onToggleNotePin: (Long, Boolean) -> Unit,
    onSaveNote: (Long, String, String, List<ChecklistItem>, Boolean) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onOpenInKeep: () -> Unit,
    onLaunchApp: (String, String?) -> Unit,
    onCreateFolder: (String, String) -> Unit,
    onRenameFolder: (LauncherFolderEntity, String, String) -> Unit,
    onDeleteFolder: (Long) -> Unit,
    onAddAppToBoard: (InstalledApp, Long?) -> Unit,
    onRemoveLauncherItem: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDashDeckThemeTokens.current.contentSpacingDp.dp

    val clockCard: @Composable () -> Unit = {
        ClockCard(
            use24HourClock = preferences.use24HourClock,
            showSeconds = preferences.showClockSeconds,
            clockStyle = preferences.clockStyle,
            clockAlignment = preferences.clockAlignment,
            showWeather = preferences.showWeather,
            weatherSnapshot = preferences.cachedWeather,
            onToggle24Hour = onToggle24HourClock,
        )
    }
    val calendarCard: @Composable () -> Unit = {
        CalendarCard(
            monthState = calendarState,
            selectedDate = selectedDate,
            excludedCalendarIds = preferences.excludedCalendarIds,
            onSelectDate = onSelectDate,
            onChangeMonth = onChangeMonth,
            onRequestPermission = onRequestCalendarPermission,
            onOpenEvent = onOpenCalendarEvent,
            onUpdateExcludedCalendars = onUpdateExcludedCalendars,
        )
    }
    val mediaCard: @Composable () -> Unit = {
        MediaControllerCard(
            state = mediaState,
            onPrevious = onMediaPrev,
            onPlayPause = onMediaPlayPause,
            onNext = onMediaNext,
            onOpenNotificationSettings = onOpenNotificationSettings,
        )
    }
    val notificationsCard: @Composable () -> Unit = {
        NotificationSummaryCard(
            state = notificationState,
            preferences = preferences,
            installedApps = installedApps,
            onTogglePrivacyMode = onTogglePrivacyMode,
            onUpdateNotificationFilters = onUpdateNotificationFilters,
            onOpenNotificationSettings = onOpenNotificationSettings,
        )
    }
    val notesCard: @Composable () -> Unit = {
        PinnedNotesCard(
            notes = notes,
            onToggleChecklist = onToggleNoteChecklist,
            onTogglePin = onToggleNotePin,
            onSaveNote = onSaveNote,
            onDeleteNote = onDeleteNote,
            onOpenInKeep = onOpenInKeep,
        )
    }
    val launcherCard: @Composable () -> Unit = {
        LauncherCard(
            boardState = launcherBoard,
            iconLoader = iconLoader,
            onLaunchApp = onLaunchApp,
            onCreateFolder = onCreateFolder,
            onRenameFolder = onRenameFolder,
            onDeleteFolder = onDeleteFolder,
            onAddApp = onAddAppToBoard,
            onRemoveItem = onRemoveLauncherItem,
            installedApps = installedApps,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        // Header with Quick Search shortcut button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "DashDeck",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Local-first desktop & mobile dashboard",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onOpenQuickSearch) {
                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (columns == DashboardColumns.ONE) "Search" else "Quick Search (Ctrl+K)")
            }
        }

        PermissionStatusBanner(
            hasCalendarPermission = calendarState.hasPermission,
            hasNotificationAccess = notificationState.hasListenerAccess,
            onOpenChecklist = onOpenPermissionsChecklist,
        )

        when (columns) {
            DashboardColumns.ONE -> {
                clockCard()
                mediaCard()
                calendarCard()
                notificationsCard()
                notesCard()
                launcherCard()
            }

            DashboardColumns.TWO -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        clockCard()
                        calendarCard()
                        launcherCard()
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        mediaCard()
                        notificationsCard()
                        notesCard()
                    }
                }
            }

            DashboardColumns.THREE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        clockCard()
                        calendarCard()
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        mediaCard()
                        notificationsCard()
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        notesCard()
                        launcherCard()
                    }
                }
            }
        }
    }
}
