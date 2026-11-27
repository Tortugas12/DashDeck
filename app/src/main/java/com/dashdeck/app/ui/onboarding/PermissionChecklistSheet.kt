package com.dashdeck.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PermissionChecklistDialog(
    hasCalendarPermission: Boolean,
    hasNotificationAccess: Boolean,
    hasLocationPermission: Boolean = false,
    onRequestCalendar: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onRequestLocation: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = "Permissions Setup",
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Grant only the modules you want to use. Weather also works with a manual city without location access:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                PermissionChecklistRow(
                    icon = Icons.Outlined.CalendarMonth,
                    title = "Calendar Access",
                    description = "Shows event dots on the month grid and your daily agenda.",
                    isGranted = hasCalendarPermission,
                    actionLabel = "Grant",
                    onAction = onRequestCalendar,
                )

                PermissionChecklistRow(
                    icon = Icons.Outlined.NotificationsActive,
                    title = "Notification & Media Access",
                    description = "Powers both Notification Summary and Currently Playing Media Controls.",
                    isGranted = hasNotificationAccess,
                    actionLabel = "Open Settings",
                    onAction = onOpenNotificationSettings,
                )

                PermissionChecklistRow(
                    icon = Icons.Outlined.Shield,
                    title = "Coarse Location (Optional)",
                    description = "Used only if you enable automatic local Weather on the Clock card.",
                    isGranted = hasLocationPermission,
                    actionLabel = "Grant",
                    onAction = onRequestLocation,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun PermissionChecklistRow(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isGranted) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                FilledTonalButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun PermissionStatusBanner(
    hasCalendarPermission: Boolean,
    hasNotificationAccess: Boolean,
    onOpenChecklist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (hasCalendarPermission && hasNotificationAccess) return

    val missing = buildList {
        if (!hasCalendarPermission) add("Calendar")
        if (!hasNotificationAccess) add("Notifications & Media")
    }.joinToString(" and ")

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Finish local setup",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Enable $missing to unlock live dashboard cards.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
            }
            Button(onClick = onOpenChecklist) {
                Text("Set Up")
            }
        }
    }
}
