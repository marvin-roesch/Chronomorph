/*
 * Chronomorph for IntelliJ
 *
 * Copyright (c) 2018-2019 PaleoCrafter
 *
 * MIT License
 */

package io.paleocrafter.chronomorph.config

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ListCellRendererWrapper
import io.paleocrafter.chronomorph.ChronomorphSettings
import java.time.LocalTime
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.UIManager

class ChronoEntryEditor(title: String, entry: ChronomorphSettings.ChronoEntry?) : DialogWrapper(true) {
    private lateinit var panel: JPanel
    private lateinit var hourSpinner: JSpinner
    private lateinit var minuteSpinner: JSpinner
    private lateinit var themeComboBox: JComboBox<UIManager.LookAndFeelInfo>
    private lateinit var colorSchemeComboBox: JComboBox<EditorColorsScheme>

    init {
        setTitle(title)
        hourSpinner.model = SpinnerNumberModel(0, 0, 23, 1)
        minuteSpinner.model = SpinnerNumberModel(0, 0, 59, 1)

        themeComboBox.renderer = ThemeComboBoxRenderer()
        themeComboBox.addItem(null)
        val themes = LafManager.getInstance().installedLookAndFeels
        for (theme in themes) {
            themeComboBox.addItem(theme)
        }

        colorSchemeComboBox.renderer = ColorSchemeComboBoxRenderer()
        colorSchemeComboBox.addItem(null)
        val colorSchemes = EditorColorsManager.getInstance().allSchemes
        for (scheme in colorSchemes) {
            colorSchemeComboBox.addItem(scheme)
        }

        if (entry != null) {
            hourSpinner.value = entry.time.hour
            minuteSpinner.value = entry.time.minute
            themeComboBox.selectedItem = entry.themeInfo
            colorSchemeComboBox.selectedItem = entry.colorSchemeInfo
        } else {
            themeComboBox.selectedItem = null
            colorSchemeComboBox.selectedItem = null
        }

        init()
    }

    val entry: ChronomorphSettings.ChronoEntry
        get() = ChronomorphSettings.ChronoEntry(
            LocalTime.of(hourSpinner.value as Int, minuteSpinner.value as Int),
            themeComboBox.value?.name,
            colorSchemeComboBox.value?.name
        )

    override fun getPreferredFocusedComponent() = hourSpinner

    override fun createActions() = arrayOf(okAction, cancelAction)

    override fun createNorthPanel() = panel

    override fun createCenterPanel(): JComponent? = null

    class ThemeComboBoxRenderer : ListCellRendererWrapper<UIManager.LookAndFeelInfo>() {
        override fun customize(list: JList<*>?, value: UIManager.LookAndFeelInfo?, index: Int, selected: Boolean, hasFocus: Boolean) {
            setText(value.userDescription)
        }
    }

    class ColorSchemeComboBoxRenderer : ListCellRendererWrapper<EditorColorsScheme>() {
        override fun customize(list: JList<*>?, value: EditorColorsScheme?, index: Int, selected: Boolean, hasFocus: Boolean) {
            setText(value.userDescription)
        }
    }
}
