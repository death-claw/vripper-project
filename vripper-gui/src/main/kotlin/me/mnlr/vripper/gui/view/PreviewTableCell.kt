package me.mnlr.vripper.gui.view

import javafx.scene.control.TableCell
import javafx.scene.image.ImageView

class PreviewTableCell<T> : TableCell<T, String>() {
    override fun updateItem(item: String?, empty: Boolean) {
        super.updateItem(item, empty)
        if (!empty) {
            graphic = ImageView("image.png").apply { fitWidth = 20.0; fitHeight = 20.0 }
        }
    }
}