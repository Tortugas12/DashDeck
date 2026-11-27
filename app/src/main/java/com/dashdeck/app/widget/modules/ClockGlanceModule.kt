package com.dashdeck.app.widget.modules

import android.util.TypedValue
import android.view.Gravity
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
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
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.dashdeck.app.R
import com.dashdeck.app.data.db.ModuleSizeTier
import com.dashdeck.app.data.repo.WeatherSnapshot
import com.dashdeck.app.widget.ClockHorizontalAlignment
import com.dashdeck.app.widget.ClockStylePreset
import com.dashdeck.app.widget.OpenClockAppActionCallback
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
@GlanceComposable
fun ClockGlanceModule(
    use24HourClock: Boolean,
    cornerRadiusDp: Int,
    nowTime: LocalTime,
    today: LocalDate,
    locale: Locale,
    sizeTier: ModuleSizeTier = ModuleSizeTier.STANDARD,
    onSurfaceColorArgb: Int,
    useLiveTextClock: Boolean = true,
    clockStyle: ClockStylePreset = ClockStylePreset.CLASSIC,
    clockAlignment: ClockHorizontalAlignment = ClockHorizontalAlignment.CENTER,
    showWeather: Boolean = false,
    weather: WeatherSnapshot? = null,
    slotWidthDp: Int = 320,
    targetHeightDp: Int? = null,
) {
    val timeSp = when (sizeTier) {
        ModuleSizeTier.COMPACT -> if (clockStyle == ClockStylePreset.EDITORIAL) 20f else 18f
        ModuleSizeTier.STANDARD -> if (clockStyle == ClockStylePreset.EDITORIAL) 27f else 24f
        ModuleSizeTier.LARGE -> if (clockStyle == ClockStylePreset.EDITORIAL) 36f else 32f
    }
    val datePattern = when (sizeTier) {
        ModuleSizeTier.COMPACT -> "EEE, MMM d"
        ModuleSizeTier.STANDARD -> "EEEE, MMM d, yyyy"
        ModuleSizeTier.LARGE -> "EEEE, MMMM d, yyyy"
    }
    val dateSp = when (sizeTier) {
        ModuleSizeTier.COMPACT -> 11f
        ModuleSizeTier.STANDARD -> 12f
        ModuleSizeTier.LARGE -> 13f
    }
    val hPad = if (sizeTier == ModuleSizeTier.COMPACT) 10.dp else 12.dp
    val vPad = when (sizeTier) {
        ModuleSizeTier.COMPACT -> 8.dp
        ModuleSizeTier.STANDARD -> 10.dp
        ModuleSizeTier.LARGE -> 12.dp
    }

    val clockFormat = if (use24HourClock) "HH:mm" else "h:mm a"
    val timeText = nowTime.format(DateTimeFormatter.ofPattern(clockFormat, locale))
    val rawDate = today.format(DateTimeFormatter.ofPattern(datePattern, locale))
    val dateText = if (clockStyle == ClockStylePreset.EDITORIAL) rawDate.uppercase(locale) else rawDate

    val weatherLine = if (showWeather && weather != null) {
        when (sizeTier) {
            ModuleSizeTier.COMPACT -> weather.compactReadout()
            ModuleSizeTier.STANDARD -> weather.mediumReadout()
            ModuleSizeTier.LARGE -> weather.fullReadout()
        }
    } else {
        null
    }

    val horizAlignment = when (clockAlignment) {
        ClockHorizontalAlignment.LEFT -> Alignment.Start
        ClockHorizontalAlignment.CENTER -> Alignment.CenterHorizontally
        ClockHorizontalAlignment.RIGHT -> Alignment.End
    }
    val textAlign = when (clockAlignment) {
        ClockHorizontalAlignment.LEFT -> TextAlign.Start
        ClockHorizontalAlignment.CENTER -> TextAlign.Center
        ClockHorizontalAlignment.RIGHT -> TextAlign.End
    }
    val androidGravity = when (clockAlignment) {
        ClockHorizontalAlignment.LEFT -> Gravity.START or Gravity.CENTER_VERTICAL
        ClockHorizontalAlignment.CENTER -> Gravity.CENTER
        ClockHorizontalAlignment.RIGHT -> Gravity.END or Gravity.CENTER_VERTICAL
    }

    val baseModifier = GlanceModifier
        .fillMaxWidth()
        .let { m -> if (targetHeightDp != null) m.height(targetHeightDp.dp) else m }
        .cornerRadius(cornerRadiusDp.dp)
        .background(GlanceTheme.colors.surface)
        .clickable(actionRunCallback<OpenClockAppActionCallback>())
        .padding(horizontal = hPad, vertical = vPad)

    val useSplitRow = clockStyle == ClockStylePreset.SPLIT && slotWidthDp >= 240

    if (useSplitRow) {
        Row(
            modifier = baseModifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start,
            ) {
                ClockTimeDisplay(
                    useLiveTextClock = useLiveTextClock,
                    clockFormat = clockFormat,
                    timeText = timeText,
                    timeSp = timeSp,
                    onSurfaceColorArgb = onSurfaceColorArgb,
                    androidGravity = Gravity.START or Gravity.CENTER_VERTICAL,
                    textAlign = TextAlign.Start,
                )
            }
            Spacer(modifier = GlanceModifier.width(10.dp))
            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = dateText,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = dateSp.sp,
                        textAlign = TextAlign.End,
                    ),
                    maxLines = 2,
                )
                if (weatherLine != null) {
                    Spacer(modifier = GlanceModifier.height(3.dp))
                    Text(
                        text = weatherLine,
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.End,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    } else {
        Column(
            modifier = baseModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = horizAlignment,
        ) {
            ClockTimeDisplay(
                useLiveTextClock = useLiveTextClock,
                clockFormat = clockFormat,
                timeText = timeText,
                timeSp = timeSp,
                onSurfaceColorArgb = onSurfaceColorArgb,
                androidGravity = androidGravity,
                textAlign = textAlign,
            )

            Spacer(modifier = GlanceModifier.height(3.dp))

            Text(
                text = dateText,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = dateSp.sp,
                    fontWeight = if (clockStyle == ClockStylePreset.EDITORIAL) FontWeight.Medium else FontWeight.Normal,
                    textAlign = textAlign,
                ),
                modifier = GlanceModifier.fillMaxWidth(),
                maxLines = 1,
            )

            if (weatherLine != null) {
                Spacer(modifier = GlanceModifier.height(4.dp))
                if (clockStyle == ClockStylePreset.EDITORIAL) {
                    Text(
                        text = weatherLine,
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = textAlign,
                        ),
                        modifier = GlanceModifier
                            .cornerRadius(8.dp)
                            .background(GlanceTheme.colors.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        maxLines = 1,
                    )
                } else {
                    Text(
                        text = weatherLine,
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = textAlign,
                        ),
                        modifier = GlanceModifier.fillMaxWidth(),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
@GlanceComposable
private fun ClockTimeDisplay(
    useLiveTextClock: Boolean,
    clockFormat: String,
    timeText: String,
    timeSp: Float,
    onSurfaceColorArgb: Int,
    androidGravity: Int,
    textAlign: TextAlign,
) {
    if (useLiveTextClock) {
        val context = LocalContext.current
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_clock_text).apply {
            setCharSequence(R.id.widget_text_clock, "setFormat12Hour", clockFormat)
            setCharSequence(R.id.widget_text_clock, "setFormat24Hour", clockFormat)
            setTextColor(R.id.widget_text_clock, onSurfaceColorArgb)
            setTextViewTextSize(R.id.widget_text_clock, TypedValue.COMPLEX_UNIT_SP, timeSp)
            setInt(R.id.widget_text_clock, "setGravity", androidGravity)
        }
        AndroidRemoteViews(
            remoteViews = remoteViews,
            modifier = GlanceModifier
                .fillMaxWidth()
                .wrapContentHeight(),
        )
    } else {
        Text(
            text = timeText,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = timeSp.sp,
                fontWeight = FontWeight.Bold,
                textAlign = textAlign,
            ),
            modifier = GlanceModifier.fillMaxWidth(),
            maxLines = 1,
        )
    }
}
