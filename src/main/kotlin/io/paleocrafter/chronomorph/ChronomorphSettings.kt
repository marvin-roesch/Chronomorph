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
import org.jdom.Element
import java.time.LocalTime
import javax.swing.UIManager

@State(name = "ChronomorphSettings", storages = [(Storage("chronomorph.xml"))])
class ChronomorphSettings : PersistentStateComponent<Element> {
    var useDayCycle = false
    val chronoEntries = mutableListOf<ChronoEntry>()

    override fun getState(): Element {
        val element = Element("ChronomorphSettingsState")
        element.addOptionTag("useDayCycle", useDayCycle.toString())
        val entries = Element("option")
        entries.setAttribute("name", "chronoEntries")
        chronoEntries.map(::writeEntry).forEach { entries.addContent(it) }
        element.addContent(entries)
        return element
    }

    override fun loadState(state: Element) {
        this.useDayCycle = state.getOption("useDayCycle")?.getAttribute("value")?.booleanValue ?: false
        chronoEntries.clear()
        state.getOption("chronoEntries")?.children?.also { entries ->
            chronoEntries.addAll(entries.filter { it.name == "entry" }.map(::readEntry))
        }
    }

    private fun Element.getOption(name: String) =
        this.children.find { it.name == "option" && it.getAttribute("name").value == name }

    private fun readEntry(element: Element): ChronoEntry {
        val time = element.getAttributeValue("time").split(':', limit = 2)
        val hour = time.getOrNull(0)?.toInt() ?: 0
        val minute = time.getOrNull(1)?.toInt() ?: 0
        return ChronoEntry(
            LocalTime.of(hour, minute),
            element.getAttributeValue("theme"),
            element.getAttributeValue("colorScheme"))
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
