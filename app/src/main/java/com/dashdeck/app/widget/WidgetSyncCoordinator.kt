package com.dashdeck.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.dashdeck.app.data.db.WidgetType
import com.dashdeck.app.data.prefs.AppPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class WidgetSyncCoordinator(
    context: Context,
    private val widgetConfigRepository: WidgetConfigRepository,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        scope.launch {
            refreshRequests
                .debounce(DEBOUNCE_MS)
                .collect {
                    refreshAllWidgetsNow()
                }
        }
    }

    fun startObservingPreferences(preferencesRepository: AppPreferencesRepository) {
        scope.launch {
            preferencesRepository.preferencesFlow
                .drop(1)
                .collect {
                    requestRefreshAsync()
                }
        }
    }

    fun requestRefreshAsync() {
        refreshRequests.tryEmit(Unit)
    }

    /**
     * Persists [config] and forces a nonce-bumped Glance update on the application-scoped
     * coroutine so finishing [WidgetConfigActivity] can never cancel the redraw.
     */
    suspend fun saveAndRefreshWidget(config: WidgetConfigModel) {
        scope.async {
            widgetConfigRepository.save(config)
            refreshWidgetNowInternal(config.appWidgetId, config.widgetType)
        }.await()
    }

    suspend fun refreshWidgetNow(appWidgetId: Int) {
        scope.async {
            val type = widgetConfigRepository.getOrDefault(appWidgetId).widgetType
            refreshWidgetNowInternal(appWidgetId, type)
        }.await()
    }

    suspend fun refreshAllWidgetsNow() {
        scope.async {
            runCatching {
                val manager = GlanceAppWidgetManager(appContext)
                val glanceIds = manager.getGlanceIds(DashboardGlanceWidget::class.java)
                val nonce = System.nanoTime()
                glanceIds.forEach { glanceId ->
                    runCatching {
                        updateAppWidgetState(appContext, glanceId) { prefs ->
                            prefs[WidgetActionKeys.LAST_REFRESH_NONCE_PREF] = nonce
                        }
                        val appWidgetId = manager.getAppWidgetId(glanceId)
                        val type = widgetConfigRepository.getOrDefault(appWidgetId).widgetType
                        DashboardGlanceWidget(type).update(appContext, glanceId)
                    }
                }
            }
        }.await()
    }

    private suspend fun refreshWidgetNowInternal(appWidgetId: Int, widgetType: WidgetType) {
        runCatching {
            val manager = GlanceAppWidgetManager(appContext)
            val glanceId = manager.getGlanceIdBy(appWidgetId)
            updateAppWidgetState(appContext, glanceId) { prefs ->
                prefs[WidgetActionKeys.LAST_REFRESH_NONCE_PREF] = System.nanoTime()
            }
            DashboardGlanceWidget(widgetType).update(appContext, glanceId)
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 450L
    }
}
