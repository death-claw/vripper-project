package me.vripper.gui.view

import javafx.geometry.Pos
import javafx.scene.control.TableCell
import javafx.scene.control.Tooltip
import javafx.scene.image.ImageView
import me.vripper.entities.domain.Status

class StatusTableCell<T> : TableCell<T, String>() {

    init {
        alignment = Pos.CENTER
    }

    override fun updateItem(item: String?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
            tooltip = null
            return
        }
        val status = Status.valueOf(item)
        tooltip = Tooltip(status.stringValue)
        graphic = when (status) {
            Status.PENDING -> ImageView("pending.png").apply { fitWidth = 25.0; fitHeight = 25.0 }
            Status.DOWNLOADING -> ImageView("below.png").apply { fitWidth = 25.0; fitHeight = 25.0 }
            Status.FINISHED -> ImageView("ok.png").apply { fitWidth = 25.0; fitHeight = 25.0 }
            Status.ERROR -> ImageView("error.png").apply { fitWidth = 25.0; fitHeight = 25.0 }
            Status.STOPPED -> ImageView("pause-round.png").apply {
                fitWidth = 25.0; fitHeight = 25.0
            }
        }
    }
}