package me.vripper.gui.view.actionbar

import javafx.scene.control.ButtonType
import javafx.scene.control.ContentDisplay
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import me.vripper.gui.Styles
import me.vripper.gui.controller.ThreadController
import me.vripper.gui.view.tables.ThreadTableView
import tornadofx.*

class ThreadActionsView : View() {

    private val threadTableView: ThreadTableView by inject()
    private val threadController: ThreadController by inject()

    override val root = hbox {
        button("Delete") {
            imageview("trash.png") {
                fitWidth = 32.0
                fitHeight = 32.0
            }
            addClass(Styles.actionBarButton)
            contentDisplay = ContentDisplay.GRAPHIC_ONLY
            tooltip("Delete selected threads [Del]")
            shortcut(KeyCodeCombination(KeyCode.DELETE))
            enableWhen(
                threadTableView.tableView.selectionModel.selectedItems.sizeProperty.greaterThan(
                    0
                )
            )
            action {
                threadTableView.deleteSelected()
            }
        }
        button("Clear") {
            imageview("broom.png") {
                fitWidth = 32.0
                fitHeight = 32.0
            }
            addClass(Styles.actionBarButton)
            contentDisplay = ContentDisplay.GRAPHIC_ONLY
            tooltip("Clear all threads [Ctrl+Del]")
            shortcut(KeyCodeCombination(KeyCode.DELETE, KeyCombination.CONTROL_DOWN))
            action {
                confirm("Clean threads", "Confirm removal of threads", ButtonType.YES, ButtonType.NO) {
                    threadController.clearAll()
                }
            }
        }
    }
}