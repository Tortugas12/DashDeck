package com.dashdeck.app.data.repo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.dashdeck.app.data.prefs.AppPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

@Serializable
data class WeatherLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val admin1: String? = null,
    val countryCode: String? = null,
) {
    fun displayLabel(): String = buildString {
        append(name)
        if (!admin1.isNullOrBlank() && !admin1.equals(name, ignoreCase = true)) {
            append(", ")
            append(admin1)
        } else if (!countryCode.isNullOrBlank()) {
            append(", ")
            append(countryCode)
        }
    }
}

@Serializable
data class WeatherSnapshot(
    val cityLabel: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureValue: Double,
    val isFahrenheit: Boolean,
    val weatherCode: Int,
    val isDay: Boolean = true,
    val fetchedAtEpochMs: Long,
) {
    fun temperatureRounded(): Int = temperatureValue.roundToInt()

    fun unitSymbol(): String = if (isFahrenheit) "°F" else "°C"

    fun compactReadout(): String = "${temperatureRounded()}°"

    fun mediumReadout(): String {
        val info = WmoWeatherMapper.describe(weatherCode, isDay)
        return "${info.symbol} ${temperatureRounded()}° ${info.shortLabel}"
    }

    fun fullReadout(): String {
        val info = WmoWeatherMapper.describe(weatherCode, isDay)
        val city = cityLabel.substringBefore(",").trim()
        return if (city.isNotBlank()) {
            "${info.symbol} ${temperatureRounded()}${unitSymbol()} · ${info.shortLabel} · $city"
        } else {
            "${info.symbol} ${temperatureRounded()}${unitSymbol()} · ${info.shortLabel}"
        }
    }

    fun isStale(nowEpochMs: Long, ttlMs: Long = WeatherRepository.CACHE_TTL_MS): Boolean {
        return (nowEpochMs - fetchedAtEpochMs) >= ttlMs
    }
}

data class WeatherConditionInfo(
    val shortLabel: String,
    val symbol: String,
)

object WmoWeatherMapper {
    fun describe(code: Int, isDay: Boolean = true): WeatherConditionInfo = when (code) {
        0 -> WeatherConditionInfo("Clear", if (isDay) "☀" else "🌙")
        1 -> WeatherConditionInfo("Mostly Clear", if (isDay) "🌤" else "🌙")
        2 -> WeatherConditionInfo("Partly Cloudy", if (isDay) "⛅" else "☁")
        3 -> WeatherConditionInfo("Overcast", "☁")
        45, 48 -> WeatherConditionInfo("Fog", "🌫")
        in 51..57 -> WeatherConditionInfo("Drizzle", "🌦")
        in 61..67 -> WeatherConditionInfo("Rain", "🌧")
        in 71..77 -> WeatherConditionInfo("Snow", "❄")
        in 80..82 -> WeatherConditionInfo("Showers", "🌦")
        in 85..86 -> WeatherConditionInfo("Snow Showers", "🌨")
        in 95..99 -> WeatherConditionInfo("Thunderstorm", "⛈")
        else -> WeatherConditionInfo("Cloudy", "☁")
    }
}

@Serializable
private data class OpenMeteoGeocodingResponse(
    val results: List<OpenMeteoGeoResult> = emptyList(),
)

@Serializable
private data class OpenMeteoGeoResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val admin1: String? = null,
    @SerialName("country_code")
    val countryCode: String? = null,
)

@Serializable
private data class OpenMeteoForecastResponse(
    val current: OpenMeteoCurrent? = null,
)

@Serializable
private data class OpenMeteoCurrent(
    @SerialName("temperature_2m")
    val temperature2m: Double,
    @SerialName("weather_code")
    val weatherCode: Int,
    @SerialName("is_day")
    val isDay: Int = 1,
)

object OpenMeteoParser {
    private val parserJson = Json { ignoreUnknownKeys = true }

    fun parseGeocoding(rawJson: String): List<WeatherLocation> {
        val decoded = parserJson.decodeFromString<OpenMeteoGeocodingResponse>(rawJson)
        return decoded.results.map { r ->
            WeatherLocation(
                name = r.name,
                latitude = r.latitude,
                longitude = r.longitude,
                admin1 = r.admin1,
                countryCode = r.countryCode,
            )
        }
    }

    fun parseForecast(
        rawJson: String,
        cityLabel: String,
        latitude: Double,
        longitude: Double,
        isFahrenheit: Boolean,
        nowEpochMs: Long,
    ): WeatherSnapshot? {
        val decoded = parserJson.decodeFromString<OpenMeteoForecastResponse>(rawJson)
        val current = decoded.current ?: return null
        return WeatherSnapshot(
            cityLabel = cityLabel,
            latitude = latitude,
            longitude = longitude,
            temperatureValue = current.temperature2m,
            isFahrenheit = isFahrenheit,
            weatherCode = current.weatherCode,
            isDay = current.isDay == 1,
            fetchedAtEpochMs = nowEpochMs,
        )
    }
}

class WeatherRepository(
    private val context: Context,
    private val preferencesRepository: AppPreferencesRepository,
) {
    fun hasCoarseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun searchCities(query: String): List<WeatherLocation> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        val encoded = URLEncoder.encode(trimmed, Charsets.UTF_8.name())
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=5&language=en&format=json"
        runCatching {
            val body = httpGet(url) ?: return@withContext emptyList()
            OpenMeteoParser.parseGeocoding(body)
        }.getOrDefault(emptyList())
    }

    suspend fun getCachedWeather(): WeatherSnapshot? {
        return preferencesRepository.getSnapshot().cachedWeather
    }

    suspend fun getOrRefreshWeather(forceRefresh: Boolean = false): WeatherSnapshot? = withContext(Dispatchers.IO) {
        val prefs = preferencesRepository.getSnapshot()
        val cached = prefs.cachedWeather
        val now = System.currentTimeMillis()

        val resolvedTarget = resolveTargetCoordinates(prefs) ?: return@withContext cached

        val cacheMatchesTarget = cached != null &&
            cached.isFahrenheit == prefs.weatherUseFahrenheit &&
            kotlin.math.abs(cached.latitude - resolvedTarget.latitude) < 0.05 &&
            kotlin.math.abs(cached.longitude - resolvedTarget.longitude) < 0.05 &&
            !cached.isStale(now)

        if (!forceRefresh && cacheMatchesTarget) {
            return@withContext cached
        }

        val unitParam = if (prefs.weatherUseFahrenheit) "fahrenheit" else "celsius"
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${resolvedTarget.latitude}" +
            "&longitude=${resolvedTarget.longitude}" +
            "&current=temperature_2m,weather_code,is_day" +
            "&temperature_unit=$unitParam" +
            "&timezone=auto"

        val fresh = runCatching {
            val body = httpGet(url) ?: return@runCatching null
            OpenMeteoParser.parseForecast(
                rawJson = body,
                cityLabel = resolvedTarget.label,
                latitude = resolvedTarget.latitude,
                longitude = resolvedTarget.longitude,
                isFahrenheit = prefs.weatherUseFahrenheit,
                nowEpochMs = now,
            )
        }.getOrNull()

        if (fresh != null) {
            preferencesRepository.setCachedWeather(fresh)
            return@withContext fresh
        }

        cached
    }

    private data class ResolvedCoords(
        val label: String,
        val latitude: Double,
        val longitude: Double,
    )

    private fun resolveTargetCoordinates(prefs: com.dashdeck.app.data.prefs.AppPreferences): ResolvedCoords? {
        if (prefs.weatherUseDeviceLocation && hasCoarseLocationPermission()) {
            val loc = getLastKnownCoarseLocation()
            if (loc != null) {
                val label = prefs.weatherCityName.ifBlank { "Local" }
                return ResolvedCoords(label, loc.first, loc.second)
            }
        }
        val lat = prefs.weatherLat
        val lon = prefs.weatherLon
        if (lat != null && lon != null) {
            return ResolvedCoords(prefs.weatherCityName.ifBlank { "Weather" }, lat, lon)
        }
        return null
    }

    private fun getLastKnownCoarseLocation(): Pair<Double, Double>? {
        if (!hasCoarseLocationPermission()) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return runCatching {
            @Suppress("MissingPermission")
            val loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            loc?.let { it.latitude to it.longitude }
        }.getOrNull()
    }

    private fun httpGet(urlString: String): String? {
        val conn = (URL(urlString).openConnection() as? HttpURLConnection) ?: return null
        return try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 6_000
            conn.readTimeout = 6_000
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        const val CACHE_TTL_MS = 30L * 60L * 1000L
    }
}
