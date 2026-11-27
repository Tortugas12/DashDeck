package com.dashdeck.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dashdeck.app.data.db.ChecklistItem
import com.dashdeck.app.data.repo.NoteModel
import com.dashdeck.app.ui.common.DashDeckCard
import com.dashdeck.app.ui.common.EmptyModuleState
import com.dashdeck.app.ui.common.ModuleCardHeader
import com.dashdeck.app.ui.common.desktopHoverHighlight
import com.dashdeck.app.ui.common.onSecondaryClick
import java.util.UUID

@Composable
fun PinnedNotesCard(
    notes: List<NoteModel>,
    onToggleChecklist: (noteId: Long, itemId: String) -> Unit,
    onTogglePin: (noteId: Long, isPinned: Boolean) -> Unit,
    onSaveNote: (id: Long, title: String, body: String, checklist: List<ChecklistItem>, isPinned: Boolean) -> Unit,
    onDeleteNote: (noteId: Long) -> Unit,
    onOpenInKeep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingNote by remember { mutableStateOf<NoteModel?>(null) }
    var showNewNoteDialog by remember { mutableStateOf(false) }
    val pinned = remember(notes) { notes.filter { it.isPinned }.take(3) }

    DashDeckCard(modifier = modifier) {
        ModuleCardHeader(
            title = "Pinned Notes",
            icon = Icons.AutoMirrored.Outlined.StickyNote2,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onOpenInKeep) {
                        Text("Open in Keep")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    IconButton(onClick = { showNewNoteDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "New note",
                        )
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (pinned.isEmpty()) {
            EmptyModuleState(
                icon = Icons.AutoMirrored.Outlined.StickyNote2,
                title = "No pinned notes yet",
                subtitle = "Share a note from Google Keep to pin it here, or tap + to add one.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pinned.forEach { note ->
                    NoteItemSurface(
                        note = note,
                        maxChecklistPreview = 4,
                        onToggleChecklist = { itemId -> onToggleChecklist(note.id, itemId) },
                        onTogglePin = { onTogglePin(note.id, !note.isPinned) },
                        onEdit = { editingNote = note },
                        onDelete = { onDeleteNote(note.id) },
                    )
                }
            }
        }
    }

    if (showNewNoteDialog) {
        NoteEditorDialog(
            initialNote = null,
            onSave = { title, body, checklist, isPinned ->
                onSaveNote(0L, title, body, checklist, isPinned)
                showNewNoteDialog = false
            },
            onDismiss = { showNewNoteDialog = false },
        )
    }

    editingNote?.let { note ->
        NoteEditorDialog(
            initialNote = note,
            onSave = { title, body, checklist, isPinned ->
                onSaveNote(note.id, title, body, checklist, isPinned)
                editingNote = null
            },
            onDismiss = { editingNote = null },
        )
    }
}

@Composable
fun NotesScreen(
    notes: List<NoteModel>,
    onToggleChecklist: (noteId: Long, itemId: String) -> Unit,
    onTogglePin: (noteId: Long, isPinned: Boolean) -> Unit,
    onSaveNote: (id: Long, title: String, body: String, checklist: List<ChecklistItem>, isPinned: Boolean) -> Unit,
    onDeleteNote: (noteId: Long) -> Unit,
    onOpenInKeep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingNote by remember { mutableStateOf<NoteModel?>(null) }
    var showNewNoteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Notes & Keep Inbox",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Share any note from Google Keep or create local checklists.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenInKeep) {
                    Text("Open in Keep")
                }
                FilledTonalButton(onClick = { showNewNoteDialog = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Note")
                }
            }
        }

        if (notes.isEmpty()) {
            DashDeckCard {
                EmptyModuleState(
                    icon = Icons.AutoMirrored.Outlined.StickyNote2,
                    title = "No notes saved yet",
                    subtitle = "Use Android Share from Google Keep -> Pin to DashDeck, or create a note above.",
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteItemSurface(
                        note = note,
                        maxChecklistPreview = Int.MAX_VALUE,
                        onToggleChecklist = { itemId -> onToggleChecklist(note.id, itemId) },
                        onTogglePin = { onTogglePin(note.id, !note.isPinned) },
                        onEdit = { editingNote = note },
                        onDelete = { onDeleteNote(note.id) },
                    )
                }
            }
        }
    }

    if (showNewNoteDialog) {
        NoteEditorDialog(
            initialNote = null,
            onSave = { title, body, checklist, isPinned ->
                onSaveNote(0L, title, body, checklist, isPinned)
                showNewNoteDialog = false
            },
            onDismiss = { showNewNoteDialog = false },
        )
    }

    editingNote?.let { note ->
        NoteEditorDialog(
            initialNote = note,
            onSave = { title, body, checklist, isPinned ->
                onSaveNote(note.id, title, body, checklist, isPinned)
                editingNote = null
            },
            onDismiss = { editingNote = null },
        )
    }
}

@Composable
private fun NoteItemSurface(
    note: NoteModel,
    maxChecklistPreview: Int,
    onToggleChecklist: (String) -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var contextMenuExpanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .desktopHoverHighlight(cornerRadius = 14.dp)
                .onSecondaryClick { contextMenuExpanded = true }
                .clickable { onEdit() },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Row {
                        IconButton(onClick = onTogglePin, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.PushPin,
                                contentDescription = if (note.isPinned) "Unpin note" else "Pin note",
                                tint = if (note.isPinned) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit note",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                if (note.body.isNotBlank()) {
                    Text(
                        text = note.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (maxChecklistPreview == Int.MAX_VALUE) 10 else 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (note.checklist.isNotEmpty()) {
                    note.checklist.take(maxChecklistPreview).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleChecklist(item.id) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = { onToggleChecklist(item.id) },
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (item.isChecked) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (note.checklist.size > maxChecklistPreview) {
                        Text(
                            text = "+${note.checklist.size - maxChecklistPreview} more items",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = contextMenuExpanded,
            onDismissRequest = { contextMenuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = {
                    contextMenuExpanded = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(if (note.isPinned) "Unpin" else "Pin to Dashboard") },
                leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
                onClick = {
                    contextMenuExpanded = false
                    onTogglePin()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                onClick = {
                    contextMenuExpanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
fun NoteEditorDialog(
    initialNote: NoteModel?,
    onSave: (title: String, body: String, checklist: List<ChecklistItem>, isPinned: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember(initialNote) { mutableStateOf(initialNote?.title.orEmpty()) }
    var body by remember(initialNote) { mutableStateOf(initialNote?.body.orEmpty()) }
    var isPinned by remember(initialNote) { mutableStateOf(initialNote?.isPinned ?: true) }
    val checklist = remember(initialNote) {
        mutableStateListOf<ChecklistItem>().apply {
            addAll(initialNote?.checklist.orEmpty())
        }
    }
    var newItemText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialNote == null) "New Note" else "Edit Note") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Body") },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Pin to Dashboard & Widgets", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                }

                Text(
                    text = "Checklist Items",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                checklist.forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = item.isChecked,
                            onCheckedChange = { checked ->
                                checklist[idx] = item.copy(isChecked = checked)
                            },
                        )
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { checklist.removeAt(idx) }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Remove item", modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OutlinedTextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        label = { Text("Add checklist item") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            val trimmed = newItemText.trim()
                            if (trimmed.isNotEmpty()) {
                                checklist.add(
                                    ChecklistItem(
                                        id = "item_${UUID.randomUUID().toString().take(8)}",
                                        text = trimmed,
                                        isChecked = false,
                                    ),
                                )
                                newItemText = ""
                            }
                        },
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add checklist item")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, body, checklist.toList(), isPinned) },
            ) {
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
