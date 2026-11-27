package com.dashdeck.app.ui.popup

import android.graphics.Rect
import androidx.compose.ui.unit.IntOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FolderPopupPositionTest {

    @Test
    fun returnsNullWhenSourceBoundsIsMissing() {
        val result = calculateAnchoredPopupOffset(
            sourceBounds = null as Rect?,
            popupWidthPx = 300,
            popupHeightPx = 240,
            containerWidthPx = 1080,
            containerHeightPx = 1920,
            edgeMarginPx = 32,
        )
        assertNull(result)
    }

    @Test
    fun centersPopupOverLauncherFolderBounds() {
        val result = calculateAnchoredPopupOffset(
            sourceBounds = AnchorBounds(left = 440, top = 700, right = 640, bottom = 900),
            popupWidthPx = 320,
            popupHeightPx = 240,
            containerWidthPx = 1080,
            containerHeightPx = 1920,
            edgeMarginPx = 32,
        )
        // Center is (540, 800) -> top-left = (540 - 160, 800 - 120) = (380, 680)
        assertEquals(IntOffset(380, 680), result)
    }

    @Test
    fun clampsLeftAndTopEdgesToSafeMargin() {
        val result = calculateAnchoredPopupOffset(
            sourceBounds = AnchorBounds(left = 0, top = 0, right = 80, bottom = 80),
            popupWidthPx = 320,
            popupHeightPx = 240,
            containerWidthPx = 1080,
            containerHeightPx = 1920,
            edgeMarginPx = 32,
        )
        assertEquals(IntOffset(32, 32), result)
    }

    @Test
    fun clampsRightAndBottomEdgesToSafeMargin() {
        val result = calculateAnchoredPopupOffset(
            sourceBounds = AnchorBounds(left = 1000, top = 1840, right = 1080, bottom = 1920),
            popupWidthPx = 320,
            popupHeightPx = 240,
            containerWidthPx = 1080,
            containerHeightPx = 1920,
            edgeMarginPx = 32,
        )
        assertEquals(IntOffset(1080 - 320 - 32, 1920 - 240 - 32), result)
    }

    @Test
    fun handlesPopupLargerThanContainerSafelyWithoutThrowing() {
        val result = calculateAnchoredPopupOffset(
            sourceBounds = AnchorBounds(left = 100, top = 100, right = 200, bottom = 200),
            popupWidthPx = 1200,
            popupHeightPx = 1000,
            containerWidthPx = 800,
            containerHeightPx = 600,
            edgeMarginPx = 32,
        )
        assertEquals(IntOffset(0, 0), result)
    }
}
