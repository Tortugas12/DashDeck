package com.dashdeck.app.data.repo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.LruCache
import com.dashdeck.app.data.backup.BackupFolder
import com.dashdeck.app.data.backup.BackupLauncherItem
import com.dashdeck.app.data.db.LauncherDao
import com.dashdeck.app.data.db.LauncherFolderEntity
import com.dashdeck.app.data.db.LauncherItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

data class InstalledApp(
    val packageName: String,
    val activityName: String,
    val label: String,
)

data class LauncherFolderWithItems(
    val folder: LauncherFolderEntity,
    val items: List<LauncherItemEntity>,
)

data class LauncherBoardState(
    val folders: List<LauncherFolderWithItems>,
    val topLevelItems: List<LauncherItemEntity>,
)

class AppLauncherRepository(
    private val context: Context,
    private val launcherDao: LauncherDao,
    private val onLauncherChanged: () -> Unit = {},
) {
    private val iconCache = object : LruCache<String, Bitmap>(MAX_CACHED_ICONS) {}

    val boardState: Flow<LauncherBoardState> = combine(
        launcherDao.observeFolders(),
        launcherDao.observeItems(),
    ) { folders, items ->
        val itemsByFolder = items.groupBy { it.folderId }
        LauncherBoardState(
            folders = folders.map { folder ->
                LauncherFolderWithItems(
                    folder = folder,
                    items = itemsByFolder[folder.folderId].orEmpty(),
                )
            },
            topLevelItems = itemsByFolder[null].orEmpty(),
        )
    }

    suspend fun getBoardSnapshot(): LauncherBoardState {
        val folders = launcherDao.getFoldersSnapshot()
        val items = launcherDao.getItemsSnapshot()
        val itemsByFolder = items.groupBy { it.folderId }
        return LauncherBoardState(
            folders = folders.map { folder ->
                LauncherFolderWithItems(
                    folder = folder,
                    items = itemsByFolder[folder.folderId].orEmpty(),
                )
            },
            topLevelItems = itemsByFolder[null].orEmpty(),
        )
    }

    suspend fun loadInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }

        resolved
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .map { info ->
                InstalledApp(
                    packageName = info.activityInfo.packageName,
                    activityName = info.activityInfo.name,
                    label = info.loadLabel(pm).toString().trim()
                        .ifBlank { info.activityInfo.packageName },
                )
            }
            .distinctBy { "${it.packageName}/${it.activityName}" }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    suspend fun cleanupUninstalledApps(): Int = withContext(Dispatchers.IO) {
        val installedPackages = loadInstalledApps().map { it.packageName }.toSet()
        if (installedPackages.isEmpty()) return@withContext 0
        val storedItems = launcherDao.getItemsSnapshot()
        val missingPackages = storedItems
            .map { it.packageName }
            .filter { it !in installedPackages }
            .distinct()
        for (pkg in missingPackages) {
            launcherDao.deleteItemsByPackage(pkg)
        }
        if (missingPackages.isNotEmpty()) {
            onLauncherChanged()
        }
        missingPackages.size
    }

    fun getAppIconBitmap(packageName: String, activityName: String? = null): Bitmap? {
        val cacheKey = if (activityName.isNullOrBlank()) packageName else "$packageName/$activityName"
        iconCache.get(cacheKey)?.let { return it }

        val pm = context.packageManager
        val drawable = runCatching {
            if (!activityName.isNullOrBlank()) {
                pm.getActivityIcon(ComponentName(packageName, activityName))
            } else {
                pm.getApplicationIcon(packageName)
            }
        }.getOrElse {
            runCatching { pm.getApplicationIcon(packageName) }.getOrNull()
        } ?: return null

        val scaled = BitmapScaler.drawableToDownscaledBitmap(drawable, MAX_ICON_PX)
        iconCache.put(cacheKey, scaled)
        return scaled
    }

    fun launchApp(packageName: String, activityName: String? = null): Boolean {
        val intent = if (!activityName.isNullOrBlank()) {
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(packageName, activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } ?: return false

        val launched = runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)

        if (!launched) {
            val fallback = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } ?: return false
            return runCatching {
                context.startActivity(fallback)
                true
            }.getOrDefault(false)
        }
        return true
    }

    suspend fun createFolder(name: String, iconTintHex: String = "#6366F1"): Long {
        val existingCount = launcherDao.getFoldersSnapshot().size
        val id = launcherDao.upsertFolder(
            LauncherFolderEntity(
                name = name.trim().ifBlank { "Folder" },
                iconTintHex = iconTintHex,
                sortOrder = existingCount,
            ),
        )
        onLauncherChanged()
        return id
    }

    suspend fun renameFolder(folder: LauncherFolderEntity, newName: String, newTintHex: String = folder.iconTintHex) {
        launcherDao.upsertFolder(
            folder.copy(
                name = newName.trim().ifBlank { folder.name },
                iconTintHex = newTintHex,
            ),
        )
        onLauncherChanged()
    }

    suspend fun deleteFolder(folderId: Long) {
        launcherDao.deleteFolderById(folderId)
        onLauncherChanged()
    }

    suspend fun addAppToBoard(app: InstalledApp, folderId: Long? = null): Long {
        val currentItems = launcherDao.getItemsSnapshot()
        val sortOrder = currentItems.count { it.folderId == folderId }
        val id = launcherDao.upsertItem(
            LauncherItemEntity(
                packageName = app.packageName,
                activityName = app.activityName,
                label = app.label,
                folderId = folderId,
                sortOrder = sortOrder,
            ),
        )
        onLauncherChanged()
        return id
    }

    suspend fun moveItemToFolder(item: LauncherItemEntity, targetFolderId: Long?) {
        launcherDao.upsertItem(item.copy(folderId = targetFolderId))
        onLauncherChanged()
    }

    suspend fun removeItem(itemId: Long) {
        launcherDao.deleteItemById(itemId)
        onLauncherChanged()
    }

    suspend fun exportToBackup(): Pair<List<BackupFolder>, List<BackupLauncherItem>> {
        val snapshot = getBoardSnapshot()
        val folders = snapshot.folders.map { folderWithItems ->
            BackupFolder(
                name = folderWithItems.folder.name,
                iconTintHex = folderWithItems.folder.iconTintHex,
                sortOrder = folderWithItems.folder.sortOrder,
                items = folderWithItems.items.map { item ->
                    BackupLauncherItem(
                        packageName = item.packageName,
                        activityName = item.activityName,
                        label = item.label,
                        sortOrder = item.sortOrder,
                    )
                },
            )
        }
        val topLevel = snapshot.topLevelItems.map { item ->
            BackupLauncherItem(
                packageName = item.packageName,
                activityName = item.activityName,
                label = item.label,
                sortOrder = item.sortOrder,
            )
        }
        return folders to topLevel
    }

    suspend fun restoreFromBackup(
        folders: List<BackupFolder>,
        topLevelApps: List<BackupLauncherItem>,
    ) {
        launcherDao.clearAllItems()
        launcherDao.clearAllFolders()

        val allItems = mutableListOf<LauncherItemEntity>()
        folders.forEachIndexed { index, backupFolder ->
            val newFolderId = launcherDao.upsertFolder(
                LauncherFolderEntity(
                    name = backupFolder.name,
                    iconTintHex = backupFolder.iconTintHex,
                    sortOrder = index,
                ),
            )
            backupFolder.items.forEachIndexed { itemIdx, backupItem ->
                allItems += LauncherItemEntity(
                    packageName = backupItem.packageName,
                    activityName = backupItem.activityName,
                    label = backupItem.label,
                    folderId = newFolderId,
                    sortOrder = itemIdx,
                )
            }
        }
        topLevelApps.forEachIndexed { itemIdx, backupItem ->
            allItems += LauncherItemEntity(
                packageName = backupItem.packageName,
                activityName = backupItem.activityName,
                label = backupItem.label,
                folderId = null,
                sortOrder = itemIdx,
            )
        }
        if (allItems.isNotEmpty()) {
            launcherDao.upsertItems(allItems)
        }
        onLauncherChanged()
    }

    companion object {
        const val MAX_ICON_PX = 96
        private const val MAX_CACHED_ICONS = 80
    }
}
