package com.dashdeck.app.data.repo

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaProgressTest {

    @Test
    fun interpolatesPositionWhenPlayingAndClampsToDuration() {
        val state = MediaPlaybackState(
            isActive = true,
            isPlaying = true,
            durationMs = 180_000L,
            positionMs = 30_000L,
            playbackSpeed = 1.0f,
            positionUpdateElapsedMs = 10_000L,
        )

        assertEquals(45_000L, state.currentPositionMs(nowElapsedMs = 25_000L))
        assertEquals(0.25f, state.progressFraction(nowElapsedMs = 25_000L), 0.001f)
        // Clamps to durationMs if elapsed exceeds track length
        assertEquals(180_000L, state.currentPositionMs(nowElapsedMs = 900_000L))
        assertEquals(1.0f, state.progressFraction(nowElapsedMs = 900_000L), 0.001f)
    }

    @Test
    fun respectsPlaybackSpeedAndDoesNotAdvanceWhenPaused() {
        val fastPlaying = MediaPlaybackState(
            isActive = true,
            isPlaying = true,
            durationMs = 200_000L,
            positionMs = 10_000L,
            playbackSpeed = 1.5f,
            positionUpdateElapsedMs = 1_000L,
        )
        assertEquals(25_000L, fastPlaying.currentPositionMs(nowElapsedMs = 11_000L))

        val paused = fastPlaying.copy(isPlaying = false)
        assertEquals(10_000L, paused.currentPositionMs(nowElapsedMs = 99_000L))
    }

    @Test
    fun zeroDurationReturnsZeroProgressFraction() {
        val liveStream = MediaPlaybackState(
            isActive = true,
            isPlaying = true,
            durationMs = 0L,
            positionMs = 5_000L,
            positionUpdateElapsedMs = 1_000L,
        )
        assertEquals(0f, liveStream.progressFraction(nowElapsedMs = 10_000L), 0.0001f)
    }

    @Test
    fun formatDurationFormatsMinutesSecondsAndHours() {
        assertEquals("0:00", formatDurationMmSs(-500L))
        assertEquals("0:05", formatDurationMmSs(5_000L))
        assertEquals("1:24", formatDurationMmSs(84_000L))
        assertEquals("3:45", formatDurationMmSs(225_000L))
        assertEquals("1:02:09", formatDurationMmSs(3_729_000L))
    }
}
