package com.dashdeck.app.ui.dashboard

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dashdeck.app.data.db.LauncherFolderEntity
import com.dashdeck.app.data.db.LauncherItemEntity
import com.dashdeck.app.data.repo.InstalledApp
import com.dashdeck.app.data.repo.LauncherBoardState
import com.dashdeck.app.data.repo.LauncherFolderWithItems
import com.dashdeck.app.theme.parseHexColor
import com.dashdeck.app.ui.common.DashDeckCard
import com.dashdeck.app.ui.common.EmptyModuleState
import com.dashdeck.app.ui.common.ModuleCardHeader
import com.dashdeck.app.ui.common.desktopHoverHighlight
import com.dashdeck.app.ui.common.onSecondaryClick

private val folderTintChoices = listOf("#6366F1", "#38BDF8", "#10B981", "#F59E0B", "#EC4899", "#C2593F")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LauncherCard(
    boardState: LauncherBoardState,
    iconLoader: (packageName: String, activityName: String?) -> Bitmap?,
    onLaunchApp: (packageName: String, activityName: String?) -> Unit,
    onCreateFolder: (name: String, tintHex: String) -> Unit,
    onRenameFolder: (LauncherFolderEntity, String, String) -> Unit,
    onDeleteFolder: (Long) -> Unit,
    onAddApp: (InstalledApp, Long?) -> Unit,
    onRemoveItem: (Long) -> Unit,
    installedApps: List<InstalledApp>,
    modifier: Modifier = Modifier,
) {
    var expandedFolderId by remember { mutableStateOf<Long?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var addAppTargetFolderId by remember { mutableStateOf<Long?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }
    var renamingFolder by remember { mutableStateOf<LauncherFolderEntity?>(null) }

    DashDeckCard(modifier = modifier) {
        ModuleCardHeader(
            title = "App Launcher",
            icon = Icons.Outlined.Apps,
            trailing = {
                Row {
                    IconButton(onClick = { showCreateFolderDialog = true }) {
                        Icon(Icons.Outlined.CreateNewFolder, contentDescription = "New folder")
                    }
                    IconButton(
                        onClick = {
                            addAppTargetFolderId = null
                            showAppPicker = true
                        },
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Pin app")
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (boardState.folders.isEmpty() && boardState.topLevelItems.isEmpty()) {
            EmptyModuleState(
                icon = Icons.Outlined.Apps,
                title = "No pinned apps or folders",
                subtitle = "Create a folder or tap + to pin your favorite apps.",
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                boardState.folders.forEach { folderWithItems ->
                    FolderTile(
                        folderWithItems = folderWithItems,
                        isExpanded = expandedFolderId == folderWithItems.folder.folderId,
                        onClick = {
                            expandedFolderId = if (expandedFolderId == folderWithItems.folder.folderId) {
                                null
                            } else {
                                folderWithItems.folder.folderId
                            }
                        },
                        onAddApp = {
                            addAppTargetFolderId = folderWithItems.folder.folderId
                            showAppPicker = true
                        },
                        onRename = { renamingFolder = folderWithItems.folder },
                        onDelete = {
                            if (expandedFolderId == folderWithItems.folder.folderId) expandedFolderId = null
                            onDeleteFolder(folderWithItems.folder.folderId)
                        },
                    )
                }

                boardState.topLevelItems.forEach { item ->
                    AppTile(
                        item = item,
                        iconBitmap = remember(item.packageName, item.activityName) {
                            iconLoader(item.packageName, item.activityName)
                        },
                        onClick = { onLaunchApp(item.packageName, item.activityName) },
                        onRemove = { onRemoveItem(item.itemId) },
                    )
                }
            }

            // Inline expanded folder tray
            val expandedFolder = boardState.folders.firstOrNull { it.folder.folderId == expandedFolderId }
            if (expandedFolder != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = expandedFolder.folder.name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            TextButton(
                                onClick = {
                                    addAppTargetFolderId = expandedFolder.folder.folderId
                                    showAppPicker = true
                                },
                            ) {
                                Text("Add App")
                            }
                        }
                        if (expandedFolder.items.isEmpty()) {
                            Text(
                                text = "Folder is empty. Tap Add App to assign apps.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                expandedFolder.items.forEach { item ->
                                    AppTile(
                                        item = item,
                                        iconBitmap = remember(item.packageName, item.activityName) {
                                            iconLoader(item.packageName, item.activityName)
                                        },
                                        onClick = { onLaunchApp(item.packageName, item.activityName) },
                                        onRemove = { onRemoveItem(item.itemId) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        FolderEditorDialog(
            initialName = "",
            initialTintHex = folderTintChoices.first(),
            title = "Create Folder",
            onConfirm = { name, tint ->
                onCreateFolder(name, tint)
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false },
        )
    }

    renamingFolder?.let { folder ->
        FolderEditorDialog(
            initialName = folder.name,
            initialTintHex = folder.iconTintHex,
            title = "Rename Folder",
            onConfirm = { name, tint ->
                onRenameFolder(folder, name, tint)
                renamingFolder = null
            },
            onDismiss = { renamingFolder = null },
        )
    }

    if (showAppPicker) {
        AppPickerDialog(
            installedApps = installedApps,
            onPick = { app ->
                onAddApp(app, addAppTargetFolderId)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false },
        )
    }
}

@Composable
fun LauncherScreen(
    boardState: LauncherBoardState,
    installedApps: List<InstalledApp>,
    iconLoader: (packageName: String, activityName: String?) -> Bitmap?,
    onLaunchApp: (packageName: String, activityName: String?) -> Unit,
    onCreateFolder: (name: String, tintHex: String) -> Unit,
    onRenameFolder: (LauncherFolderEntity, String, String) -> Unit,
    onDeleteFolder: (Long) -> Unit,
    onAddApp: (InstalledApp, Long?) -> Unit,
    onRemoveItem: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filterText by remember { mutableStateOf("") }
    val filteredApps = remember(installedApps, filterText) {
        if (filterText.isBlank()) installedApps
        else installedApps.filter { it.label.contains(filterText, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "App Launcher & Folders",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        LauncherCard(
            boardState = boardState,
            iconLoader = iconLoader,
            onLaunchApp = onLaunchApp,
            onCreateFolder = onCreateFolder,
            onRenameFolder = onRenameFolder,
            onDeleteFolder = onDeleteFolder,
            onAddApp = onAddApp,
            onRemoveItem = onRemoveItem,
            installedApps = installedApps,
        )

        DashDeckCard {
            Text(
                text = "All Installed Apps (${filteredApps.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = filterText,
                onValueChange = { filterText = it },
                placeholder = { Text("Filter apps...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                filteredApps.take(40).forEach { app ->
                    val bmp = remember(app.packageName, app.activityName) {
                        iconLoader(app.packageName, app.activityName)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .desktopHoverHighlight(cornerRadius = 10.dp)
                            .clickable { onLaunchApp(app.packageName, app.activityName) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIconBox(bitmap = bmp, label = app.label, sizeDp = 32)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        OutlinedButton(onClick = { onAddApp(app, null) }) {
                            Text("Pin")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderTile(
    folderWithItems: LauncherFolderWithItems,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onAddApp: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val tint = parseHexColor(folderWithItems.folder.iconTintHex, MaterialTheme.colorScheme.primary)

    Box {
        Column(
            modifier = Modifier
                .width(74.dp)
                .desktopHoverHighlight(cornerRadius = 14.dp)
                .onSecondaryClick { menuExpanded = true }
                .clickable { onClick() }
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tint.copy(alpha = if (isExpanded) 0.30f else 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.FolderOpen else Icons.Outlined.Folder,
                    contentDescription = folderWithItems.folder.name,
                    tint = tint,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = folderWithItems.folder.name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "${folderWithItems.items.size} apps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }

        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Add App to Folder") },
                leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onAddApp()
                },
            )
            DropdownMenuItem(
                text = { Text("Rename Folder") },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete Folder") },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun AppTile(
    item: LauncherItemEntity,
    iconBitmap: Bitmap?,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Column(
            modifier = Modifier
                .width(74.dp)
                .desktopHoverHighlight(cornerRadius = 14.dp)
                .onSecondaryClick { menuExpanded = true }
                .clickable { onClick() }
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIconBox(bitmap = iconBitmap, label = item.label, sizeDp = 44)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Remove from Launcher") },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onRemove()
                },
            )
        }
    }
}

@Composable
fun AppIconBox(
    bitmap: Bitmap?,
    label: String,
    sizeDp: Int,
) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = label,
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
    } else {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FolderEditorDialog(
    initialName: String,
    initialTintHex: String,
    title: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var tintHex by remember { mutableStateOf(initialTintHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    folderTintChoices.forEach { hex ->
                        val color = parseHexColor(hex)
                        Box(
                            modifier = Modifier
                                .size(if (tintHex.equals(hex, ignoreCase = true)) 32.dp else 26.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { tintHex = hex },
                        )
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = { onConfirm(name, tintHex) }) {
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
private fun AppPickerDialog(
    installedApps: List<InstalledApp>,
    onPick: (InstalledApp) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(installedApps, query) {
        if (query.isBlank()) installedApps
        else installedApps.filter { it.label.contains(query, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick an App") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search apps...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                ) {
                    items(filtered, key = { "${it.packageName}/${it.activityName}" }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(app) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
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
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
