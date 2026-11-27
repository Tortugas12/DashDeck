package com.dashdeck.app.data.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {
    @Query("SELECT * FROM pinned_notes ORDER BY is_pinned DESC, updated_at_epoch_ms DESC")
    fun observeAllNotes(): Flow<List<PinnedNoteEntity>>

    @Query("SELECT * FROM pinned_notes WHERE is_pinned = 1 ORDER BY updated_at_epoch_ms DESC")
    fun observePinnedNotes(): Flow<List<PinnedNoteEntity>>

    @Query("SELECT * FROM pinned_notes WHERE id = :noteId LIMIT 1")
    suspend fun getNoteById(noteId: Long): PinnedNoteEntity?

    @Query("SELECT * FROM pinned_notes WHERE is_pinned = 1 ORDER BY updated_at_epoch_ms DESC")
    suspend fun getPinnedNotesSnapshot(): List<PinnedNoteEntity>

    @Upsert
    suspend fun upsertNote(note: PinnedNoteEntity): Long

    @Query("UPDATE pinned_notes SET is_pinned = :isPinned, updated_at_epoch_ms = :updatedAt WHERE id = :noteId")
    suspend fun setPinned(noteId: Long, isPinned: Boolean, updatedAt: Long)

    @Query("DELETE FROM pinned_notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)
}

@Dao
interface LauncherDao {
    @Query("SELECT * FROM launcher_folders ORDER BY sort_order ASC, folder_id ASC")
    fun observeFolders(): Flow<List<LauncherFolderEntity>>

    @Query("SELECT * FROM launcher_folders ORDER BY sort_order ASC, folder_id ASC")
    suspend fun getFoldersSnapshot(): List<LauncherFolderEntity>

    @Query("SELECT * FROM launcher_items ORDER BY sort_order ASC, item_id ASC")
    fun observeItems(): Flow<List<LauncherItemEntity>>

    @Query("SELECT * FROM launcher_items ORDER BY sort_order ASC, item_id ASC")
    suspend fun getItemsSnapshot(): List<LauncherItemEntity>

    @Query("SELECT * FROM launcher_items WHERE folder_id = :folderId ORDER BY sort_order ASC, item_id ASC")
    suspend fun getItemsInFolder(folderId: Long): List<LauncherItemEntity>

    @Upsert
    suspend fun upsertFolder(folder: LauncherFolderEntity): Long

    @Upsert
    suspend fun upsertItem(item: LauncherItemEntity): Long

    @Upsert
    suspend fun upsertItems(items: List<LauncherItemEntity>)

    @Query("DELETE FROM launcher_folders WHERE folder_id = :folderId")
    suspend fun deleteFolderById(folderId: Long)

    @Query("DELETE FROM launcher_items WHERE item_id = :itemId")
    suspend fun deleteItemById(itemId: Long)

    @Query("DELETE FROM launcher_items WHERE package_name = :packageName")
    suspend fun deleteItemsByPackage(packageName: String)

    @Query("DELETE FROM launcher_items")
    suspend fun clearAllItems()

    @Query("DELETE FROM launcher_folders")
    suspend fun clearAllFolders()
}

@Dao
interface WidgetConfigDao {
    @Query("SELECT * FROM widget_configs ORDER BY app_widget_id ASC")
    fun observeAllConfigs(): Flow<List<WidgetConfigEntity>>

    @Query("SELECT * FROM widget_configs WHERE app_widget_id = :appWidgetId LIMIT 1")
    fun observeConfig(appWidgetId: Int): Flow<WidgetConfigEntity?>

    @Query("SELECT * FROM widget_configs WHERE app_widget_id = :appWidgetId LIMIT 1")
    suspend fun getConfig(appWidgetId: Int): WidgetConfigEntity?

    @Query("SELECT * FROM widget_configs ORDER BY app_widget_id ASC")
    suspend fun getAllConfigsSnapshot(): List<WidgetConfigEntity>

    @Upsert
    suspend fun upsertConfig(config: WidgetConfigEntity)

    @Query("UPDATE widget_configs SET theme_tokens_json = :themeTokensJson")
    suspend fun updateThemeForAllWidgets(themeTokensJson: String)

    @Query("DELETE FROM widget_configs WHERE app_widget_id = :appWidgetId")
    suspend fun deleteConfig(appWidgetId: Int)
}
