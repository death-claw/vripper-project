package me.vripper.gui.components.cells

import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.TableCell
import org.kordamp.ikonli.feather.Feather
import org.kordamp.ikonli.javafx.FontIcon

class PreviewTableCell<T> : TableCell<T, ObservableList<String>>() {
    init {
        alignment = Pos.CENTER
    }

    override fun updateItem(item: ObservableList<String>?, empty: Boolean) {
        super.updateItem(item, empty)
        graphic = if (!empty) {
            FontIcon.of(Feather.IMAGE)
        } else {
            null
        }
    }
}