package me.vripper.gui.model

import javafx.beans.property.SimpleBooleanProperty
import tornadofx.getValue
import tornadofx.setValue

class LogsColumnsModel(
    time: Boolean,
    type: Boolean,
    status: Boolean,
    message: Boolean,
) {
    val timeProperty = SimpleBooleanProperty(time)
    var time: Boolean by timeProperty

    val typeProperty = SimpleBooleanProperty(type)
    var type: Boolean by typeProperty

    val statusProperty = SimpleBooleanProperty(status)
    var status: Boolean by statusProperty

    val messageProperty = SimpleBooleanProperty(message)
    var message: Boolean by messageProperty
}