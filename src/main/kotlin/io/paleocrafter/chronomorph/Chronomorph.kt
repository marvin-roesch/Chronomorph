/*
 * Chronomorph for IntelliJ
 *
 * Copyright (c) 2018 PaleoCrafter
 *
 * MIT License
 */

package io.paleocrafter.chronomorph

import com.intellij.concurrency.JobScheduler
import com.intellij.ide.actions.QuickChangeLookAndFeel
import com.intellij.ide.ui.LafManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class Chronomorph : ApplicationComponent {
    private lateinit var timer: ScheduledFuture<*>

    override fun initComponent() {
        super.initComponent()
        val now = LocalTime.now()
        check(now, firstRun = true)
        timer = JobScheduler.getScheduler().scheduleWithFixedDelay(
            ::check,
            now.until(now.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES), ChronoUnit.MILLIS),
            60 * 1000,
            TimeUnit.MILLISECONDS
        )
    }

    override fun disposeComponent() {
        super.disposeComponent()
        timer.cancel(true)
    }

    fun check() {
        check(LocalTime.now(), false)
    }

    private fun check(time: LocalTime, firstRun: Boolean) {
        val settings = ChronomorphSettings.instance
        if (!settings.useDayCycle) {
            getClosestEntry(time)?.also { applyEntry(it, firstRun) }
            return
        }
        DaylightCycle.getCycle().whenComplete { c, _ ->
            val cycle = c ?: DaylightCycle.DEFAULT
            val entry = if (time in cycle.sunrise..cycle.sunset) settings.daySettings else settings.nightSettings
            applyEntry(entry, firstRun)
        }
    }

    fun getClosestEntry(time: LocalTime): ChronomorphSettings.ChronoEntry? {
        val entries = completeEntries(ChronomorphSettings.instance.chronoEntries)
        if (entries.isEmpty()) {
            return null
        }
        return entries.windowed(2, partialWindows = true).findLast {
            if (it.size == 1) {
                val entry = it.first()
                return@findLast entry.time <= time
            }
            val a = it.first()
            val b = it.last()
            return@findLast time in a.time..b.time
        }?.first()
    }

    private fun applyEntry(entry: ChronomorphSettings.ChronoEntry, firstRun: Boolean = false) {
        val theme = entry.themeInfo
        val colorScheme = entry.colorSchemeInfo

        var update = false
        val schemeManager = EditorColorsManager.getInstance()
        val currentScheme = schemeManager.globalScheme
        if (colorScheme != null && currentScheme.name != colorScheme.name) {
            schemeManager.globalScheme = colorScheme
            update = true
        }

        val lafManager = LafManager.getInstance()
        if (theme != null && lafManager.currentLookAndFeel != theme) {
            runOrSchedule(firstRun) {
                QuickChangeLookAndFeel.switchLafAndUpdateUI(lafManager, theme, true)
            }
            if (!update && !firstRun) {
                EditorFactory.getInstance().refreshAllEditors()
            }
        }
    }

    private fun runOrSchedule(schedule: Boolean, runnable: () -> Unit) {
        if (schedule) {
            ApplicationManager.getApplication().invokeLater(runnable)
        } else {
            runnable.invoke()
        }
    }

    fun completeEntries(entries: List<ChronomorphSettings.ChronoEntry>): List<ChronomorphSettings.ChronoEntry> {
        if (entries.isEmpty()) {
            return emptyList()
        }
        val sorted = entries.sortedBy { it.time.toSecondOfDay() }.toMutableList()
        val first = sorted.first()
        val last = sorted.last()
        if (first.time.hour > 0 || first.time.minute > 0) {
            sorted.add(0, last.copy(time = LocalTime.MIDNIGHT))
        }
        return sorted
    }

    companion object {
        val instance: Chronomorph
            get() = ApplicationManager.getApplication().getComponent(Chronomorph::class.java)
    }
}
