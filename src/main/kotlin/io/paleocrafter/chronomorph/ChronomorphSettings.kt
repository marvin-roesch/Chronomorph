/*
 * Chronomorph for IntelliJ
 *
 * Copyright (c) 2018 PaleoCrafter
 *
 * MIT License
 */

package io.paleocrafter.chronomorph

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.util.addOptionTag
import com.intellij.util.get
import org.jdom.Element
import java.time.LocalTime
import javax.swing.UIManager

@State(name = "ChronomorphSettings", storages = [(Storage("chronomorph.xml"))])
class ChronomorphSettings : PersistentStateComponent<Element> {
    var useDayCycle = false
    var daySettings: ChronoEntry = ChronoEntry(LocalTime.NOON, null, null)
    var nightSettings: ChronoEntry = ChronoEntry(LocalTime.MIDNIGHT, null, null)
    var latitude: String = "0"
    var longitude: String = "0"
    val chronoEntries = mutableListOf<ChronoEntry>()

    override fun getState(): Element {
        val element = Element("ChronomorphSettingsState")
        element.addOptionTag("useDayCycle", useDayCycle.toString())

        val location = Element("location")
        location.setAttribute("latitude", latitude)
        location.setAttribute("longitude", longitude)
        element.addContent(location)

        val dayEntry = writeEntry(daySettings).setName("daySettings")
        dayEntry.removeAttribute("time")
        element.addContent(dayEntry)

        val nightEntry = writeEntry(nightSettings).setName("nightSettings")
        nightEntry.removeAttribute("time")
        element.addContent(nightEntry)

        val entries = Element("option")
        entries.setAttribute("name", "chronoEntries")
        chronoEntries.map(::writeEntry).forEach { entries.addContent(it) }
        element.addContent(entries)

        return element
    }

    override fun loadState(state: Element) {
        this.useDayCycle = state.getOption("useDayCycle")?.getAttribute("value")?.booleanValue ?: false
        state.get("location")?.also {
            this.latitude = it.getAttributeValue("latitude")
            this.longitude = it.getAttributeValue("longitude")
        }
        state.get("daySettings")?.also {
            this.daySettings = readEntry(it, LocalTime.NOON)
        }
        state.get("nightSettings")?.also {
            this.nightSettings = readEntry(it, LocalTime.MIDNIGHT)
        }
        chronoEntries.clear()
        state.getOption("chronoEntries")?.children?.also { entries ->
            chronoEntries.addAll(entries.filter { it.name == "entry" }.map { readEntry(it) })
        }
    }

    private fun Element.getOption(name: String) =
        this.children.find { it.name == "option" && it.getAttribute("name").value == name }

    private fun readEntry(element: Element, defaultTime: LocalTime? = null): ChronoEntry {
        val time = element.getAttributeValue("time")?.split(':', limit = 2)
        val hour = time?.getOrNull(0)?.toInt() ?: defaultTime?.hour ?: 0
        val minute = time?.getOrNull(1)?.toInt() ?: defaultTime?.minute ?: 0
        return ChronoEntry(
            LocalTime.of(hour, minute),
            element.getAttributeValue("theme"),
            element.getAttributeValue("colorScheme")
        )
    }

    private fun writeEntry(entry: ChronoEntry): Element {
        val element = Element("entry")
        element.setAttribute("time", "%02d:%02d".format(entry.time.hour, entry.time.minute))
        if (entry.theme != null)
            element.setAttribute("theme", entry.theme)
        if (entry.colorScheme != null)
            element.setAttribute("colorScheme", entry.colorScheme)
        return element
    }

    data class ChronoEntry(val time: LocalTime, val theme: String?, val colorScheme: String?) {
        val themeInfo: UIManager.LookAndFeelInfo?
            get() = LafManager.getInstance().installedLookAndFeels.find { it.className == theme }
        val colorSchemeInfo: EditorColorsScheme?
            get() = EditorColorsManager.getInstance().allSchemes.find { it.name == colorScheme }
    }

    companion object {
        val instance: ChronomorphSettings
            get() = ServiceManager.getService(ChronomorphSettings::class.java)
    }
}
