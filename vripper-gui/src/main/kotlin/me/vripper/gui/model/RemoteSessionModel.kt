package me.vripper.gui.model

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.getValue
import tornadofx.setValue

class RemoteSessionModel(
    host: String,
    port: Int,
) {
    val hostProperty = SimpleStringProperty(host)
    var host: String by hostProperty

    val portProperty = SimpleIntegerProperty(port)
    var port: Int by portProperty
}