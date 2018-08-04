/*
 * Chronomorph for IntelliJ
 *
 * Copyright (c) 2018 PaleoCrafter
 *
 * MIT License
 */

package io.paleocrafter.chronomorph.config

import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SchemeManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.ToolbarDecorator
import io.paleocrafter.chronomorph.Chronomorph
import io.paleocrafter.chronomorph.ChronomorphSettings
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.UIManager

class ChronomorphConfigurable : Configurable {

    private lateinit var panel: JPanel
    private lateinit var useDayCycleCheckBox: JCheckBox
    private lateinit var entryTable: ChronoEntryTable
    private lateinit var entryPanel: JPanel

    @Nls
    override fun getDisplayName() = "Chronomorph"

    override fun getHelpTopic(): String? = null

    override fun createComponent(): JComponent {
        entryTable = ChronoEntryTable()
        entryPanel.add(
            ToolbarDecorator.createDecorator(entryTable)
                .setAddAction { entryTable.addEntry() }
                .setRemoveAction { entryTable.removeEntry() }
                .setEditAction { entryTable.editEntry() }
                .disableUpDownActions().createPanel(),
            BorderLayout.CENTER)
        return panel
    }

    private fun init() {
        val settings = ChronomorphSettings.instance

        useDayCycleCheckBox.isSelected = settings.useDayCycle
        entryTable.init(settings.chronoEntries)
    }

    override fun isModified(): Boolean {
        val settings = ChronomorphSettings.instance

        return useDayCycleCheckBox.isSelected != settings.useDayCycle || settings.chronoEntries != entryTable.entries
    }

    override fun apply() {
        val settings = ChronomorphSettings.instance

        settings.useDayCycle = useDayCycleCheckBox.isSelected
        settings.chronoEntries.clear()
        settings.chronoEntries.addAll(entryTable.entries)
        Chronomorph.instance.check()
    }

    override fun reset() {
        init()
    }
}

val UIManager.LookAndFeelInfo?.userDescription: String
    get() = this?.name ?: "Leave unchanged"

val EditorColorsScheme?.userDescription: String
    get() = this?.let { StringUtil.trimStart(it.name, SchemeManager.EDITABLE_COPY_PREFIX) } ?: "Leave unchanged"
