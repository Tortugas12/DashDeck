package com.dashdeck.app.data.repo

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.dashdeck.app.data.prefs.AppPreferencesRepository
import com.dashdeck.app.service.DashboardNotificationListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class RawNotificationItem(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postTimeMs: Long,
    val isOngoing: Boolean = false,
)

data class NotificationItem(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val previewText: String,
    val postTimeMs: Long,
)

data class NotificationAppGroup(
    val packageName: String,
    val appLabel: String,
    val count: Int,
    val latestTimeMs: Long,
    val items: List<NotificationItem>,
)

data class NotificationSummaryState(
    val hasListenerAccess: Boolean = false,
    val totalCount: Int = 0,
    val groups: List<NotificationAppGroup> = emptyList(),
    val privacyModeEnabled: Boolean = false,
)

object NotificationFilterEngine {
    const val PRIVACY_REDACTED_TEXT = "Content hidden (Privacy Mode)"

    fun buildSummary(
        rawNotifications: List<RawNotificationItem>,
        hasListenerAccess: Boolean,
        privacyModeEnabled: Boolean,
        useAllowList: Boolean,
        allowedPackages: Set<String>,
        blockedPackages: Set<String>,
        selfPackageName: String = "com.dashdeck.app",
    ): NotificationSummaryState {
        val filtered = rawNotifications
            .asSequence()
            .filter { !it.isOngoing }
            .filter { it.packageName != selfPackageName }
            .filter { item ->
                if (useAllowList) {
                    item.packageName in allowedPackages
                } else {
                    item.packageName !in blockedPackages
                }
            }
            .sortedByDescending { it.postTimeMs }
            .toList()

        val groups = filtered
            .groupBy { it.packageName }
            .map { (pkg, pkgItems) ->
                val sorted = pkgItems.sortedByDescending { it.postTimeMs }
                NotificationAppGroup(
                    packageName = pkg,
                    appLabel = sorted.first().appLabel.ifBlank { pkg },
                    count = sorted.size,
                    latestTimeMs = sorted.first().postTimeMs,
                    items = sorted.map { item ->
                        NotificationItem(
                            key = item.key,
                            packageName = item.packageName,
                            appLabel = item.appLabel.ifBlank { item.packageName },
                            title = if (privacyModeEnabled) item.appLabel.ifBlank { item.packageName } else item.title,
                            previewText = if (privacyModeEnabled) PRIVACY_REDACTED_TEXT else item.text,
                            postTimeMs = item.postTimeMs,
                        )
                    },
                )
            }
            .sortedByDescending { it.latestTimeMs }

        return NotificationSummaryState(
            hasListenerAccess = hasListenerAccess,
            totalCount = filtered.size,
            groups = groups,
            privacyModeEnabled = privacyModeEnabled,
        )
    }
}

class NotificationSummaryRepository(
    private val context: Context,
    preferencesRepository: AppPreferencesRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val rawNotifications = MutableStateFlow<List<RawNotificationItem>>(emptyList())
    private val listenerAccessFlow = MutableStateFlow(isNotificationListenerEnabled(context))

    val state: StateFlow<NotificationSummaryState> = combine(
        rawNotifications,
        listenerAccessFlow,
        preferencesRepository.preferencesFlow,
    ) { raw, hasAccess, prefs ->
        NotificationFilterEngine.buildSummary(
            rawNotifications = raw,
            hasListenerAccess = hasAccess,
            privacyModeEnabled = prefs.privacyHideNotificationText,
            useAllowList = prefs.useAllowListForNotifications,
            allowedPackages = prefs.allowedNotificationPackages,
            blockedPackages = prefs.blockedNotificationPackages,
            selfPackageName = context.packageName,
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = NotificationSummaryState(
            hasListenerAccess = isNotificationListenerEnabled(context),
        ),
    )

    fun refreshAccessStatus() {
        listenerAccessFlow.value = isNotificationListenerEnabled(context)
    }

    fun updateActiveNotifications(items: List<RawNotificationItem>, listenerConnected: Boolean) {
        listenerAccessFlow.value = listenerConnected || isNotificationListenerEnabled(context)
        rawNotifications.value = items
    }

    companion object {
        fun isNotificationListenerEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ).orEmpty()
            if (flat.isBlank()) return false
            val target = ComponentName(context, DashboardNotificationListenerService::class.java)
            return flat.split(":").any { entry ->
                val cn = ComponentName.unflattenFromString(entry)
                cn != null && cn.packageName == target.packageName && cn.className == target.className
            }
        }
    }
}
