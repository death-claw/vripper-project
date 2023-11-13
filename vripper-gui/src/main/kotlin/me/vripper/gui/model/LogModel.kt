package me.vripper.gui.model

import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.getValue
import tornadofx.setValue

class LogModel(
    id: Long,
    type: String,
    status: String,
    time: String,
    message: String
) {
    val idProperty = SimpleLongProperty(id)
    var id: Long by idProperty

    val typeProperty = SimpleStringProperty(type)
    var type: String by typeProperty

    val statusProperty = SimpleStringProperty(status)
    var status: String by statusProperty

    val timeProperty = SimpleStringProperty(time)
    var time: String by timeProperty

    val messageProperty = SimpleStringProperty(message)
    var message: String by messageProperty
}
