package me.mnlr.vripper.gui.view.status

import javafx.geometry.Orientation
import javafx.geometry.Pos
import me.mnlr.vripper.gui.controller.GlobalStateController
import tornadofx.*

class StatusBarView : View("Status bar") {

    private val globalStateController: GlobalStateController by inject()

    override val root = borderpane {
        left {
            text(globalStateController.globalState.loggedUserProperty.map { "Logged in as: $it" }) {
                visibleWhen(globalStateController.globalState.loggedUserProperty.isNotBlank())
            }
        }
        right {
            padding = insets(right = 5, left = 5, top = 3, bottom = 3)
            hbox {
                spacing = 3.0
                text(globalStateController.globalState.downloadSpeedProperty.map { "$it/s" })
                separator(Orientation.VERTICAL)
                label("Downloading")
                text(globalStateController.globalState.runningProperty.asString())
                separator(Orientation.VERTICAL)
                label("Pending")
                text(globalStateController.globalState.remainingProperty.asString())
                separator(Orientation.VERTICAL)
                label("Error")
                text(globalStateController.globalState.errorProperty.asString())
                alignment = Pos.CENTER_RIGHT
            }
        }
    }
}