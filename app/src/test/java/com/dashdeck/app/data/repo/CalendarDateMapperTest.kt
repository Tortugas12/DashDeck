package com.dashdeck.app.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class CalendarDateMapperTest {

    private val zone = ZoneId.of("America/Los_Angeles")

    @Test
    fun mapsAllDayEventWithoutShiftingIntoAdjacentDay() {
        val startUtc = LocalDate.of(2026, 11, 25).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endUtc = LocalDate.of(2026, 11, 26).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val event = CalendarEvent(
            eventId = 1L,
            calendarId = 10L,
            title = "All-Day Summit",
            startMillis = startUtc,
            endMillis = endUtc,
            allDay = true,
            color = 0xFF6366F1.toInt(),
        )

        val mapped = CalendarDateMapper.groupEventsByDate(listOf(event), zone)
        assertEquals(1, mapped.size)
        assertEquals("All-Day Summit", mapped[LocalDate.of(2026, 11, 25)]?.single()?.title)
        assertNull(mapped[LocalDate.of(2026, 11, 24)])
        assertNull(mapped[LocalDate.of(2026, 11, 26)])
    }

    @Test
    fun expandsMultiDayTimedEventAndSortsAllDayFirst() {
        val day1Start = LocalDate.of(2026, 11, 25).atTime(22, 0).atZone(zone).toInstant().toEpochMilli()
        val day2End = LocalDate.of(2026, 11, 26).atTime(2, 0).atZone(zone).toInstant().toEpochMilli()
        val allDayStart = LocalDate.of(2026, 11, 25).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val allDayEnd = LocalDate.of(2026, 11, 26).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val timedEvent = CalendarEvent(
            eventId = 2L,
            calendarId = 10L,
            title = "Overnight Deploy",
            startMillis = day1Start,
            endMillis = day2End,
            allDay = false,
            color = 0xFF38BDF8.toInt(),
        )
        val allDayEvent = CalendarEvent(
            eventId = 3L,
            calendarId = 10L,
            title = "Holiday",
            startMillis = allDayStart,
            endMillis = allDayEnd,
            allDay = true,
            color = 0xFFC2593F.toInt(),
        )

        val mapped = CalendarDateMapper.groupEventsByDate(listOf(timedEvent, allDayEvent), zone)

        val nov25 = mapped[LocalDate.of(2026, 11, 25)].orEmpty()
        val nov26 = mapped[LocalDate.of(2026, 11, 26)].orEmpty()
        assertEquals(listOf("Holiday", "Overnight Deploy"), nov25.map { it.title })
        assertEquals(listOf("Overnight Deploy"), nov26.map { it.title })
    }
}
