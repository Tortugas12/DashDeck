package com.dashdeck.app.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class DashboardModuleType(val displayName: String) {
    CLOCK("Clock"),
    CALENDAR("Calendar"),
    NOTIFICATIONS("Notifications"),
    NOTES("Pinned Notes"),
    MEDIA("Media Controls"),
    LAUNCHER("App Launcher"),
}

@Serializable
enum class WidgetType {
    DASHBOARD,
    MODULAR,
}

@Serializable
enum class FolderStyle(val displayName: String) {
    INLINE("Inline Expansion"),
    POPUP("Translucent Popup Overlay"),
}

@Serializable
enum class ModuleSizeTier(val shortLabel: String, val displayName: String) {
    COMPACT("S", "Compact"),
    STANDARD("M", "Standard"),
    LARGE("L", "Large"),
}

@Serializable
data class ModuleCardSize(
    val tier: ModuleSizeTier = ModuleSizeTier.STANDARD,
    val fullWidth: Boolean = false,
)

@Serializable
data class ChecklistItem(
    val id: String,
    val text: String,
    val isChecked: Boolean = false,
)

@Entity(tableName = "widget_configs")
data class WidgetConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "app_widget_id")
    val appWidgetId: Int,
    @ColumnInfo(name = "widget_type")
    val widgetType: String = WidgetType.DASHBOARD.name,
    @ColumnInfo(name = "enabled_modules_json")
    val enabledModulesJson: String,
    @ColumnInfo(name = "theme_tokens_json")
    val themeTokensJson: String,
    @ColumnInfo(name = "folder_style")
    val folderStyle: String = FolderStyle.INLINE.name,
    @ColumnInfo(name = "use_24_hour_clock")
    val use24HourClock: Boolean = false,
    @ColumnInfo(name = "show_quick_settings_gear")
    val showQuickSettingsGear: Boolean = true,
    @ColumnInfo(name = "selected_note_ids_json")
    val selectedNoteIdsJson: String = "[]",
    @ColumnInfo(name = "selected_folder_ids_json")
    val selectedFolderIdsJson: String = "[]",
    @ColumnInfo(name = "privacy_hide_notification_text")
    val privacyHideNotificationText: Boolean = false,
    @ColumnInfo(name = "module_sizes_json")
    val moduleSizesJson: String = "{}",
    @ColumnInfo(name = "layout_spec_json")
    val layoutSpecJson: String = "",
    @ColumnInfo(name = "clock_style")
    val clockStyle: String = "CLASSIC",
    @ColumnInfo(name = "clock_alignment")
    val clockAlignment: String = "CENTER",
    @ColumnInfo(name = "show_weather")
    val showWeather: Boolean = false,
)

@Entity(tableName = "pinned_notes")
data class PinnedNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val body: String,
    @ColumnInfo(name = "checklist_json")
    val checklistJson: String = "[]",
    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = true,
    @ColumnInfo(name = "source_package")
    val sourcePackage: String? = null,
    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long,
)

@Entity(tableName = "launcher_folders")
data class LauncherFolderEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "folder_id")
    val folderId: Long = 0L,
    val name: String,
    @ColumnInfo(name = "icon_tint_hex")
    val iconTintHex: String = "#6366F1",
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "launcher_items",
    foreignKeys = [
        ForeignKey(
            entity = LauncherFolderEntity::class,
            parentColumns = ["folder_id"],
            childColumns = ["folder_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["folder_id"]),
        Index(value = ["package_name", "activity_name"]),
    ],
)
data class LauncherItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "item_id")
    val itemId: Long = 0L,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "activity_name")
    val activityName: String,
    val label: String,
    @ColumnInfo(name = "folder_id")
    val folderId: Long? = null,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
)
