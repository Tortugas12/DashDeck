package com.dashdeck.app.di

import android.content.Context
import com.dashdeck.app.data.backup.BackupSerializer
import com.dashdeck.app.data.db.DashDeckDatabase
import com.dashdeck.app.data.prefs.AppPreferencesRepository
import com.dashdeck.app.data.repo.AppLauncherRepository
import com.dashdeck.app.data.repo.CalendarRepository
import com.dashdeck.app.data.repo.MediaControlRepository
import com.dashdeck.app.data.repo.NotesRepository
import com.dashdeck.app.data.repo.NotificationSummaryRepository
import kotlinx.serialization.json.Json

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    @Volatile
    private var widgetRefreshCallback: (suspend () -> Unit)? = null

    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val database: DashDeckDatabase by lazy {
        DashDeckDatabase.create(appContext)
    }

    val notesDao by lazy { database.notesDao() }
    val launcherDao by lazy { database.launcherDao() }
    val widgetConfigDao by lazy { database.widgetConfigDao() }

    val preferencesRepository: AppPreferencesRepository by lazy {
        AppPreferencesRepository(appContext, json)
    }

    val backupSerializer: BackupSerializer by lazy {
        BackupSerializer()
    }

    val calendarRepository: CalendarRepository by lazy {
        CalendarRepository(appContext)
    }

    val notificationSummaryRepository: NotificationSummaryRepository by lazy {
        NotificationSummaryRepository(appContext, preferencesRepository)
    }

    val mediaControlRepository: MediaControlRepository by lazy {
        MediaControlRepository(appContext)
    }

    val notesRepository: NotesRepository by lazy {
        NotesRepository(notesDao, json) {
            onRepositoryMutated()
        }
    }

    val appLauncherRepository: AppLauncherRepository by lazy {
        AppLauncherRepository(appContext, launcherDao) {
            onRepositoryMutated()
        }
    }

    val widgetConfigRepository: com.dashdeck.app.widget.WidgetConfigRepository by lazy {
        com.dashdeck.app.widget.WidgetConfigRepository(widgetConfigDao, json)
    }

    val weatherRepository: com.dashdeck.app.data.repo.WeatherRepository by lazy {
        com.dashdeck.app.data.repo.WeatherRepository(appContext, preferencesRepository)
    }

    @Volatile
    private var repoMutationListener: (() -> Unit)? = null

    @Volatile
    private var singleWidgetRefreshCallback: (suspend (Int) -> Unit)? = null

    @Volatile
    private var saveAndRefreshWidgetCallback: (suspend (com.dashdeck.app.widget.WidgetConfigModel) -> Unit)? = null

    fun setWidgetRefreshHooks(
        onSuspendRefresh: suspend () -> Unit,
        onAsyncRefresh: () -> Unit,
        onRefreshSingleWidget: suspend (Int) -> Unit = {},
        onSaveAndRefreshWidget: suspend (com.dashdeck.app.widget.WidgetConfigModel) -> Unit = {
            widgetConfigRepository.save(it)
        },
    ) {
        widgetRefreshCallback = onSuspendRefresh
        repoMutationListener = onAsyncRefresh
        singleWidgetRefreshCallback = onRefreshSingleWidget
        saveAndRefreshWidgetCallback = onSaveAndRefreshWidget
    }

    suspend fun notifyWidgetDataChanged() {
        widgetRefreshCallback?.invoke()
    }

    suspend fun refreshWidgetNow(appWidgetId: Int) {
        singleWidgetRefreshCallback?.invoke(appWidgetId)
    }

    suspend fun saveAndRefreshWidget(config: com.dashdeck.app.widget.WidgetConfigModel) {
        val hook = saveAndRefreshWidgetCallback
        if (hook != null) {
            hook.invoke(config)
        } else {
            widgetConfigRepository.save(config)
        }
    }

    fun requestWidgetRefreshAsync() {
        repoMutationListener?.invoke()
    }

    private fun onRepositoryMutated() {
        repoMutationListener?.invoke()
    }
}
