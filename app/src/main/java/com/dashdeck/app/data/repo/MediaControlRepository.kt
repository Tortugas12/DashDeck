package com.dashdeck.app.data.repo

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class MediaPlaybackState(
    val hasListenerAccess: Boolean = false,
    val isActive: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val packageName: String? = null,
    val isPlaying: Boolean = false,
    val albumArt: Bitmap? = null,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val positionUpdateElapsedMs: Long = 0L,
) {
    fun currentPositionMs(nowElapsedMs: Long): Long {
        val basePos = positionMs.coerceAtLeast(0L)
        if (!isPlaying) return if (durationMs > 0L) basePos.coerceAtMost(durationMs) else basePos
        val deltaMs = (nowElapsedMs - positionUpdateElapsedMs).coerceAtLeast(0L)
        val effectiveSpeed = playbackSpeed.takeIf { it > 0f } ?: 1f
        val interpolated = basePos + (deltaMs * effectiveSpeed).toLong()
        val upperBound = durationMs.coerceAtLeast(basePos)
        return interpolated.coerceIn(0L, upperBound)
    }

    fun progressFraction(nowElapsedMs: Long): Float {
        if (durationMs <= 0L) return 0f
        val pos = currentPositionMs(nowElapsedMs)
        return (pos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }
}

fun formatDurationMmSs(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L)) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

class MediaControlRepository(private val context: Context) {

    private val _state = MutableStateFlow(
        MediaPlaybackState(
            hasListenerAccess = NotificationSummaryRepository.isNotificationListenerEnabled(context),
        ),
    )
    val state: StateFlow<MediaPlaybackState> = _state.asStateFlow()

    @Volatile
    private var activeController: MediaController? = null

    fun refreshAccessStatus() {
        val hasAccess = NotificationSummaryRepository.isNotificationListenerEnabled(context)
        _state.value = _state.value.copy(hasListenerAccess = hasAccess)
    }

    fun updateController(
        controller: MediaController?,
        listenerConnected: Boolean,
        metadataOverride: MediaMetadata? = null,
        playbackStateOverride: PlaybackState? = null,
    ) {
        activeController = controller
        val hasAccess = listenerConnected || NotificationSummaryRepository.isNotificationListenerEnabled(context)
        if (controller == null) {
            _state.value = MediaPlaybackState(
                hasListenerAccess = hasAccess,
                isActive = false,
            )
            return
        }

        val metadata = metadataOverride ?: controller.metadata
        val playbackState = playbackStateOverride ?: controller.playbackState
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val rawArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val scaledArt = rawArt?.let { BitmapScaler.downscaleBitmap(it, MAX_ALBUM_ART_PX) }
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L
        val rawPos = playbackState?.position?.takeIf { it >= 0L } ?: 0L
        val speed = playbackState?.playbackSpeed?.takeIf { it > 0f } ?: 1f
        val updateTime = playbackState?.lastPositionUpdateTime?.takeIf { it > 0L }
            ?: SystemClock.elapsedRealtime()

        _state.value = MediaPlaybackState(
            hasListenerAccess = hasAccess,
            isActive = title.isNotBlank() || artist.isNotBlank() || isPlaying,
            title = title,
            artist = artist,
            album = album,
            packageName = controller.packageName,
            isPlaying = isPlaying,
            albumArt = scaledArt,
            durationMs = durationMs,
            positionMs = rawPos,
            playbackSpeed = speed,
            positionUpdateElapsedMs = updateTime,
        )
    }

    fun refreshActiveController() {
        val controller = activeController ?: return
        updateController(
            controller = controller,
            listenerConnected = true,
            metadataOverride = controller.metadata,
            playbackStateOverride = controller.playbackState,
        )
    }

    fun togglePlayPause(): Boolean {
        val controller = activeController ?: return false
        val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
        if (isPlaying) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
        return true
    }

    fun skipNext(): Boolean {
        val controller = activeController ?: return false
        controller.transportControls.skipToNext()
        return true
    }

    fun skipPrevious(): Boolean {
        val controller = activeController ?: return false
        controller.transportControls.skipToPrevious()
        return true
    }

    companion object {
        const val MAX_ALBUM_ART_PX = 160
    }
}
