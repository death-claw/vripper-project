package me.vripper.gui.view

import javafx.geometry.Pos
import javafx.scene.control.TableCell
import javafx.scene.image.ImageView

class PreviewTableCell<T> : TableCell<T, String>() {
    init {
        alignment = Pos.CENTER
    }
    override fun updateItem(item: String?, empty: Boolean) {
        super.updateItem(item, empty)
        if (!empty) {
            graphic = ImageView("image.png").apply { fitWidth = 25.0; fitHeight = 25.0 }
        } else {
            graphic = null
        }
    }
}