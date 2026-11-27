package com.dashdeck.app.widget.modules

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.dashdeck.app.data.db.ModuleSizeTier
import com.dashdeck.app.data.repo.MediaPlaybackState
import com.dashdeck.app.data.repo.formatDurationMmSs
import com.dashdeck.app.widget.LaunchAppActionCallback
import com.dashdeck.app.widget.MediaControlActionCallback
import com.dashdeck.app.widget.OpenNotificationAccessSettingsActionCallback
import com.dashdeck.app.widget.WidgetActionKeys

@Composable
@GlanceComposable
fun MediaGlanceModule(
    state: MediaPlaybackState,
    cornerRadiusDp: Int,
    sizeTier: ModuleSizeTier = ModuleSizeTier.STANDARD,
    slotWidthDp: Int = 320,
    nowElapsedMs: Long = SystemClock.elapsedRealtime(),
    targetHeightDp: Int? = null,
) {
    val rootModifier = GlanceModifier
        .fillMaxWidth()
        .let { m -> if (targetHeightDp != null) m.height(targetHeightDp.dp) else m }
        .cornerRadius(cornerRadiusDp.dp)
        .background(GlanceTheme.colors.surface)
        .padding(if (sizeTier == ModuleSizeTier.COMPACT) 10.dp else 12.dp)

    Column(modifier = rootModifier) {
        Text(
            text = "Now Playing",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )

        Spacer(modifier = GlanceModifier.height(6.dp))

        if (!state.hasListenerAccess) {
            Text(
                text = "Tap to enable Notification Access for Media Controls",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                ),
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionRunCallback<OpenNotificationAccessSettingsActionCallback>())
                    .padding(vertical = 2.dp),
                maxLines = 2,
            )
            return@Column
        }

        if (!state.isActive) {
            Text(
                text = "No media playing right now",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
                maxLines = 1,
            )
            return@Column
        }

        val mediaPkg = state.packageName?.takeIf { it.isNotBlank() }
        val baseMetaModifier = if (mediaPkg != null) {
            GlanceModifier
                .cornerRadius(8.dp)
                .clickable(
                    actionRunCallback<LaunchAppActionCallback>(
                        actionParametersOf(WidgetActionKeys.PARAM_PACKAGE_NAME to mediaPkg),
                    ),
                )
        } else {
            GlanceModifier
        }

        if (sizeTier == ModuleSizeTier.LARGE) {
            Row(
                modifier = baseMetaModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val art = state.albumArt
                if (art != null) {
                    Image(
                        provider = ImageProvider(art),
                        contentDescription = "Album art",
                        modifier = GlanceModifier
                            .size(56.dp)
                            .cornerRadius(12.dp),
                    )
                    Spacer(modifier = GlanceModifier.width(12.dp))
                }

                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = state.title.ifBlank { "Playing Media" },
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                    if (state.artist.isNotBlank()) {
                        Text(
                            text = state.artist,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp,
                            ),
                            maxLines = 1,
                        )
                    }
                    if (state.album.isNotBlank()) {
                        Text(
                            text = state.album,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 11.sp,
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }

            if (state.durationMs > 0L) {
                Spacer(modifier = GlanceModifier.height(8.dp))
                MediaProgressSection(
                    state = state,
                    nowElapsedMs = nowElapsedMs,
                    showTimestamps = slotWidthDp >= 220,
                )
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Dedicated non-clickable buffer container around transport controls
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MediaControlChip(label = "⏮", command = WidgetActionKeys.CMD_PREVIOUS)
                Spacer(modifier = GlanceModifier.width(8.dp))
                MediaControlChip(
                    label = if (state.isPlaying) "⏸" else "▶",
                    command = WidgetActionKeys.CMD_PLAY_PAUSE,
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                MediaControlChip(label = "⏭", command = WidgetActionKeys.CMD_NEXT)
            }
        } else {
            val artSize = if (sizeTier == ModuleSizeTier.COMPACT) 36.dp else 44.dp
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Only album art + track metadata block is clickable
                Row(
                    modifier = baseMetaModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val art = state.albumArt
                    if (art != null) {
                        Image(
                            provider = ImageProvider(art),
                            contentDescription = "Album art",
                            modifier = GlanceModifier
                                .size(artSize)
                                .cornerRadius(10.dp),
                        )
                        Spacer(modifier = GlanceModifier.width(10.dp))
                    }

                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = state.title.ifBlank { "Playing Media" },
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                        )
                        if (sizeTier != ModuleSizeTier.COMPACT && state.artist.isNotBlank()) {
                            Text(
                                text = state.artist,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 11.sp,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }

                // Clear separation + non-clickable buffer container around playback controls
                Spacer(modifier = GlanceModifier.width(12.dp))

                Row(
                    modifier = GlanceModifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MediaControlChip(label = "⏮", command = WidgetActionKeys.CMD_PREVIOUS)
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    MediaControlChip(
                        label = if (state.isPlaying) "⏸" else "▶",
                        command = WidgetActionKeys.CMD_PLAY_PAUSE,
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    MediaControlChip(label = "⏭", command = WidgetActionKeys.CMD_NEXT)
                }
            }

            if (state.durationMs > 0L) {
                Spacer(modifier = GlanceModifier.height(6.dp))
                MediaProgressSection(
                    state = state,
                    nowElapsedMs = nowElapsedMs,
                    showTimestamps = sizeTier != ModuleSizeTier.COMPACT && slotWidthDp >= 220,
                )
            }
        }
    }
}

@Composable
@GlanceComposable
private fun MediaProgressSection(
    state: MediaPlaybackState,
    nowElapsedMs: Long,
    showTimestamps: Boolean,
) {
    val progress = state.progressFraction(nowElapsedMs)
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = progress,
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(4.dp),
            color = GlanceTheme.colors.primary,
            backgroundColor = GlanceTheme.colors.surfaceVariant,
        )
        if (showTimestamps) {
            Spacer(modifier = GlanceModifier.height(3.dp))
            val elapsed = formatDurationMmSs(state.currentPositionMs(nowElapsedMs))
            val total = formatDurationMmSs(state.durationMs)
            Text(
                text = "$elapsed / $total",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
@GlanceComposable
private fun MediaControlChip(
    label: String,
    command: String,
) {
    Text(
        text = label,
        style = TextStyle(
            color = GlanceTheme.colors.onPrimaryContainer,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        ),
        modifier = GlanceModifier
            .cornerRadius(8.dp)
            .background(GlanceTheme.colors.primaryContainer)
            .clickable(
                actionRunCallback<MediaControlActionCallback>(
                    actionParametersOf(WidgetActionKeys.PARAM_MEDIA_CMD to command),
                ),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
