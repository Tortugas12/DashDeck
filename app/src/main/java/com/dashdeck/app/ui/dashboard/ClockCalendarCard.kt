package com.dashdeck.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dashdeck.app.data.repo.CalendarAccount
import com.dashdeck.app.data.repo.CalendarEvent
import com.dashdeck.app.data.repo.CalendarMonthState
import com.dashdeck.app.ui.common.DashDeckCard
import com.dashdeck.app.ui.common.EmptyModuleState
import com.dashdeck.app.ui.common.ModuleCardHeader
import com.dashdeck.app.ui.common.PermissionMissingState
import com.dashdeck.app.ui.common.desktopHoverHighlight
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ClockCard(
    use24HourClock: Boolean,
    showSeconds: Boolean,
    onToggle24Hour: () -> Unit,
    modifier: Modifier = Modifier,
    clockStyle: com.dashdeck.app.widget.ClockStylePreset = com.dashdeck.app.widget.ClockStylePreset.CLASSIC,
    clockAlignment: com.dashdeck.app.widget.ClockHorizontalAlignment = com.dashdeck.app.widget.ClockHorizontalAlignment.CENTER,
    showWeather: Boolean = false,
    weatherSnapshot: com.dashdeck.app.data.repo.WeatherSnapshot? = null,
) {
    val activeLocale = androidx.compose.ui.platform.LocalLocale.current.platformLocale
    var nowTime by remember { mutableStateOf(LocalTime.now()) }
    var today by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(showSeconds) {
        while (true) {
            nowTime = LocalTime.now()
            today = LocalDate.now()
            delay(if (showSeconds) 1_000L else 15_000L)
        }
    }

    val timePattern = remember(use24HourClock, showSeconds) {
        when {
            use24HourClock && showSeconds -> "HH:mm:ss"
            use24HourClock -> "HH:mm"
            showSeconds -> "h:mm:ss a"
            else -> "h:mm a"
        }
    }
    val timeText = remember(nowTime, timePattern, activeLocale) {
        nowTime.format(DateTimeFormatter.ofPattern(timePattern, activeLocale))
    }
    val rawDate = remember(today, activeLocale) {
        today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", activeLocale))
    }
    val dateText = if (clockStyle == com.dashdeck.app.widget.ClockStylePreset.EDITORIAL) {
        rawDate.uppercase(activeLocale)
    } else {
        rawDate
    }

    val horiz = when (clockAlignment) {
        com.dashdeck.app.widget.ClockHorizontalAlignment.LEFT -> Alignment.Start
        com.dashdeck.app.widget.ClockHorizontalAlignment.CENTER -> Alignment.CenterHorizontally
        com.dashdeck.app.widget.ClockHorizontalAlignment.RIGHT -> Alignment.End
    }
    val textAlign = when (clockAlignment) {
        com.dashdeck.app.widget.ClockHorizontalAlignment.LEFT -> TextAlign.Start
        com.dashdeck.app.widget.ClockHorizontalAlignment.CENTER -> TextAlign.Center
        com.dashdeck.app.widget.ClockHorizontalAlignment.RIGHT -> TextAlign.End
    }

    DashDeckCard(modifier = modifier) {
        ModuleCardHeader(
            title = "Clock",
            icon = Icons.Outlined.Schedule,
            trailing = {
                TextButton(onClick = onToggle24Hour) {
                    Text(if (use24HourClock) "24h" else "12h")
                }
            },
        )
        Spacer(modifier = Modifier.height(6.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = horiz,
        ) {
            Text(
                text = timeText,
                style = if (clockStyle == com.dashdeck.app.widget.ClockStylePreset.EDITORIAL) {
                    MaterialTheme.typography.displayMedium
                } else {
                    MaterialTheme.typography.displaySmall
                },
                fontWeight = FontWeight.Bold,
                textAlign = textAlign,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign,
            )
            if (showWeather && weatherSnapshot != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = weatherSnapshot.fullReadout(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    textAlign = textAlign,
                )
            }
        }
    }
}

@Composable
fun CalendarCard(
    monthState: CalendarMonthState,
    selectedDate: LocalDate,
    excludedCalendarIds: Set<Long>,
    onSelectDate: (LocalDate) -> Unit,
    onChangeMonth: (YearMonth) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenEvent: (CalendarEvent) -> Unit,
    onUpdateExcludedCalendars: (Set<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
    val monthTitle = remember(monthState.yearMonth) {
        monthState.yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    }

    DashDeckCard(modifier = modifier) {
        ModuleCardHeader(
            title = "Calendar",
            icon = Icons.Outlined.CalendarMonth,
            trailing = {
                if (monthState.hasPermission) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (monthState.yearMonth != YearMonth.from(today) || selectedDate != today) {
                            TextButton(
                                onClick = {
                                    onChangeMonth(YearMonth.from(today))
                                    onSelectDate(today)
                                },
                            ) {
                                Text("Today")
                            }
                        }
                        if (monthState.calendars.isNotEmpty()) {
                            IconButton(onClick = { showFilterDialog = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.FilterList,
                                    contentDescription = "Filter calendars",
                                )
                            }
                        }
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!monthState.hasPermission) {
            PermissionMissingState(
                icon = Icons.Outlined.CalendarMonth,
                message = "Grant calendar access to see event dots and your daily agenda.",
                actionLabel = "Grant Calendar Access",
                onActionClick = onRequestPermission,
            )
            return@DashDeckCard
        }

        // Month navigation header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { onChangeMonth(monthState.yearMonth.minusMonths(1)) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                )
            }
            Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = { onChangeMonth(monthState.yearMonth.plusMonths(1)) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Next month",
                )
            }
        }

        // Weekday labels
        val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { dayLabel ->
                Text(
                    text = dayLabel,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Month grid with event dots
        val firstDay = monthState.yearMonth.atDay(1)
        val leadingBlanks = firstDay.dayOfWeek.value % 7 // Sunday = 0
        val totalDays = monthState.yearMonth.lengthOfMonth()
        val totalCells = ((leadingBlanks + totalDays + 6) / 7) * 7
        val rows = totalCells / 7

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - leadingBlanks + 1
                        if (dayNumber in 1..totalDays) {
                            val date = monthState.yearMonth.atDay(dayNumber)
                            val events = monthState.eventsByDate[date].orEmpty()
                            val isSelected = date == selectedDate
                            val isToday = date == today

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .desktopHoverHighlight(cornerRadius = 10.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color.Transparent
                                        },
                                    )
                                    .clickable { onSelectDate(date) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                    if (events.isNotEmpty()) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            events.take(3).forEach { ev ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) {
                                                                MaterialTheme.colorScheme.onPrimary
                                                            } else if (ev.color != 0) {
                                                                Color(ev.color)
                                                            } else {
                                                                MaterialTheme.colorScheme.primary
                                                            },
                                                        ),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).height(38.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(8.dp))

        // Selected day agenda
        val agendaEvents = monthState.eventsByDate[selectedDate].orEmpty()
        val selectedLabel = remember(selectedDate) {
            selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
        }
        Text(
            text = "Agenda · $selectedLabel",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (agendaEvents.isEmpty()) {
            EmptyModuleState(
                icon = Icons.Outlined.CalendarMonth,
                title = "No events scheduled",
                subtitle = "Enjoy your open day or select another date.",
            )
        } else {
            val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()) }
            val zone = remember { ZoneId.systemDefault() }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                agendaEvents.take(5).forEach { event ->
                    val timeString = if (event.allDay) {
                        "All day"
                    } else {
                        Instant.ofEpochMilli(event.startMillis).atZone(zone).toLocalTime()
                            .format(timeFormatter)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .desktopHoverHighlight(cornerRadius = 10.dp)
                            .clickable { onOpenEvent(event) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (event.color != 0) Color(event.color)
                                    else MaterialTheme.colorScheme.primary,
                                ),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val meta = buildString {
                                append(timeString)
                                if (!event.location.isNullOrBlank()) {
                                    append(" · ")
                                    append(event.location)
                                }
                            }
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        CalendarFilterDialog(
            calendars = monthState.calendars,
            excludedIds = excludedCalendarIds,
            onSave = { updated ->
                onUpdateExcludedCalendars(updated)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false },
        )
    }
}

@Composable
private fun CalendarFilterDialog(
    calendars: List<CalendarAccount>,
    excludedIds: Set<Long>,
    onSave: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var localExcluded by remember(excludedIds) { mutableStateOf(excludedIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Visible Calendars") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                calendars.forEach { cal ->
                    val isVisible = cal.id !in localExcluded
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                localExcluded = if (isVisible) localExcluded + cal.id else localExcluded - cal.id
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isVisible,
                            onCheckedChange = { checked ->
                                localExcluded = if (checked) localExcluded - cal.id else localExcluded + cal.id
                            },
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (cal.color != 0) Color(cal.color) else MaterialTheme.colorScheme.primary),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = cal.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (cal.accountName.isNotBlank()) {
                                Text(
                                    text = cal.accountName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(localExcluded) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
