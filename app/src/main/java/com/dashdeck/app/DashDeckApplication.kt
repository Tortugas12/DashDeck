package com.dashdeck.app

import android.app.Application
import android.content.res.Configuration
import com.dashdeck.app.di.AppContainer
import com.dashdeck.app.widget.WidgetSyncCoordinator

class DashDeckApplication : Application() {
    lateinit var container: AppContainer
        private set

    private lateinit var widgetSyncCoordinator: WidgetSyncCoordinator
    private var lastNightModeMask: Int = Configuration.UI_MODE_NIGHT_UNDEFINED

    override fun onCreate() {
        super.onCreate()
        lastNightModeMask = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        container = AppContainer(this)
        widgetSyncCoordinator = WidgetSyncCoordinator(
            context = this,
            widgetConfigRepository = container.widgetConfigRepository,
        )

        container.setWidgetRefreshHooks(
            onSuspendRefresh = { widgetSyncCoordinator.refreshAllWidgetsNow() },
            onAsyncRefresh = { widgetSyncCoordinator.requestRefreshAsync() },
            onRefreshSingleWidget = { id -> widgetSyncCoordinator.refreshWidgetNow(id) },
            onSaveAndRefreshWidget = { cfg -> widgetSyncCoordinator.saveAndRefreshWidget(cfg) },
        )
        widgetSyncCoordinator.startObservingPreferences(container.preferencesRepository)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newNightMask = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (newNightMask != lastNightModeMask) {
            lastNightModeMask = newNightMask
            widgetSyncCoordinator.requestRefreshAsync()
        }
    }
}
