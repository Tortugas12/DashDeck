package com.dashdeck.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dashdeck.app.data.repo.WeatherSnapshot
import com.dashdeck.app.theme.BuiltInThemes
import com.dashdeck.app.theme.ThemeTokens
import com.dashdeck.app.widget.ClockHorizontalAlignment
import com.dashdeck.app.widget.ClockStylePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dashDeckDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dashdeck_preferences",
)

data class AppPreferences(
    val globalTheme: ThemeTokens = BuiltInThemes.FollowSystem,
    val use24HourClock: Boolean = false,
    val showClockSeconds: Boolean = false,
    val clockStyle: ClockStylePreset = ClockStylePreset.CLASSIC,
    val clockAlignment: ClockHorizontalAlignment = ClockHorizontalAlignment.CENTER,
    val showWeather: Boolean = false,
    val weatherUseFahrenheit: Boolean = true,
    val weatherUseDeviceLocation: Boolean = false,
    val weatherCityName: String = "",
    val weatherLat: Double? = null,
    val weatherLon: Double? = null,
    val cachedWeather: WeatherSnapshot? = null,
    val privacyHideNotificationText: Boolean = false,
    val useAllowListForNotifications: Boolean = false,
    val allowedNotificationPackages: Set<String> = emptySet(),
    val blockedNotificationPackages: Set<String> = emptySet(),
    val excludedCalendarIds: Set<Long> = emptySet(),
)

class AppPreferencesRepository(
    context: Context,
    private val json: Json,
) {
    private val dataStore = context.applicationContext.dashDeckDataStore

    val preferencesFlow: Flow<AppPreferences> = dataStore.data.map { prefs ->
        val themeJson = prefs[KEY_GLOBAL_THEME_JSON]
        val parsedTheme = themeJson?.let {
            runCatching { json.decodeFromString<ThemeTokens>(it).validated() }.getOrNull()
        } ?: BuiltInThemes.FollowSystem

        val excludedCalIds = prefs[KEY_EXCLUDED_CALENDAR_IDS]
            .orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()

        val parsedClockStyle = prefs[KEY_CLOCK_STYLE]?.let {
            runCatching { ClockStylePreset.valueOf(it) }.getOrNull()
        } ?: ClockStylePreset.CLASSIC

        val parsedClockAlignment = prefs[KEY_CLOCK_ALIGNMENT]?.let {
            runCatching { ClockHorizontalAlignment.valueOf(it) }.getOrNull()
        } ?: ClockHorizontalAlignment.CENTER

        val parsedWeather = prefs[KEY_CACHED_WEATHER_JSON]?.let {
            runCatching { json.decodeFromString<WeatherSnapshot>(it) }.getOrNull()
        }

        AppPreferences(
            globalTheme = parsedTheme,
            use24HourClock = prefs[KEY_USE_24_HOUR_CLOCK] ?: false,
            showClockSeconds = prefs[KEY_SHOW_CLOCK_SECONDS] ?: false,
            clockStyle = parsedClockStyle,
            clockAlignment = parsedClockAlignment,
            showWeather = prefs[KEY_SHOW_WEATHER] ?: false,
            weatherUseFahrenheit = prefs[KEY_WEATHER_USE_FAHRENHEIT] ?: true,
            weatherUseDeviceLocation = prefs[KEY_WEATHER_USE_DEVICE_LOCATION] ?: false,
            weatherCityName = prefs[KEY_WEATHER_CITY_NAME] ?: "",
            weatherLat = prefs[KEY_WEATHER_LAT],
            weatherLon = prefs[KEY_WEATHER_LON],
            cachedWeather = parsedWeather,
            privacyHideNotificationText = prefs[KEY_PRIVACY_HIDE_NOTIFICATION_TEXT] ?: false,
            useAllowListForNotifications = prefs[KEY_USE_ALLOW_LIST_NOTIFICATIONS] ?: false,
            allowedNotificationPackages = prefs[KEY_ALLOWED_NOTIFICATION_PACKAGES].orEmpty(),
            blockedNotificationPackages = prefs[KEY_BLOCKED_NOTIFICATION_PACKAGES].orEmpty(),
            excludedCalendarIds = excludedCalIds,
        )
    }

    suspend fun getSnapshot(): AppPreferences = preferencesFlow.first()

    suspend fun setGlobalTheme(tokens: ThemeTokens) {
        val encoded = json.encodeToString(tokens.validated())
        dataStore.edit { it[KEY_GLOBAL_THEME_JSON] = encoded }
    }

    suspend fun setUse24HourClock(enabled: Boolean) {
        dataStore.edit { it[KEY_USE_24_HOUR_CLOCK] = enabled }
    }

    suspend fun setShowClockSeconds(enabled: Boolean) {
        dataStore.edit { it[KEY_SHOW_CLOCK_SECONDS] = enabled }
    }

    suspend fun setClockStyle(style: ClockStylePreset) {
        dataStore.edit { it[KEY_CLOCK_STYLE] = style.name }
    }

    suspend fun setClockAlignment(alignment: ClockHorizontalAlignment) {
        dataStore.edit { it[KEY_CLOCK_ALIGNMENT] = alignment.name }
    }

    suspend fun setShowWeather(enabled: Boolean) {
        dataStore.edit { it[KEY_SHOW_WEATHER] = enabled }
    }

    suspend fun setWeatherUseFahrenheit(useFahrenheit: Boolean) {
        dataStore.edit { it[KEY_WEATHER_USE_FAHRENHEIT] = useFahrenheit }
    }

    suspend fun setWeatherUseDeviceLocation(useLocation: Boolean) {
        dataStore.edit { it[KEY_WEATHER_USE_DEVICE_LOCATION] = useLocation }
    }

    suspend fun setWeatherLocation(cityName: String, lat: Double, lon: Double) {
        dataStore.edit {
            it[KEY_WEATHER_CITY_NAME] = cityName
            it[KEY_WEATHER_LAT] = lat
            it[KEY_WEATHER_LON] = lon
            it[KEY_SHOW_WEATHER] = true
        }
    }

    suspend fun setCachedWeather(snapshot: WeatherSnapshot) {
        val encoded = json.encodeToString(snapshot)
        dataStore.edit { it[KEY_CACHED_WEATHER_JSON] = encoded }
    }

    suspend fun setPrivacyHideNotificationText(enabled: Boolean) {
        dataStore.edit { it[KEY_PRIVACY_HIDE_NOTIFICATION_TEXT] = enabled }
    }

    suspend fun setNotificationFilterMode(useAllowList: Boolean) {
        dataStore.edit { it[KEY_USE_ALLOW_LIST_NOTIFICATIONS] = useAllowList }
    }

    suspend fun setAllowedNotificationPackages(packages: Set<String>) {
        dataStore.edit { it[KEY_ALLOWED_NOTIFICATION_PACKAGES] = packages }
    }

    suspend fun setBlockedNotificationPackages(packages: Set<String>) {
        dataStore.edit { it[KEY_BLOCKED_NOTIFICATION_PACKAGES] = packages }
    }

    suspend fun setExcludedCalendarIds(calendarIds: Set<Long>) {
        dataStore.edit {
            it[KEY_EXCLUDED_CALENDAR_IDS] = calendarIds.map(Long::toString).toSet()
        }
    }

    private companion object {
        val KEY_GLOBAL_THEME_JSON = stringPreferencesKey("global_theme_json")
        val KEY_USE_24_HOUR_CLOCK = booleanPreferencesKey("use_24_hour_clock")
        val KEY_SHOW_CLOCK_SECONDS = booleanPreferencesKey("show_clock_seconds")
        val KEY_CLOCK_STYLE = stringPreferencesKey("clock_style")
        val KEY_CLOCK_ALIGNMENT = stringPreferencesKey("clock_alignment")
        val KEY_SHOW_WEATHER = booleanPreferencesKey("show_weather")
        val KEY_WEATHER_USE_FAHRENHEIT = booleanPreferencesKey("weather_use_fahrenheit")
        val KEY_WEATHER_USE_DEVICE_LOCATION = booleanPreferencesKey("weather_use_device_location")
        val KEY_WEATHER_CITY_NAME = stringPreferencesKey("weather_city_name")
        val KEY_WEATHER_LAT = doublePreferencesKey("weather_lat")
        val KEY_WEATHER_LON = doublePreferencesKey("weather_lon")
        val KEY_CACHED_WEATHER_JSON = stringPreferencesKey("cached_weather_json")
        val KEY_PRIVACY_HIDE_NOTIFICATION_TEXT = booleanPreferencesKey("privacy_hide_notification_text")
        val KEY_USE_ALLOW_LIST_NOTIFICATIONS = booleanPreferencesKey("use_allow_list_notifications")
        val KEY_ALLOWED_NOTIFICATION_PACKAGES = stringSetPreferencesKey("allowed_notification_packages")
        val KEY_BLOCKED_NOTIFICATION_PACKAGES = stringSetPreferencesKey("blocked_notification_packages")
        val KEY_EXCLUDED_CALENDAR_IDS = stringSetPreferencesKey("excluded_calendar_ids")
    }
}
