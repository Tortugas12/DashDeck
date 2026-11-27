package com.dashdeck.app.ui.dashboard

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dashdeck.app.data.repo.InstalledApp
import com.dashdeck.app.data.repo.NoteModel
import com.dashdeck.app.ui.common.desktopHoverHighlight

@Composable
fun QuickSearchDialog(
    installedApps: List<InstalledApp>,
    notes: List<NoteModel>,
    iconLoader: (String, String?) -> Bitmap?,
    onLaunchApp: (InstalledApp) -> Unit,
    onSelectNote: (NoteModel) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val matchingApps = remember(installedApps, query) {
        if (query.isBlank()) installedApps.take(6)
        else installedApps.filter { it.label.contains(query, ignoreCase = true) }.take(8)
    }

    val matchingNotes = remember(notes, query) {
        if (query.isBlank()) notes.take(4)
        else notes.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.body.contains(query, ignoreCase = true) ||
                it.checklist.any { item -> item.text.contains(query, ignoreCase = true) }
        }.take(6)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                        onDismiss()
                        true
                    } else if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                        matchingApps.firstOrNull()?.let {
                            onLaunchApp(it)
                            onDismiss()
                            return@onPreviewKeyEvent true
                        }
                        false
                    } else {
                        false
                    }
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search apps & notes… (Esc to close)") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (matchingApps.isNotEmpty()) {
                        item {
                            Text(
                                text = "Apps",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                        items(matchingApps, key = { "app_${it.packageName}/${it.activityName}" }) { app ->
                            val bmp = remember(app.packageName, app.activityName) {
                                iconLoader(app.packageName, app.activityName)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .desktopHoverHighlight(cornerRadius = 10.dp)
                                    .clickable {
                                        onLaunchApp(app)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppIconBox(bitmap = bmp, label = app.label, sizeDp = 28)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    if (matchingNotes.isNotEmpty()) {
                        item {
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            )
                        }
                        items(matchingNotes, key = { "note_${it.id}" }) { note ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .desktopHoverHighlight(cornerRadius = 10.dp)
                                    .clickable {
                                        onSelectNote(note)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (note.body.isNotBlank()) {
                                        Text(
                                            text = note.body,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (matchingApps.isEmpty() && matchingNotes.isEmpty()) {
                        item {
                            Text(
                                text = "No matching apps or notes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
