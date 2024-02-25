package me.vripper.gui.components.cells

import javafx.beans.value.ObservableValue
import javafx.event.EventHandler
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.input.MouseEvent

class ProgressTableCell<S> : TableCell<S, Double>() {

    /* *************************************************************************
     *                                                                         *
     * Fields                                                                  *
     *                                                                         *
     **************************************************************************/
    private var progressBar: ProgressBar

    private var observable: ObservableValue<Double>? = null

    init {
        styleClass.add("progress-bar-table-cell")
        progressBar = ProgressBar()
        progressBar.maxWidth = Double.MAX_VALUE
    }

    fun setOnMouseClick(clickEvent: EventHandler<in MouseEvent>) {
        progressBar.onMouseClicked = clickEvent
    }

    override fun updateItem(item: Double?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty) {
            setGraphic(null)
        } else {
            progressBar.progressProperty().unbind()
            val column: TableColumn<S, Double>? = tableColumn
            observable = column?.getCellObservableValue(index)
            if (observable != null) {
                progressBar.progressProperty().bind(observable)
            } else if (item != null) {
                progressBar.progress = item
            }
            setGraphic(progressBar)
        }
    }
}