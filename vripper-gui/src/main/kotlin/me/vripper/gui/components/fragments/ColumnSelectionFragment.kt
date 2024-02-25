package me.vripper.gui.components.fragments

import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import tornadofx.Fragment
import tornadofx.listview
import tornadofx.useCheckbox

class ColumnSelectionFragment : Fragment("Column Selection") {

    val map: MutableMap<String, SimpleBooleanProperty> by param()

    override val root = listview<String> {
        items = FXCollections.observableArrayList(map.keys)
        useCheckbox { listItem ->
            map[listItem]!!
        }
    }
}