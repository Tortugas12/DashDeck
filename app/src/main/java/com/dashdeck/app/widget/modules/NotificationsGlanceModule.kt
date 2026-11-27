package com.dashdeck.app.widget.modules

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.dashdeck.app.data.db.ModuleSizeTier
import com.dashdeck.app.data.repo.NotificationFilterEngine
import com.dashdeck.app.data.repo.NotificationSummaryState
import com.dashdeck.app.widget.LaunchAppActionCallback
import com.dashdeck.app.widget.OpenNotificationAccessSettingsActionCallback
import com.dashdeck.app.widget.WidgetActionKeys

@Composable
@GlanceComposable
fun NotificationsGlanceModule(
    state: NotificationSummaryState,
    cornerRadiusDp: Int,
    sizeTier: ModuleSizeTier = ModuleSizeTier.STANDARD,
) {
    val maxGroups = when (sizeTier) {
        ModuleSizeTier.COMPACT -> 2
        ModuleSizeTier.STANDARD -> 4
        ModuleSizeTier.LARGE -> 6
    }
    val lineMax = if (sizeTier == ModuleSizeTier.LARGE && !state.privacyModeEnabled) 2 else 1

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(cornerRadiusDp.dp)
            .background(GlanceTheme.colors.surface)
            .padding(if (sizeTier == ModuleSizeTier.COMPACT) 10.dp else 12.dp),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = "Notifications",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
            )
            if (state.totalCount > 0) {
                Text(
                    text = "${state.totalCount}",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        if (!state.hasListenerAccess) {
            Text(
                text = "Tap to enable Notification Access",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionRunCallback<OpenNotificationAccessSettingsActionCallback>())
                    .padding(vertical = 2.dp),
                maxLines = 2,
            )
            return@Column
        }

        if (state.groups.isEmpty()) {
            Text(
                text = if (state.privacyModeEnabled) {
                    "All caught up · Privacy Mode active"
                } else {
                    "All caught up"
                },
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
                maxLines = 1,
            )
            return@Column
        }

        state.groups.take(maxGroups).forEachIndexed { idx, group ->
            if (idx > 0) Spacer(modifier = GlanceModifier.height(4.dp))
            val latest = group.items.firstOrNull()
            val detail = when {
                state.privacyModeEnabled -> NotificationFilterEngine.PRIVACY_REDACTED_TEXT
                latest != null && latest.previewText.isNotBlank() -> latest.previewText
                latest != null -> latest.title
                else -> ""
            }
            val header = if (group.count > 1) "${group.appLabel} (${group.count})" else group.appLabel
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .cornerRadius(8.dp)
                    .clickable(
                        actionRunCallback<LaunchAppActionCallback>(
                            actionParametersOf(WidgetActionKeys.PARAM_PACKAGE_NAME to group.packageName),
                        ),
                    )
                    .padding(vertical = 2.dp),
            ) {
                Text(
                    text = if (detail.isNotBlank()) "$header · $detail" else header,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 11.sp,
                    ),
                    maxLines = lineMax,
                )
            }
        }
    }
}
