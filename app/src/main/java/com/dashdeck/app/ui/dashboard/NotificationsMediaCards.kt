package com.dashdeck.app.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dashdeck.app.data.prefs.AppPreferences
import com.dashdeck.app.data.repo.InstalledApp
import com.dashdeck.app.data.repo.MediaPlaybackState
import com.dashdeck.app.data.repo.NotificationSummaryState
import com.dashdeck.app.ui.common.DashDeckCard
import com.dashdeck.app.ui.common.EmptyModuleState
import com.dashdeck.app.ui.common.ModuleCardHeader
import com.dashdeck.app.ui.common.PermissionMissingState
import com.dashdeck.app.ui.common.desktopHoverHighlight

@Composable
fun NotificationSummaryCard(
    state: NotificationSummaryState,
    preferences: AppPreferences,
    installedApps: List<InstalledApp>,
    onTogglePrivacyMode: (Boolean) -> Unit,
    onUpdateNotificationFilters: (useAllowList: Boolean, allowed: Set<String>, blocked: Set<String>) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFilterDialog by remember { mutableStateOf(false) }

    DashDeckCard(modifier = modifier) {
        ModuleCardHeader(
            title = "Notifications",
            icon = Icons.Outlined.Notifications,
            trailing = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (state.totalCount > 0) {
                        Badge { Text(state.totalCount.toString()) }
                    }
                    IconButton(onClick = { onTogglePrivacyMode(!preferences.privacyHideNotificationText) }) {
                        Icon(
                            imageVector = Icons.Outlined.PrivacyTip,
                            contentDescription = "Toggle Privacy Mode",
                            tint = if (preferences.privacyHideNotificationText) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = "Manage app notification filters",
                        )
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!state.hasListenerAccess) {
            PermissionMissingState(
                icon = Icons.Outlined.Notifications,
                message = "Enable Notification Access to summarize alerts locally (also enables Media Controls).",
                actionLabel = "Enable Notification Access",
                onActionClick = onOpenNotificationSettings,
            )
            return@DashDeckCard
        }

        if (state.groups.isEmpty()) {
            EmptyModuleState(
                icon = Icons.Outlined.Notifications,
                title = "All caught up",
                subtitle = if (preferences.privacyHideNotificationText) {
                    "Privacy Mode is active · No active notifications."
                } else {
                    "No active notifications right now."
                },
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.groups.take(5).forEach { group ->
                    val latest = group.items.firstOrNull()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .desktopHoverHighlight(cornerRadius = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = group.appLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (group.count > 1) {
                                        Text(
                                            text = "${group.count}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                                if (latest != null) {
                                    val summaryLine = buildString {
                                        if (!state.privacyModeEnabled && latest.title.isNotBlank() && latest.title != group.appLabel) {
                                            append(latest.title)
                                            if (latest.previewText.isNotBlank()) append(": ")
                                        }
                                        append(latest.previewText)
                                    }
                                    if (summaryLine.isNotBlank()) {
                                        Text(
                                            text = summaryLine,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
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

    if (showFilterDialog) {
        NotificationAppFilterDialog(
            installedApps = installedApps,
            useAllowList = preferences.useAllowListForNotifications,
            allowedPackages = preferences.allowedNotificationPackages,
            blockedPackages = preferences.blockedNotificationPackages,
            onSave = { allowMode, allowed, blocked ->
                onUpdateNotificationFilters(allowMode, allowed, blocked)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false },
        )
    }
}

@Composable
private fun NotificationAppFilterDialog(
    installedApps: List<InstalledApp>,
    useAllowList: Boolean,
    allowedPackages: Set<String>,
    blockedPackages: Set<String>,
    onSave: (Boolean, Set<String>, Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var localAllowMode by remember(useAllowList) { mutableStateOf(useAllowList) }
    var localAllowed by remember(allowedPackages) { mutableStateOf(allowedPackages) }
    var localBlocked by remember(blockedPackages) { mutableStateOf(blockedPackages) }
    val uniqueApps = remember(installedApps) { installedApps.distinctBy { it.packageName } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notification App Filters") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !localAllowMode,
                        onClick = { localAllowMode = false },
                        label = { Text("Block Selected") },
                    )
                    FilterChip(
                        selected = localAllowMode,
                        onClick = { localAllowMode = true },
                        label = { Text("Allow Only") },
                    )
                }
                Text(
                    text = if (localAllowMode) {
                        "Only checked apps will appear in Notification Summary."
                    } else {
                        "Checked apps will be hidden from Notification Summary."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                ) {
                    items(uniqueApps, key = { it.packageName }) { app ->
                        val checked = if (localAllowMode) {
                            app.packageName in localAllowed
                        } else {
                            app.packageName in localBlocked
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (localAllowMode) {
                                        localAllowed = if (checked) localAllowed - app.packageName else localAllowed + app.packageName
                                    } else {
                                        localBlocked = if (checked) localBlocked - app.packageName else localBlocked + app.packageName
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    if (localAllowMode) {
                                        localAllowed = if (isChecked) localAllowed + app.packageName else localAllowed - app.packageName
                                    } else {
                                        localBlocked = if (isChecked) localBlocked + app.packageName else localBlocked - app.packageName
                                    }
                                },
                            )
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(localAllowMode, localAllowed, localBlocked) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun MediaControllerCard(
    state: MediaPlaybackState,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DashDeckCard(modifier = modifier) {
        ModuleCardHeader(
            title = "Now Playing",
            icon = Icons.Outlined.MusicNote,
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (!state.hasListenerAccess) {
            PermissionMissingState(
                icon = Icons.Outlined.MusicNote,
                message = "Enable Notification Access to display artwork and control active media.",
                actionLabel = "Enable Access",
                onActionClick = onOpenNotificationSettings,
            )
            return@DashDeckCard
        }

        if (!state.isActive) {
            EmptyModuleState(
                icon = Icons.Outlined.MusicNote,
                title = "No media playing right now",
                subtitle = "Start music, podcasts, or video in any app to control it here.",
            )
            return@DashDeckCard
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val artBitmap = state.albumArt
            if (artBitmap != null) {
                Image(
                    bitmap = artBitmap.asImageBitmap(),
                    contentDescription = "Album artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title.ifBlank { "Playing Media" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.artist.isNotBlank()) {
                    Text(
                        text = state.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state.album.isNotBlank()) {
                    Text(
                        text = state.album,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (state.durationMs > 0L) {
            var nowElapsed by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableLongStateOf(android.os.SystemClock.elapsedRealtime())
            }
            androidx.compose.runtime.LaunchedEffect(state.isPlaying, state.positionMs, state.durationMs) {
                nowElapsed = android.os.SystemClock.elapsedRealtime()
                while (state.isPlaying) {
                    kotlinx.coroutines.delay(1_000L)
                    nowElapsed = android.os.SystemClock.elapsedRealtime()
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { state.progressFraction(nowElapsed) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = com.dashdeck.app.data.repo.formatDurationMmSs(state.currentPositionMs(nowElapsed)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = com.dashdeck.app.data.repo.formatDurationMmSs(state.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.desktopHoverHighlight(cornerRadius = 20.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.SkipPrevious,
                    contentDescription = "Previous track",
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            FilledIconButton(
                onClick = onPlayPause,
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = onNext,
                modifier = Modifier.desktopHoverHighlight(cornerRadius = 20.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.SkipNext,
                    contentDescription = "Next track",
                )
            }
        }
    }
}
