package com.dashdeck.app.widget.modules

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.dashdeck.app.data.db.FolderStyle
import com.dashdeck.app.data.db.LauncherItemEntity
import com.dashdeck.app.data.db.ModuleSizeTier
import com.dashdeck.app.data.repo.LauncherBoardState
import com.dashdeck.app.data.repo.LauncherFolderWithItems
import com.dashdeck.app.ui.popup.FolderPopupActivity
import com.dashdeck.app.widget.LaunchAppActionCallback
import com.dashdeck.app.widget.ToggleInlineFolderActionCallback
import com.dashdeck.app.widget.WidgetActionKeys

@Composable
@GlanceComposable
fun LauncherGlanceModule(
    appWidgetId: Int,
    boardState: LauncherBoardState,
    folderStyle: FolderStyle,
    expandedFolderId: Long,
    iconBitmaps: Map<String, Bitmap>,
    cornerRadiusDp: Int,
    sizeTier: ModuleSizeTier = ModuleSizeTier.STANDARD,
) {
    val maxTiles = when (sizeTier) {
        ModuleSizeTier.COMPACT -> 4
        ModuleSizeTier.STANDARD -> 12
        ModuleSizeTier.LARGE -> 20
    }

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(cornerRadiusDp.dp)
            .background(GlanceTheme.colors.surface)
            .padding(if (sizeTier == ModuleSizeTier.COMPACT) 10.dp else 12.dp),
    ) {
        Text(
            text = "Apps",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )

        Spacer(modifier = GlanceModifier.height(6.dp))

        if (boardState.folders.isEmpty() && boardState.topLevelItems.isEmpty()) {
            Text(
                text = "Pin apps or create folders in DashDeck",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                ),
                maxLines = 2,
            )
            return@Column
        }

        val tiles: List<LauncherTileModel> = buildList {
            boardState.folders.forEach { add(LauncherTileModel.Folder(it)) }
            boardState.topLevelItems.forEach { add(LauncherTileModel.App(it)) }
        }.take(maxTiles)

        tiles.chunked(4).forEachIndexed { rowIdx, rowTiles ->
            if (rowIdx > 0) Spacer(modifier = GlanceModifier.height(6.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowTiles.forEach { tile ->
                    when (tile) {
                        is LauncherTileModel.Folder -> {
                            val f = tile.folderWithItems.folder
                            val isExpanded = folderStyle == FolderStyle.INLINE && expandedFolderId == f.folderId
                            val clickAction = if (folderStyle == FolderStyle.INLINE) {
                                actionRunCallback<ToggleInlineFolderActionCallback>(
                                    actionParametersOf(WidgetActionKeys.PARAM_FOLDER_ID to f.folderId),
                                )
                            } else {
                                val context = LocalContext.current
                                val popupIntent = Intent(context, FolderPopupActivity::class.java).apply {
                                    putExtra(FolderPopupActivity.EXTRA_FOLDER_ID, f.folderId)
                                    putExtra(FolderPopupActivity.EXTRA_APP_WIDGET_ID, appWidgetId)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    data = "dashdeck://folder/$appWidgetId/${f.folderId}".toUri()
                                }
                                actionStartActivity(popupIntent)
                            }
                            Column(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .cornerRadius(10.dp)
                                    .background(
                                        if (isExpanded) GlanceTheme.colors.primaryContainer
                                        else GlanceTheme.colors.surfaceVariant,
                                    )
                                    .clickable(clickAction)
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = if (isExpanded) "📂" else "📁",
                                    style = TextStyle(fontSize = 16.sp),
                                )
                                Text(
                                    text = f.name,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    maxLines = 1,
                                )
                            }
                        }

                        is LauncherTileModel.App -> {
                            val app = tile.item
                            val iconKey = "${app.packageName}/${app.activityName}"
                            val bmp = iconBitmaps[iconKey] ?: iconBitmaps[app.packageName]
                            WidgetAppTile(
                                item = app,
                                bitmap = bmp,
                                modifier = GlanceModifier.defaultWeight(),
                            )
                        }
                    }
                }
                repeat(4 - rowTiles.size) {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                }
            }
        }

        // Inline expanded folder tray
        if (folderStyle == FolderStyle.INLINE && expandedFolderId != -1L) {
            val activeFolder = boardState.folders.firstOrNull { it.folder.folderId == expandedFolderId }
            if (activeFolder != null) {
                Spacer(modifier = GlanceModifier.height(8.dp))
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .cornerRadius(10.dp)
                        .background(GlanceTheme.colors.surfaceVariant)
                        .padding(8.dp),
                ) {
                    Text(
                        text = activeFolder.folder.name,
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    if (activeFolder.items.isEmpty()) {
                        Text(
                            text = "No apps in folder",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 10.sp,
                            ),
                        )
                    } else {
                        activeFolder.items.take(8).chunked(4).forEach { chunk ->
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                chunk.forEach { app ->
                                    val iconKey = "${app.packageName}/${app.activityName}"
                                    val bmp = iconBitmaps[iconKey] ?: iconBitmaps[app.packageName]
                                    WidgetAppTile(
                                        item = app,
                                        bitmap = bmp,
                                        modifier = GlanceModifier.defaultWeight(),
                                    )
                                }
                                repeat(4 - chunk.size) {
                                    Spacer(modifier = GlanceModifier.defaultWeight())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@GlanceComposable
private fun WidgetAppTile(
    item: LauncherItemEntity,
    bitmap: Bitmap?,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(
        modifier = modifier
            .cornerRadius(10.dp)
            .clickable(
                actionRunCallback<LaunchAppActionCallback>(
                    actionParametersOf(
                        WidgetActionKeys.PARAM_PACKAGE_NAME to item.packageName,
                        WidgetActionKeys.PARAM_ACTIVITY_NAME to item.activityName,
                    ),
                ),
            )
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = item.label,
                modifier = GlanceModifier
                    .size(28.dp)
                    .cornerRadius(8.dp),
            )
        } else {
            Text(
                text = item.label.take(1).uppercase(),
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Text(
            text = item.label,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 10.sp,
            ),
            maxLines = 1,
        )
    }
}

private sealed interface LauncherTileModel {
    data class Folder(val folderWithItems: LauncherFolderWithItems) : LauncherTileModel
    data class App(val item: LauncherItemEntity) : LauncherTileModel
}
