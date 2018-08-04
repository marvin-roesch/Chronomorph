/*
 * Chronomorph for IntelliJ
 *
 * Copyright (c) 2018 PaleoCrafter
 *
 * MIT License
 */

package io.paleocrafter.chronomorph.config

import com.intellij.ui.table.JBTable
import io.paleocrafter.chronomorph.ChronomorphSettings
import java.util.Arrays
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel

class ChronoEntryTable : JBTable() {
    val entries = mutableListOf<ChronomorphSettings.ChronoEntry>()
    private val model = Model()

    init {
        setModel(model)
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        emptyText.text = "No change points added yet. Chronomorph won't change your themes."
    }

    fun init(entries: List<ChronomorphSettings.ChronoEntry>) {
        this.entries.clear()
        this.entries.addAll(entries)
        updateEntries()
    }

    fun addEntry() {
        val editor = ChronoEntryEditor("Add Change Point", null)
        if (editor.showAndGet()) {
            entries.add(editor.entry)
            updateEntries()
        }
    }

    fun removeEntry() {
        val selectedRows = selectedRows
        if (selectedRows.isEmpty()) return
        Arrays.sort(selectedRows)
        val originalRow = selectedRows[0]
        for (i in selectedRows.indices.reversed()) {
            val selectedRow = selectedRows[i]
            if (isValidRow(selectedRow)) {
                entries.removeAt(selectedRow)
            }
        }
        model.fireTableDataChanged()
        if (originalRow < rowCount) {
            setRowSelectionInterval(originalRow, originalRow)
        } else if (rowCount > 0) {
            val index = rowCount - 1
            setRowSelectionInterval(index, index)
        }
    }

    fun editEntry() {
        if (selectedRowCount != 1) {
            return
        }
        val selectedEntry = entries[selectedRow]
        val editor = ChronoEntryEditor("Edit Change Point", selectedEntry)
        if (editor.showAndGet()) {
            entries[selectedRow] = editor.entry
            updateEntries()
        }
    }

    private fun updateEntries() {
        entries.sortBy { it.time.toSecondOfDay() }
        model.fireTableDataChanged()
    }

    private fun isValidRow(selectedRow: Int): Boolean {
        return selectedRow >= 0 && selectedRow < entries.size
    }

    inner class Model : AbstractTableModel() {
        override fun getRowCount() = entries.size

        override fun getColumnCount() = 3

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
            val entry = entries[rowIndex]
            return when (columnIndex) {
                0 -> "%02d:%02d".format(entry.time.hour, entry.time.minute)
                1 -> entry.themeInfo.userDescription
                2 -> entry.colorSchemeInfo.userDescription
                else -> null
            }
        }

        override fun getColumnName(column: Int): String {
            return when (column) {
                0 -> "Time"
                1 -> "Theme"
                2 -> "Color Scheme"
                else -> "Unknown"
            }
        }
    }
}
