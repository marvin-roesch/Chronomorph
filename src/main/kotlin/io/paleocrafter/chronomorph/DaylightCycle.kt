/*
 * Chronomorph for IntelliJ
 *
 * Copyright (c) 2018 PaleoCrafter
 *
 * MIT License
 */

package io.paleocrafter.chronomorph

import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import java.net.HttpURLConnection
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DaylightCycle {
    private val LOG = Logger.getInstance("Chronomorph Daylight Cycle")
    val DEFAULT = Cycle(LocalTime.of(6, 0), LocalTime.of(20, 0))

    private val cache = mutableMapOf<CacheKey, Cycle>()

    fun getCycle(): Cycle? {
        val settings = ChronomorphSettings.instance
        return getCycle(settings.latitude, settings.longitude)
    }

    fun getCycle(latitude: String, longitude: String): Cycle? {
        val date = LocalDate.now()
        val cacheKey = CacheKey(latitude, longitude, date)
        try {
            return getCacheValue(cacheKey, false)
                ?: HttpRequests.request("https://api.sunrise-sunset.org/json?lat=$latitude&lng=$longitude&formatted=0")
                    .accept("application/json")
                    .connect {
                        val pastValue = getCacheValue(cacheKey, true)
                        val connection = it.connection as? HttpURLConnection ?: return@connect pastValue
                        if (connection.responseCode != 200 || !connection.contentType.startsWith("application/json")) {
                            return@connect pastValue
                        }
                        val text = it.readString()
                        val json = JsonParser().parse(text).asJsonObject
                        if (json.get("status").asString.toLowerCase() != "ok") {
                            return@connect pastValue
                        }
                        val sunriseText = json.getAsJsonObject("results").get("sunrise").asString
                        val sunsetText = json.getAsJsonObject("results").get("sunset").asString
                        val sunrise = ZonedDateTime.parse(sunriseText, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                            .withZoneSameInstant(ZoneId.systemDefault()).toLocalTime().truncatedTo(ChronoUnit.MINUTES)
                        val sunset = ZonedDateTime.parse(sunsetText, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                            .withZoneSameInstant(ZoneId.systemDefault()).toLocalTime().truncatedTo(ChronoUnit.MINUTES)
                        val cycle = Cycle(sunrise, sunset)
                        cache[cacheKey] = cycle
                        return@connect cycle
                    }
        } catch (e: Exception) {
            LOG.error("Failed to retrieve daylight cycle data at $latitude, $longitude on $date!", e)
            return getCacheValue(cacheKey, true)
        }
    }

    private fun getCacheValue(cacheKey: CacheKey, allowPast: Boolean): Cycle? {
        if (!cache.containsKey(cacheKey)) {
            if (allowPast) {
                for (i in 1..7) {
                    val pastValue = getCacheValue(cacheKey.copy(date = cacheKey.date.minusDays(i.toLong())), false)
                    if (pastValue != null) {
                        return pastValue
                    }
                }
            }
            return null
        }
        return cache[cacheKey]
    }

    private data class CacheKey(val latitude: String, val longitude: String, val date: LocalDate)

    data class Cycle(val sunrise: LocalTime, val sunset: LocalTime)
}
