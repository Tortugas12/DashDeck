package com.dashdeck.app.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.dashdeck.app.DashDeckApplication
import com.dashdeck.app.data.repo.RawNotificationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class DashboardNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val widgetRefreshTriggers = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private var mediaSessionManager: MediaSessionManager? = null
    private val trackedSessions = linkedMapOf<MediaSession.Token, Pair<MediaController, MediaController.Callback>>()
    private var selectedToken: MediaSession.Token? = null
    private var progressTickerJob: Job? = null

    private val appContainer
        get() = (application as DashDeckApplication).container

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        syncMediaSessions(controllers.orEmpty())
    }

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            widgetRefreshTriggers
                .debounce(WIDGET_REFRESH_DEBOUNCE_MS)
                .collect {
                    appContainer.notifyWidgetDataChanged()
                }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        syncNotifications(listenerConnected = true)
        startMediaSessionTracking()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        stopMediaSessionTracking()
        appContainer.notificationSummaryRepository.updateActiveNotifications(
            items = emptyList(),
            listenerConnected = false,
        )
        appContainer.mediaControlRepository.updateController(
            controller = null,
            listenerConnected = false,
        )
        scheduleDebouncedWidgetRefresh()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        syncNotifications(listenerConnected = true)
        if (sbn != null && isMediaNotification(sbn)) {
            refreshMediaSessions()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        syncNotifications(listenerConnected = true)
        if (sbn != null && isMediaNotification(sbn)) {
            refreshMediaSessions()
        }
    }

    override fun onDestroy() {
        stopMediaSessionTracking()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun isMediaNotification(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        if (notification.category == Notification.CATEGORY_TRANSPORT) return true
        return notification.extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true
    }

    private fun syncNotifications(listenerConnected: Boolean) {
        val active = runCatching { activeNotifications.orEmpty().toList() }.getOrDefault(emptyList())
        val pm = packageManager
        val items = active
            .asSequence()
            .filter { sbn ->
                val flags = sbn.notification.flags
                (flags and Notification.FLAG_GROUP_SUMMARY) == 0
            }
            .map { sbn ->
                val extras = sbn.notification.extras
                val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
                val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
                    ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
                    ?: ""
                val appLabel = runCatching {
                    val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                }.getOrDefault(sbn.packageName)

                RawNotificationItem(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    appLabel = appLabel,
                    title = title.ifBlank { appLabel },
                    text = text,
                    postTimeMs = sbn.postTime,
                    isOngoing = sbn.isOngoing,
                )
            }
            .toList()

        appContainer.notificationSummaryRepository.updateActiveNotifications(
            items = items,
            listenerConnected = listenerConnected,
        )
        scheduleDebouncedWidgetRefresh()
    }

    private fun startMediaSessionTracking() {
        val manager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
        mediaSessionManager = manager
        val componentName = ComponentName(this, DashboardNotificationListenerService::class.java)
        runCatching {
            manager.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName)
            val controllers = manager.getActiveSessions(componentName)
            syncMediaSessions(controllers)
        }
    }

    private fun refreshMediaSessions() {
        val manager = mediaSessionManager ?: return
        val componentName = ComponentName(this, DashboardNotificationListenerService::class.java)
        runCatching {
            val controllers = manager.getActiveSessions(componentName)
            syncMediaSessions(controllers)
        }
    }

    private fun syncMediaSessions(controllers: List<MediaController>) {
        val incomingTokens = controllers.map { it.sessionToken }.toSet()

        // Unregister callbacks for destroyed/gone sessions
        val toRemove = trackedSessions.keys.filter { it !in incomingTokens }
        toRemove.forEach { token ->
            trackedSessions.remove(token)?.let { (ctrl, cb) ->
                runCatching { ctrl.unregisterCallback(cb) }
            }
        }

        // Register callbacks for all active sessions
        controllers.forEach { controller ->
            val token = controller.sessionToken
            if (token !in trackedSessions) {
                val cb = createSessionCallback(controller)
                trackedSessions[token] = controller to cb
                runCatching { controller.registerCallback(cb) }
            }
        }

        val best = pickBestController(controllers)
        selectedToken = best?.sessionToken
        appContainer.mediaControlRepository.updateController(
            controller = best,
            listenerConnected = true,
        )
        updateProgressTicker()
        scheduleDebouncedWidgetRefresh()
    }

    private fun createSessionCallback(controller: MediaController): MediaController.Callback {
        return object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                val token = controller.sessionToken
                val becamePlaying = state?.state == PlaybackState.STATE_PLAYING
                if (becamePlaying || selectedToken == null || selectedToken == token) {
                    selectedToken = token
                    appContainer.mediaControlRepository.updateController(
                        controller = controller,
                        listenerConnected = true,
                        playbackStateOverride = state,
                    )
                    updateProgressTicker()
                    scheduleDebouncedWidgetRefresh()
                }
            }

            override fun onMetadataChanged(metadata: MediaMetadata?) {
                val token = controller.sessionToken
                if (selectedToken == null || selectedToken == token ||
                    controller.playbackState?.state == PlaybackState.STATE_PLAYING
                ) {
                    selectedToken = token
                    appContainer.mediaControlRepository.updateController(
                        controller = controller,
                        listenerConnected = true,
                        metadataOverride = metadata,
                    )
                    updateProgressTicker()
                    scheduleDebouncedWidgetRefresh()
                }
            }

            override fun onSessionDestroyed() {
                refreshMediaSessions()
            }
        }
    }

    private fun pickBestController(controllers: List<MediaController>): MediaController? {
        return controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull { it.sessionToken == selectedToken }
            ?: controllers.firstOrNull { it.metadata != null }
            ?: controllers.firstOrNull()
    }

    private fun updateProgressTicker() {
        val current = appContainer.mediaControlRepository.state.value
        val shouldTick = current.isPlaying && current.durationMs > 0L
        if (!shouldTick) {
            progressTickerJob?.cancel()
            progressTickerJob = null
            return
        }
        if (progressTickerJob?.isActive == true) return
        progressTickerJob = serviceScope.launch {
            while (isActive) {
                delay(PROGRESS_TICK_INTERVAL_MS)
                val state = appContainer.mediaControlRepository.state.value
                if (!state.isPlaying || state.durationMs <= 0L) break
                val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (pm == null || pm.isInteractive) {
                    appContainer.notifyWidgetDataChanged()
                }
            }
        }
    }

    private fun stopMediaSessionTracking() {
        progressTickerJob?.cancel()
        progressTickerJob = null
        trackedSessions.values.forEach { (ctrl, cb) ->
            runCatching { ctrl.unregisterCallback(cb) }
        }
        trackedSessions.clear()
        selectedToken = null
        runCatching { mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener) }
        mediaSessionManager = null
    }

    private fun scheduleDebouncedWidgetRefresh() {
        widgetRefreshTriggers.tryEmit(Unit)
    }

    companion object {
        private const val WIDGET_REFRESH_DEBOUNCE_MS = 450L
        private const val PROGRESS_TICK_INTERVAL_MS = 15_000L
    }
}
