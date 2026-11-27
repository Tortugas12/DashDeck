package com.dashdeck.app.widget.modules

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.dashdeck.app.data.db.ModuleSizeTier
import com.dashdeck.app.data.repo.CalendarEvent
import com.dashdeck.app.widget.OpenAppSettingsActionCallback
import com.dashdeck.app.widget.OpenCalendarAppActionCallback
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class WidgetCalendarSnapshot(
    val hasPermission: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val eventDatesInWeek: Set<LocalDate> = emptySet(),
    val monthEventDates: Set<LocalDate> = eventDatesInWeek,
    val todayEvents: List<CalendarEvent> = emptyList(),
)

@Composable
@GlanceComposable
fun CalendarGlanceModule(
    snapshot: WidgetCalendarSnapshot,
    cornerRadiusDp: Int,
    locale: Locale,
    sizeTier: ModuleSizeTier = ModuleSizeTier.STANDARD,
) {
    val headerPattern = if (sizeTier == ModuleSizeTier.COMPACT) "MMMM d" else "MMMM yyyy"
    val headerText = snapshot.today.format(DateTimeFormatter.ofPattern(headerPattern, locale))
    val clickAction = if (snapshot.hasPermission) {
        actionRunCallback<OpenCalendarAppActionCallback>()
    } else {
        actionRunCallback<OpenAppSettingsActionCallback>()
    }

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(cornerRadiusDp.dp)
            .background(GlanceTheme.colors.surface)
            .clickable(clickAction)
            .padding(if (sizeTier == ModuleSizeTier.COMPACT) 10.dp else 12.dp),
    ) {
        Text(
            text = headerText,
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )

        Spacer(modifier = GlanceModifier.height(if (sizeTier == ModuleSizeTier.COMPACT) 4.dp else 6.dp))

        if (!snapshot.hasPermission) {
            Text(
                text = "Tap to grant Calendar access",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
                maxLines = 2,
            )
            return@Column
        }

        when (sizeTier) {
            ModuleSizeTier.COMPACT -> {
                RenderTodayEvents(events = snapshot.todayEvents, maxEvents = 1)
            }

            ModuleSizeTier.STANDARD -> {
                val weekStart = snapshot.today.minusDays((snapshot.today.dayOfWeek.value % 7).toLong())
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (offset in 0..6) {
                        val date = weekStart.plusDays(offset.toLong())
                        val isToday = date == snapshot.today
                        val hasEvents = date in snapshot.monthEventDates || date in snapshot.eventDatesInWeek
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = TextStyle(
                                    color = if (isToday) GlanceTheme.colors.primary else GlanceTheme.colors.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                ),
                                maxLines = 1,
                            )
                            Text(
                                text = if (hasEvents) "•" else " ",
                                style = TextStyle(
                                    color = GlanceTheme.colors.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }
                Spacer(modifier = GlanceModifier.height(4.dp))
                RenderTodayEvents(events = snapshot.todayEvents, maxEvents = 2)
            }

            ModuleSizeTier.LARGE -> {
                val yearMonth = YearMonth.from(snapshot.today)
                val firstDay = yearMonth.atDay(1)
                val leadingBlanks = firstDay.dayOfWeek.value % 7
                val totalDays = yearMonth.lengthOfMonth()
                val totalCells = ((leadingBlanks + totalDays + 6) / 7) * 7
                val rows = totalCells / 7
                val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")

                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    weekdays.forEach { dayLabel ->
                        Text(
                            text = dayLabel,
                            modifier = GlanceModifier.defaultWeight(),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                            ),
                            maxLines = 1,
                        )
                    }
                }
                Spacer(modifier = GlanceModifier.height(2.dp))

                for (row in 0 until rows) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - leadingBlanks + 1
                            if (dayNumber in 1..totalDays) {
                                val date = yearMonth.atDay(dayNumber)
                                val isToday = date == snapshot.today
                                val hasEvents = date in snapshot.monthEventDates || date in snapshot.eventDatesInWeek
                                Column(
                                    modifier = GlanceModifier
                                        .defaultWeight()
                                        .cornerRadius(6.dp)
                                        .background(
                                            if (isToday) GlanceTheme.colors.primaryContainer
                                            else GlanceTheme.colors.surface,
                                        )
                                        .padding(vertical = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        style = TextStyle(
                                            color = if (isToday) {
                                                GlanceTheme.colors.onPrimaryContainer
                                            } else {
                                                GlanceTheme.colors.onSurface
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        ),
                                        maxLines = 1,
                                    )
                                    if (hasEvents) {
                                        Text(
                                            text = "•",
                                            style = TextStyle(
                                                color = if (isToday) {
                                                    GlanceTheme.colors.onPrimaryContainer
                                                } else {
                                                    GlanceTheme.colors.primary
                                                },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            maxLines = 1,
                                        )
                                    } else {
                                        Spacer(modifier = GlanceModifier.height(10.dp))
                                    }
                                }
                            } else {
                                Spacer(modifier = GlanceModifier.defaultWeight())
                            }
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(6.dp))
                RenderTodayEvents(events = snapshot.todayEvents, maxEvents = 4)
            }
        }
    }
}

@Composable
@GlanceComposable
private fun RenderTodayEvents(
    events: List<CalendarEvent>,
    maxEvents: Int,
) {
    if (events.isEmpty()) {
        Text(
            text = "No events today",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 11.sp,
            ),
            maxLines = 1,
        )
    } else {
        events.take(maxEvents).forEach { event ->
            Text(
                text = "• ${event.title}",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 11.sp,
                ),
                maxLines = 1,
            )
        }
    }
}
