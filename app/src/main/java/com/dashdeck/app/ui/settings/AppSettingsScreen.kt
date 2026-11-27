package com.dashdeck.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dashdeck.app.data.db.WidgetConfigEntity
import com.dashdeck.app.data.prefs.AppPreferences
import com.dashdeck.app.data.repo.WeatherLocation
import com.dashdeck.app.theme.ThemeTokens
import com.dashdeck.app.ui.common.DashDeckCard
import com.dashdeck.app.ui.common.ModuleCardHeader
import com.dashdeck.app.ui.common.UnifiedThemePicker
import com.dashdeck.app.ui.common.desktopHoverHighlight
import com.dashdeck.app.widget.ClockHorizontalAlignment
import com.dashdeck.app.widget.ClockStylePreset
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppSettingsScreen(
    preferences: AppPreferences,
    activeWidgets: List<WidgetConfigEntity>,
    backupStatusMessage: String?,
    onSelectTheme: (ThemeTokens) -> Unit,
    onApplyThemeToAllWidgets: () -> Unit,
    onSelectClockStyle: (ClockStylePreset) -> Unit,
    onSelectClockAlignment: (ClockHorizontalAlignment) -> Unit,
    onToggle24HourClock: (Boolean) -> Unit,
    onToggleClockSeconds: (Boolean) -> Unit,
    onToggleShowWeather: (Boolean) -> Unit,
    onToggleWeatherFahrenheit: (Boolean) -> Unit,
    onToggleWeatherDeviceLocation: (Boolean) -> Unit,
    onSearchWeatherCities: suspend (String) -> List<WeatherLocation>,
    onSelectWeatherCity: (WeatherLocation) -> Unit,
    onRefreshWeatherNow: () -> Unit,
    onTogglePrivacyMode: (Boolean) -> Unit,
    onOpenPermissionsChecklist: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onOpenWidgetConfig: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCitySearchDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Settings & Theme Studio",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        // 1. Unified Theme Studio + Apply to All Widgets
        UnifiedThemePicker(
            tokens = preferences.globalTheme,
            onTokensChange = onSelectTheme,
            title = "Theme Studio",
            showSpacingSlider = true,
        )

        FilledTonalButton(
            onClick = onApplyThemeToAllWidgets,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Widgets, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Apply Theme to All Widgets")
        }

        // 2. Clock & Weather
        DashDeckCard {
            ModuleCardHeader(title = "Clock & Weather", icon = Icons.Outlined.Schedule)
            Spacer(modifier = Modifier.height(8.dp))

            Text("Clock Style", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ClockStylePreset.entries.forEach { style ->
                    FilterChip(
                        selected = preferences.clockStyle == style,
                        onClick = { onSelectClockStyle(style) },
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
                        selected = preferences.clockAlignment == align,
                        onClick = { onSelectClockAlignment(align) },
                        label = { Text(align.displayName) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            SettingsSwitchRow(
                title = "24-hour clock",
                subtitle = "Format clock in 24-hour time",
                checked = preferences.use24HourClock,
                onCheckedChange = onToggle24HourClock,
            )
            SettingsSwitchRow(
                title = "Show seconds",
                subtitle = "Display live seconds on the app clock",
                checked = preferences.showClockSeconds,
                onCheckedChange = onToggleClockSeconds,
            )
            SettingsSwitchRow(
                title = "Show Weather",
                subtitle = "Fetch current conditions via Open-Meteo (cached 30 min)",
                checked = preferences.showWeather,
                onCheckedChange = onToggleShowWeather,
            )

            if (preferences.showWeather) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = preferences.weatherUseFahrenheit,
                        onClick = { onToggleWeatherFahrenheit(true) },
                        label = { Text("°F Fahrenheit") },
                    )
                    FilterChip(
                        selected = !preferences.weatherUseFahrenheit,
                        onClick = { onToggleWeatherFahrenheit(false) },
                        label = { Text("°C Celsius") },
                    )
                }

                SettingsSwitchRow(
                    title = "Use device coarse location (optional)",
                    subtitle = "Leave off to use a manual city/ZIP search below",
                    checked = preferences.weatherUseDeviceLocation,
                    onCheckedChange = onToggleWeatherDeviceLocation,
                )

                val cityLabel = preferences.weatherCityName.ifBlank { "No city selected" }
                val cachedSummary = preferences.cachedWeather?.fullReadout()
                Text(
                    text = if (cachedSummary != null) "Current: $cachedSummary" else "City: $cityLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { showCitySearchDialog = true }) {
                        Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick City / ZIP")
                    }
                    OutlinedButton(onClick = onRefreshWeatherNow) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Refresh")
                    }
                }
            }
        }

        // 3. Privacy & Permissions
        DashDeckCard {
            ModuleCardHeader(title = "Privacy & Permissions", icon = Icons.Outlined.Shield)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSwitchRow(
                title = "Privacy Mode",
                subtitle = "Hide notification message previews on dashboard and all widgets",
                checked = preferences.privacyHideNotificationText,
                onCheckedChange = onTogglePrivacyMode,
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(onClick = onOpenPermissionsChecklist) {
                Text("Review Permissions")
            }
        }

        // 4. Backup & Restore
        DashDeckCard {
            ModuleCardHeader(title = "Backup & Restore (JSON)", icon = Icons.Outlined.Backup)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Export or import your theme tokens and app launcher folders as a local JSON file.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onExportBackup) {
                    Icon(Icons.Outlined.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export JSON")
                }
                OutlinedButton(onClick = onImportBackup) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import JSON")
                }
            }
            if (!backupStatusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = backupStatusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // 5. Active Widgets
        DashDeckCard {
            ModuleCardHeader(title = "Active Widgets", icon = Icons.Outlined.Widgets)
            Spacer(modifier = Modifier.height(8.dp))
            if (activeWidgets.isEmpty()) {
                Text(
                    text = "No home-screen or desktop widgets placed yet. Long-press your home screen or desktop to add a DashDeck widget.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    activeWidgets.forEach { widget ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .desktopHoverHighlight(cornerRadius = 10.dp)
                                .clickable { onOpenWidgetConfig(widget.appWidgetId) }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = "${widget.widgetType} Widget #${widget.appWidgetId}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Folder style: ${widget.folderStyle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "Configure",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCitySearchDialog) {
        WeatherCitySearchDialog(
            initialQuery = preferences.weatherCityName,
            onSearch = onSearchWeatherCities,
            onSelect = { loc ->
                onSelectWeatherCity(loc)
                showCitySearchDialog = false
            },
            onDismiss = { showCitySearchDialog = false },
        )
    }
}

@Composable
private fun WeatherCitySearchDialog(
    initialQuery: String,
    onSearch: suspend (String) -> List<WeatherLocation>,
    onSelect: (WeatherLocation) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<WeatherLocation>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchedOnce by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search Weather Location") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("City name or ZIP") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalButton(
                        enabled = !searching && query.trim().length >= 2,
                        onClick = {
                            searching = true
                            scope.launch {
                                results = onSearch(query)
                                searchedOnce = true
                                searching = false
                            }
                        },
                    ) {
                        Text(if (searching) "…" else "Go")
                    }
                }
                if (results.isEmpty() && searchedOnce && !searching) {
                    Text(
                        text = "No matching locations found.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                    ) {
                        items(results) { loc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .desktopHoverHighlight(cornerRadius = 8.dp)
                                    .clickable { onSelect(loc) }
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    text = loc.displayLabel(),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun TokenSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = androidx.compose.runtime.remember(valueRange) {
        androidx.compose.material3.SliderState(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            steps = 0,
            trackRange = valueRange,
        )
    }
    androidx.compose.runtime.LaunchedEffect(value) {
        val clamped = value.coerceIn(valueRange.start, valueRange.endInclusive)
        if (kotlin.math.abs(state.value - clamped) > 0.001f) {
            state.value = clamped
        }
    }
    Slider(
        state = state,
        onValueChange = { updated ->
            state.value = updated
            onValueChange(updated)
        },
        modifier = modifier,
    )
}
