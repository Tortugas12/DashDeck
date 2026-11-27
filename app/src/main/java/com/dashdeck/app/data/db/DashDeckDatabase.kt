package com.dashdeck.app.data.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [
        WidgetConfigEntity::class,
        PinnedNoteEntity::class,
        LauncherFolderEntity::class,
        LauncherItemEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class DashDeckDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao
    abstract fun launcherDao(): LauncherDao
    abstract fun widgetConfigDao(): WidgetConfigDao

    companion object {
        private const val DB_NAME = "dashdeck.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE widget_configs ADD COLUMN module_sizes_json TEXT NOT NULL DEFAULT '{}'",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE widget_configs ADD COLUMN layout_spec_json TEXT NOT NULL DEFAULT ''",
                )
                connection.execSQL(
                    "ALTER TABLE widget_configs ADD COLUMN clock_style TEXT NOT NULL DEFAULT 'CLASSIC'",
                )
                connection.execSQL(
                    "ALTER TABLE widget_configs ADD COLUMN clock_alignment TEXT NOT NULL DEFAULT 'CENTER'",
                )
                connection.execSQL(
                    "ALTER TABLE widget_configs ADD COLUMN show_weather INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        fun create(context: Context): DashDeckDatabase {
            return Room.databaseBuilder<DashDeckDatabase>(
                context = context.applicationContext,
                name = DB_NAME,
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }
    }
}
