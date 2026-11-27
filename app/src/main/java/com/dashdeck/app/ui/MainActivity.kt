package com.dashdeck.app.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.dashdeck.app.DashDeckApplication
import com.dashdeck.app.data.backup.DashDeckBackup
import com.dashdeck.app.data.prefs.AppPreferences
import com.dashdeck.app.data.repo.CalendarMonthState
import com.dashdeck.app.data.repo.InstalledApp
import com.dashdeck.app.data.repo.LauncherBoardState
import com.dashdeck.app.di.AppContainer
import com.dashdeck.app.theme.DashDeckTheme
import com.dashdeck.app.ui.dashboard.DashboardColumns
import com.dashdeck.app.ui.dashboard.DashboardScreen
import com.dashdeck.app.ui.dashboard.LauncherScreen
import com.dashdeck.app.ui.dashboard.NotesScreen
import com.dashdeck.app.ui.dashboard.QuickSearchDialog
import com.dashdeck.app.ui.onboarding.PermissionChecklistDialog
import com.dashdeck.app.ui.settings.AppSettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

private enum class AppDestination(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Outlined.Dashboard),
    NOTES("Notes", Icons.AutoMirrored.Outlined.StickyNote2),
    LAUNCHER("Launcher", Icons.Outlined.Apps),
    SETTINGS("Settings", Icons.Outlined.Settings),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as DashDeckApplication).container

        setContent {
            DashDeckRoot(
                appContainer = appContainer,
                onOpenNotificationSettings = { openNotificationListenerSettings() },
                onShowToast = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() },
                onOpenWidgetConfig = { appWidgetId ->
                    val intent = Intent(this, com.dashdeck.app.ui.config.WidgetConfigActivity::class.java).apply {
                        putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    startActivity(intent)
                },
            )
        }
    }

    private fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(intent) }
    }
}

@Composable
private fun DashDeckRoot(
    appContainer: AppContainer,
    onOpenNotificationSettings: () -> Unit,
    onShowToast: (String) -> Unit,
    onOpenWidgetConfig: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val preferences by appContainer.preferencesRepository.preferencesFlow
        .collectAsStateWithLifecycle(initialValue = AppPreferences())
    val notificationState by appContainer.notificationSummaryRepository.state
        .collectAsStateWithLifecycle()
    val mediaState by appContainer.mediaControlRepository.state
        .collectAsStateWithLifecycle()
    val notes by appContainer.notesRepository.observeAllNotes()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val launcherBoard by appContainer.appLauncherRepository.boardState
        .collectAsStateWithLifecycle(initialValue = LauncherBoardState(emptyList(), emptyList()))
    val activeWidgets by appContainer.widgetConfigDao.observeAllConfigs()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedDestination by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(AppDestination.DASHBOARD)
    }
    var showQuickSearch by remember { mutableStateOf(false) }
    var showPermissionChecklist by remember { mutableStateOf(false) }
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var calendarState by remember {
        mutableStateOf(
            CalendarMonthState(
                hasPermission = appContainer.calendarRepository.hasCalendarPermission(),
                yearMonth = currentMonth,
                eventsByDate = emptyMap(),
                calendars = emptyList(),
            ),
        )
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        refreshTick++
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            scope.launch {
                appContainer.preferencesRepository.setWeatherUseDeviceLocation(true)
                appContainer.weatherRepository.getOrRefreshWeather(forceRefresh = true)
            }
        }
        refreshTick++
    }

    LaunchedEffect(
        preferences.showWeather,
        preferences.weatherUseFahrenheit,
        preferences.weatherUseDeviceLocation,
        preferences.weatherCityName,
        refreshTick,
    ) {
        if (preferences.showWeather) {
            appContainer.weatherRepository.getOrRefreshWeather(forceRefresh = false)
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val (folders, topApps) = appContainer.appLauncherRepository.exportToBackup()
                    val backup = DashDeckBackup(
                        schemaVersion = DashDeckBackup.CURRENT_SCHEMA_VERSION,
                        exportedAtEpochMs = System.currentTimeMillis(),
                        globalTheme = preferences.globalTheme,
                        folders = folders,
                        topLevelApps = topApps,
                    )
                    val encoded = appContainer.backupSerializer.encode(backup)
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(encoded.toByteArray(Charsets.UTF_8))
                    } ?: error("Could not open destination file.")
                }
            }
            backupStatusMessage = if (result.isSuccess) {
                "Backup exported successfully."
            } else {
                "Export failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.readBytes().toString(Charsets.UTF_8)
                    } ?: error("Could not read selected file.")
                    val backup = appContainer.backupSerializer.decode(raw).getOrThrow()
                    appContainer.preferencesRepository.setGlobalTheme(backup.globalTheme)
                    appContainer.appLauncherRepository.restoreFromBackup(
                        folders = backup.folders,
                        topLevelApps = backup.topLevelApps,
                    )
                    backup
                }
            }
            backupStatusMessage = if (result.isSuccess) {
                val restored = result.getOrThrow()
                "Imported theme and ${restored.folders.size} folders."
            } else {
                "Import failed: ${result.exceptionOrNull()?.message ?: "Invalid backup JSON"}"
            }
        }
    }

    // Refresh permissions & installed apps whenever user returns from System Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                appContainer.notificationSummaryRepository.refreshAccessStatus()
                appContainer.mediaControlRepository.refreshAccessStatus()
                refreshTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(currentMonth, preferences.excludedCalendarIds, refreshTick) {
        calendarState = appContainer.calendarRepository.loadMonthState(
            yearMonth = currentMonth,
            excludedCalendarIds = preferences.excludedCalendarIds,
        )
    }

    LaunchedEffect(refreshTick) {
        installedApps = appContainer.appLauncherRepository.loadInstalledApps()
        appContainer.appLauncherRepository.cleanupUninstalledApps()
    }

    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    val isMedium = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val useNavigationRail = isMedium || isExpanded
    val dashboardColumns = when {
        isExpanded -> DashboardColumns.THREE
        isMedium -> DashboardColumns.TWO
        else -> DashboardColumns.ONE
    }

    DashDeckTheme(tokens = preferences.globalTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        event.isCtrlPressed && event.key == Key.K -> {
                            showQuickSearch = true
                            true
                        }
                        event.key == Key.Slash && !showQuickSearch -> {
                            showQuickSearch = true
                            true
                        }
                        event.key == Key.Escape && (showQuickSearch || showPermissionChecklist) -> {
                            showQuickSearch = false
                            showPermissionChecklist = false
                            true
                        }
                        else -> false
                    }
                },
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar {
                            AppDestination.entries.forEach { dest ->
                                NavigationBarItem(
                                    selected = selectedDestination == dest,
                                    onClick = { selectedDestination = dest },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.label) },
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (useNavigationRail) {
                        NavigationRail(modifier = Modifier.fillMaxHeight()) {
                            AppDestination.entries.forEach { dest ->
                                NavigationRailItem(
                                    selected = selectedDestination == dest,
                                    onClick = { selectedDestination = dest },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.label) },
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        when (selectedDestination) {
                            AppDestination.DASHBOARD -> DashboardScreen(
                                columns = dashboardColumns,
                                preferences = preferences,
                                calendarState = calendarState,
                                selectedDate = selectedDate,
                                notificationState = notificationState,
                                mediaState = mediaState,
                                notes = notes,
                                launcherBoard = launcherBoard,
                                installedApps = installedApps,
                                iconLoader = { pkg, act ->
                                    appContainer.appLauncherRepository.getAppIconBitmap(pkg, act)
                                },
                                onOpenQuickSearch = { showQuickSearch = true },
                                onOpenPermissionsChecklist = { showPermissionChecklist = true },
                                onRequestCalendarPermission = {
                                    calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                                },
                                onOpenNotificationSettings = onOpenNotificationSettings,
                                onToggle24HourClock = {
                                    scope.launch {
                                        appContainer.preferencesRepository.setUse24HourClock(!preferences.use24HourClock)
                                    }
                                },
                                onSelectDate = { selectedDate = it },
                                onChangeMonth = { currentMonth = it },
                                onOpenCalendarEvent = { ev ->
                                    if (!appContainer.calendarRepository.openEventInSystemCalendar(ev.eventId, ev.startMillis)) {
                                        onShowToast("No calendar app found to open this event.")
                                    }
                                },
                                onUpdateExcludedCalendars = { excluded ->
                                    scope.launch {
                                        appContainer.preferencesRepository.setExcludedCalendarIds(excluded)
                                    }
                                },
                                onTogglePrivacyMode = { enabled ->
                                    scope.launch {
                                        appContainer.preferencesRepository.setPrivacyHideNotificationText(enabled)
                                    }
                                },
                                onUpdateNotificationFilters = { allowMode, allowed, blocked ->
                                    scope.launch {
                                        appContainer.preferencesRepository.setNotificationFilterMode(allowMode)
                                        appContainer.preferencesRepository.setAllowedNotificationPackages(allowed)
                                        appContainer.preferencesRepository.setBlockedNotificationPackages(blocked)
                                    }
                                },
                                onMediaPrev = { appContainer.mediaControlRepository.skipPrevious() },
                                onMediaPlayPause = { appContainer.mediaControlRepository.togglePlayPause() },
                                onMediaNext = { appContainer.mediaControlRepository.skipNext() },
                                onToggleNoteChecklist = { noteId, itemId ->
                                    scope.launch {
                                        appContainer.notesRepository.toggleChecklistItem(noteId, itemId)
                                    }
                                },
                                onToggleNotePin = { noteId, pinned ->
                                    scope.launch {
                                        appContainer.notesRepository.setPinned(noteId, pinned)
                                    }
                                },
                                onSaveNote = { id, title, body, checklist, pinned ->
                                    scope.launch {
                                        appContainer.notesRepository.saveNote(
                                            id = id,
                                            title = title,
                                            body = body,
                                            checklist = checklist,
                                            isPinned = pinned,
                                        )
                                    }
                                },
                                onDeleteNote = { noteId ->
                                    scope.launch { appContainer.notesRepository.deleteNote(noteId) }
                                },
                                onOpenInKeep = {
                                    if (!appContainer.notesRepository.openInGoogleKeep(context)) {
                                        onShowToast("Google Keep is not installed on this device.")
                                    }
                                },
                                onLaunchApp = { pkg, act ->
                                    if (!appContainer.appLauncherRepository.launchApp(pkg, act)) {
                                        onShowToast("Unable to launch app.")
                                    }
                                },
                                onCreateFolder = { name, tint ->
                                    scope.launch { appContainer.appLauncherRepository.createFolder(name, tint) }
                                },
                                onRenameFolder = { folder, name, tint ->
                                    scope.launch { appContainer.appLauncherRepository.renameFolder(folder, name, tint) }
                                },
                                onDeleteFolder = { folderId ->
                                    scope.launch { appContainer.appLauncherRepository.deleteFolder(folderId) }
                                },
                                onAddAppToBoard = { app, folderId ->
                                    scope.launch { appContainer.appLauncherRepository.addAppToBoard(app, folderId) }
                                },
                                onRemoveLauncherItem = { itemId ->
                                    scope.launch { appContainer.appLauncherRepository.removeItem(itemId) }
                                },
                            )

                            AppDestination.NOTES -> NotesScreen(
                                notes = notes,
                                onToggleChecklist = { noteId, itemId ->
                                    scope.launch { appContainer.notesRepository.toggleChecklistItem(noteId, itemId) }
                                },
                                onTogglePin = { noteId, pinned ->
                                    scope.launch { appContainer.notesRepository.setPinned(noteId, pinned) }
                                },
                                onSaveNote = { id, title, body, checklist, pinned ->
                                    scope.launch {
                                        appContainer.notesRepository.saveNote(
                                            id = id,
                                            title = title,
                                            body = body,
                                            checklist = checklist,
                                            isPinned = pinned,
                                        )
                                    }
                                },
                                onDeleteNote = { noteId ->
                                    scope.launch { appContainer.notesRepository.deleteNote(noteId) }
                                },
                                onOpenInKeep = {
                                    if (!appContainer.notesRepository.openInGoogleKeep(context)) {
                                        onShowToast("Google Keep is not installed on this device.")
                                    }
                                },
                            )

                            AppDestination.LAUNCHER -> LauncherScreen(
                                boardState = launcherBoard,
                                installedApps = installedApps,
                                iconLoader = { pkg, act ->
                                    appContainer.appLauncherRepository.getAppIconBitmap(pkg, act)
                                },
                                onLaunchApp = { pkg, act ->
                                    if (!appContainer.appLauncherRepository.launchApp(pkg, act)) {
                                        onShowToast("Unable to launch app.")
                                    }
                                },
                                onCreateFolder = { name, tint ->
                                    scope.launch { appContainer.appLauncherRepository.createFolder(name, tint) }
                                },
                                onRenameFolder = { folder, name, tint ->
                                    scope.launch { appContainer.appLauncherRepository.renameFolder(folder, name, tint) }
                                },
                                onDeleteFolder = { folderId ->
                                    scope.launch { appContainer.appLauncherRepository.deleteFolder(folderId) }
                                },
                                onAddApp = { app, folderId ->
                                    scope.launch { appContainer.appLauncherRepository.addAppToBoard(app, folderId) }
                                },
                                onRemoveItem = { itemId ->
                                    scope.launch { appContainer.appLauncherRepository.removeItem(itemId) }
                                },
                            )

                            AppDestination.SETTINGS -> AppSettingsScreen(
                                preferences = preferences,
                                activeWidgets = activeWidgets,
                                backupStatusMessage = backupStatusMessage,
                                onSelectTheme = { tokens ->
                                    scope.launch { appContainer.preferencesRepository.setGlobalTheme(tokens) }
                                },
                                onApplyThemeToAllWidgets = {
                                    scope.launch {
                                        appContainer.widgetConfigRepository.updateThemeForAllWidgets(preferences.globalTheme)
                                        appContainer.notifyWidgetDataChanged()
                                        onShowToast("Theme applied to all widgets.")
                                    }
                                },
                                onSelectClockStyle = { style ->
                                    scope.launch { appContainer.preferencesRepository.setClockStyle(style) }
                                },
                                onSelectClockAlignment = { align ->
                                    scope.launch { appContainer.preferencesRepository.setClockAlignment(align) }
                                },
                                onToggle24HourClock = { enabled ->
                                    scope.launch { appContainer.preferencesRepository.setUse24HourClock(enabled) }
                                },
                                onToggleClockSeconds = { enabled ->
                                    scope.launch { appContainer.preferencesRepository.setShowClockSeconds(enabled) }
                                },
                                onToggleShowWeather = { enabled ->
                                    scope.launch {
                                        appContainer.preferencesRepository.setShowWeather(enabled)
                                        if (enabled) {
                                            appContainer.weatherRepository.getOrRefreshWeather(forceRefresh = false)
                                            appContainer.requestWidgetRefreshAsync()
                                        }
                                    }
                                },
                                onToggleWeatherFahrenheit = { useF ->
                                    scope.launch {
                                        appContainer.preferencesRepository.setWeatherUseFahrenheit(useF)
                                        appContainer.weatherRepository.getOrRefreshWeather(forceRefresh = true)
                                        appContainer.requestWidgetRefreshAsync()
                                    }
                                },
                                onToggleWeatherDeviceLocation = { useLoc ->
                                    if (useLoc && !appContainer.weatherRepository.hasCoarseLocationPermission()) {
                                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    } else {
                                        scope.launch {
                                            appContainer.preferencesRepository.setWeatherUseDeviceLocation(useLoc)
                                            appContainer.weatherRepository.getOrRefreshWeather(forceRefresh = true)
                                            appContainer.requestWidgetRefreshAsync()
                                        }
                                    }
                                },
                                onSearchWeatherCities = { query ->
                                    appContainer.weatherRepository.searchCities(query)
                                },
                                onSelectWeatherCity = { loc ->
                                    scope.launch {
                                        appContainer.preferencesRepository.setWeatherLocation(
                                            cityName = loc.displayLabel(),
                                            lat = loc.latitude,
                                            lon = loc.longitude,
                                        )
                                        appContainer.weatherRepository.getOrRefreshWeather(forceRefresh = true)
                                        appContainer.requestWidgetRefreshAsync()
                                        onShowToast("Weather set to ${loc.displayLabel()}")
                                    }
                                },
                                onRefreshWeatherNow = {
                                    scope.launch {
                                        val updated = appContainer.weatherRepository.getOrRefreshWeather(forceRefresh = true)
                                        appContainer.requestWidgetRefreshAsync()
                                        onShowToast(
                                            if (updated != null) "Weather updated: ${updated.fullReadout()}"
                                            else "Pick a city or enable location first.",
                                        )
                                    }
                                },
                                onTogglePrivacyMode = { enabled ->
                                    scope.launch {
                                        appContainer.preferencesRepository.setPrivacyHideNotificationText(enabled)
                                    }
                                },
                                onOpenPermissionsChecklist = { showPermissionChecklist = true },
                                onExportBackup = { exportBackupLauncher.launch("dashdeck-backup.json") },
                                onImportBackup = { importBackupLauncher.launch(arrayOf("application/json", "text/plain")) },
                                onOpenWidgetConfig = onOpenWidgetConfig,
                            )
                        }
                    }
                }
            }

            if (showQuickSearch) {
                QuickSearchDialog(
                    installedApps = installedApps,
                    notes = notes,
                    iconLoader = { pkg, act -> appContainer.appLauncherRepository.getAppIconBitmap(pkg, act) },
                    onLaunchApp = { app ->
                        appContainer.appLauncherRepository.launchApp(app.packageName, app.activityName)
                    },
                    onSelectNote = {
                        selectedDestination = AppDestination.NOTES
                    },
                    onDismiss = { showQuickSearch = false },
                )
            }

            if (showPermissionChecklist) {
                PermissionChecklistDialog(
                    hasCalendarPermission = calendarState.hasPermission,
                    hasNotificationAccess = notificationState.hasListenerAccess,
                    hasLocationPermission = appContainer.weatherRepository.hasCoarseLocationPermission(),
                    onRequestCalendar = {
                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    },
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    onRequestLocation = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    onDismiss = { showPermissionChecklist = false },
                )
            }
        }
    }
}
