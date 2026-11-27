package com.dashdeck.app.ui.popup

import android.appwidget.AppWidgetManager
import android.graphics.Rect
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dashdeck.app.DashDeckApplication
import com.dashdeck.app.data.db.LauncherItemEntity
import com.dashdeck.app.data.prefs.AppPreferences
import com.dashdeck.app.data.repo.LauncherBoardState
import com.dashdeck.app.theme.DashDeckTheme
import com.dashdeck.app.theme.ThemeTokens
import com.dashdeck.app.theme.parseHexColor
import com.dashdeck.app.ui.common.desktopHoverHighlight
import com.dashdeck.app.ui.dashboard.AppIconBox
import kotlin.math.roundToInt

/**
 * Pure integer rectangle so [calculateAnchoredPopupOffset] can be unit-tested on JVM
 * without Android framework [Rect] stubbing limitations.
 */
data class AnchorBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2
}

/**
 * Pure, unit-testable anchor calculation positioning the folder popup over [sourceBounds]
 * while clamping inside [containerWidthPx] x [containerHeightPx] with [edgeMarginPx] padding.
 * Returns `null` when [sourceBounds] is unavailable so the UI falls back to centered display.
 */
fun calculateAnchoredPopupOffset(
    sourceBounds: Rect?,
    popupWidthPx: Int,
    popupHeightPx: Int,
    containerWidthPx: Int,
    containerHeightPx: Int,
    edgeMarginPx: Int,
): IntOffset? = calculateAnchoredPopupOffset(
    sourceBounds = sourceBounds?.let { AnchorBounds(it.left, it.top, it.right, it.bottom) },
    popupWidthPx = popupWidthPx,
    popupHeightPx = popupHeightPx,
    containerWidthPx = containerWidthPx,
    containerHeightPx = containerHeightPx,
    edgeMarginPx = edgeMarginPx,
)

fun calculateAnchoredPopupOffset(
    sourceBounds: AnchorBounds?,
    popupWidthPx: Int,
    popupHeightPx: Int,
    containerWidthPx: Int,
    containerHeightPx: Int,
    edgeMarginPx: Int,
): IntOffset? {
    if (sourceBounds == null || containerWidthPx <= 0 || containerHeightPx <= 0) {
        return null
    }

    val safeW = popupWidthPx.coerceAtLeast(0)
    val safeH = popupHeightPx.coerceAtLeast(0)
    val margin = edgeMarginPx.coerceAtLeast(0)

    val desiredX = sourceBounds.centerX - safeW / 2
    val desiredY = sourceBounds.centerY - safeH / 2

    val minX = if (safeW + 2 * margin <= containerWidthPx) {
        margin
    } else {
        ((containerWidthPx - safeW) / 2).coerceAtLeast(0)
    }
    val maxX = (containerWidthPx - safeW - margin).coerceAtLeast(minX)

    val minY = if (safeH + 2 * margin <= containerHeightPx) {
        margin
    } else {
        ((containerHeightPx - safeH) / 2).coerceAtLeast(0)
    }
    val maxY = (containerHeightPx - safeH - margin).coerceAtLeast(minY)

    return IntOffset(
        x = desiredX.coerceIn(minX, maxX),
        y = desiredY.coerceIn(minY, maxY),
    )
}

class FolderPopupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val folderId = intent.getLongExtra(EXTRA_FOLDER_ID, -1L)
        if (folderId == -1L) {
            finish()
            return
        }

        val appWidgetId = intent.getIntExtra(
            EXTRA_APP_WIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        val sourceBounds: Rect? = intent.sourceBounds
        val container = (application as DashDeckApplication).container

        setContent {
            val prefs by container.preferencesRepository.preferencesFlow
                .collectAsStateWithLifecycle(initialValue = AppPreferences())
            val board by container.appLauncherRepository.boardState
                .collectAsStateWithLifecycle(initialValue = LauncherBoardState(emptyList(), emptyList()))
            var widgetTokens by remember { mutableStateOf<ThemeTokens?>(null) }

            LaunchedEffect(appWidgetId) {
                widgetTokens = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    container.widgetConfigRepository.getOrDefault(appWidgetId).themeTokens
                } else {
                    null
                }
            }

            val effectiveTokens = (widgetTokens ?: prefs.globalTheme).validated()
            val folderWithItems = board.folders.firstOrNull { it.folder.folderId == folderId }

            DashDeckTheme(tokens = effectiveTokens) {
                FolderPopupOverlay(
                    folderName = folderWithItems?.folder?.name ?: "Folder",
                    tintHex = folderWithItems?.folder?.iconTintHex ?: "#6366F1",
                    items = folderWithItems?.items.orEmpty(),
                    themeTokens = effectiveTokens,
                    sourceBounds = sourceBounds,
                    iconLoader = { pkg, act -> container.appLauncherRepository.getAppIconBitmap(pkg, act) },
                    onLaunch = { pkg, act ->
                        container.appLauncherRepository.launchApp(pkg, act)
                        finish()
                    },
                    onDismiss = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_FOLDER_ID = "com.dashdeck.app.extra.FOLDER_ID"
        const val EXTRA_APP_WIDGET_ID = "com.dashdeck.app.extra.APP_WIDGET_ID"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderPopupOverlay(
    folderName: String,
    tintHex: String,
    items: List<LauncherItemEntity>,
    themeTokens: ThemeTokens,
    sourceBounds: Rect?,
    iconLoader: (String, String?) -> android.graphics.Bitmap?,
    onLaunch: (String, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val tint = parseHexColor(tintHex, MaterialTheme.colorScheme.primary)
    val dismissInteraction = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val view = LocalView.current
    val edgeMarginPx = with(density) { 16.dp.roundToPx() }
    var popupSize by remember { mutableStateOf(IntSize.Zero) }
    var containerScreenOffset by remember { mutableStateOf(IntOffset.Zero) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                val loc = IntArray(2)
                view.getLocationOnScreen(loc)
                val posInWin = coords.positionInWindow()
                containerScreenOffset = IntOffset(
                    x = loc[0] + posInWin.x.roundToInt(),
                    y = loc[1] + posInWin.y.roundToInt(),
                )
            }
            .onPreviewKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyDown && ev.key == Key.Escape) {
                    onDismiss()
                    true
                } else {
                    false
                }
            }
            .clickable(
                interactionSource = dismissInteraction,
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        val containerWidthPx = with(density) { maxWidth.roundToPx() }
        val containerHeightPx = with(density) { maxHeight.roundToPx() }

        val localSourceBounds = remember(sourceBounds, containerScreenOffset) {
            sourceBounds?.let { b ->
                Rect(
                    b.left - containerScreenOffset.x,
                    b.top - containerScreenOffset.y,
                    b.right - containerScreenOffset.x,
                    b.bottom - containerScreenOffset.y,
                )
            }
        }

        val anchoredOffset = remember(
            localSourceBounds,
            popupSize,
            containerWidthPx,
            containerHeightPx,
            edgeMarginPx,
        ) {
            if (popupSize.width == 0 || popupSize.height == 0) {
                null
            } else {
                calculateAnchoredPopupOffset(
                    sourceBounds = localSourceBounds,
                    popupWidthPx = popupSize.width,
                    popupHeightPx = popupSize.height,
                    containerWidthPx = containerWidthPx,
                    containerHeightPx = containerHeightPx,
                    edgeMarginPx = edgeMarginPx,
                )
            }
        }

        val waitingForMeasurement = sourceBounds != null && popupSize == IntSize.Zero
        val alignmentModifier = if (anchoredOffset != null) {
            Modifier
                .align(Alignment.TopStart)
                .offset { anchoredOffset }
        } else {
            Modifier
                .align(Alignment.Center)
                .padding(16.dp)
        }

        Surface(
            shape = RoundedCornerShape(themeTokens.cornerRadiusDp.dp),
            color = MaterialTheme.colorScheme.surface.copy(
                alpha = themeTokens.backgroundAlpha.coerceAtLeast(0.88f),
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 8.dp,
            modifier = alignmentModifier
                .widthIn(min = 260.dp, max = 400.dp)
                .onSizeChanged { popupSize = it }
                .alpha(if (waitingForMeasurement) 0f else 1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = tint,
                        )
                        Text(
                            text = folderName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close folder")
                    }
                }

                if (items.isEmpty()) {
                    Text(
                        text = "This folder has no apps yet. Open DashDeck to add apps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items.forEach { app ->
                            val bmp = remember(app.packageName, app.activityName) {
                                iconLoader(app.packageName, app.activityName)
                            }
                            Column(
                                modifier = Modifier
                                    .width(76.dp)
                                    .desktopHoverHighlight(cornerRadius = 14.dp)
                                    .clickable { onLaunch(app.packageName, app.activityName) }
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                AppIconBox(bitmap = bmp, label = app.label, sizeDp = 44)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
