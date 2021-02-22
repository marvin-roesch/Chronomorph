/*
 * Chronomorph for IntelliJ
 *
 * Copyright (c) 2018-2020 PaleoCrafter
 *
 * MIT License
 */

package io.paleocrafter.chronomorph

import org.shredzone.commons.suncalc.SunTimes
import java.time.LocalDate
import java.time.LocalTime

object DaylightCycle {
    val DEFAULT = Cycle(LocalTime.of(6, 0), LocalTime.of(20, 0))

    private val cache = mutableMapOf<CacheKey, Cycle>()

    fun getCycle(): Cycle? {
        val settings = ChronomorphSettings.instance
        return getCycle(settings.latitude, settings.longitude)
    }

    fun getCycle(latitude: String, longitude: String): Cycle? {
        val date = LocalDate.now()
        val cacheKey = CacheKey(latitude.toDouble(), longitude.toDouble(), date)
        val settings = ChronomorphSettings.instance
        val cached = getCacheValue(cacheKey, false)
        return if(settings.useDayCycle) {
            cached ?: calculateCycle(cacheKey)
        } else {
            cached ?: DEFAULT
        }
    }

    private fun calculateCycle(cacheKey: CacheKey): Cycle {
        val times = SunTimes.compute()
            .on(cacheKey.date)
            .at(cacheKey.latitude, cacheKey.longitude)
            .fullCycle()
            .execute()
        val cycle = Cycle(times.rise!!.toLocalTime(), times.set!!.toLocalTime())
        cache[cacheKey] = cycle

        return cycle
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

    private data class CacheKey(val latitude: Double, val longitude: Double, val date: LocalDate)

    data class Cycle(val sunrise: LocalTime, val sunset: LocalTime)
}
