package com.dashdeck.app.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationFilterTest {

    private val sampleRaw = listOf(
        RawNotificationItem(
            key = "1",
            packageName = "com.example.chat",
            appLabel = "Chat",
            title = "Alex",
            text = "See you at 3pm",
            postTimeMs = 2000L,
        ),
        RawNotificationItem(
            key = "2",
            packageName = "com.example.chat",
            appLabel = "Chat",
            title = "Sam",
            text = "Draft ready",
            postTimeMs = 3000L,
        ),
        RawNotificationItem(
            key = "3",
            packageName = "com.example.mail",
            appLabel = "Mail",
            title = "Invoice",
            text = "Your statement is available",
            postTimeMs = 2500L,
        ),
        RawNotificationItem(
            key = "4",
            packageName = "com.example.music",
            appLabel = "Music",
            title = "Playing",
            text = "Track 1",
            postTimeMs = 4000L,
            isOngoing = true,
        ),
        RawNotificationItem(
            key = "5",
            packageName = "com.dashdeck.app",
            appLabel = "DashDeck",
            title = "Self",
            text = "Ignored",
            postTimeMs = 5000L,
        ),
    )

    @Test
    fun blockListExcludesBlockedPackagesOngoingAndSelf() {
        val state = NotificationFilterEngine.buildSummary(
            rawNotifications = sampleRaw,
            hasListenerAccess = true,
            privacyModeEnabled = false,
            useAllowList = false,
            allowedPackages = emptySet(),
            blockedPackages = setOf("com.example.mail"),
        )

        assertTrue(state.hasListenerAccess)
        assertEquals(2, state.totalCount)
        assertEquals(1, state.groups.size)
        assertEquals("com.example.chat", state.groups.first().packageName)
        assertEquals(2, state.groups.first().count)
        assertEquals("Sam", state.groups.first().items.first().title)
    }

    @Test
    fun allowListOnlyIncludesExplicitlyAllowedPackages() {
        val state = NotificationFilterEngine.buildSummary(
            rawNotifications = sampleRaw,
            hasListenerAccess = true,
            privacyModeEnabled = false,
            useAllowList = true,
            allowedPackages = setOf("com.example.mail"),
            blockedPackages = emptySet(),
        )

        assertEquals(1, state.totalCount)
        assertEquals("com.example.mail", state.groups.single().packageName)
    }

    @Test
    fun privacyModeRedactsMessageContentAndTitles() {
        val state = NotificationFilterEngine.buildSummary(
            rawNotifications = sampleRaw,
            hasListenerAccess = true,
            privacyModeEnabled = true,
            useAllowList = false,
            allowedPackages = emptySet(),
            blockedPackages = emptySet(),
        )

        assertTrue(state.privacyModeEnabled)
        assertEquals(3, state.totalCount)
        val firstItem = state.groups.first().items.first()
        assertEquals("Chat", firstItem.title)
        assertEquals(NotificationFilterEngine.PRIVACY_REDACTED_TEXT, firstItem.previewText)
    }
}
