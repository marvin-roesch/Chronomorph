/*
 * Chronomorph for IntelliJ
 *
 * Copyright (c) 2018-2020 PaleoCrafter
 *
 * MIT License
 */

package io.paleocrafter.chronomorph

import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import java.net.HttpURLConnection
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

object DaylightCycle {
    private val LOG = Logger.getInstance("Chronomorph Daylight Cycle")
    val DEFAULT = Cycle(LocalTime.of(6, 0), LocalTime.of(20, 0))

    private val cache = mutableMapOf<CacheKey, Cycle>()

    fun getCycle(): CompletableFuture<Cycle?> {
        val settings = ChronomorphSettings.instance
        return getCycle(settings.latitude, settings.longitude)
    }

    fun getCycle(latitude: String, longitude: String): CompletableFuture<Cycle?> {
        val date = LocalDate.now()
        val cacheKey = CacheKey(latitude, longitude, date)
        val settings = ChronomorphSettings.instance
        val cached = getCacheValue(cacheKey, false)?.let { CompletableFuture.completedFuture(it) }
        return if(settings.useDayCycle) {
            cached ?: wrapFuture(
                    ApplicationManager.getApplication().executeOnPooledThread<Cycle?> {
                        getCycleSync(cacheKey)
                    })
        }else
            cached ?: wrapFuture(ApplicationManager.getApplication().executeOnPooledThread<Cycle?>{
                DEFAULT
            })

    }

    private fun getCycleSync(cacheKey: CacheKey): Cycle? {
        try {
            return HttpRequests.request("https://api.sunrise-sunset.org/json?lat=${cacheKey.latitude}&lng=${cacheKey.longitude}&formatted=0")
                .accept("application/json")
                .throwStatusCodeException(false)
                .connect {
                    val pastValue = getCacheValue(cacheKey, true)
                    val connection = it.connection as? HttpURLConnection ?: return@connect pastValue
                    if (connection.responseCode != HttpURLConnection.HTTP_OK || !connection.contentType.startsWith("application/json")) {
                        return@connect pastValue
                    }
                    val text = it.readString()
                    val json = JsonParser.parseString(text).asJsonObject
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
            LOG.error("Failed to retrieve daylight cycle data at ${cacheKey.latitude}, ${cacheKey.longitude} on ${cacheKey.date}!", e)
            return getCacheValue(cacheKey, true)
        }
    }

    private fun wrapFuture(future: Future<Cycle?>): CompletableFuture<Cycle?> =
        CompletableFuture.supplyAsync {
            try {
                future.get()
            } catch (exception: Exception) {
                when (exception) {
                    is InterruptedException, is ExecutionException -> null
                    else -> throw exception
                }
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
