package me.mnlr.vripper.gui.view.actionbar

import javafx.scene.control.ButtonType
import javafx.scene.control.ContentDisplay
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import me.mnlr.vripper.gui.Styles
import me.mnlr.vripper.gui.controller.LogController
import me.mnlr.vripper.gui.view.tables.LogTableView
import tornadofx.*

class LogActionsView : View() {

    private val logController: LogController by inject()
    private val logTableView: LogTableView by inject()

    override val root = hbox {
        button("Clear") {
            imageview("broom.png") {
                fitWidth = 32.0
                fitHeight = 32.0
            }
            addClass(Styles.actionBarButton)
            contentDisplay = ContentDisplay.GRAPHIC_ONLY
            tooltip("Clear logs [Ctrl+Del]")
            shortcut(KeyCodeCombination(KeyCode.DELETE, KeyCombination.CONTROL_DOWN))
            action {
                confirm("Clear logs", "Are you sure you want to clear the logs", ButtonType.YES, ButtonType.NO) {
                    logController.clear()
                    logTableView.items.clear()
                }
            }
        }
    }
}