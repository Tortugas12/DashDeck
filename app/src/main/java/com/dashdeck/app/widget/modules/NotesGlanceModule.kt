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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.dashdeck.app.data.db.ModuleSizeTier
import com.dashdeck.app.data.repo.NoteModel
import com.dashdeck.app.data.repo.NotesRepository
import com.dashdeck.app.widget.OpenKeepOrNoteAppActionCallback
import com.dashdeck.app.widget.WidgetActionKeys

@Composable
@GlanceComposable
fun NotesGlanceModule(
    notes: List<NoteModel>,
    cornerRadiusDp: Int,
    sizeTier: ModuleSizeTier = ModuleSizeTier.STANDARD,
) {
    val maxNotes = when (sizeTier) {
        ModuleSizeTier.COMPACT -> 1
        ModuleSizeTier.STANDARD -> 2
        ModuleSizeTier.LARGE -> 4
    }
    val maxChecklistItems = when (sizeTier) {
        ModuleSizeTier.COMPACT -> 1
        ModuleSizeTier.STANDARD -> 3
        ModuleSizeTier.LARGE -> 5
    }
    val maxBodyLines = when (sizeTier) {
        ModuleSizeTier.COMPACT -> 1
        ModuleSizeTier.STANDARD -> 2
        ModuleSizeTier.LARGE -> 3
    }

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(cornerRadiusDp.dp)
            .background(GlanceTheme.colors.surface)
            .padding(if (sizeTier == ModuleSizeTier.COMPACT) 10.dp else 12.dp),
    ) {
        Text(
            text = "Pinned Notes",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionRunCallback<OpenKeepOrNoteAppActionCallback>()),
            maxLines = 1,
        )

        Spacer(modifier = GlanceModifier.height(6.dp))

        if (notes.isEmpty()) {
            Text(
                text = "Share a note from Google Keep to pin it here",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                ),
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionRunCallback<OpenKeepOrNoteAppActionCallback>())
                    .padding(vertical = 2.dp),
                maxLines = 2,
            )
            return@Column
        }

        notes.take(maxNotes).forEachIndexed { index, note ->
            if (index > 0) Spacer(modifier = GlanceModifier.height(6.dp))
            val targetPkg = note.sourcePackage ?: NotesRepository.GOOGLE_KEEP_PACKAGE
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .cornerRadius(8.dp)
                    .clickable(
                        actionRunCallback<OpenKeepOrNoteAppActionCallback>(
                            actionParametersOf(WidgetActionKeys.PARAM_PACKAGE_NAME to targetPkg),
                        ),
                    )
                    .padding(vertical = 2.dp),
            ) {
                Text(
                    text = note.title,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
                if (note.checklist.isNotEmpty()) {
                    note.checklist.take(maxChecklistItems).forEach { item ->
                        val prefix = if (item.isChecked) "☑" else "☐"
                        Text(
                            text = "$prefix ${item.text}",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 11.sp,
                            ),
                            maxLines = 1,
                        )
                    }
                } else if (note.body.isNotBlank()) {
                    Text(
                        text = note.body,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp,
                        ),
                        maxLines = maxBodyLines,
                    )
                }
            }
        }
    }
}
