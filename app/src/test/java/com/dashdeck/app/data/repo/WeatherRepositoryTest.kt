package com.dashdeck.app.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRepositoryTest {

    @Test
    fun parsesOpenMeteoGeocodingResults() {
        val raw = """
            {
              "results": [
                {
                  "name": "Toledo",
                  "latitude": 41.6639,
                  "longitude": -83.5552,
                  "admin1": "Ohio",
                  "country_code": "US"
                }
              ]
            }
        """.trimIndent()

        val locations = OpenMeteoParser.parseGeocoding(raw)
        assertEquals(1, locations.size)
        val loc = locations.first()
        assertEquals("Toledo", loc.name)
        assertEquals("Toledo, Ohio", loc.displayLabel())
        assertEquals(41.6639, loc.latitude, 0.0001)
        assertEquals(-83.5552, loc.longitude, 0.0001)
    }

    @Test
    fun parsesOpenMeteoForecastAndFormatsReadouts() {
        val raw = """
            {
              "current": {
                "temperature_2m": 42.3,
                "weather_code": 0,
                "is_day": 1
              }
            }
        """.trimIndent()

        val snapshot = OpenMeteoParser.parseForecast(
            rawJson = raw,
            cityLabel = "Toledo, Ohio",
            latitude = 41.6639,
            longitude = -83.5552,
            isFahrenheit = true,
            nowEpochMs = 1_000_000L,
        )
        assertNotNull(snapshot)
        assertEquals(42, snapshot!!.temperatureRounded())
        assertEquals("42°", snapshot.compactReadout())
        assertEquals("☀ 42° Clear", snapshot.mediumReadout())
        assertEquals("☀ 42°F · Clear · Toledo", snapshot.fullReadout())
    }

    @Test
    fun wmoMapperCoversDayNightAndSevereWeatherCodes() {
        assertEquals("Clear", WmoWeatherMapper.describe(0, isDay = true).shortLabel)
        assertEquals("☀", WmoWeatherMapper.describe(0, isDay = true).symbol)
        assertEquals("🌙", WmoWeatherMapper.describe(0, isDay = false).symbol)
        assertEquals("Fog", WmoWeatherMapper.describe(45).shortLabel)
        assertEquals("Rain", WmoWeatherMapper.describe(63).shortLabel)
        assertEquals("Snow", WmoWeatherMapper.describe(73).shortLabel)
        assertEquals("Thunderstorm", WmoWeatherMapper.describe(95).shortLabel)
    }

    @Test
    fun weatherSnapshotRespectsThirtyMinuteCacheTtl() {
        val snapshot = WeatherSnapshot(
            cityLabel = "Toledo, Ohio",
            latitude = 41.6639,
            longitude = -83.5552,
            temperatureValue = 50.0,
            isFahrenheit = true,
            weatherCode = 2,
            isDay = true,
            fetchedAtEpochMs = 10_000L,
        )
        assertFalse(snapshot.isStale(nowEpochMs = 10_000L + 15 * 60 * 1000L))
        assertTrue(snapshot.isStale(nowEpochMs = 10_000L + WeatherRepository.CACHE_TTL_MS))
    }
}
