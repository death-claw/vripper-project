package me.vripper.gui.model

import javafx.beans.property.SimpleDoubleProperty
import tornadofx.getValue
import tornadofx.setValue

class LogsColumnsWidthModel(
    time: Double,
    type: Double,
    status: Double,
    message: Double,
) {
    val timeProperty = SimpleDoubleProperty(time)
    var time: Double by timeProperty

    val typeProperty = SimpleDoubleProperty(type)
    var type: Double by typeProperty

    val statusProperty = SimpleDoubleProperty(status)
    var status: Double by statusProperty

    val messageProperty = SimpleDoubleProperty(message)
    var message: Double by messageProperty
}