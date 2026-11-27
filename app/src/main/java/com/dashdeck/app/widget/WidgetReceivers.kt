package com.dashdeck.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.dashdeck.app.DashDeckApplication
import com.dashdeck.app.data.db.WidgetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

abstract class BaseDashDeckWidgetReceiver(
    private val defaultWidgetType: WidgetType,
) : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget
        get() = DashboardGlanceWidget(defaultWidgetType)

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val appContainer = (context.applicationContext as? DashDeckApplication)?.container ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                appWidgetIds.forEach { id ->
                    appContainer.widgetConfigRepository.delete(id)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class DashboardWidgetReceiver : BaseDashDeckWidgetReceiver(WidgetType.DASHBOARD)

class ModularWidgetReceiver : BaseDashDeckWidgetReceiver(WidgetType.MODULAR)
