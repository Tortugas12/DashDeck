package com.dashdeck.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.dashdeck.app.DashDeckApplication
import com.dashdeck.app.data.repo.NotesRepository
import com.dashdeck.app.ui.MainActivity
import com.dashdeck.app.ui.config.WidgetConfigActivity

object WidgetActionKeys {
    val EXPANDED_FOLDER_ID_PREF = longPreferencesKey("expanded_folder_id")
    val LAST_REFRESH_NONCE_PREF = longPreferencesKey("last_refresh_nonce")

    val PARAM_MEDIA_CMD = ActionParameters.Key<String>("media_cmd")
    val PARAM_PACKAGE_NAME = ActionParameters.Key<String>("package_name")
    val PARAM_ACTIVITY_NAME = ActionParameters.Key<String>("activity_name")
    val PARAM_FOLDER_ID = ActionParameters.Key<Long>("folder_id")
    val PARAM_APP_WIDGET_ID = ActionParameters.Key<Int>("app_widget_id")

    const val CMD_PLAY_PAUSE = "play_pause"
    const val CMD_NEXT = "next"
    const val CMD_PREVIOUS = "previous"
}

class MediaControlActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val container = (context.applicationContext as? DashDeckApplication)?.container ?: return
        when (parameters[WidgetActionKeys.PARAM_MEDIA_CMD]) {
            WidgetActionKeys.CMD_PLAY_PAUSE -> container.mediaControlRepository.togglePlayPause()
            WidgetActionKeys.CMD_NEXT -> container.mediaControlRepository.skipNext()
            WidgetActionKeys.CMD_PREVIOUS -> container.mediaControlRepository.skipPrevious()
        }
        kotlinx.coroutines.delay(350L)
        container.mediaControlRepository.refreshActiveController()
        val appWidgetId = runCatching {
            androidx.glance.appwidget.GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            container.refreshWidgetNow(appWidgetId)
        } else {
            DashboardGlanceWidget().update(context, glanceId)
        }
    }
}

class LaunchAppActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val container = (context.applicationContext as? DashDeckApplication)?.container ?: return
        val pkg = parameters[WidgetActionKeys.PARAM_PACKAGE_NAME] ?: return
        val act = parameters[WidgetActionKeys.PARAM_ACTIVITY_NAME]
        container.appLauncherRepository.launchApp(pkg, act)
    }
}

class ToggleInlineFolderActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val folderId = parameters[WidgetActionKeys.PARAM_FOLDER_ID] ?: return
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[WidgetActionKeys.EXPANDED_FOLDER_ID_PREF] ?: -1L
            prefs[WidgetActionKeys.EXPANDED_FOLDER_ID_PREF] = if (current == folderId) -1L else folderId
        }
        DashboardGlanceWidget().update(context, glanceId)
    }
}

class OpenClockAppActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launched = runCatching {
            context.startActivity(alarmIntent)
            true
        }.getOrDefault(false)

        if (!launched) {
            val fallbacks = listOf("com.google.android.deskclock", "com.android.deskclock")
            for (pkg in fallbacks) {
                val launch = context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                } ?: continue
                if (runCatching { context.startActivity(launch); true }.getOrDefault(false)) {
                    break
                }
            }
        }
    }
}

class OpenCalendarAppActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val builder = CalendarContract.CONTENT_URI.buildUpon()
            .appendPath("time")
            .appendPath(System.currentTimeMillis().toString())
        val intent = Intent(Intent.ACTION_VIEW, builder.build()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}

class OpenKeepOrNoteAppActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val pkg = parameters[WidgetActionKeys.PARAM_PACKAGE_NAME]
            ?.takeIf { it.isNotBlank() }
            ?: NotesRepository.GOOGLE_KEEP_PACKAGE
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: context.packageManager.getLaunchIntentForPackage(NotesRepository.GOOGLE_KEEP_PACKAGE)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: return
        runCatching { context.startActivity(intent) }
    }
}

class OpenAppSettingsActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}

class OpenNotificationAccessSettingsActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}

class OpenWidgetSettingsActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val appWidgetId = parameters[WidgetActionKeys.PARAM_APP_WIDGET_ID]
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val intent = Intent(context, WidgetConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}

class OpenMainAppActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
