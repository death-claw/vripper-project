package me.mnlr.vripper.gui.model

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class GlobalStateModel(
    running: Int,
    remaining: Int,
    error: Int,
    loggedUser: String,
    downloadSpeed: String
) {
    val runningProperty = SimpleIntegerProperty(running)
    var running: Int by runningProperty

    val remainingProperty = SimpleIntegerProperty(remaining)
    var remaining: Int by remainingProperty

    val errorProperty = SimpleIntegerProperty(error)
    var error: Int by errorProperty

    val loggedUserProperty = SimpleStringProperty(loggedUser)
    var loggedUser: String by loggedUserProperty

    val downloadSpeedProperty = SimpleStringProperty(downloadSpeed)
    var downloadSpeed: String by downloadSpeedProperty
}
