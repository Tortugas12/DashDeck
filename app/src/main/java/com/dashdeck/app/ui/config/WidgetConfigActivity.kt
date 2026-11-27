package com.dashdeck.app.ui.config

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.dashdeck.app.DashDeckApplication
import com.dashdeck.app.data.db.DashboardModuleType
import com.dashdeck.app.data.db.FolderStyle
import com.dashdeck.app.data.db.ModuleSizeTier
import com.dashdeck.app.data.db.WidgetType
import com.dashdeck.app.data.prefs.AppPreferences
import com.dashdeck.app.data.repo.LauncherBoardState
import com.dashdeck.app.data.repo.NoteModel
import com.dashdeck.app.data.repo.WeatherSnapshot
import com.dashdeck.app.theme.DashDeckTheme
import com.dashdeck.app.ui.common.DashDeckCard
import com.dashdeck.app.ui.common.ModuleCardHeader
import com.dashdeck.app.ui.common.UnifiedThemePicker
import com.dashdeck.app.widget.CardHeightPreset
import com.dashdeck.app.widget.ClockHorizontalAlignment
import com.dashdeck.app.widget.ClockStylePreset
import com.dashdeck.app.widget.RowTemplate
import com.dashdeck.app.widget.SplitWidthRatio
import com.dashdeck.app.widget.WidgetConfigModel
import com.dashdeck.app.widget.WidgetRowSpec
import com.dashdeck.app.widget.WidgetSlotSpec
import com.dashdeck.app.widget.defaultLayoutRowsFor
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(RESULT_CANCELED)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val container = (application as DashDeckApplication).container
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val providerClass = appWidgetManager.getAppWidgetInfo(appWidgetId)?.provider?.className.orEmpty()
        val inferredType = if (providerClass.contains("Modular", ignoreCase = true)) {
            WidgetType.MODULAR
        } else {
            WidgetType.DASHBOARD
        }

        setContent {
            val scope = rememberCoroutineScope()
            var config by remember {
                mutableStateOf(
                    WidgetConfigModel(
                        appWidgetId = appWidgetId,
                        widgetType = inferredType,
                        layoutRows = defaultLayoutRowsFor(inferredType),
                    ),
                )
            }
            var isSaving by remember { mutableStateOf(false) }
            val appPrefs by container.preferencesRepository.preferencesFlow
                .collectAsStateWithLifecycle(initialValue = AppPreferences())
            val notes by container.notesRepository.observePinnedNotes()
                .collectAsStateWithLifecycle(initialValue = emptyList())
            val board by container.appLauncherRepository.boardState
                .collectAsStateWithLifecycle(initialValue = LauncherBoardState(emptyList(), emptyList()))

            LaunchedEffect(appWidgetId) {
                config = container.widgetConfigRepository.getOrDefault(
                    appWidgetId = appWidgetId,
                    defaultType = inferredType,
                )
            }

            DashDeckTheme(tokens = config.themeTokens) {
                WidgetConfigScreen(
                    config = config,
                    appPrefs = appPrefs,
                    pinnedNotes = notes,
                    launcherBoard = board,
                    isSaving = isSaving,
                    onConfigChange = { config = it },
                    onCancel = { finish() },
                    onSave = {
                        if (isSaving) return@WidgetConfigScreen
                        isSaving = true
                        scope.launch {
                            // Runs on application-scoped WidgetSyncCoordinator so finish() cannot cancel redraw
                            container.saveAndRefreshWidget(config)
                            val resultIntent = Intent().putExtra(
                                AppWidgetManager.EXTRA_APPWIDGET_ID,
                                appWidgetId,
                            )
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun WidgetConfigScreen(
    config: WidgetConfigModel,
    appPrefs: AppPreferences,
    pinnedNotes: List<NoteModel>,
    launcherBoard: LauncherBoardState,
    isSaving: Boolean,
    onConfigChange: (WidgetConfigModel) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isWideDesktop = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Outlined.Close, contentDescription = "Cancel")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Customize Widget",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${config.widgetType.name} · #${config.appWidgetId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Button(onClick = onSave, enabled = !isSaving) {
                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isSaving) "Saving…" else "Save Widget")
                    }
                }
            }
        },
    ) { innerPadding ->
        if (isWideDesktop) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    ConfigControlsColumn(
                        config = config,
                        appPrefs = appPrefs,
                        pinnedNotes = pinnedNotes,
                        launcherBoard = launcherBoard,
                        onConfigChange = onConfigChange,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Top,
                ) {
                    LiveWidgetPreviewPanel(
                        config = config,
                        appPrivacyMode = appPrefs.privacyHideNotificationText,
                        weatherSnapshot = appPrefs.cachedWeather,
                        pinnedNotes = pinnedNotes,
                        launcherBoard = launcherBoard,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                LiveWidgetPreviewPanel(
                    config = config,
                    appPrivacyMode = appPrefs.privacyHideNotificationText,
                    weatherSnapshot = appPrefs.cachedWeather,
                    pinnedNotes = pinnedNotes,
                    launcherBoard = launcherBoard,
                )
                ConfigControlsColumn(
                    config = config,
                    appPrefs = appPrefs,
                    pinnedNotes = pinnedNotes,
                    launcherBoard = launcherBoard,
                    onConfigChange = onConfigChange,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfigControlsColumn(
    config: WidgetConfigModel,
    appPrefs: AppPreferences,
    pinnedNotes: List<NoteModel>,
    launcherBoard: LauncherBoardState,
    onConfigChange: (WidgetConfigModel) -> Unit,
) {
    var addRowMenuExpanded by remember { mutableStateOf(false) }

    fun updateRows(nextRows: List<WidgetRowSpec>) {
        val flatModules = nextRows.flatMap { it.allSlots() }.map { it.module }
        onConfigChange(config.copy(layoutRows = nextRows, enabledModules = flatModules))
    }

    // 1. Flexible Row-and-Slot Layout Builder
    DashDeckCard {
        ModuleCardHeader(
            title = "Layout Rows",
            icon = Icons.Outlined.Widgets,
            trailing = {
                Box {
                    FilledTonalButton(onClick = { addRowMenuExpanded = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Row")
                    }
                    DropdownMenu(
                        expanded = addRowMenuExpanded,
                        onDismissRequest = { addRowMenuExpanded = false },
                    ) {
                        RowTemplate.entries.forEach { template ->
                            DropdownMenuItem(
                                text = { Text(template.displayName) },
                                onClick = {
                                    addRowMenuExpanded = false
                                    updateRows(config.layoutRows + createDefaultRow(template))
                                },
                            )
                        }
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (config.layoutRows.isEmpty()) {
            Text(
                text = "No rows added yet. Tap Add Row to build your widget layout.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                config.layoutRows.forEachIndexed { rowIdx, row ->
                    RowEditorCard(
                        row = row,
                        rowIndex = rowIdx,
                        totalRows = config.layoutRows.size,
                        onRowChange = { updatedRow ->
                            val mutable = config.layoutRows.toMutableList()
                            mutable[rowIdx] = updatedRow
                            updateRows(mutable)
                        },
                        onMoveUp = {
                            if (rowIdx > 0) {
                                val mutable = config.layoutRows.toMutableList()
                                val item = mutable.removeAt(rowIdx)
                                mutable.add(rowIdx - 1, item)
                                updateRows(mutable)
                            }
                        },
                        onMoveDown = {
                            if (rowIdx < config.layoutRows.lastIndex) {
                                val mutable = config.layoutRows.toMutableList()
                                val item = mutable.removeAt(rowIdx)
                                mutable.add(rowIdx + 1, item)
                                updateRows(mutable)
                            }
                        },
                        onDelete = {
                            val mutable = config.layoutRows.toMutableList()
                            mutable.removeAt(rowIdx)
                            updateRows(mutable)
                        },
                    )
                }
            }
        }
    }

    // 2. Clock Style, Alignment & Weather
    DashDeckCard {
        ModuleCardHeader(title = "Clock & Weather", icon = Icons.Outlined.Schedule)
        Spacer(modifier = Modifier.height(8.dp))

        Text("Clock Style", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ClockStylePreset.entries.forEach { style ->
                FilterChip(
                    selected = config.clockStyle == style,
                    onClick = { onConfigChange(config.copy(clockStyle = style)) },
                    label = { Text(style.displayName) },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Alignment", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ClockHorizontalAlignment.entries.forEach { align ->
                FilterChip(
                    selected = config.clockAlignment == align,
                    onClick = { onConfigChange(config.copy(clockAlignment = align)) },
                    label = { Text(align.displayName) },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        ConfigSwitchRow(
            title = "24-hour clock",
            checked = config.use24HourClock,
            onCheckedChange = { onConfigChange(config.copy(use24HourClock = it)) },
        )
        ConfigSwitchRow(
            title = "Show weather on Clock card",
            checked = config.clockShowWeather,
            onCheckedChange = { onConfigChange(config.copy(clockShowWeather = it)) },
        )
        if (config.clockShowWeather && appPrefs.weatherCityName.isBlank() && !appPrefs.weatherUseDeviceLocation) {
            Text(
                text = "Set your weather city in DashDeck Settings -> Clock & Weather.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

    // 3. Unified Theme Picker
    UnifiedThemePicker(
        tokens = config.themeTokens,
        onTokensChange = { onConfigChange(config.copy(themeTokens = it)) },
        title = "Widget Theme",
        showSpacingSlider = false,
    )

    // 4. Folder Style & Module Options
    DashDeckCard {
        ModuleCardHeader(title = "Behavior & Folders", icon = Icons.Outlined.Folder)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Widget Folder Style", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FolderStyle.entries.forEach { style ->
                FilterChip(
                    selected = config.folderStyle == style,
                    onClick = { onConfigChange(config.copy(folderStyle = style)) },
                    label = { Text(style.displayName) },
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        ConfigSwitchRow(
            title = "Show quick Settings gear on widget",
            checked = config.showQuickSettingsGear,
            onCheckedChange = { onConfigChange(config.copy(showQuickSettingsGear = it)) },
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Notifications follow app Privacy Mode · Currently ${if (appPrefs.privacyHideNotificationText) "On" else "Off"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    // 5. Optional Note & Folder Picker
    if (pinnedNotes.isNotEmpty() || launcherBoard.folders.isNotEmpty()) {
        DashDeckCard {
            ModuleCardHeader(title = "Pin Specific Notes & Folders", icon = Icons.Outlined.Tune)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Leave all unchecked to automatically show all pinned notes / folders.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (pinnedNotes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Notes", style = MaterialTheme.typography.labelLarge)
                pinnedNotes.forEach { note ->
                    val checked = note.id in config.selectedNoteIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val next = if (checked) config.selectedNoteIds - note.id else config.selectedNoteIds + note.id
                                onConfigChange(config.copy(selectedNoteIds = next))
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                val next = if (isChecked) config.selectedNoteIds + note.id else config.selectedNoteIds - note.id
                                onConfigChange(config.copy(selectedNoteIds = next))
                            },
                        )
                        Text(note.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (launcherBoard.folders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Folders", style = MaterialTheme.typography.labelLarge)
                launcherBoard.folders.forEach { f ->
                    val fid = f.folder.folderId
                    val checked = fid in config.selectedFolderIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val next = if (checked) config.selectedFolderIds - fid else config.selectedFolderIds + fid
                                onConfigChange(config.copy(selectedFolderIds = next))
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                val next = if (isChecked) config.selectedFolderIds + fid else config.selectedFolderIds - fid
                                onConfigChange(config.copy(selectedFolderIds = next))
                            },
                        )
                        Text(f.folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RowEditorCard(
    row: WidgetRowSpec,
    rowIndex: Int,
    totalRows: Int,
    onRowChange: (WidgetRowSpec) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Row ${rowIndex + 1} · ${row.template.displayName}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Row {
                    IconButton(enabled = rowIndex > 0, onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move row up", modifier = Modifier.size(16.dp))
                    }
                    IconButton(enabled = rowIndex < totalRows - 1, onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.ArrowDownward, contentDescription = "Move row down", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Remove row", modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (row.template != RowTemplate.FULL_WIDTH) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SplitWidthRatio.entries.forEach { ratio ->
                        FilterChip(
                            selected = row.splitRatio == ratio,
                            onClick = { onRowChange(row.copy(splitRatio = ratio)) },
                            label = { Text(ratio.label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }

            row.leftSlots.forEachIndexed { slotIdx, slot ->
                val slotLabel = when (row.template) {
                    RowTemplate.FULL_WIDTH -> "Card"
                    RowTemplate.LEFT_STACK_RIGHT_TALL -> "Left ${slotIdx + 1}"
                    else -> "Left"
                }
                SlotEditorRow(
                    label = slotLabel,
                    slot = slot,
                    onSlotChange = { updated ->
                        val nextLeft = row.leftSlots.toMutableList().apply { this[slotIdx] = updated }
                        onRowChange(row.copy(leftSlots = nextLeft))
                    },
                )
            }

            row.rightSlots.forEachIndexed { slotIdx, slot ->
                val slotLabel = if (row.template == RowTemplate.LEFT_TALL_RIGHT_STACK) {
                    "Right ${slotIdx + 1}"
                } else {
                    "Right"
                }
                SlotEditorRow(
                    label = slotLabel,
                    slot = slot,
                    onSlotChange = { updated ->
                        val nextRight = row.rightSlots.toMutableList().apply { this[slotIdx] = updated }
                        onRowChange(row.copy(rightSlots = nextRight))
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotEditorRow(
    label: String,
    slot: WidgetSlotSpec,
    onSlotChange: (WidgetSlotSpec) -> Unit,
) {
    var moduleMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(54.dp),
            )
            Box {
                OutlinedButton(onClick = { moduleMenuExpanded = true }) {
                    Text(slot.module.displayName, style = MaterialTheme.typography.labelMedium)
                }
                DropdownMenu(
                    expanded = moduleMenuExpanded,
                    onDismissRequest = { moduleMenuExpanded = false },
                ) {
                    DashboardModuleType.entries.forEach { mod ->
                        DropdownMenuItem(
                            text = { Text(mod.displayName) },
                            onClick = {
                                moduleMenuExpanded = false
                                onSlotChange(slot.copy(module = mod))
                            },
                        )
                    }
                }
            }
        }
        FlowRow(
            modifier = Modifier.padding(start = 54.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ModuleSizeTier.entries.forEach { tier ->
                FilterChip(
                    selected = slot.sizeTier == tier,
                    onClick = { onSlotChange(slot.copy(sizeTier = tier)) },
                    label = { Text(tier.shortLabel) },
                )
            }
            CardHeightPreset.entries.forEach { preset ->
                FilterChip(
                    selected = slot.heightPreset == preset,
                    onClick = { onSlotChange(slot.copy(heightPreset = preset)) },
                    label = { Text(preset.shortLabel, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}

private fun createDefaultRow(template: RowTemplate): WidgetRowSpec {
    val id = "row_${UUID.randomUUID().toString().take(8)}"
    return when (template) {
        RowTemplate.FULL_WIDTH -> WidgetRowSpec(
            id = id,
            template = template,
            leftSlots = listOf(WidgetSlotSpec(DashboardModuleType.CLOCK)),
        )

        RowTemplate.SPLIT_TWO -> WidgetRowSpec(
            id = id,
            template = template,
            splitRatio = SplitWidthRatio.EVEN_50_50,
            leftSlots = listOf(WidgetSlotSpec(DashboardModuleType.CLOCK)),
            rightSlots = listOf(WidgetSlotSpec(DashboardModuleType.MEDIA)),
        )

        RowTemplate.LEFT_TALL_RIGHT_STACK -> WidgetRowSpec(
            id = id,
            template = template,
            splitRatio = SplitWidthRatio.LEFT_60_40,
            leftSlots = listOf(WidgetSlotSpec(DashboardModuleType.CALENDAR, ModuleSizeTier.LARGE, CardHeightPreset.TALL)),
            rightSlots = listOf(
                WidgetSlotSpec(DashboardModuleType.CLOCK, ModuleSizeTier.COMPACT),
                WidgetSlotSpec(DashboardModuleType.MEDIA, ModuleSizeTier.COMPACT),
            ),
        )

        RowTemplate.LEFT_STACK_RIGHT_TALL -> WidgetRowSpec(
            id = id,
            template = template,
            splitRatio = SplitWidthRatio.RIGHT_40_60,
            leftSlots = listOf(
                WidgetSlotSpec(DashboardModuleType.CLOCK, ModuleSizeTier.COMPACT),
                WidgetSlotSpec(DashboardModuleType.MEDIA, ModuleSizeTier.COMPACT),
            ),
            rightSlots = listOf(WidgetSlotSpec(DashboardModuleType.CALENDAR, ModuleSizeTier.LARGE, CardHeightPreset.TALL)),
        )
    }
}

@Composable
private fun LiveWidgetPreviewPanel(
    config: WidgetConfigModel,
    appPrivacyMode: Boolean,
    weatherSnapshot: WeatherSnapshot?,
    pinnedNotes: List<NoteModel>,
    launcherBoard: LauncherBoardState,
) {
    val tokens = config.themeTokens.validated()
    val timeText = remember(config.use24HourClock) {
        val pattern = if (config.use24HourClock) "HH:mm" else "h:mm a"
        LocalTime.now().format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }
    val spacing = tokens.contentSpacingDp.coerceIn(6, 16).dp
    val moduleCorner = (tokens.cornerRadiusDp - 4).coerceIn(6, 24).dp
    val rows = config.effectiveLayoutRows().filter { it.allSlots().isNotEmpty() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Live Widget Preview",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape = RoundedCornerShape(tokens.cornerRadiusDp.dp),
            color = MaterialTheme.colorScheme.background.copy(alpha = tokens.backgroundAlpha),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                if (config.showQuickSettingsGear) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "DashDeck",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "⚙",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (rows.isEmpty()) {
                    Text(
                        text = "Add at least one row to preview your widget.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    rows.forEach { row ->
                        if (row.template == RowTemplate.FULL_WIDTH) {
                            row.allSlots().forEach { slot ->
                                PreviewSlotCard(
                                    slot = slot,
                                    config = config,
                                    appPrivacyMode = appPrivacyMode,
                                    weatherSnapshot = weatherSnapshot,
                                    timeText = timeText,
                                    pinnedNotes = pinnedNotes,
                                    launcherBoard = launcherBoard,
                                    cornerShape = RoundedCornerShape(moduleCorner),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        } else {
                            val leftWeight = row.splitRatio.leftFraction
                            val rightWeight = (1f - leftWeight).coerceAtLeast(0.2f)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(
                                    modifier = Modifier.weight(leftWeight),
                                    verticalArrangement = Arrangement.spacedBy(spacing),
                                ) {
                                    row.leftSlots.forEach { slot ->
                                        PreviewSlotCard(
                                            slot = slot,
                                            config = config,
                                            appPrivacyMode = appPrivacyMode,
                                            weatherSnapshot = weatherSnapshot,
                                            timeText = timeText,
                                            pinnedNotes = pinnedNotes,
                                            launcherBoard = launcherBoard,
                                            cornerShape = RoundedCornerShape(moduleCorner),
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(rightWeight),
                                    verticalArrangement = Arrangement.spacedBy(spacing),
                                ) {
                                    row.rightSlots.forEach { slot ->
                                        PreviewSlotCard(
                                            slot = slot,
                                            config = config,
                                            appPrivacyMode = appPrivacyMode,
                                            weatherSnapshot = weatherSnapshot,
                                            timeText = timeText,
                                            pinnedNotes = pinnedNotes,
                                            launcherBoard = launcherBoard,
                                            cornerShape = RoundedCornerShape(moduleCorner),
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewSlotCard(
    slot: WidgetSlotSpec,
    config: WidgetConfigModel,
    appPrivacyMode: Boolean,
    weatherSnapshot: WeatherSnapshot?,
    timeText: String,
    pinnedNotes: List<NoteModel>,
    launcherBoard: LauncherBoardState,
    cornerShape: RoundedCornerShape,
    modifier: Modifier = Modifier,
) {
    val tier = slot.sizeTier
    val pad = if (tier == ModuleSizeTier.COMPACT) 8.dp else 10.dp
    val heightMod = slot.heightPreset.targetHeightDp?.let { modifier.height(it.dp) } ?: modifier

    Surface(
        shape = cornerShape,
        color = MaterialTheme.colorScheme.surface,
        modifier = heightMod,
    ) {
        when (slot.module) {
            DashboardModuleType.CLOCK -> {
                val datePattern = when (tier) {
                    ModuleSizeTier.COMPACT -> "EEE, MMM d"
                    ModuleSizeTier.STANDARD -> "EEEE, MMM d, yyyy"
                    ModuleSizeTier.LARGE -> "EEEE, MMMM d, yyyy"
                }
                val rawDate = remember(datePattern) {
                    LocalDate.now().format(DateTimeFormatter.ofPattern(datePattern, Locale.getDefault()))
                }
                val dateText = if (config.clockStyle == ClockStylePreset.EDITORIAL) rawDate.uppercase() else rawDate
                val horiz = when (config.clockAlignment) {
                    ClockHorizontalAlignment.LEFT -> Alignment.Start
                    ClockHorizontalAlignment.CENTER -> Alignment.CenterHorizontally
                    ClockHorizontalAlignment.RIGHT -> Alignment.End
                }
                val textAlign = when (config.clockAlignment) {
                    ClockHorizontalAlignment.LEFT -> TextAlign.Start
                    ClockHorizontalAlignment.CENTER -> TextAlign.Center
                    ClockHorizontalAlignment.RIGHT -> TextAlign.End
                }
                val weatherText = if (config.clockShowWeather) {
                    weatherSnapshot?.let {
                        when (tier) {
                            ModuleSizeTier.COMPACT -> it.compactReadout()
                            ModuleSizeTier.STANDARD -> it.mediumReadout()
                            ModuleSizeTier.LARGE -> it.fullReadout()
                        }
                    } ?: "☀ 68° Clear"
                } else {
                    null
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(pad),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = horiz,
                ) {
                    Text(
                        text = timeText,
                        style = when (tier) {
                            ModuleSizeTier.COMPACT -> MaterialTheme.typography.titleMedium
                            ModuleSizeTier.STANDARD -> MaterialTheme.typography.headlineSmall
                            ModuleSizeTier.LARGE -> MaterialTheme.typography.headlineMedium
                        },
                        fontWeight = FontWeight.Bold,
                        textAlign = textAlign,
                        maxLines = 1,
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = textAlign,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (weatherText != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = weatherText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = textAlign,
                            maxLines = 1,
                        )
                    }
                }
            }

            DashboardModuleType.CALENDAR -> {
                Column(modifier = Modifier.padding(pad)) {
                    Text(
                        text = "Calendar · ${tier.shortLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    when (tier) {
                        ModuleSizeTier.COMPACT -> Text(
                            text = "Today · 1 event",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        ModuleSizeTier.STANDARD -> Text(
                            text = "7-day strip · up to 2 events",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        ModuleSizeTier.LARGE -> {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                                    Text(
                                        text = d,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(
                                text = "Full month grid · 4 agenda items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            DashboardModuleType.NOTIFICATIONS -> {
                Column(modifier = Modifier.padding(pad)) {
                    Text(
                        text = "Notifications · ${tier.shortLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (appPrivacyMode) "Privacy Mode On · Previews hidden" else "Grouped recent alerts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            DashboardModuleType.NOTES -> {
                Column(modifier = Modifier.padding(pad)) {
                    Text(
                        text = "Pinned Notes · ${tier.shortLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = pinnedNotes.firstOrNull()?.title ?: "Share a note from Google Keep",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            DashboardModuleType.MEDIA -> {
                Column(modifier = Modifier.padding(pad), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Now Playing · ${tier.shortLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "⏮   ▶   ⏭",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LinearProgressIndicator(
                        progress = { 0.42f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                    )
                    if (tier != ModuleSizeTier.COMPACT) {
                        Text(
                            text = "1:24 / 3:45",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            DashboardModuleType.LAUNCHER -> {
                Column(modifier = Modifier.padding(pad)) {
                    Text(
                        text = "Apps · ${config.folderStyle.displayName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${launcherBoard.folders.size} folders · ${launcherBoard.topLevelItems.size} pinned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
