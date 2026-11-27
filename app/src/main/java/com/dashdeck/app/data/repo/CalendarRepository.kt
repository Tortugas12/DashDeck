package com.dashdeck.app.data.repo

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

data class CalendarAccount(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int,
)

data class CalendarEvent(
    val eventId: Long,
    val calendarId: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val color: Int,
    val location: String? = null,
)

data class CalendarMonthState(
    val hasPermission: Boolean,
    val yearMonth: YearMonth,
    val eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    val calendars: List<CalendarAccount>,
)

object CalendarDateMapper {
    fun groupEventsByDate(
        events: List<CalendarEvent>,
        zoneId: ZoneId,
    ): Map<LocalDate, List<CalendarEvent>> {
        if (events.isEmpty()) return emptyMap()
        val result = linkedMapOf<LocalDate, MutableList<CalendarEvent>>()

        for (event in events) {
            val startDate: LocalDate
            val endDateInclusive: LocalDate

            if (event.allDay) {
                // CalendarContract stores all-day timestamps as UTC midnight; end is exclusive.
                startDate = Instant.ofEpochMilli(event.startMillis).atZone(ZoneOffset.UTC).toLocalDate()
                val rawEnd = Instant.ofEpochMilli(event.endMillis).atZone(ZoneOffset.UTC).toLocalDate()
                endDateInclusive = if (rawEnd.isAfter(startDate)) rawEnd.minusDays(1) else startDate
            } else {
                startDate = Instant.ofEpochMilli(event.startMillis).atZone(zoneId).toLocalDate()
                val adjustedEndMillis = if (event.endMillis > event.startMillis) {
                    event.endMillis - 1L
                } else {
                    event.startMillis
                }
                endDateInclusive = Instant.ofEpochMilli(adjustedEndMillis).atZone(zoneId).toLocalDate()
            }

            var cursor = startDate
            var guardDays = 0
            while (!cursor.isAfter(endDateInclusive) && guardDays < 62) {
                result.getOrPut(cursor) { mutableListOf() }.add(event)
                cursor = cursor.plusDays(1)
                guardDays++
            }
        }

        return result.mapValues { (_, list) ->
            list.sortedWith(compareByDescending<CalendarEvent> { it.allDay }.thenBy { it.startMillis })
        }
    }
}

class CalendarRepository(private val context: Context) {

    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun loadMonthState(
        yearMonth: YearMonth,
        excludedCalendarIds: Set<Long>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): CalendarMonthState = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) {
            return@withContext CalendarMonthState(
                hasPermission = false,
                yearMonth = yearMonth,
                eventsByDate = emptyMap(),
                calendars = emptyList(),
            )
        }

        val calendars = queryCalendars()
        val startMillis = yearMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val events = queryInstances(startMillis, endMillis, excludedCalendarIds)

        CalendarMonthState(
            hasPermission = true,
            yearMonth = yearMonth,
            eventsByDate = CalendarDateMapper.groupEventsByDate(events, zoneId),
            calendars = calendars,
        )
    }

    fun openEventInSystemCalendar(eventId: Long, startMillis: Long): Boolean {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = uri
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun queryCalendars(): List<CalendarAccount> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
        )
        val calendars = mutableListOf<CalendarAccount>()
        runCatching {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.VISIBLE} = 1",
                null,
                "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC",
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val colorIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
                while (cursor.moveToNext()) {
                    calendars += CalendarAccount(
                        id = cursor.getLong(idIdx),
                        displayName = cursor.getString(nameIdx).orEmpty().ifBlank { "Calendar" },
                        accountName = cursor.getString(accountIdx).orEmpty(),
                        color = cursor.getInt(colorIdx),
                    )
                }
            }
        }
        return calendars
    }

    private fun queryInstances(
        startMillis: Long,
        endMillis: Long,
        excludedCalendarIds: Set<Long>,
    ): List<CalendarEvent> {
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.EVENT_LOCATION,
        )

        val events = mutableListOf<CalendarEvent>()
        runCatching {
            context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { cursor ->
                val eventIdIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val calIdIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
                val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val allDayIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val colorIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.DISPLAY_COLOR)
                val locIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)

                while (cursor.moveToNext()) {
                    val calId = cursor.getLong(calIdIdx)
                    if (calId in excludedCalendarIds) continue
                    events += CalendarEvent(
                        eventId = cursor.getLong(eventIdIdx),
                        calendarId = calId,
                        title = cursor.getString(titleIdx).orEmpty().ifBlank { "(No title)" },
                        startMillis = cursor.getLong(beginIdx),
                        endMillis = cursor.getLong(endIdx),
                        allDay = cursor.getInt(allDayIdx) == 1,
                        color = cursor.getInt(colorIdx),
                        location = cursor.getString(locIdx)?.takeIf { it.isNotBlank() },
                    )
                }
            }
        }
        return events
    }
}
