package me.vripper.gui.view.status

import javafx.geometry.Orientation
import javafx.geometry.Pos
import me.vripper.gui.controller.GlobalStateController
import tornadofx.*

class StatusBarView : View("Status bar") {

    private val globalStateController: GlobalStateController by inject()

    override val root = borderpane {
        left {
            hbox {
                text(globalStateController.globalState.loggedUserProperty.map { "Logged in as: $it" }) {
                    visibleWhen(globalStateController.globalState.loggedUserProperty.isNotBlank())
                }
            }
        }
        right {
            padding = insets(right = 5, left = 5, top = 3, bottom = 3)
            hbox {
                spacing = 3.0
                progressbar {
                    visibleWhen(globalStateController.globalState.loadingProperty)
                }
                separator(Orientation.VERTICAL)
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

    override fun onDock() {
        globalStateController.init()
    }
}