package me.mnlr.vripper.model.settings

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class ClipboardSettingsModel {
    val enableProperty = SimpleBooleanProperty()
    var enable: Boolean by enableProperty

    val pollingRateProperty = SimpleStringProperty()
    var pollingRate: String by pollingRateProperty
}
